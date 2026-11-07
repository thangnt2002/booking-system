package com.booking.common.events;

import java.time.Instant;
import java.util.UUID;

/** Published by order-service once an order is persisted and its stock is held in Redis. */
public record OrderPlacedEvent(UUID orderId, UUID customerId, UUID ticketId, int quantity, Instant expiresAt) {
}
