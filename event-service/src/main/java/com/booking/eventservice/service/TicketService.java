package com.booking.eventservice.service;

import com.booking.eventservice.dtos.request.TicketRequestDTO;
import com.booking.eventservice.dtos.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;

import java.util.UUID;

public interface TicketService {

    Ticket create(TicketRequestDTO request);

    Ticket findById(UUID ticketId, Long version);

    Ticket update(TicketRequestDTO request);

    Ticket softDelete(UUID ticketId);

    TicketResponseDTO findTicketById(UUID ticketId, Long version);

    /**
     * Seeds the Redis available-stock counter from the database with SET NX, so it can
     * never overwrite (and thereby inflate) a counter that is already being decremented.
     */
    boolean warmStock(UUID ticketId);

}
