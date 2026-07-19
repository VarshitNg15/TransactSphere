package com.transactsphere.account.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Component
public class AntiStampedeCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // SingleFlight mechanism to ensure only one thread recomputes a specific key
    private final Map<String, CompletableFuture<Object>> inFlightComputations = new ConcurrentHashMap<>();
    
    // Executor for background refreshes
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(10);
    private final Random random = new Random();

    // Tuning parameter for early expiration probability
    private static final double BETA = 1.0;

    /**
     * @param key The unique cache key
     * @param ttlMillis Time To Live in milliseconds
     * @param dbLoader The function that hits the database
     * @param <V> The type of the value
     * @return The cached or freshly loaded value
     */
    @SuppressWarnings("unchecked")
    public <V> V get(String key, long ttlMillis, Supplier<V> dbLoader) {
        CacheEntry entry = cache.get(key);
        long now = System.currentTimeMillis();

        // 1. Cache Miss or hard expiration (Synchronous Load with SingleFlight)
        if (entry == null || now >= entry.getHardExpirationTime()) {
            return (V) loadSynchronously(key, ttlMillis, dbLoader);
        }

        // 2. Probabilistic Early Expiration Check (XFetch algorithm)
        double logRand = Math.log(random.nextDouble());
        long probabilisticExpiryGap = (long) (-entry.getComputeTimeMillis() * BETA * logRand);

        if (now >= entry.getHardExpirationTime() - probabilisticExpiryGap) {
            // Time to refresh early! Fire a background async refresh, but return the stale data.
            triggerBackgroundRefresh(key, ttlMillis, dbLoader);
        }

        return (V) entry.getValue();
    }

    /**
     * Evicts a key from the cache.
     */
    public void evict(String key) {
        cache.remove(key);
    }

    private Object loadSynchronously(String key, long ttlMillis, Supplier<?> dbLoader) {
        CompletableFuture<Object> future = inFlightComputations.computeIfAbsent(key, k -> {
            return CompletableFuture.supplyAsync(() -> executeAndCache(key, ttlMillis, dbLoader), backgroundExecutor);
        });

        try {
            return future.get(); // Wait for the DB call
        } catch (Exception e) {
            inFlightComputations.remove(key);
            throw new RuntimeException("Database load failed for key: " + key, e);
        }
    }

    private void triggerBackgroundRefresh(String key, long ttlMillis, Supplier<?> dbLoader) {
        inFlightComputations.computeIfAbsent(key, k -> {
            return CompletableFuture.supplyAsync(() -> executeAndCache(key, ttlMillis, dbLoader), backgroundExecutor);
        });
    }

    private Object executeAndCache(String key, long ttlMillis, Supplier<?> dbLoader) {
        long start = System.currentTimeMillis();
        try {
            Object value = dbLoader.get();
            long computeTime = System.currentTimeMillis() - start;

            CacheEntry newEntry = new CacheEntry(value, System.currentTimeMillis() + ttlMillis, computeTime);
            cache.put(key, newEntry);

            return value;
        } finally {
            inFlightComputations.remove(key);
        }
    }

    private static class CacheEntry {
        private final Object value;
        private final long hardExpirationTime;
        private final long computeTimeMillis;

        public CacheEntry(Object value, long hardExpirationTime, long computeTimeMillis) {
            this.value = value;
            this.hardExpirationTime = hardExpirationTime;
            this.computeTimeMillis = Math.max(1, computeTimeMillis);
        }

        public Object getValue() { return value; }
        public long getHardExpirationTime() { return hardExpirationTime; }
        public long getComputeTimeMillis() { return computeTimeMillis; }
    }
}
