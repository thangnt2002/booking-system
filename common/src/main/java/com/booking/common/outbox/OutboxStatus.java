package com.booking.common.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    /** Gave up after max retries: needs manual inspection / replay. */
    FAILED
}
