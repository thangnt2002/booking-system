package com.booking.eventservice.service.impl;

import com.booking.eventservice.cache.TicketCacheService;
import com.booking.eventservice.distributed.RedisInfraService;
import com.booking.eventservice.dto.cache.TicketCache;
import com.booking.eventservice.dto.request.TicketRequestDTO;
import com.booking.eventservice.dto.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;
import com.booking.eventservice.exception.BusinessException;
import com.booking.eventservice.exception.ErrorCode;
import com.booking.eventservice.exception.NotFoundException;
import com.booking.eventservice.mapper.TicketMapper;
import com.booking.eventservice.repository.EventRepository;
import com.booking.eventservice.repository.TicketRepository;
import com.booking.eventservice.service.TicketService;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.booking.eventservice.common.Utils.genDistributedTicketStockAvailableKey;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TicketServiceImpl implements TicketService {

    TicketRepository ticketRepository;
    EventRepository evenEventRepository;
    TicketMapper ticketMapper;
    TicketCacheService ticketCacheService;
    RedisInfraService redisInfraService;

    @Override
    @Transactional
    public Ticket create(TicketRequestDTO request) {
        if (request.getSaleStartTime() != null && request.getSaleEndTime() != null) {
            if (request.getSaleStartTime().isAfter(request.getSaleEndTime())) {
                throw new BusinessException(ErrorCode.INVALID_START_TIME);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.getSaleStartTime() != null && request.getSaleStartTime().isBefore(now)) {
            throw new BusinessException(ErrorCode.START_TIME_CANNOT_BE_IN_THE_PAST);
        }

        if (!evenEventRepository.existsById(request.getEventId())) {
            log.error("Event {} not found", request.getEventId());
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket findById(String ticketId, Long version) {
        TicketCache ticketCache = getTicketFromCache(ticketId, version);
        return ticketMapper.fromCacheToEntity(ticketCache);
    }

    @Override
    public Ticket update(TicketRequestDTO request) {
        return null;
    }

    @Override
    public Ticket softDelete(String ticketId) {
        return null;
    }

    @Override
    public TicketResponseDTO findTicketById(String ticketId, Long version) {
        TicketCache ticketCache = getTicketFromCache(ticketId, version);
        TicketResponseDTO dto = ticketMapper.toResponse(ticketCache.getTicket());
        dto.setVersion(ticketCache.getVersion());
        return dto;
    }

    @Override
    @Transactional
    public boolean reserveStock(String ticketId, int quantity) {
        boolean isRedisDecremented = false;
        try {
            int deductionResult = ticketCacheService.decreaseStock(ticketId, quantity);
            if (deductionResult == -1) {
                log.info("decrease stock: cache miss for ticketId={}", ticketId);
                warmCache(ticketId);
                // decrease after add stock to cache
                deductionResult = ticketCacheService.decreaseStock(ticketId, quantity);
            }
            if (deductionResult == 0) {
                log.info("Redis stock insufficient for ticketId={}", ticketId);
                return false;
            }
            isRedisDecremented = true;
            log.info("DecreaseStock success for ticketId={}, quantity = {}", ticketId, quantity);
            return true;
        } catch (PessimisticLockException e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
            log.error("PessimisticLockException ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        } catch (LockTimeoutException e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
            log.error("LockTimeoutException ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        } catch (Exception e) {
            if (isRedisDecremented) {
                ticketCacheService.increaseStock(ticketId, quantity);
            }
            log.error("Exception ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        }
    }

    private void warmCache(String ticketId) {
        if (ticketId == null) {
            return;
        }
        TicketCache cached = ticketCacheService.getTicket(ticketId, null);
        if (cached == null || cached.isNullObject()){
            return;
        }
        log.info("Warming cache for ticket = {}", ticketId);
        Ticket ticket = cached.getTicket();
        String stockAvailableCacheKey = genDistributedTicketStockAvailableKey(ticketId);
        redisInfraService.setObject(stockAvailableCacheKey, ticket.getStockAvailable());
    }

    @Override
    @Transactional
    public boolean decreaseStock(String ticketId, int quantity) {
        int isDbDecremented = ticketRepository.decreaseStock(ticketId, quantity);

        try {
            if (isDbDecremented > 0) {
                return true;
            }
            log.info("DecreaseStock failed for ticketId={}, quantity = {}", ticketId, quantity);
            return false;
        } catch (Exception e) {
            log.error("decreaseStock err. Id = {}, quantity = {}, err= {}.", ticketId, quantity, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean releaseStock(String ticketId, int quantity) {
        return ticketCacheService.increaseStock(ticketId, quantity);
    }

    private TicketCache getTicketFromCache(String id, Long version) {
        if (id == null) {
            return null;
        }
        return ticketCacheService.getTicket(id, version);
    }

}
