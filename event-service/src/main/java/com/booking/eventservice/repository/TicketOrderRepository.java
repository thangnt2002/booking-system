package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Order;

import java.util.List;

public interface TicketOrderRepository {
    void insertOrder(String tableName, Order order);

    List<Object> findAll();
}
