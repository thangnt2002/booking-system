package com.booking.orderservice.entities;


import com.booking.orderservice.enums.OrderStatus;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String orderNumber;

    UUID customerId;

    LocalDateTime orderDate;

    OrderStatus status;

    UUID ticketId;

    Integer quantity;

    BigDecimal price;

    LocalDateTime updatedAt;

    LocalDateTime createdAt;
}
