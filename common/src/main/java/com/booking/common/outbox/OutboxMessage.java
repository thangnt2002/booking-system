package com.booking.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. Written in the same DB transaction as the business change,
 * relayed to Kafka by {@link OutboxRelay}. The id doubles as the Kafka message id used
 * for consumer-side de-duplication.
 */
@Entity
@Table(name = "outbox_messages",
        indexes = @Index(name = "ix_outbox_status_created", columnList = "status, created_at"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxMessage {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    UUID id;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;

    @Column(name = "topic", nullable = false, length = 200)
    String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    int retryCount;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    String lastError;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "published_at")
    Instant publishedAt;

    /** Earliest time of the next publish attempt (exponential back-off after failures). */
    @Column(name = "next_attempt_at")
    Instant nextAttemptAt;

    void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.publishedAt = now;
        this.lastError = null;
    }

    void markAttemptFailed(String error, int maxRetries, Instant now, Duration maxBackoff) {
        this.retryCount++;
        this.lastError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
            return;
        }
        // 1s, 2s, 4s ... capped: a Kafka outage does not burn all retries in seconds
        long backoffMillis = Math.min(maxBackoff.toMillis(), 1000L << Math.min(this.retryCount - 1, 20));
        this.nextAttemptAt = now.plusMillis(backoffMillis);
    }
}
