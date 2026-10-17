package com.booking.eventservice.service.impl;

import com.booking.eventservice.cache.EventCacheService;
import com.booking.eventservice.dtos.CursorDTO;
import com.booking.eventservice.dtos.Page;
import com.booking.eventservice.dtos.cache.EventCache;
import com.booking.eventservice.dtos.request.EventRequestDTO;
import com.booking.eventservice.dtos.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.eventservice.mapper.EventMapper;
import com.booking.eventservice.repository.EventRepository;
import com.booking.eventservice.service.CursorService;
import com.booking.eventservice.service.EventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    EventCacheService eventCacheService;
    CursorService cursorService;

    @Override
    @Transactional
    public Event create(EventRequestDTO request) {
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new BusinessException(ErrorCode.INVALID_START_TIME);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime() != null && request.getStartTime().isBefore(now)) {
            throw new BusinessException(ErrorCode.START_TIME_CANNOT_BE_IN_THE_PAST);
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
    public EventResponseDTO findEventById(String id, Long version) {
        EventCache cachedEvent = getEventFromCache(id, version);
        EventResponseDTO response = eventMapper.toResponse(cachedEvent.getEvent());
        response.setVersion(cachedEvent.getVersion());
        return response;
    }

    @Override
    public Page<EventResponseDTO> getCusrorPage(String cursor, int limit, String searchKey) {
        int safeLimit = Math.min(limit, 50) + 1;
        CursorDTO cursorDTO = cursorService.parseCursor(cursor);
        List<Event> events = eventRepository.findCursorPage(cursorDTO.getId(), cursorDTO.getDate(), safeLimit, searchKey);
        if (events.isEmpty()) {
            return Page.<EventResponseDTO>builder()
                    .data(List.of())
                    .build();
        }

        boolean hasNext = false;
        String nextCursor = StringUtils.EMPTY;
        if (events.size() == safeLimit) {
            hasNext = true;
            events.removeLast();
            Event lastEvent = events.getLast();
            nextCursor = cursorService.generateCursor(lastEvent.getId(), lastEvent.getCreatedAt());
        }
        List<EventResponseDTO> response = events.stream().map(eventMapper::toResponse).toList();

        return Page.<EventResponseDTO>builder()
                .hasNext(hasNext)
                .cursor(nextCursor)
                .data(response)
                .build();
    }

    @Override
    public List<Event> getList() {
        return eventRepository.findAll();
    }

    @Override
    @Transactional
    public Event update(String id, EventRequestDTO requestDTO) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        eventMapper.updateEntity(requestDTO, existingEvent);
        existingEvent.setUpdatedAt(LocalDateTime.now());
        eventCacheService.invalidateEvent(id);
        eventCacheService.buildCache(existingEvent);
        return existingEvent;
    }

    @Override
    public Event softDelete(String id) {
        return null;
    }

    private EventCache getEventFromCache(String id, Long version) {
        if (id == null) {
            return null;
        }
        return eventCacheService.getEvent(id, version);
    }
}
