package com.booking.orderservice.enums;

/**
 * PENDING -> SUCCESS (paid) | CANCELLED (user) | EXPIRED (payment timeout) | FAILED (no stock in DB).
 * Every transition is a conditional UPDATE from PENDING, so concurrent actors cannot both win.
 */
public enum OrderStatus {
    PENDING,
    SUCCESS,
    CANCELLED,
    EXPIRED,
    FAILED,
    REFUNDED
}
