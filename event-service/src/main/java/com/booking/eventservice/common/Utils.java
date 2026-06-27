package com.booking.eventservice.common;

public class Utils {

    public static <T> String genEventLockKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("LOCK_KEY_%s_%s", className, id);
    }

    public static <T> String genEventLocalCacheKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("LOCAL_CACHE_%s_%s", className, id);
    }

    public static <T> String genEventDistributedCacheKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("DISTRIBUTED_CACHE_%s_%s", className, id);
    }
}


