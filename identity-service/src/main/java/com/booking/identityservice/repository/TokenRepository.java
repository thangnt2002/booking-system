package com.booking.identityservice.repository;

import com.booking.identityservice.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {
    int deleteByExpiryTimeBefore(Date expiryTime);

    Optional<Token> findByJti(String jti);
}
