package com.booking.orderservice.mapper;

import com.booking.orderservice.dto.response.OrderResponseDTO;
import com.booking.orderservice.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponseDTO toResponse(Order order);
}
