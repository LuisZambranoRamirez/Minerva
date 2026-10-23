package com.minerva.integration.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ConcurrentTestExecutor {

    private static final long TIMEOUT_SECONDS = 10;

    private final PlatformTransactionManager transactionManager;

    public ConcurrentTestExecutor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public <T> List<Outcome<T>> runPair(Callable<T> first, Callable<T> second) {
        CountDownLatch transactionsStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Outcome<T>> firstResult = executor.submit(
                    () -> execute(first, transactionsStarted, releaseWorkers));
            Future<Outcome<T>> secondResult = executor.submit(
                    () -> execute(second, transactionsStarted, releaseWorkers));

            if (!transactionsStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent workers did not start transactions in time.");
            }
            releaseWorkers.countDown();

            return List.of(
                    firstResult.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    secondResult.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Concurrent test did not finish cleanly.", exception);
        }
    }

    public <T> T readCommitted(Supplier<T> assertion) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setReadOnly(true);
        return transaction.execute(status -> assertion.get());
    }

    private <T> Outcome<T> execute(
            Callable<T> action,
            CountDownLatch transactionsStarted,
            CountDownLatch releaseWorkers
    ) {
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            T value = transaction.execute(status -> {
                transactionsStarted.countDown();
                await(releaseWorkers);
                try {
                    return action.call();
                } catch (Exception exception) {
                    throw new WorkerFailure(exception);
                }
            });
            return Outcome.success(value);
        } catch (Throwable failure) {
            Throwable cause = failure instanceof WorkerFailure && failure.getCause() != null
                    ? failure.getCause()
                    : failure;
            return Outcome.failure(cause);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent workers were not released in time.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent worker was interrupted.", exception);
        }
    }

    public record Outcome<T>(T value, Throwable failure) {
        public static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        public static <T> Outcome<T> failure(Throwable failure) {
            return new Outcome<>(null, failure);
        }

        public boolean succeeded() {
            return failure == null;
        }
    }

    private static final class WorkerFailure extends RuntimeException {
        private WorkerFailure(Throwable cause) {
            super(cause);
        }
    }
}
