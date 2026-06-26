package com.booking.eventservice.cache;

import com.booking.eventservice.distributed.RedisDistributedService;
import com.booking.eventservice.distributed.RedisInfraService;
import com.booking.eventservice.dto.cache.TicketCache;
import com.booking.eventservice.repository.TicketRepository;
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
public class TicketCacheService {

    RedisInfraService redisInfraService;
    RedisDistributedService redisDistributedService;
    TicketRepository ticketRepository;

    static Cache<String, TicketCache> ticketLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(12)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
}
