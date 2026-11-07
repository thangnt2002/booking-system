package com.booking.orderservice.entities;

import com.booking.orderservice.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "outbox_messages")
public class OutboxMessage extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "aggregate_id")
    UUID aggregateId;

    @Column(name = "aggregate_type")
    String aggregateType;

    @Column(name = "event_type")
    String eventType;

    String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    OutboxStatus status;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "retry_count")
    int retryCount;

    @Column(name = "last_error")
    String lastError;
}
