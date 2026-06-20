package com.booking.identityservice.kafka;

import com.booking.identityservice.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.booking.identityservice.common.Constant.TOPIC_USER_OB;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationProducer {

    KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(NotificationEvent notificationEvent) {
        kafkaTemplate.send(TOPIC_USER_OB, notificationEvent);
        log.info("Event = {}", notificationEvent);
    }
}
