package com.booking.profileservice.kafka.producer;

import com.booking.profileservice.event.NotificationEvent;
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

    public void sendNotificationEvent(String topic, NotificationEvent msg){
        log.info("Send user ob event = {}", msg);
        kafkaTemplate.send(topic, msg);
    }

}
