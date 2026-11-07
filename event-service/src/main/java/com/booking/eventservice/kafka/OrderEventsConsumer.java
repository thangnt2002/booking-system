package com.booking.eventservice.kafka;

import com.booking.common.events.OrderCancelledEvent;
import com.booking.common.events.OrderPaidEvent;
import com.booking.common.events.OrderPlacedEvent;
import com.booking.common.events.Topics;
import com.booking.common.idempotency.IdempotencyGuard;
import com.booking.common.messaging.KafkaRecords;
import com.booking.eventservice.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Order saga consumers. Messages are keyed by ticketId, so all changes to one ticket are
 * applied sequentially on one partition (no DB lock contention between consumer threads).
 * Exceptions propagate on purpose: the transaction (inbox row included) rolls back and
 * the shared error handler retries with back-off, then dead-letters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

    private static final String GROUP = "event-service";

    private final IdempotencyGuard idempotencyGuard;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = GROUP)
    @Transactional
    public void onOrderPlaced(ConsumerRecord<String, String> record) {
        handle(record, OrderPlacedEvent.class, reservationService::reserve);
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = GROUP)
    @Transactional
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        handle(record, OrderCancelledEvent.class, reservationService::cancel);
    }

    @KafkaListener(topics = Topics.ORDER_PAID, groupId = GROUP)
    @Transactional
    public void onOrderPaid(ConsumerRecord<String, String> record) {
        handle(record, OrderPaidEvent.class, reservationService::confirm);
    }

    private <T> void handle(ConsumerRecord<String, String> record, Class<T> type, Consumer<T> handler) {
        UUID messageId = KafkaRecords.messageId(record);
        if (!idempotencyGuard.tryAcquire(messageId, GROUP + "." + record.topic())) {
            log.debug("Duplicate message {} on {} skipped", messageId, record.topic());
            return;
        }
        handler.accept(objectMapper.readValue(record.value(), type));
    }
}
