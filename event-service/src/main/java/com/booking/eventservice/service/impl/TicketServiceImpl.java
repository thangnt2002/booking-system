package com.booking.eventservice.service.impl;

import com.booking.eventservice.cache.TicketCacheService;
import com.booking.common.distributed.RedisInfraService;
import com.booking.eventservice.dtos.cache.TicketCache;
import com.booking.eventservice.dtos.request.TicketRequestDTO;
import com.booking.eventservice.dtos.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.eventservice.mapper.TicketMapper;
import com.booking.eventservice.repository.EventRepository;
import com.booking.eventservice.repository.TicketRepository;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.booking.common.utils.CacheKeyUtils.ticketStockKey;
import static com.booking.common.utils.UserUtils.getCurrentUserId;


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
        Instant now = Instant.now();
        if (request.getSaleStartTime() != null && request.getSaleStartTime().isBefore(now)) {
            throw new BusinessException(ErrorCode.START_TIME_CANNOT_BE_IN_THE_PAST);
        }

        if (!evenEventRepository.existsById(request.getEventId())) {
            log.error("Event {} not found", request.getEventId());
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }

        Ticket ticket = ticketMapper.toEntity(request);
        UUID userId = getCurrentUserId();
        ticket.setCreatedBy(userId);
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket findById(UUID ticketId, Long version) {
        TicketCache ticketCache = getTicketFromCache(ticketId, version);
        return ticketMapper.fromCacheToEntity(ticketCache);
    }

    @Override
    public Ticket update(TicketRequestDTO request) {
        return null;
    }

    @Override
    public Ticket softDelete(UUID ticketId) {
        return null;
    }

    @Override
    public TicketResponseDTO findTicketById(UUID ticketId, Long version) {
        TicketCache ticketCache = getTicketFromCache(ticketId, version);
        if (ticketCache == null || ticketCache.getTicket() == null) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }
        TicketResponseDTO dto = ticketMapper.toResponse(ticketCache.getTicket());
        dto.setVersion(ticketCache.getVersion());
        return dto;
    }

    @Override
    public boolean warmStock(UUID ticketId) {
        if (ticketId == null) {
            return false;
        }
        // read the live DB value: the cached Ticket object can be minutes old
        return ticketRepository.findStockAvailable(ticketId)
                .map(available -> {
                    boolean written = redisInfraService.setIfAbsent(ticketStockKey(ticketId), String.valueOf(available));
                    log.info("Warm stock ticket={} available={} written={}", ticketId, available, written);
                    return true;
                })
                .orElse(false);
    }

    private TicketCache getTicketFromCache(UUID id, Long version) {
        if (id == null) {
            return null;
        }
        return ticketCacheService.getTicket(id, version);
    }

}
