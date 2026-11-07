package com.booking.orderservice.service;

import com.booking.orderservice.dtos.Page;
import com.booking.orderservice.dtos.response.OrderResponseDTO;

import java.util.UUID;

public interface OrderService {
    boolean order(UUID ticketId, int quantity, UUID idempotencyKey);

    boolean cancelOrder(String orderNumber);

    Page<OrderResponseDTO> getPage(String userId, String table, String cursor, int limit, String search);
}
