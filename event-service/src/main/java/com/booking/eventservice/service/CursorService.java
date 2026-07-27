package com.booking.eventservice.service;

import com.booking.eventservice.dto.CursorDTO;

import java.time.LocalDateTime;

public interface CursorService {

    String generateCursor(String id, LocalDateTime time);

    CursorDTO parseCursor(String cursor);
}
