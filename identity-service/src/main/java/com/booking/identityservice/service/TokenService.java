package com.booking.identityservice.service;

import com.booking.identityservice.entity.Token;
import com.booking.identityservice.entity.User;

import java.util.Date;

public interface TokenService {
    int deleteByExpiryTimeBefore(Date date);
    Token createRefreshToken(String tokenId, User user);
}
