package com.booking.orderservice.outbox;

import com.booking.orderservice.entities.Order;
import com.booking.orderservice.service.OutboxService;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderOutboxPublisher extends BaseOutboxPublisher<Order> {

    @Autowired
    public OrderOutboxPublisher(OutboxService outboxService,
                                ObjectMapper objectMapper) {
        super(outboxService, objectMapper);
    }

    @Override
    public String getAggregateType() {
        return "ORDER";
    }

    @Override
    public UUID getAggregateId(Order order) {
        return order.getId();
    }
}
