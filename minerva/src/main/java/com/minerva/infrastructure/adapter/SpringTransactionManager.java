package com.minerva.application.port.driven;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SpringTransactionManager implements TransactionManager {

    @Transactional
    @Override
    public void execute(Runnable action) {
        action.run();
    }
}
