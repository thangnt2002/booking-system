package com.booking.eventservice.service;

import com.booking.eventservice.dtos.CursorDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CursorService {

    String generateCursor(UUID id, LocalDateTime time);

    CursorDTO parseCursor(String cursor);
}
