package com.booking.eventservice.repository;

import com.booking.eventservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    boolean existsByEventId(String eventId);
}
