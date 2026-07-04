package com.booking.orderservice.kafka.producer;

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
public class EventProducer {

    KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void sendMsg(String topic, T msg) {
        log.info("Send msg for topic = {}, msg ={}", topic, msg);
        kafkaTemplate.send(topic, msg);
    }
}
