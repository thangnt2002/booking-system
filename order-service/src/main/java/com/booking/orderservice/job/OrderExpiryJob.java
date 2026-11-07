package com.booking.orderservice.job;

import com.booking.orderservice.config.OrderProperties;
import com.booking.orderservice.service.OrderTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Expires unpaid orders and returns their stock. Safe to run on every instance: each batch
 * claims disjoint rows with UPDLOCK/READPAST, one short transaction per batch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryJob {

    private static final int MAX_BATCHES_PER_RUN = 50;

    private final OrderTxService orderTxService;
    private final OrderProperties properties;

    @Scheduled(fixedDelayString = "${app.order.expiry-scan-interval-ms:15000}")
    public void expireOverdueOrders() {
        try {
            int total = 0;
            for (int i = 0; i < MAX_BATCHES_PER_RUN; i++) {
                int expired = orderTxService.expireBatch(Instant.now(), properties.expiryBatchSize());
                total += expired;
                if (expired < properties.expiryBatchSize()) {
                    break;
                }
            }
            if (total > 0) {
                log.info("Expired {} unpaid orders", total);
            }
        } catch (Exception e) {
            log.error("Order expiry run failed", e);
        }
    }
}
