package com.booking.orderservice.common;

public class Utils {

    public static <T> String genRequestLockKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("LOCK_KEY_%s_%s", className, id);
    }

    public static <T> String genLocalCacheKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("LOCAL_CACHE_%s_%s", className, id);
    }

    public static <T> String genDistributedCacheKey(Class<T> type, String id) {
        String className = type.getSimpleName();
        return String.format("DISTRIBUTED_CACHE_%s_%s", className, id);
    }

    public static String genDistributedTicketStockAvailableKey(String ticketId){
        return String.format("DISTRIBUTED_%s_STOCK_AVAILABLE", ticketId);
    }
}


