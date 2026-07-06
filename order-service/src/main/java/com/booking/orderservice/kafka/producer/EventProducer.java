package com.booking.orderservice.kafka.producer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventProducer {

    KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void sendMsg(String topic, String key, T msg) {
        log.info("Send msg for topic = {}, msg ={}", topic, msg);
        kafkaTemplate.send(topic, key, msg);
    }

    public <T> void sendAndWaitAck(String topic, String key, T msg)
            throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Send msg for topic = {}, msg ={}", topic, msg);
        kafkaTemplate.send(topic, key, msg).get(5, TimeUnit.SECONDS);
    }
}
