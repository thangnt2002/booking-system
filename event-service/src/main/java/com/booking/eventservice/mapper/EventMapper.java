package com.booking.eventservice.mapper;

import com.booking.eventservice.dto.cache.EventCache;
import com.booking.eventservice.dto.request.EventRequestDTO;
import com.booking.eventservice.dto.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {
    Event toEntity (EventRequestDTO eventRequestDTO);
    EventResponseDTO toResponse (Event event);
    Event fromCacheToEntity (EventCache eventCache);
    EventResponseDTO fromCacheToResponse (EventCache eventCache);
}
