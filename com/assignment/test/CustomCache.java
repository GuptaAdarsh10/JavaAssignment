package com.assignment.test;

import java.util.Map;
import java.util.concurrent.*;

public class CustomCache<K, V> {
    private final ConcurrentHashMap<K, CacheValue<V>> cacheMap;
    private final ScheduledExecutorService scheduler;

    public CustomCache(long cleanupIntervalInMillis) {
        cacheMap = new ConcurrentHashMap<>();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask(cleanupIntervalInMillis);
    }

    public void put(K key, V value, long ttlInMillis) {
        long expiryTime = System.currentTimeMillis() + ttlInMillis;
        cacheMap.put(key, new CacheValue<>(value, expiryTime));
    }

    public V get(K key) {
        CacheValue<V> cacheValue = cacheMap.get(key);
        if (cacheValue == null || cacheValue.isExpired()) {
            cacheMap.remove(key);
            return null;
        }
        return cacheValue.value;
    }

    private void startCleanupTask(long cleanupIntervalInMillis) {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<K, CacheValue<V>> entry : cacheMap.entrySet()) {
                if (entry.getValue().isExpired()) {
                    cacheMap.remove(entry.getKey());
                }
            }
        }, cleanupIntervalInMillis, cleanupIntervalInMillis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private static class CacheValue<V> {
        private final V value;
        private final long expiryTime;

        CacheValue(V value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CustomCache<String, String> cache = new CustomCache<>(5000);

        cache.put("key1", "value1", 3000);
        System.out.println("Get key1: " + cache.get("key1"));

        Thread.sleep(4000);
        System.out.println("Get key1 after expiration: " + cache.get("key1"));
        cache.put("key2", "value2", 10000);
        System.out.println("Get key2: " + cache.get("key2"));

        cache.shutdown();
    }
}
