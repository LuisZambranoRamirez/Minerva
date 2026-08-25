package com.minerva.application.port.driven;

public interface TransactionManager {
    void execute(Runnable action);
}
