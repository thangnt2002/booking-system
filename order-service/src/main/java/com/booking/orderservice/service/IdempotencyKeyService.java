package com.booking.orderservice.service;

public interface IdempotencyKeyService {

    boolean tryInsert(String id);
}


