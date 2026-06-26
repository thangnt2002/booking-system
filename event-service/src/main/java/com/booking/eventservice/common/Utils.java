package com.booking.eventservice.common;

public class Utils {

    public static <T> String genEventLockKey(T type, String id) {
        return String.format("LOCK_KEY_%s_%s", type.getClass().getName(), id);
    }

    public static <T> String genEventLocalCacheKey(T type, String id) {
        return String.format("LOCAL_CACHE_%s_%s", type.getClass().getName(), id);
    }

    public static <T> String genEventDistributedCacheKey(T type, String id) {
        return String.format("DISTRIBUTED_CACHE_%s_%s", type.getClass().getName(), id);
    }
}
