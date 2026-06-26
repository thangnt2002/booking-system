package com.booking.eventservice.service;

import com.booking.eventservice.dto.request.EventRequestDTO;
import com.booking.eventservice.dto.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;

public interface EventService {

    Event create (EventRequestDTO event);

    Event findById (String id, Long version);

    Event update (String id, EventRequestDTO event);

    Event softDelete (String id);

    EventResponseDTO findEventById(String id, Long version);

}
