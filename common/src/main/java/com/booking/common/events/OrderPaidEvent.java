package com.booking.common.events;

import java.util.UUID;

/** Published when payment for a PENDING order is confirmed. */
public record OrderPaidEvent(UUID orderId, UUID ticketId, int quantity) {
}
