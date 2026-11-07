package com.booking.common.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Appends an event to the outbox. {@link Propagation#MANDATORY} guarantees the event is
 * committed atomically with the caller's business change - calling it outside a
 * transaction is a programming error and fails fast.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxWriter {

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID append(String topic, UUID partitionKey, UUID aggregateId, Object event) {
        OutboxMessage message = OutboxMessage.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(event.getClass().getSimpleName())
                .topic(topic)
                .messageKey(partitionKey.toString())
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
        entityManager.persist(message);
        return message.getId();
    }
}
