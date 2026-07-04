package com.booking.orderservice.dto.response;

import com.booking.orderservice.enums.SellingType;
import com.booking.orderservice.enums.TicketStatus;
import com.booking.orderservice.enums.TicketType;
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
public class TicketResponseDTO {
    String eventId;

    TicketStatus status;

    TicketType type;

    SellingType sellingType;

    Integer stockInitial;

    Integer stockAvailable;

    BigDecimal originalPrice;

    BigDecimal flashPrice;

    LocalDateTime saleStartTime;

    LocalDateTime saleEndTime;

    Long version;
}
