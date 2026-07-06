package com.booking.orderservice.outbox.scheduler;

import com.booking.orderservice.enums.AggregateTypeMap;
import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.kafka.producer.EventProducer;
import com.booking.orderservice.outbox.entity.OutboxMessage;
import com.booking.orderservice.service.OutboxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxScheduler {

    OutboxService outboxService;
    ObjectMapper objectMapper;
    EventProducer eventProducer;

    @Scheduled(fixedRate = 1000)
    public void publish() {
        publishRow();
    }

    @Transactional(rollbackFor = Exception.class)
    protected void publishRow() {
        List<OutboxMessage> pendingMessages = outboxService.findByStatus(OutboxStatus.PENDING);
        if (pendingMessages.isEmpty()) {
            return;
        }
        pendingMessages.forEach(msg -> {
            try {
                Class<?> clazz = AggregateTypeMap.toClass(msg.getAggregateType());
                Object payload = objectMapper.readValue(msg.getPayload(), clazz);
                eventProducer.sendAndWaitAck(msg.getTopic(), msg.getId(), payload);
                outboxService.markAsPublish(msg.getId());
            } catch (Exception e) {
                //TODO add retry logic or manual handle
                log.error("OutboxPublisher failed for msgId = {}, err = {}", msg.getId(), e.getMessage());
            }
        });
    }

}

