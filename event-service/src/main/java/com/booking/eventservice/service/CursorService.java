package com.booking.eventservice.service;

import com.booking.eventservice.dtos.CursorDTO;

import java.time.Instant;
import java.util.UUID;

public interface CursorService {

    String generateCursor(UUID id, Instant time);

    CursorDTO parseCursor(String cursor);
}
