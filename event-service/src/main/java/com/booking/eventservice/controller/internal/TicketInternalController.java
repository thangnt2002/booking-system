package com.booking.eventservice.controller.internal;

import com.booking.eventservice.dto.ApiResponse;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/tickets")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TicketInternalController {

    TicketService ticketService;


    @PostMapping("/decrement/{ticketId}/{quantity}")
    public ResponseEntity<ApiResponse<Boolean>> create(@PathVariable("ticketId") String ticketId, @PathVariable("quantity") int quantity){
        boolean result = ticketService.decreaseStock(ticketId, quantity);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .code(200)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
