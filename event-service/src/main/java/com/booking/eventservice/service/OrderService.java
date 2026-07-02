package com.booking.eventservice.service;

public interface OrderService {
    boolean order(String ticketId, int quantity);

    boolean cancelOrder(String orderNumber);
}
