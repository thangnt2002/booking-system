package com.booking.eventservice.repository;

import com.booking.eventservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    boolean existsByEventId(UUID eventId);
}
