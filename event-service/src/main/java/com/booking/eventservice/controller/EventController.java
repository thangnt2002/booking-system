package com.booking.eventservice.controller;

import com.booking.eventservice.dto.ApiResponse;
import com.booking.eventservice.dto.request.EventRequestDTO;
import com.booking.eventservice.dto.response.EventResponseDTO;
import com.booking.eventservice.entity.Event;
import com.booking.eventservice.mapper.EventMapper;
import com.booking.eventservice.service.EventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventController {

    EventService eventService;
    EventMapper eventMapper;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<EventResponseDTO>> create(@RequestBody EventRequestDTO request){
        Event x = eventService.create(request);
        EventResponseDTO event = eventMapper.toResponse(x);
        ApiResponse<EventResponseDTO> response = ApiResponse.<EventResponseDTO>builder()
                .success(true)
                .code(201)
                .data(event)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<EventResponseDTO>> findById(@PathVariable("id") String id,
        @RequestParam(name = "version", required = false) Long version
    ){
        EventResponseDTO dto = eventService.findEventById(id, version);
        ApiResponse<EventResponseDTO> response = ApiResponse.<EventResponseDTO>builder()
                .success(true)
                .code(200)
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{id}")
    ResponseEntity<ApiResponse<EventResponseDTO>> update(@PathVariable("id") String id,
                                                         @RequestBody EventRequestDTO eventRequestDTO
    ){
        Event event = eventService.update(id, eventRequestDTO);
        EventResponseDTO dto = eventMapper.toResponse(event);
        ApiResponse<EventResponseDTO> response = ApiResponse.<EventResponseDTO>builder()
                .success(true)
                .code(200)
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
