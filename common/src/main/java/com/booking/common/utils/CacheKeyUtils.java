package com.booking.common.utils;

import java.util.UUID;

public class CacheKeyUtils {
    public static <T> String genRequestLockKey(Class<T> type, UUID id) {
        String className = type.getSimpleName();
        return String.format("LOCK_KEY_%s_%s", className, id);
    }

    public static <T> String genLocalCacheKey(Class<T> type, UUID id) {
        String className = type.getSimpleName();
        return String.format("LOCAL_CACHE_%s_%s", className, id);
    }

    public static <T> String genDistributedCacheKey(Class<T> type, UUID id) {
        String className = type.getSimpleName();
        return String.format("DISTRIBUTED_CACHE_%s_%s", className, id);
    }

    public static String genDistributedTicketStockAvailableKey(UUID ticketId){
        return String.format("DISTRIBUTED_%s_STOCK_AVAILABLE", ticketId);
    }
}


