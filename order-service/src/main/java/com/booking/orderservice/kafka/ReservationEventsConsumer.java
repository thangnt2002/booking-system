package com.booking.orderservice.kafka;

import com.booking.common.events.ReservationFailedEvent;
import com.booking.common.events.Topics;
import com.booking.common.idempotency.IdempotencyGuard;
import com.booking.common.messaging.KafkaRecords;
import com.booking.orderservice.service.OrderTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Exceptions are deliberately NOT caught: the shared DefaultErrorHandler retries with
 * back-off and then routes the record to the DLT, and the transaction (inbox row
 * included) rolls back so a retry starts clean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventsConsumer {

    private static final String CONSUMER = "order-service.reservation-failed";

    private final IdempotencyGuard idempotencyGuard;
    private final OrderTxService orderTxService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.RESERVATION_FAILED, groupId = "order-service")
    @Transactional
    public void onReservationFailed(ConsumerRecord<String, String> record) {
        UUID messageId = KafkaRecords.messageId(record);
        if (!idempotencyGuard.tryAcquire(messageId, CONSUMER)) {
            log.debug("Duplicate message {} skipped", messageId);
            return;
        }
        ReservationFailedEvent event = objectMapper.readValue(record.value(), ReservationFailedEvent.class);
        log.warn("Reservation failed for order {}: {}", event.orderId(), event.reason());
        orderTxService.failReservation(event);
    }
}
