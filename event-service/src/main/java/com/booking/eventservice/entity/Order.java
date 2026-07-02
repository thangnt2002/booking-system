package com.booking.eventservice.entity;

import com.booking.eventservice.enums.OrderStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {
    String id;

    String orderNumber;

    String customerId;

    LocalDateTime orderDate;

    OrderStatus status;

    String ticketId;

    Integer quantity;

    BigDecimal price;

    LocalDateTime updatedAt;

    LocalDateTime createdAt;
}
