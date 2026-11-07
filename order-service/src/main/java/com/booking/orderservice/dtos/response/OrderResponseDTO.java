package com.booking.orderservice.dtos.response;

import com.booking.orderservice.enums.OrderStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponseDTO {
    UUID id;
    String orderNumber;
    UUID customerId;
    UUID ticketId;
    OrderStatus status;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalAmount;
    Instant orderDate;
    Instant expireTime;
}
