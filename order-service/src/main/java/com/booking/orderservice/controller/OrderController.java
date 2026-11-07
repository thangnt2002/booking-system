package com.booking.orderservice.controller;

import com.booking.common.dtos.ApiResponse;
import com.booking.orderservice.dtos.Page;
import com.booking.orderservice.dtos.request.PlaceOrderRequest;
import com.booking.orderservice.dtos.response.OrderResponseDTO;
import com.booking.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a PENDING order holding the tickets for the payment window.
     * Clients must send a fresh Idempotency-Key per purchase and reuse it on retries.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponseDTO order = orderService.placeOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok(HttpStatus.CREATED, order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ok(HttpStatus.OK, orderService.getMyOrder(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable UUID orderId) {
        orderService.cancelMyOrder(orderId);
        return ResponseEntity.ok(ok(HttpStatus.OK, null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> myOrders(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(ok(HttpStatus.OK, orderService.getMyOrders(cursor, limit, search)));
    }

    private static <T> ApiResponse<T> ok(HttpStatus status, T data) {
        return ApiResponse.<T>builder().success(true).code(status.value()).data(data).build();
    }
}
