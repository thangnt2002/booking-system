package com.booking.common.events;

import java.util.UUID;

/** Published when a PENDING order is cancelled by the user or expires without payment. */
public record OrderCancelledEvent(UUID orderId, UUID customerId, UUID ticketId, int quantity, CancelReason reason) {
}
