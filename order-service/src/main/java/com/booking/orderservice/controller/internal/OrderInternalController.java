package com.booking.orderservice.controller.internal;

import com.booking.common.dtos.ApiResponse;
import com.booking.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Service-to-service API (protected by the internal key filter chain on /internal/**). */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    /** Hook for the payment service: idempotent, only succeeds while the order is PENDING and not expired. */
    @PostMapping("/{orderId}/paid")
    public ResponseEntity<ApiResponse<Void>> markPaid(@PathVariable UUID orderId) {
        orderService.confirmPayment(orderId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).code(200).build());
    }
}
