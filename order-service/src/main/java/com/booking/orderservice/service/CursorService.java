package com.booking.orderservice.service;

import com.booking.orderservice.dto.CursorDTO;

import java.time.LocalDateTime;

public interface CursorService {

    String generateCursor(String id, LocalDateTime time);

    CursorDTO parseCursor(String cursor);

}
