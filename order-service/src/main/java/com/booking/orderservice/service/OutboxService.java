package com.booking.orderservice.service;

import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.outbox.entity.OutboxMessage;

import java.util.List;

public interface OutboxService {
    OutboxMessage save(OutboxMessage outboxMessage);
    List<OutboxMessage> findByStatus(OutboxStatus status);
    void markAsPublish(String outboxId);
}
