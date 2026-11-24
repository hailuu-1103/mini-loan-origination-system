package vn.hailuu.mini_los.loan.application;

import vn.hailuu.mini_los.loan.domain.LoanApplication;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Fires two decisions on the same application at (as near as possible) the same instant.
 * Both workers park on a shared start latch; the main thread opens it only after both are ready.
 */
final class TwoOfficerRace implements AutoCloseable {
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    Outcome run(Callable<LoanApplication> officerA, Callable<LoanApplication> officerB) throws InterruptedException {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        Future<LoanApplication> a = pool.submit(gated(officerA, ready, start));
        Future<LoanApplication> b = pool.submit(gated(officerB, ready, start));

        ready.await();
        start.countDown();
        return new Outcome(Attempt.of(a), Attempt.of(b));
    }

    private static Callable<LoanApplication> gated(Callable<LoanApplication> decision,
                                                   CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return decision.call();
        };
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }

    record Outcome(Attempt a, Attempt b) {
        boolean bothSucceeded() {
            return a.succeeded() && b.succeeded();
        }

        boolean exactlyOneSucceeded() {
            return a.succeeded() ^ b.succeeded();
        }
    }

    record Attempt(LoanApplication result, Throwable error) {
        boolean succeeded() {
            return error == null && result != null;
        }

        static Attempt of(Future<LoanApplication> future) throws InterruptedException {
            try {
                return new Attempt(future.get(), null);
            } catch (ExecutionException e) {
                return new Attempt(null, e.getCause());
            }
        }
    }
}
