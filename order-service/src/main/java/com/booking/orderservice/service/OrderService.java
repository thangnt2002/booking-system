package com.booking.orderservice.service;

import com.booking.orderservice.dtos.Page;
import com.booking.orderservice.dtos.request.PlaceOrderRequest;
import com.booking.orderservice.dtos.response.OrderResponseDTO;

import java.util.UUID;

public interface OrderService {

    OrderResponseDTO placeOrder(PlaceOrderRequest request, UUID idempotencyKey);

    OrderResponseDTO getMyOrder(UUID orderId);

    void cancelMyOrder(UUID orderId);

    void confirmPayment(UUID orderId);

    Page<OrderResponseDTO> getMyOrders(String cursor, int limit, String search);
}
