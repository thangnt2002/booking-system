package com.booking.identityservice.service.impl;

import com.booking.identityservice.entity.Token;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.repository.TokenRepository;
import com.booking.identityservice.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenServiceImpl implements TokenService {

    TokenRepository tokenRepository;

    @Override
    public int deleteByExpiryTimeBefore(Date date) {
         return tokenRepository.deleteByExpiryTimeBefore(new Date());
    }

    @Override
    @Transactional
    public Token createRefreshToken(String tokenId, User user) {
        Token token = Token.builder()
                .jti(tokenId)
                .isRevoked(false)
                .user(user)
                .createdAt(new Date()).build();
        return tokenRepository.save(token);
    }
}
