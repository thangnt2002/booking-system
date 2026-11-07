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

    boolean reserveStock(UUID ticketId, int quantity);

    boolean releaseStock(UUID ticketId, int quantity);

    boolean decreaseStock(UUID ticketId, int quantity);

    boolean warmCache(UUID ticketId);

}
