package com.booking.orderservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID ticketId,
        @NotNull @Min(1) Integer quantity) {
}
