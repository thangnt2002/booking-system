package com.booking.orderservice.service.impl;

import com.booking.common.distributed.RedisInfraService;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.orderservice.clients.EventClient;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.booking.common.utils.CacheKeyUtils.genDistributedTicketStockAvailableKey;
import static com.booking.common.utils.FeignClientUtils.getApiResDataOrDefaultValue;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockCacheImpl implements StockCacheService {

    private final EventClient eventClient;
    private final RedisInfraService redisInfraService;

    // Optional.empty() = "không xác định", cache được để Redis cũng không bị đập khi key mất / Redis lỗi
    private final Cache<UUID, Optional<Long>> localCache = CacheBuilder.newBuilder()
            .expireAfterWrite(300, TimeUnit.MILLISECONDS)
            .maximumSize(10_000)
            .build();

    @Override
    public Long getRemaining(UUID ticketId) {
        try {
            return localCache.get(ticketId, () -> loadFromRedis(ticketId)).orElse(null);
        } catch (Exception e) {
            log.warn("Read stock view failed, ticketId={}", ticketId, e);
            return null;
        }
    }

    @Override
    public int reserveByLUA(UUID ticketId, Integer quantity) {
        String stockAvailableCacheKey = genDistributedTicketStockAvailableKey(ticketId);
        // return -1 when key doesn't exist (cache not warmed), 0 when out of stock, 1 when success
        String luaScript =
                "local stock = redis.call('GET', KEYS[1]); " +
                        "if stock == false then return -1 end; " +
                        "stock = tonumber(stock); " +
                        "if (stock >= tonumber(ARGV[1])) then " +
                        "   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); " +
                        "   return 1; " +
                        "end; " +
                        "return 0; ";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisInfraService.getRedisTemplate().execute(redisScript, Collections.singletonList(stockAvailableCacheKey), quantity);
        return result != null ? result.intValue() : -1;
    }

    @Override
    public boolean warmCache(UUID ticketId) {
        boolean warmed = getApiResDataOrDefaultValue(eventClient.warmCache(ticketId), false);
        if(!warmed){
            log.error("Ticket {} not found", ticketId);
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }
        return true;
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