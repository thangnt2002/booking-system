package com.booking.eventservice.cache;

import com.booking.eventservice.distributed.RedisDistributedLocker;
import com.booking.eventservice.distributed.RedisDistributedService;
import com.booking.eventservice.distributed.RedisInfraService;
import com.booking.eventservice.dto.cache.EventCache;
import com.booking.eventservice.entity.Event;
import com.booking.eventservice.repository.EventRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventCacheService {

    RedisInfraService redisInfraService;
    RedisDistributedService redisDistributedService;
    EventRepository eventRepository;

    static Cache<String, EventCache> eventLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(12)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public EventCache getEvent(String eventId, Long version) {
        EventCache localCachedEvent = getEventFromLocalCache(eventId);
        if (localCachedEvent != null) {
            if (version == null ||
                    version.equals(localCachedEvent.getVersion()) ||
                    version < localCachedEvent.getVersion()) {
                return localCachedEvent;
            }
        }
        log.info("NOT FOUND EVENT {} IN LOCAL", eventId);
        return getEventFromDistributedCache(eventId);
    }

    EventCache getEventFromLocalCache(String eventId) {
        return eventLocalCache.getIfPresent(genEventLocalCacheKey(eventId));
    }

    EventCache getEventFromDistributedCache(String eventId) {
        EventCache eventCache = redisInfraService.getObject(genEventDistributedCacheKey(eventId), EventCache.class);
        if (eventCache == null) {
            log.info("NOT FOUND EVENT {} IN REDIS", eventId);
            eventCache = getEventDB(eventId);
        }
        eventLocalCache.put(genEventLocalCacheKey(eventId), eventCache);
        return eventCache;

    }

    EventCache getEventDB(String eventId) {
        RedisDistributedLocker locker = redisDistributedService.getDistributedLock(genEventLockKey(eventId));
        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);
            if(!isLock){
                return null;
            }
            EventCache eventCache = redisInfraService.getObject(genEventDistributedCacheKey(eventId), EventCache.class);
            if(eventCache != null){
                return eventCache;
            }

            Event event = eventRepository.findById(eventId).orElse(null);
            if(event == null){
                return null;
            }
            eventCache = new EventCache().withClone(event).withVersion(System.currentTimeMillis());
            log.info("SET REDIS FOR {} EVENT", eventId);
            redisInfraService.setObject(genEventDistributedCacheKey(eventId), eventCache);
            return eventCache;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }

    }

    String genEventLockKey(String eventId) {
        return "LOCK_KEY_EVENT_" + eventId;
    }

    String genEventLocalCacheKey(String id) {
        return "LOCAL_CACHE_EVENT :" + id;
    }

    String genEventDistributedCacheKey(String id) {
        return "DISTRIBUTEED_CACHE_EVENT :" + id;
    }

}
