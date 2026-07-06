package com.booking.orderservice.service.impl;

import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;
import com.booking.orderservice.outbox.entity.OutboxMessage;
import com.booking.orderservice.repository.OutboxRepository;
import com.booking.orderservice.service.OutboxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxServiceImpl implements OutboxService {

    OutboxRepository outboxRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutboxMessage save(OutboxMessage outboxMessage) {
        try {
            return outboxRepository.save(outboxMessage);
        } catch (Exception e) {
            //noti for admin to manual handle
            log.error("Failed to save to outbox for msg = {}", outboxMessage);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public List<OutboxMessage> findByStatus(OutboxStatus status) {
        return outboxRepository.findByStatus(status);
    }

    @Override
    public void markAsPublish(String outboxId) {
        outboxRepository.updateStatus(outboxId, OutboxStatus.SENT);
    }
}
