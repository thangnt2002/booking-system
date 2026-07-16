package com.booking.orderservice.service;

import com.booking.orderservice.dto.Page;
import com.booking.orderservice.dto.response.OrderResponseDTO;

public interface OrderService {
    boolean order(String ticketId, int quantity);

    boolean cancelOrder(String orderNumber);

    Page<OrderResponseDTO> getPage(String userId, String table, String cursor, int limit, String search);
}
