package com.booking.eventservice.service.impl;

import com.booking.eventservice.cache.EventCacheService;
import com.booking.eventservice.dto.cache.EventCache;
import com.booking.eventservice.dto.request.EventRequestDTO;
import com.booking.eventservice.dto.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;
import com.booking.eventservice.exception.BusinessException;
import com.booking.eventservice.exception.ErrorCode;
import com.booking.eventservice.exception.NotFoundException;
import com.booking.eventservice.mapper.EventMapper;
import com.booking.eventservice.repository.EventRepository;
import com.booking.eventservice.service.EventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    EventCacheService eventCacheService;


    @Override
    @Transactional
    public Event create(EventRequestDTO request) {
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new BusinessException(ErrorCode.INVALID_EVENT_START_TIME);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime() != null && request.getStartTime().isBefore(now)) {
            throw new BusinessException(ErrorCode.EVENT_TIME_CANNOT_BE_IN_THE_PAST);
        }

        Event event = eventMapper.toEntity(request);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return eventRepository.save(event);
    }

    @Override
    public Event findById(String id, Long version) {
        EventCache cachedEvent = getEventFromCache(id, version);
        return eventMapper.fromCacheToEntity(cachedEvent);
    }

    @Override
    public EventResponseDTO findEventById(String id, Long version){
        EventCache cachedEvent = getEventFromCache(id, version);
        EventResponseDTO response = eventMapper.toResponse(cachedEvent.getEvent());
        response.setVersion(cachedEvent.getVersion());
        return response;
    }

    @Override
    @Transactional
    public Event update(String id, EventRequestDTO requestDTO) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        eventMapper.updateEntity(requestDTO, existingEvent);
        existingEvent.setUpdatedAt(LocalDateTime.now());
        eventCacheService.invalidateEvent(id);
        //TODO invalidate in other instances
        return existingEvent;
    }

    @Override
    public Event softDelete(String id) {
        return null;
    }

    private EventCache getEventFromCache(String id, Long version){
        if(id == null){
            return null;
        }
        return eventCacheService.getEvent(id, version);
    }
}
