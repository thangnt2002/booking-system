package com.booking.orderservice.clients;

import com.booking.common.dtos.ApiResponse;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.orderservice.config.OrderProperties;
import com.booking.orderservice.dtos.response.TicketResponseDTO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Ticket lookups with a short local cache: during a flash sale thousands of requests per
 * second hit the same few tickets, so a ~2s TTL collapses them into one call per instance
 * (Guava's get(key, loader) also coalesces concurrent misses) while price changes still
 * propagate quickly.
 */
@Slf4j
@Component
public class TicketCatalog {

    private final EventClient eventClient;
    private final Cache<UUID, TicketResponseDTO> cache;

    public TicketCatalog(EventClient eventClient, OrderProperties properties) {
        this.eventClient = eventClient;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(properties.ticketCacheTtl())
                .maximumSize(10_000)
                .build();
    }

    public TicketResponseDTO getTicket(UUID ticketId) {
        try {
            return cache.get(ticketId, () -> load(ticketId));
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public void warmStock(UUID ticketId) {
        try {
            ApiResponse<Boolean> response = eventClient.warmStock(ticketId);
            if (response == null || !Boolean.TRUE.equals(response.getData())) {
                throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        } catch (FeignException e) {
            log.error("Warm stock failed for ticket {}", ticketId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private TicketResponseDTO load(UUID ticketId) {
        try {
            ApiResponse<TicketResponseDTO> response = eventClient.findTicketById(ticketId);
            if (response == null || response.getData() == null) {
                throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        } catch (FeignException e) {
            log.error("Load ticket {} from event-service failed", ticketId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
