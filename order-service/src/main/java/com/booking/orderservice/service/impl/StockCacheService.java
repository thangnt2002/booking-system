package com.booking.orderservice.service.impl;

import java.util.UUID;

public interface StockCacheService {
    Long getRemaining(UUID ticketId);

    int reserveByLUA(UUID ticketId, Integer quantity);

    boolean warmCache(UUID ticketId);
}
