package com.booking.eventservice.controller;

import com.booking.eventservice.dto.ApiResponse;
import com.booking.eventservice.dto.request.TicketRequestDTO;
import com.booking.eventservice.dto.response.TicketResponseDTO;
import com.booking.eventservice.entity.Ticket;
import com.booking.eventservice.mapper.TicketMapper;
import com.booking.eventservice.service.TicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketController {

    TicketService ticketService;
    TicketMapper ticketMapper;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> create(@RequestBody TicketRequestDTO request) {
        Ticket ticket = ticketService.create(request);
        TicketResponseDTO dto = ticketMapper.toResponse(ticket);
        ApiResponse<TicketResponseDTO> response = ApiResponse.<TicketResponseDTO>builder()
                .success(true)
                .code(201)
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<TicketResponseDTO>> findById(@PathVariable("id") String id,
                                                           @RequestParam(name = "version", required = false) Long version
    ){
        TicketResponseDTO dto = ticketService.findTicketById(id, version);
        ApiResponse<TicketResponseDTO> response = ApiResponse.<TicketResponseDTO>builder()
                .success(true)
                .code(200)
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
