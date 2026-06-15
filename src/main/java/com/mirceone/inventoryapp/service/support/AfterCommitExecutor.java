package com.mirceone.inventoryapp.service.support;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AfterCommitExecutor {

    public void execute(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    public void executeQuietly(String actionName, Logger log, Runnable action) {
        execute(() -> {
            try {
                action.run();
            } catch (RuntimeException ex) {
                log.error("After-commit action failed action={}: {}", actionName, ex.getMessage(), ex);
            }
        });
    }
}
