package com.booking.eventservice.entity;

import com.booking.common.events.CancelReason;
import com.booking.eventservice.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
// one reservation per order: the unique key makes OrderPlaced processing idempotent at the DB level
@Table(name = "reservations",
        uniqueConstraints = @UniqueConstraint(name = "uq_reservations_order", columnNames = "order_id"),
        indexes = @Index(name = "ix_reservations_ticket_status", columnList = "ticket_id, status"))
public class Reservation extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "customer_id", nullable = false)
    UUID customerId;

    @Column(name = "ticket_id", nullable = false)
    UUID ticketId;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    CancelReason cancelReason;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "confirmed_at")
    Instant confirmedAt;

    @Column(name = "cancelled_at")
    Instant cancelledAt;
}