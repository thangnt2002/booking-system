package com.booking.orderservice.dtos.response;

import com.booking.orderservice.enums.SellingType;
import com.booking.orderservice.enums.TicketStatus;
import com.booking.orderservice.enums.TicketType;
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
public class TicketResponseDTO {
    String id;
    String eventId;

    TicketStatus status;

    TicketType type;

    SellingType sellingType;

    Integer stockInitial;

    Integer stockAvailable;

    BigDecimal originalPrice;

    BigDecimal flashPrice;

    Instant saleStartTime;

    Instant saleEndTime;

    Long version;
}
