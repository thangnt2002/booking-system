package com.booking.common.messaging;

import com.booking.common.events.MessageHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class KafkaRecords {

    private KafkaRecords() {
    }

    /** @throws IllegalArgumentException (non-retryable -> DLT) when the header is missing or malformed */
    public static UUID messageId(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(MessageHeaders.MESSAGE_ID);
        if (header == null || header.value() == null) {
            throw new IllegalArgumentException("Missing header " + MessageHeaders.MESSAGE_ID
                    + " on " + record.topic() + "-" + record.partition() + "@" + record.offset());
        }
        return UUID.fromString(new String(header.value(), StandardCharsets.UTF_8));
    }
}
