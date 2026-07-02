package com.booking.orderservice.enums;

public enum OrderStatus {
    PENDING, // payment pending
    SUCCESS,
    CANCELLED,
    EXPIRED, // payment time expired -> auto cancel
    REFUNDED
}
