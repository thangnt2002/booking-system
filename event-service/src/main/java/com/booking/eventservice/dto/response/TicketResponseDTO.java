package com.booking.eventservice.dto.response;

import com.booking.eventservice.enums.SellingType;
import com.booking.eventservice.enums.TicketStatus;
import com.booking.eventservice.enums.TicketType;
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
