package com.booking.orderservice.kafka.consumer;

import com.booking.orderservice.dto.response.TicketResponseDTO;
import com.booking.orderservice.entity.Order;
import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.event.TicketStockEvent;
import com.booking.orderservice.outbox.entity.OutboxMessage;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.repository.OutboxRepository;
import com.booking.orderservice.repository.http.EventClient;
import com.booking.orderservice.service.IdempotencyKeyService;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.booking.orderservice.common.Constant.PLACE_ORDER_TOPIC;
import static com.booking.orderservice.common.Constant.TICKET_DECREASE_STOCK_TOPIC;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventConsumer {

    IdempotencyKeyService idempotencyKeyService;

    EventClient eventClient;

    OrderRepository orderRepository;

    ObjectMapper objectMapper;

    OutboxRepository outboxRepository;

    @KafkaListener(topics = PLACE_ORDER_TOPIC, groupId = "order-group")
    @Transactional(rollbackFor = Exception.class)
    public void listenPlaceOrderStock(@Payload Order message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Place Order message received: {}", message);
        boolean isNewEvent = idempotencyKeyService.tryInsert(key);
        if (!isNewEvent) {
            log.info("Event id ={} was already consumed", key);
            return;
        }

        // create order
        String ticketId = message.getTicketId();
        message.setPrice(getEffectivePrice(ticketId));
        String tableName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        orderRepository.insertOrder(tableName, message);
        log.info("Order {} created!", message.getOrderNumber());

        TicketStockEvent ticketDecreaseStockEvent = new TicketStockEvent(ticketId, message.getQuantity());
        String jsonMsg = objectMapper.writeValueAsString(ticketDecreaseStockEvent);
        OutboxMessage outboxMessage = OutboxMessage.builder()
                .topic(TICKET_DECREASE_STOCK_TOPIC)
                .aggregateType(TicketStockEvent.class.getSimpleName())
                .payload(jsonMsg)
                .eventType("decreaseStock")
                .createdAt(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .build();
        outboxRepository.save(outboxMessage);
    }

    private BigDecimal getEffectivePrice(String ticketId) {
        TicketResponseDTO ticket = eventClient.findTicketById(ticketId).getData();
        if (ticket == null) return BigDecimal.valueOf(-1);
        if (ticket.getFlashPrice() != null && ticket.getFlashPrice().compareTo(BigDecimal.valueOf(0)) > 0) {
            return ticket.getOriginalPrice();
        }
        return ticket.getOriginalPrice() != null ? ticket.getOriginalPrice() : BigDecimal.valueOf(-1);
    }

}
