package com.booking.common.outbox;

import com.booking.common.events.MessageHeaders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Polling outbox publisher.
 * <p>
 * Each round locks a batch with {@code UPDLOCK, READPAST}: concurrent relays on other
 * instances skip locked rows instead of blocking, so the relay scales horizontally with
 * no leader election and no double publishing. Rows are marked SENT in the same
 * transaction only after Kafka acknowledged them (acks=all), giving at-least-once
 * delivery; consumers de-duplicate on the {@link MessageHeaders#MESSAGE_ID} header.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxRelay {

    private static final String LOCK_BATCH_SQL = """
            SELECT TOP (:limit) * FROM outbox_messages WITH (UPDLOCK, READPAST, ROWLOCK)
            WHERE status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY created_at
            """;
    private static final String PURGE_SQL = """
            DELETE TOP (:limit) FROM outbox_messages
            WHERE status = 'SENT' AND published_at < :before
            """;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int maxRoundsPerTick;
    private final int maxRetries;
    private final Duration sendTimeout;
    private final Duration retention;
    private final Duration maxBackoff;

    @PersistenceContext
    private EntityManager entityManager;

    public OutboxRelay(KafkaTemplate<String, String> kafkaTemplate,
                       PlatformTransactionManager transactionManager,
                       @Value("${app.outbox.batch-size:200}") int batchSize,
                       @Value("${app.outbox.max-rounds-per-tick:20}") int maxRoundsPerTick,
                       @Value("${app.outbox.max-retries:20}") int maxRetries,
                       @Value("${app.outbox.send-timeout:10s}") Duration sendTimeout,
                       @Value("${app.outbox.retention:7d}") Duration retention,
                       @Value("${app.outbox.max-backoff:5m}") Duration maxBackoff) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
        this.maxRoundsPerTick = maxRoundsPerTick;
        this.maxRetries = maxRetries;
        this.sendTimeout = sendTimeout;
        this.retention = retention;
        this.maxBackoff = maxBackoff;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:200}")
    public void relay() {
        try {
            for (int round = 0; round < maxRoundsPerTick; round++) {
                Integer processed = transactionTemplate.execute(status -> publishNextBatch());
                if (processed == null || processed < batchSize) {
                    return;
                }
            }
        } catch (Exception e) {
            // never let the scheduler thread die; the next tick retries
            log.error("Outbox relay round failed", e);
        }
    }

    @Scheduled(cron = "${app.outbox.purge-cron:0 0 3 * * *}")
    public void purgeSent() {
        Instant before = Instant.now().minus(retention);
        int deleted;
        do {
            deleted = transactionTemplate.execute(status -> entityManager.createNativeQuery(PURGE_SQL)
                    .setParameter("limit", 5_000)
                    .setParameter("before", before)
                    .executeUpdate());
        } while (deleted > 0);
    }

    @SuppressWarnings("unchecked")
    private int publishNextBatch() {
        List<OutboxMessage> batch = entityManager.createNativeQuery(LOCK_BATCH_SQL, OutboxMessage.class)
                .setParameter("limit", batchSize)
                .setParameter("now", Instant.now())
                .getResultList();
        if (batch.isEmpty()) {
            return 0;
        }

        // fire all sends first so the producer can batch them, then wait for the acks
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(batch.size());
        for (OutboxMessage message : batch) {
            futures.add(send(message));
        }

        long deadline = System.nanoTime() + sendTimeout.toNanos();
        Instant now = Instant.now();
        for (int i = 0; i < batch.size(); i++) {
            OutboxMessage message = batch.get(i);
            try {
                long remaining = Math.max(0, deadline - System.nanoTime());
                futures.get(i).get(remaining, TimeUnit.NANOSECONDS);
                message.markSent(now);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                message.markAttemptFailed("interrupted", maxRetries, now, maxBackoff);
            } catch (ExecutionException | TimeoutException e) {
                Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
                message.markAttemptFailed(cause.toString(), maxRetries, now, maxBackoff);
                log.warn("Outbox publish failed id={} topic={} attempt={}",
                        message.getId(), message.getTopic(), message.getRetryCount(), cause);
            }
        }
        return batch.size();
    }

    private CompletableFuture<SendResult<String, String>> send(OutboxMessage message) {
        try {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(message.getTopic(), message.getMessageKey(), message.getPayload());
            record.headers()
                    .add(MessageHeaders.MESSAGE_ID, message.getId().toString().getBytes(StandardCharsets.UTF_8))
                    .add(MessageHeaders.EVENT_TYPE, message.getEventType().getBytes(StandardCharsets.UTF_8));
            return kafkaTemplate.send(record);
        } catch (Exception e) {
            // synchronous failures (e.g. metadata timeout) surface through the future
            return CompletableFuture.failedFuture(e);
        }
    }
}
