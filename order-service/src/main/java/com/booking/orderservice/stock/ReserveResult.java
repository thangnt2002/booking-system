package com.booking.orderservice.stock;

public enum ReserveResult {
    RESERVED,
    OUT_OF_STOCK,
    /** The counter is not in Redis yet (first request, eviction or restart). */
    NOT_WARMED,
    /** A previous attempt with the same order id already holds the stock. */
    ALREADY_HELD;

    static ReserveResult fromScript(Long code) {
        if (code == null) {
            throw new IllegalStateException("reserve_stock.lua returned null");
        }
        return switch (code.intValue()) {
            case 1 -> RESERVED;
            case 0 -> OUT_OF_STOCK;
            case -1 -> NOT_WARMED;
            case 2 -> ALREADY_HELD;
            default -> throw new IllegalStateException("Unexpected reserve_stock.lua result " + code);
        };
    }
}
