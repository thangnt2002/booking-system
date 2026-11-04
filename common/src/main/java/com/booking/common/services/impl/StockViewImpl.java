package com.booking.common.services.impl;

import com.booking.common.distributed.RedisInfraService;
import com.booking.common.services.StockViewService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.booking.common.utils.CacheKeyUtils.genDistributedTicketStockAvailableKey;

@Component
@Slf4j
public class StockViewImpl implements StockViewService {

    private final RedisInfraService redisInfraService;

    // Optional.empty() = "không xác định", cache được để Redis cũng không bị đập khi key mất / Redis lỗi
    private final Cache<UUID, Optional<Long>> localCache = CacheBuilder.newBuilder()
            .expireAfterWrite(300, TimeUnit.MILLISECONDS)
            .maximumSize(10_000)
            .build();

    public StockViewImpl(RedisInfraService redisInfraService) {
        this.redisInfraService = redisInfraService;
    }

    @Override
    public Long getRemaining(UUID ticketId) {
        try {
            return localCache.get(ticketId, () -> loadFromRedis(ticketId)).orElse(null);
        } catch (Exception e) {
            log.warn("Read stock view failed, ticketId={}", ticketId, e);
            return null;
        }
    }

    private Optional<Long> loadFromRedis(UUID ticketId) {
        try {
            String key = genDistributedTicketStockAvailableKey(ticketId);   // dùng chung hàm sinh key với decreaseStock
            Object raw = redisInfraService.getRedisTemplate().opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();           // chưa warm-up hoặc Redis mất key
            }
            long value = (raw instanceof Number n) ? n.longValue() : Long.parseLong(raw.toString());
            return Optional.of(value);
        } catch (Exception e) {
            log.warn("Load stock from Redis failed, ticketId={}", ticketId, e);
            return Optional.empty();               // không xác định thì không chặn
        }
    }
}