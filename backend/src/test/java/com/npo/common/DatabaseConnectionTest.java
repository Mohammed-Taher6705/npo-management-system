package com.npo.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    void concurrentCallsReturnTheSameInstance() throws Exception {
        int callers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<DatabaseConnection>> results = new ArrayList<>();

            for (int i = 0; i < callers; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timed out");
                    }

                    return DatabaseConnection.getInstance();
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            DatabaseConnection first =
                    results.get(0).get(10, TimeUnit.SECONDS);

            assertNotNull(first);

            for (Future<DatabaseConnection> result : results) {
                assertSame(first, result.get(10, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }
}