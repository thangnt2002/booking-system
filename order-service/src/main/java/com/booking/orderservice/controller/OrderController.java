package com.booking.orderservice.controller;


import com.booking.orderservice.dto.ApiResponse;
import com.booking.orderservice.dto.CursorDTO;
import com.booking.orderservice.dto.Page;
import com.booking.orderservice.dto.response.OrderResponseDTO;
import com.booking.orderservice.repository.OrderRepository;
import com.booking.orderservice.service.CursorService;
import com.booking.orderservice.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping("/{ticketId}/{quantity}")
    public ResponseEntity<ApiResponse<Void>> order(@PathVariable("ticketId") String ticketId,
                                                   @PathVariable("quantity") int quantity) {
        orderService.order(ticketId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).
                body(ApiResponse.<Void>builder().code(201).success(true).build());
    }

    @PostMapping("/cancel/{orderNumber}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable("orderNumber") String orderNumber) {
        boolean response = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.<Void>builder().code(200).success(response).build());
    }

    @GetMapping("/page/{userId}")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> pageOrderByUser(
            @PathVariable("userId") String userId,
            @RequestParam(value = "table", required = true) String table,
            @RequestParam(value = "cursor", defaultValue = "") String cursor,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", defaultValue = "") String search
    ) {
         Page<OrderResponseDTO> page = orderService.getPage(userId, table, cursor, limit, search);
         ApiResponse<Page<OrderResponseDTO>> response = ApiResponse.<Page<OrderResponseDTO>>builder()
                 .success(true)
                 .code(200)
                 .data(page)
                 .build();
         return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
