package com.booking.orderservice.enums;

import com.booking.orderservice.entity.Order;
import com.booking.orderservice.event.TicketStockEvent;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;

import java.util.Arrays;

public enum AggregateTypeMap {
    TICKET_RESTOCK("TicketStockEvent", TicketStockEvent.class),
    ORDER_CREATED("Order", Order.class);

    String type;
    Class<?> clazz;

    AggregateTypeMap(String type, Class<?> clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    public static Class<?> toClass(String targetType) {
        return Arrays.stream(values())
                .filter(typeMap -> typeMap.type.equals(targetType))
                .map(typeMap -> typeMap.clazz)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVER_ERROR));
    }


}
