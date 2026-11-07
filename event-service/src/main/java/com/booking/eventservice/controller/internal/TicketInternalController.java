package com.booking.eventservice.controller.internal;

import com.booking.common.dtos.ApiResponse;
import com.booking.eventservice.dtos.response.TicketResponseDTO;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/tickets")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TicketInternalController {

    TicketService ticketService;


    @PostMapping("/{ticketId}/{quantity}/reserve")
    public ResponseEntity<ApiResponse<Boolean>> reserveStock(@PathVariable("ticketId") UUID ticketId, @PathVariable("quantity") int quantity){
        boolean result = ticketService.reserveStock(ticketId, quantity);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .code(200)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{ticketId}/{quantity}/release")
    public ResponseEntity<ApiResponse<Boolean>> releaseStock(@PathVariable("ticketId") UUID ticketId, @PathVariable("quantity") int quantity){
        boolean result = ticketService.releaseStock(ticketId, quantity);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .code(200)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{ticketId}/{quantity}/decrease")
    public ResponseEntity<ApiResponse<Boolean>> decreaseStock(@PathVariable("ticketId") UUID ticketId, @PathVariable("quantity") int quantity){
        boolean result = ticketService.decreaseStock(ticketId, quantity);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .code(200)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{ticketId}")
    ResponseEntity<ApiResponse<TicketResponseDTO>> findById(@PathVariable("ticketId") UUID ticketId,
                                                            @RequestParam(name = "version", required = false) Long version
    ){
        TicketResponseDTO dto = ticketService.findTicketById(ticketId, version);
        ApiResponse<TicketResponseDTO> response = ApiResponse.<TicketResponseDTO>builder()
                .success(true)
                .code(200)
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{ticketId}/{quantity}/decrease")
    public ResponseEntity<ApiResponse<Boolean>> warmCache(@PathVariable("ticketId") UUID ticketId){
        boolean result = ticketService.warmCache(ticketId);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .code(200)
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
