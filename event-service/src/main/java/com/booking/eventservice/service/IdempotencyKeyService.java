package com.booking.eventservice.service;

import com.booking.eventservice.entity.IdempotencyKey;

import java.util.UUID;

public interface IdempotencyKeyService {

    boolean tryInsert(UUID id);

}
