package com.booking.eventservice.enums;

public enum OrderStatus {
    PENDING, // payment pending
    SUCCESS,
    CANCELLED,
    EXPIRED, // payment time expired -> auto cancel
    REFUNDED
}
