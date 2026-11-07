package com.booking.orderservice.entities;

import com.booking.orderservice.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The id is assigned by the application (derived from customer + Idempotency-Key), which
 * makes order creation naturally idempotent. A null {@link #version} tells Spring Data the
 * entity is new, so save() issues a plain INSERT without a prior SELECT.
 */
@Entity
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_orders_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "ix_orders_customer_created", columnList = "customer_id, created_date, id"),
                @Index(name = "ix_orders_status_expire", columnList = "status, expire_time")
        })
public class Order extends BaseModel {

    @Id
    UUID id;

    @Column(name = "order_number", nullable = false, length = 40)
    String orderNumber;

    @Column(name = "customer_id", nullable = false)
    UUID customerId;

    @Column(name = "ticket_id", nullable = false)
    UUID ticketId;

    @Column(name = "order_date", nullable = false)
    Instant orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    OrderStatus status;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "expire_time", nullable = false)
    Instant expireTime;

    @Version
    @Column(name = "version")
    Long version;
}
