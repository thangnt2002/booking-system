package com.booking.orderservice.dto.response;

import com.booking.orderservice.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponseDTO {
    String orderNumber;

    String customerId;

    LocalDateTime orderDate;

    OrderStatus status;

    String ticketId;

    Integer quantity;

    BigDecimal price;

}
