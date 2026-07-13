package com.booking.eventservice.dto.request;

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
public class TicketRequestDTO {

    private String eventId;

    private TicketStatus status;

    private TicketType type;

    private SellingType sellingType;

    private Integer stockInitial;

    private Integer stockAvailable;

    private Integer reservedStock;

    private BigDecimal originalPrice;

    private BigDecimal flashPrice;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;
}
