package com.booking.common.services;

import java.util.UUID;

public interface StockViewService {
    Long getRemaining(UUID ticketId);
}
