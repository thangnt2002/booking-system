package com.booking.orderservice.service.impl;

import com.booking.orderservice.outbox.entity.IdempotencyKey;
import com.booking.orderservice.repository.IdempotencyKeyRepository;
import com.booking.orderservice.service.IdempotencyKeyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class IdempotencyKeyServiceImpl implements IdempotencyKeyService {

    IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    public boolean tryInsert(String id) {
        try {
            IdempotencyKey key = IdempotencyKey
                    .builder()
                    .eventId(id)
                    .processedAt(LocalDateTime.now())
                    .build();
            idempotencyKeyRepository.save(key);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.error("Idempotency err for event {}", id);
            return false;
        }
    }

}
