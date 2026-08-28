package com.booking.identityservice.service;

import com.booking.identityservice.entity.Token;
import com.booking.identityservice.entity.User;

import java.util.Date;
import java.util.UUID;

public interface TokenService {
    int deleteByExpiryTimeBefore(Date date);
    Token createRefreshToken(UUID tokenId, User user, Date expiryTime);
}
