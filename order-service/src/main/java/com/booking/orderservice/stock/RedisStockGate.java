package com.booking.orderservice.stock;

import com.booking.orderservice.config.OrderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.booking.common.utils.CacheKeyUtils.ticketHoldKey;
import static com.booking.common.utils.CacheKeyUtils.ticketStockKey;

/**
 * Admission gate in front of the database. Absorbs the flash-sale burst in Redis with a
 * single atomic Lua call per request so only requests that won a ticket reach SQL Server.
 * The database (event-service) stays the source of truth and re-checks every reservation.
 */
@Slf4j
@Component
public class RedisStockGate {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> reserveScript;
    private final RedisScript<Long> releaseScript;
    private final String holdTtlSeconds;

    public RedisStockGate(StringRedisTemplate redis,
                          @Qualifier("reserveStockScript") RedisScript<Long> reserveScript,
                          @Qualifier("releaseStockScript") RedisScript<Long> releaseScript,
                          OrderProperties properties) {
        this.redis = redis;
        this.reserveScript = reserveScript;
        this.releaseScript = releaseScript;
        this.holdTtlSeconds = String.valueOf(properties.redisHoldTtl().toSeconds());
    }

    public ReserveResult reserve(UUID ticketId, UUID orderId, int quantity) {
        Long code = redis.execute(reserveScript, keys(ticketId, orderId), String.valueOf(quantity), holdTtlSeconds);
        return ReserveResult.fromScript(code);
    }

    /** Idempotent. Never throws: a failed release is logged and left to stock reconciliation. */
    public void release(UUID ticketId, UUID orderId) {
        try {
            Long released = redis.execute(releaseScript, keys(ticketId, orderId));
            log.debug("Release hold ticket={} order={} released={}", ticketId, orderId, released);
        } catch (Exception e) {
            log.error("Failed to release Redis hold ticket={} order={}", ticketId, orderId, e);
        }
    }

    /** Payment succeeded: the units are sold, so just drop the hold without giving stock back. */
    public void consume(UUID ticketId, UUID orderId) {
        try {
            redis.delete(ticketHoldKey(ticketId, orderId));
        } catch (Exception e) {
            log.warn("Failed to delete Redis hold ticket={} order={} (expires by TTL)", ticketId, orderId, e);
        }
    }

    private static List<String> keys(UUID ticketId, UUID orderId) {
        return List.of(ticketStockKey(ticketId), ticketHoldKey(ticketId, orderId));
    }
}
