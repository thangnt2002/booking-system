package com.booking.identityservice.kafka.producer;

import com.booking.identityservice.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationProducer {

    KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotificationEvent(String topic, NotificationEvent notificationEvent) {
        log.info("Send notification event = {}", notificationEvent);
        kafkaTemplate.send(topic, notificationEvent);
    }
}
