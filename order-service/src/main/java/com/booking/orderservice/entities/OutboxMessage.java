package com.booking.orderservice.entities;

import com.booking.orderservice.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "outbox_message")
public class OutboxMessage {
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

    @Column(name = "created_at")
    Instant createdAt;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "retry_count")
    int retryCount;

    @Column(name = "last_error")
    String lastError;
}
