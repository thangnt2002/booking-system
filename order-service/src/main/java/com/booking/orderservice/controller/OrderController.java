package com.booking.orderservice.controller;


import com.booking.orderservice.dto.ApiResponse;
import com.booking.orderservice.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping("/{ticketId}/{quantity}")
    public ResponseEntity<ApiResponse<Void>> order(@PathVariable("ticketId") String ticketId,
                                                   @PathVariable("quantity") int quantity ){
        orderService.order(ticketId, quantity);
        return ResponseEntity.ok(ApiResponse.<Void>builder().code(200).success(true).build());
    }

    @PostMapping("/cancel/{orderNumber}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable("orderNumber") String orderNumber){
        boolean response = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.<Void>builder().code(200).success(response).build());
    }

}
