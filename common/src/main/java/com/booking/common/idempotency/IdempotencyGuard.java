package com.booking.common.idempotency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Exactly-once effect on top of at-least-once delivery: the inbox row is inserted in the
 * same transaction as the business change, so either both commit or neither does.
 * <p>
 * A concurrent duplicate (possible only during a consumer-group rebalance) fails on the
 * primary key, rolls the whole transaction back and is redelivered; on redelivery
 * {@link #tryAcquire} returns false and the message is skipped.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class IdempotencyGuard {

    @PersistenceContext
    private EntityManager entityManager;

    /** @return true if the message is new and the caller must process it. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryAcquire(UUID messageId, String consumer) {
        if (entityManager.find(ProcessedMessage.class, messageId) != null) {
            return false;
        }
        entityManager.persist(new ProcessedMessage(messageId, consumer, Instant.now()));
        entityManager.flush();
        return true;
    }
}
