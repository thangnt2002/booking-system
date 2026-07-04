package com.booking.eventservice.kafka.consumer;

import com.booking.eventservice.event.TicketRestockEvent;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.booking.eventservice.common.Constant.TICKET_RESTOCK_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderConsumer {

     TicketService ticketService;

    @KafkaListener(topics = TICKET_RESTOCK_TOPIC, groupId = "event-group")
    public void listenTicketRestock(TicketRestockEvent message) {
        log.info("Message received: {}", message);
        ticketService.increaseStock(message.getTicketId(), message.getQuantity());
    }
}
