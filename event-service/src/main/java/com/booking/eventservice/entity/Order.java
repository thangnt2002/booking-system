package com.booking.eventservice.entity;

import com.booking.eventservice.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "order")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "order_number", nullable = false, unique = true)
    String orderNumber;

    @Column(name = "customer_id", nullable = false)
    String customerId;

    @Column(name = "order_date", nullable = false)
    LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    OrderStatus status;

    @Column(name = "ticket_id", nullable = false)
    String ticketId;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    BigDecimal price;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
