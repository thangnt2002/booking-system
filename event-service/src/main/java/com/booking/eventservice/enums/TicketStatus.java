package com.booking.eventservice.enums;

public enum TicketStatus {
    DRAFT, // not open yet
    ACTIVE, // selling time
    SOLD_OUT,
    EXPIRED,
    CANCELLED
}
