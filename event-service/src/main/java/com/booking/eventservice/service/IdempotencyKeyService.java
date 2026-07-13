package com.booking.eventservice.service;

import com.booking.eventservice.entity.IdempotencyKey;

public interface IdempotencyKeyService {

    boolean tryInsert(String id);

}
