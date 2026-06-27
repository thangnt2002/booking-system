package com.booking.eventservice.mapper;

import com.booking.eventservice.dto.cache.TicketCache;
import com.booking.eventservice.dto.request.TicketRequestDTO;
import com.booking.eventservice.dto.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    Ticket toEntity(TicketRequestDTO requestDTO);
    TicketResponseDTO toResponse(Ticket ticket);
    Ticket fromCacheToEntity(TicketCache ticketCache);
}
