package com.booking.orderservice.dtos.response;

import com.booking.orderservice.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponseDTO {
    String orderNumber;

    String customerId;

    Instant orderDate;

    OrderStatus status;

    String ticketId;

    Integer quantity;

    BigDecimal price;

}
