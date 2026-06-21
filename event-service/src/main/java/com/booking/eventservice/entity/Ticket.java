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
    private String id;

    private String eventId;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "selling_type", nullable = false)
    private SellingType sellingType;

    @Column(name = "flash_initial", nullable = false)
    private Integer stockInitial;

    @Column(name = "stock_available", nullable = false)
    private Integer stockAvailable;

    @Column(name = "original_price", precision = 18, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "flash_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal flashPrice;

    @Column(name = "sale_start_time", nullable = false)
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    private LocalDateTime saleEndTime;

    @Column(name = "deleted", columnDefinition = "BIT DEFAULT 0", nullable = false)
    private boolean isDeleted;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
