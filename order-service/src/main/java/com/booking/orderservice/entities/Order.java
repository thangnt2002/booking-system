package com.booking.orderservice.entities;


import com.booking.orderservice.enums.OrderStatus;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "order_number", nullable = false)
    String orderNumber;

    @Column(name = "customer_id", nullable = false)
    UUID customerId;

    @Column(name = "ticket_id", nullable = false)
    UUID ticketId;

    @Column(name = "order_date", nullable = false)
    Instant orderDate;

    @Column(name = "status", nullable = false)
    OrderStatus status;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "price", nullable = false)
    BigDecimal price;

    @Column(name = "expire_time", nullable = false)
    Instant expireTime;

}
