package com.booking.common.events;

/**
 * Kafka header names. {@link #MESSAGE_ID} is the outbox row id and is the
 * de-duplication key used by idempotent consumers.
 */
public final class MessageHeaders {
    public static final String MESSAGE_ID = "x-message-id";
    public static final String EVENT_TYPE = "x-event-type";

    private MessageHeaders() {
    }
}
