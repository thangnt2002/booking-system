package com.booking.common.events;

/**
 * Kafka topic names shared by producers and consumers.
 * Versioned so a breaking contract change can be rolled out on a new topic.
 */
public final class Topics {
    public static final String ORDER_PLACED = "order.placed.v1";
    public static final String ORDER_CANCELLED = "order.cancelled.v1";
    public static final String ORDER_PAID = "order.paid.v1";
    public static final String RESERVATION_FAILED = "reservation.failed.v1";

    public static final String DLT_SUFFIX = ".DLT";

    private Topics() {
    }
}
