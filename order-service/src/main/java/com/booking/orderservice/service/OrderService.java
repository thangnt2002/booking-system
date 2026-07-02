package com.booking.orderservice.service;

public interface OrderService {
    boolean order(String ticketId, int quantity);

    boolean cancelOrder(String orderNumber);
}
