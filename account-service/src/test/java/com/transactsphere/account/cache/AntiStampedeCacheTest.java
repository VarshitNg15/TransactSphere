package com.transactsphere.account.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntiStampedeCacheTest {

    private AntiStampedeCache cache;

    @BeforeEach
    void setUp() {
        cache = new AntiStampedeCache();
    }

    @Test
    void testSingleFlightPreventsStampede() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger databaseCallCount = new AtomicInteger(0);

        // Simulated slow database loader
        Supplier<String> slowDbLoader = () -> {
            databaseCallCount.incrementAndGet();
            try {
                Thread.sleep(500); // Simulate slow DB call
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ValueFromDB";
        };

        // Fire 100 concurrent requests at exactly the same time
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // Wait for the start signal
                    String value = cache.get("testKey", 60000, slowDbLoader);
                    assertEquals("ValueFromDB", value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Start all threads simultaneously
        doneLatch.await(); // Wait for all threads to finish
        
        executor.shutdown();

        // The assertion that proves the anti-stampede works:
        // Out of 100 concurrent requests, the database was hit EXACTLY ONCE
        assertEquals(1, databaseCallCount.get(), "Database should only be hit once due to SingleFlight!");
    }
}
