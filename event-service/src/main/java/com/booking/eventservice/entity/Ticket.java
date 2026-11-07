package com.booking.eventservice.entity;

import com.booking.eventservice.enums.SellingType;
import com.booking.eventservice.enums.TicketStatus;
import com.booking.eventservice.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tickets")
public class Ticket extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "event_id", nullable = false)
    UUID eventId;

    @Enumerated(EnumType.STRING)
    TicketStatus status;

    @Enumerated(EnumType.STRING)
    TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "selling_type", nullable = false)
    SellingType sellingType;

    @Column(name = "stock_initial", nullable = false)
    Integer stockInitial;

    @Column(name = "stock_available", nullable = false)
    Integer stockAvailable;

    @Column(name = "reserved_stock", nullable = false, columnDefinition = "INT DEFAULT 0")
    Integer reservedStock;

    @Column(name = "sold", nullable = false, columnDefinition = "INT DEFAULT 0")
    Integer sold;

    @Column(name = "original_price", precision = 18, scale = 2)
    BigDecimal originalPrice;

    @Column(name = "flash_price", nullable = false, precision = 18, scale = 2)
    BigDecimal flashPrice;

    @Column(name = "sale_start_time", nullable = false)
    Instant saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    Instant saleEndTime;
}
