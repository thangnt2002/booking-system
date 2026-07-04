package com.booking.orderservice.enums;

public enum TicketStatus {
    DRAFT, // not open yet
    ACTIVE, // selling time
    SOLD_OUT,
    EXPIRED,
    CANCELLED
}
