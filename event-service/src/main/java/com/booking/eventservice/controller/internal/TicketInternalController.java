package com.booking.eventservice.controller.internal;

import com.booking.common.dtos.ApiResponse;
import com.booking.eventservice.dtos.response.TicketResponseDTO;
import com.booking.eventservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only service-to-service API. Stock changes arrive exclusively through Kafka
 * order events (see OrderEventsConsumer), never through synchronous HTTP calls.
 */
@RestController
@RequestMapping("/internal/tickets")
@RequiredArgsConstructor
public class TicketInternalController {

    private final TicketService ticketService;

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> findById(
            @PathVariable("ticketId") UUID ticketId,
            @RequestParam(name = "version", required = false) Long version) {
        return ResponseEntity.ok(ok(ticketService.findTicketById(ticketId, version)));
    }

    @PostMapping("/{ticketId}/warm-cache")
    public ResponseEntity<ApiResponse<Boolean>> warmStock(@PathVariable("ticketId") UUID ticketId) {
        return ResponseEntity.ok(ok(ticketService.warmStock(ticketId)));
    }

    private static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).code(200).data(data).build();
    }
}
