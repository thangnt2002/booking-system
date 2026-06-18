package com.booking.identityservice.service;

public interface InvalidateJWTService {
    void logout(String token);
}
