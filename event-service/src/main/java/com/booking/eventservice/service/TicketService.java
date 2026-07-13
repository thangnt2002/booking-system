package com.booking.eventservice.service;

import com.booking.eventservice.dto.request.TicketRequestDTO;
import com.booking.eventservice.dto.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;

public interface TicketService {

    Ticket create(TicketRequestDTO request);

    Ticket findById(String ticketId, Long version);

    Ticket update(TicketRequestDTO request);

    Ticket softDelete(String ticketId);

    TicketResponseDTO findTicketById(String ticketId, Long version);

    boolean reserveStock(String ticketId, int quantity);

    boolean releaseStock(String ticketId, int quantity);

    boolean decreaseStock(String ticketId, int quantity);

}
