package com.booking.orderservice.outbox.entity;

import com.booking.orderservice.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    String id;

    @Column(name = "aggregate_id")
    String aggregateId;

    @Column(name = "aggregate_type")
    String aggregateType;

    @Column(name = "event_id")
    String eventType;

    String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    OutboxStatus status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;
}
