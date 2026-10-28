package com.booking.orderservice.repository;

import com.booking.orderservice.entities.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    boolean existsByEventId(String eventId);
}