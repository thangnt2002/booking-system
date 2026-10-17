package com.booking.eventservice.mapper;

import com.booking.eventservice.dtos.cache.EventCache;
import com.booking.eventservice.dtos.request.EventRequestDTO;
import com.booking.eventservice.dtos.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {
    Event toEntity (EventRequestDTO eventRequestDTO);
    EventResponseDTO toResponse (Event event);
    Event fromCacheToEntity (EventCache eventCache);
    void updateEntity(EventRequestDTO dto, @MappingTarget Event entity);
}
