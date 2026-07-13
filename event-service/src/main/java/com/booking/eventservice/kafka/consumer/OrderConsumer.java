package com.booking.eventservice.kafka.consumer;

import com.booking.eventservice.event.TicketStockEvent;
import com.booking.eventservice.exception.BusinessException;
import com.booking.eventservice.exception.ErrorCode;
import com.booking.eventservice.service.IdempotencyKeyService;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.booking.eventservice.common.Constant.TICKET_DECREASE_STOCK_TOPIC;
import static com.booking.eventservice.common.Constant.TICKET_RESTOCK_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderConsumer {

    TicketService ticketService;
    IdempotencyKeyService idempotencyKeyService;

    @KafkaListener(topics = TICKET_RESTOCK_TOPIC, groupId = "event-group")
    @Transactional(rollbackFor = Exception.class)
    public void listenTicketRestock(@Payload TicketStockEvent message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Message received: {}", message);
        boolean isNewEvent = idempotencyKeyService.tryInsert(key);
        if (!isNewEvent) {
            log.info("Event id ={} was already consumed", key);
            return;
        }
        try {
            boolean increaseSuccess = ticketService.releaseStock(message.getTicketId(), message.getQuantity());
            if(!increaseSuccess){
                log.error("Increase stock failed for event = {}", key);
                throw new BusinessException(ErrorCode.SERVER_ERROR);
            }
        } catch (Exception e) {
            // noti admin
            log.error("Restore ticket err, event id = {}", key);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    @KafkaListener(topics = TICKET_DECREASE_STOCK_TOPIC, groupId = "event-group")
    @Transactional(rollbackFor = Exception.class)
    public void listenDecreaseStock(@Payload TicketStockEvent message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Decrease stock message received: {}", message);
        boolean isNewEvent = idempotencyKeyService.tryInsert(key);
        if (!isNewEvent) {
            log.info("Event id ={} was already consumed", key);
            return;
        }
        try {
            boolean decreaseSuccess = ticketService.decreaseStock(message.getTicketId(), message.getQuantity());
            if (!decreaseSuccess) {
                // noti admin
                log.error("Decrease ticket failed, event id = {}", key);
                throw new BusinessException(ErrorCode.SERVER_ERROR);
            }
            log.info("Decrease stock success for ticket = {}, quantity = {}", message.getTicketId(), message.getQuantity());
        } catch (Exception e) {
            // noti admin
            log.error("Decrease ticket err, event id = {}", key);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }
}
