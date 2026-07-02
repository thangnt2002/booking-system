package com.booking.eventservice.cache;

import com.booking.eventservice.distributed.RedisDistributedLocker;
import com.booking.eventservice.distributed.RedisDistributedService;
import com.booking.eventservice.distributed.RedisInfraService;
import com.booking.eventservice.dto.cache.TicketCache;
import com.booking.eventservice.entity.Ticket;
import com.booking.eventservice.repository.TicketRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import static com.booking.eventservice.common.Utils.genDistributedTicketStockAvailableKey;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static com.booking.eventservice.common.Utils.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketCacheService {

    RedisInfraService redisInfraService;
    RedisDistributedService redisDistributedService;
    TicketRepository ticketRepository;

    static Cache<String, TicketCache> ticketLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(12)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public TicketCache getTicket(String ticketId, Long version){
        TicketCache ticketCache = getFromLocalCache(ticketId);

        if (ticketCache != null) {
            if (ticketCache.isNullObject()) {
                return null;
            }
            Long localVersion = ticketCache.getVersion();
            if (version == null || version <= localVersion) {
                return ticketCache;
            }
        }

        ticketCache = getFromDistributedCache(ticketId);
        return (ticketCache == null || ticketCache.isNullObject()) ? null : ticketCache;
    }

    public TicketCache getFromLocalCache(String ticketId){
        return ticketLocalCache.getIfPresent(genLocalCacheKey(Ticket.class, ticketId));
    }

    public TicketCache getFromDistributedCache(String ticketId) {
        String redisKey = genDistributedCacheKey(Ticket.class, ticketId);
        TicketCache ticketCache = redisInfraService.getObject(redisKey, TicketCache.class);

        if (ticketCache == null) {
            log.info("Redis for ticket {} not found", ticketId);
            ticketCache = getFromDB(ticketId);
        }

        if (ticketCache != null) {
            ticketLocalCache.put(genLocalCacheKey(Ticket.class, ticketId), ticketCache);
        }
        return ticketCache;
    }

    public TicketCache getFromDB(String ticketId) {
        String redisKey = genDistributedCacheKey(Ticket.class, ticketId);
        RedisDistributedLocker locker = redisDistributedService.getDistributedLock(genRequestLockKey(Ticket.class, ticketId));
        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                return null;
            }

            TicketCache ticketCache = redisInfraService.getObject(redisKey, TicketCache.class);
            if (ticketCache != null) {
                return ticketCache;
            }

            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            log.info("SET REDIS FOR {} TICKET", ticket);

            ticketCache = new TicketCache();
            if (ticket == null) {
                ticketCache.markAsNull();
                redisInfraService.setObject(redisKey, ticketCache);
                return ticketCache;
            }

            ticketCache = ticketCache.withClone(ticket).withVersion(System.currentTimeMillis());
            redisInfraService.setObject(redisKey, ticketCache);
            return ticketCache;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }
    }


    public int decreaseStock(String ticketId, int quantity) {
        String stockAvailableCacheKey = genDistributedTicketStockAvailableKey(ticketId);
        String luaScript = "local stock = redis.call('GET', [KEYS]);" +
                "if stock == false then return -1 end; " +
                "stock = tonumber(stock); " +
                "if (stock >= tonumber(ARGV[1])) then " +
                "   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1]));" +
                "   return 1;" +
                "end;" +
                "return 0";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisInfraService.getRedisTemplate().execute(redisScript, Collections.singletonList(stockAvailableCacheKey), quantity);
        return result != null ? result.intValue() : 0;
    }

    public boolean increaseStock(String ticketId, int quantity) {
        String stockAvailableCacheKey = genDistributedTicketStockAvailableKey(ticketId);
        String luaScript = "local stock = redis.call('GET', [KEYS]); " +
                "if (stock) then " +
                " redis.call('SET', KEY[1], stock + tonumber(ARGV[1]));" +
                " return 1;" +
                "end;" +
                "return 0";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisInfraService.getRedisTemplate().execute(redisScript, Collections.singletonList(stockAvailableCacheKey), quantity);
        return result != null && result.intValue() == 1;
    }


}
