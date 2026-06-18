package com.booking.identityservice.repository;

import com.booking.identityservice.entity.InvalidateJWT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidateJWTRepository extends JpaRepository<InvalidateJWT, String> {
}
