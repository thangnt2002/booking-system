package com.booking.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param paymentTimeout    how long a PENDING order holds its tickets before it expires
 * @param redisHoldTtl      safety TTL of the per-order hold key; must be far longer than paymentTimeout
 * @param maxQuantityPerOrder anti-hoarding limit
 * @param expiryBatchSize   rows expired per transaction by the expiry job
 * @param ticketCacheTtl    how long ticket price/status is cached locally to shield event-service
 */
@ConfigurationProperties("app.order")
public record OrderProperties(
        @DefaultValue("15m") Duration paymentTimeout,
        @DefaultValue("24h") Duration redisHoldTtl,
        @DefaultValue("10") int maxQuantityPerOrder,
        @DefaultValue("200") int expiryBatchSize,
        @DefaultValue("2s") Duration ticketCacheTtl) {
}
