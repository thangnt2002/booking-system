package com.booking.eventservice.service;

import com.booking.eventservice.dtos.Page;
import com.booking.eventservice.dtos.request.EventRequestDTO;
import com.booking.eventservice.dtos.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;

import java.util.List;

public interface EventService {

    Event create (EventRequestDTO event);

    Event findById (String id, Long version);

    Event update (String id, EventRequestDTO event);

    Event softDelete (String id);

    EventResponseDTO findEventById(String id, Long version);

    Page<EventResponseDTO> getCusrorPage(String cursor, int limit, String searchKey);

    List<Event> getList();

}
