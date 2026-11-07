package com.booking.orderservice.repository;

import com.booking.orderservice.entities.Order;
import com.booking.orderservice.enums.OrderStatus;

import java.time.Instant;
import java.util.List;

public interface OrderRepository {
    void insertOrder(String tableName, Order order);

    List<Object> findAll();

    Order findByOrderNumber(String yearMonth, String orderNumber);

    boolean changeStatus(String yearMonth, String orderNumber, OrderStatus status);

    Order findByDate(String yearMonth, Instant date);

    List<Order> findPage(String userId, String yearMonth, int limit, String search);

    List<Order> findCursorPage(String userId, String orderId, String yearMonth, Instant createdDate, int limit, String search);
}
