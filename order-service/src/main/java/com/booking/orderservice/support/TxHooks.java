package com.booking.orderservice.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Side effects on non-transactional resources (Redis) that must only happen once the DB commit is durable. */
@Slf4j
public final class TxHooks {

    private TxHooks() {
    }

    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("afterCommit hook requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (Exception e) {
                    log.error("afterCommit hook failed", e);
                }
            }
        });
    }
}
