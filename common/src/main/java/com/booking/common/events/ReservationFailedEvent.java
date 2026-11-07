package com.booking.common.events;

import java.util.UUID;

/**
 * Published by event-service when the database (source of truth) cannot reserve stock
 * for an order that passed the Redis gate, e.g. after the Redis counter was re-warmed.
 */
public record ReservationFailedEvent(UUID orderId, UUID ticketId, String reason) {
}
