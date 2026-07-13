package com.booking.eventservice.service.impl;

import com.booking.eventservice.entity.IdempotencyKey;
import com.booking.eventservice.repository.IdempotencyKeyRepository;
import com.booking.eventservice.service.IdempotencyKeyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
