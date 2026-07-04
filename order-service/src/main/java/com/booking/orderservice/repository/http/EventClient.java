package com.booking.orderservice.repository.http;

import com.booking.orderservice.configurations.ClientAuthenRequestInterceptor;
import com.booking.orderservice.dto.ApiResponse;
import com.booking.orderservice.dto.response.TicketResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "event-service",
        url = "${app.profile.event}",
        configuration = {ClientAuthenRequestInterceptor.class}
)
public interface EventClient {

    @GetMapping(value = "/internal/tickets/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<TicketResponseDTO> findTicketById(@PathVariable("ticketId") String ticketId);

    @PostMapping(value = "/internal/tickets/decrement/{ticketId}/{quantity}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<Boolean> decreaseStock(@PathVariable("ticketId") String ticketId, @PathVariable("quantity") int quantity);

    @PostMapping(value = "/internal/tickets/increment/{ticketId}/{quantity}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<Boolean> increaseStock(@PathVariable("ticketId") String ticketId, @PathVariable("quantity") int quantity);
}
