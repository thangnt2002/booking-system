package com.booking.common.messaging;

import com.booking.common.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

import java.util.ArrayList;
import java.util.List;

/**
 * Reliable-messaging defaults shared by services that set {@code app.messaging.enabled=true}:
 * bounded exponential retries, then the record goes to {@code <topic>.DLT} so one poison
 * message never blocks a partition. Malformed payloads skip the retries.
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class MessagingConfiguration {

    private static final List<String> TOPICS = List.of(
            Topics.ORDER_PLACED, Topics.ORDER_CANCELLED, Topics.ORDER_PAID, Topics.RESERVATION_FAILED);

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations,
                                                @Value("${app.messaging.retry.max-attempts:5}") int maxAttempts,
                                                @Value("${app.messaging.retry.initial-interval-ms:500}") long initialInterval,
                                                @Value("${app.messaging.retry.max-interval-ms:10000}") long maxInterval) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, ex) -> {
                    log.error("Sending record to DLT topic={} partition={} offset={}",
                            record.topic(), record.partition(), record.offset(), ex);
                    // partition < 0 lets Kafka pick the partition, so the DLT may have fewer partitions
                    return new TopicPartition(record.topic() + Topics.DLT_SUFFIX, -1);
                });

        ExponentialBackOff backOff = new ExponentialBackOff(initialInterval, 2.0);
        backOff.setMaxInterval(maxInterval);
        backOff.setMaxAttempts(maxAttempts);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                JacksonException.class, DeserializationException.class, IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public KafkaAdmin.NewTopics bookingTopics(@Value("${app.messaging.partitions:6}") int partitions,
                                              @Value("${app.messaging.replicas:1}") int replicas) {
        List<NewTopic> topics = new ArrayList<>();
        for (String name : TOPICS) {
            topics.add(TopicBuilder.name(name).partitions(partitions).replicas(replicas).build());
            topics.add(TopicBuilder.name(name + Topics.DLT_SUFFIX).partitions(1).replicas(replicas).build());
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }
}
