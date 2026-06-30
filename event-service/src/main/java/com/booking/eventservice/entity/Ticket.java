package com.booking.eventservice.entity;

import com.booking.eventservice.enums.SellingType;
import com.booking.eventservice.enums.TicketStatus;
import com.booking.eventservice.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "ticket")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String eventId;

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

    @Column(name = "reserved_stock", nullable = false)
    Integer reservedStock;

    @Column(name = "original_price", precision = 18, scale = 2)
    BigDecimal originalPrice;

    @Column(name = "flash_price", nullable = false, precision = 18, scale = 2)
    BigDecimal flashPrice;

    @Column(name = "sale_start_time", nullable = false)
    LocalDateTime saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    LocalDateTime saleEndTime;

    @Column(name = "deleted", columnDefinition = "BIT DEFAULT 0", nullable = false)
    boolean deleted;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

}
