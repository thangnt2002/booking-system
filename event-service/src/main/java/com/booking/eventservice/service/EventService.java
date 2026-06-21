package com.booking.eventservice.service;

import com.booking.eventservice.dto.request.TicketRequestDTO;
import com.booking.eventservice.entity.Ticket;

public interface EventService {

    Ticket create(TicketRequestDTO request);

    Ticket findById(String ticketId);

    Ticket update(TicketRequestDTO request);

    Ticket softDelete(String ticketId);
}
