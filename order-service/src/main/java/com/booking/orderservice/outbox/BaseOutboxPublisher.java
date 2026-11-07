package com.booking.orderservice.outbox;

import com.booking.orderservice.entities.OutboxMessage;
import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.repository.OutboxRepository;
import com.booking.orderservice.service.OutboxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Instant;
import java.util.UUID;

import static com.booking.common.utils.UserUtils.getCurrentUserId;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class BaseOutboxPublisher<T> {
    OutboxService outboxService;
    ObjectMapper objectMapper;

    public abstract String getAggregateType();
    public abstract UUID getAggregateId(T aggregate);

    @Transactional
    public void publishEvent(T aggregate, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(aggregate);

            OutboxMessage outbox = OutboxMessage.builder()
                    .aggregateType(getAggregateType())
                    .aggregateId(getAggregateId(aggregate))
                    .payload(payload)
                    .topic(topic)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdBy(getCurrentUserId())
                    .build();

            outboxService.save(outbox);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing outbox payload", e);
        }
    }
}
