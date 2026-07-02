package com.booking.orderservice.repository;



import com.booking.orderservice.entity.Order;
import com.booking.orderservice.enums.OrderStatus;

import java.util.List;

public interface OrderRepository {
    void insertOrder(String tableName, Order order);

    List<Object> findAll();

    Order findByOrderNumber(String yearMonth, String orderNumber);

    boolean changeStatus(String yearMonth, String orderNumber, OrderStatus status);
}
