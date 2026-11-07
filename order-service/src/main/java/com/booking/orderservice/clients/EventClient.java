package com.booking.orderservice.clients;

import com.booking.common.configurations.ClientAuthenRequestInterceptor;
import com.booking.common.dtos.ApiResponse;
import com.booking.orderservice.dtos.response.TicketResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

/**
 * Read-only access to event-service. Stock is never mutated over HTTP: it flows through
 * Kafka events so it survives restarts and retries without double counting.
 */
@FeignClient(name = "event-service",
        url = "${app.profile.event}",
        configuration = {ClientAuthenRequestInterceptor.class}
)
public interface EventClient {

    @GetMapping(value = "/internal/tickets/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<TicketResponseDTO> findTicketById(@PathVariable("ticketId") UUID ticketId);

    /** Loads the available-stock counter into Redis with SET NX (never overwrites a live counter). */
    @PostMapping(value = "/internal/tickets/{ticketId}/warm-cache", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<Boolean> warmStock(@PathVariable("ticketId") UUID ticketId);
}
