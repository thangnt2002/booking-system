package com.booking.orderservice.service;

import com.booking.orderservice.dtos.CursorDTO;

import java.time.Instant;

public interface CursorService {

    String generateCursor(String id, Instant time);

    CursorDTO parseCursor(String cursor);

}
