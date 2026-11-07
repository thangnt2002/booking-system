package com.booking.common.utils;

import java.util.UUID;

public final class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    public static <T> String genRequestLockKey(Class<T> type, UUID id) {
        return String.format("LOCK_KEY_%s_%s", type.getSimpleName(), id);
    }

    public static <T> String genLocalCacheKey(Class<T> type, UUID id) {
        return String.format("LOCAL_CACHE_%s_%s", type.getSimpleName(), id);
    }

    public static <T> String genDistributedCacheKey(Class<T> type, UUID id) {
        return String.format("DISTRIBUTED_CACHE_%s_%s", type.getSimpleName(), id);
    }

    /**
     * Available-stock counter used as the admission gate. The {ticketId} hash tag keeps the
     * counter and its hold keys in the same Redis Cluster slot so Lua scripts stay atomic.
     */
    public static String ticketStockKey(UUID ticketId) {
        return "stock:{" + ticketId + "}";
    }

    /** Per-order hold: stores the quantity taken from {@link #ticketStockKey(UUID)} by one order. */
    public static String ticketHoldKey(UUID ticketId, UUID orderId) {
        return "stock:{" + ticketId + "}:hold:" + orderId;
    }
}
