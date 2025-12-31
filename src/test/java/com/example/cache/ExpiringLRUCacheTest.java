package com.example.cache;

import java.time.Duration;

public class ExpiringLRUCacheTest {

    public static void main(String[] args) {
        testPutAndGetBeforeExpiry();
        testExpirationOnAccess();
        testLRUEviction();
        testCleanExpiredRemovesEntries();
        testUpdateRenewsExpiration();
        System.out.println("All tests passed.");
    }

    private static void testPutAndGetBeforeExpiry() {
        AdjustableTimeProvider time = new AdjustableTimeProvider();
        ExpiringLRUCache<String, String> cache = new ExpiringLRUCache<>(2, time);
        cache.put("a", "alpha", 5);
        String value = cache.get("a");
        assertEquals("alpha", value, "Value should be returned before expiry");
        assertEquals(1, cache.size(), "Cache size should be 1");
    }

    private static void testExpirationOnAccess() {
        AdjustableTimeProvider time = new AdjustableTimeProvider();
        ExpiringLRUCache<String, String> cache = new ExpiringLRUCache<>(2, time);
        cache.put("a", "alpha", 1);
        time.advanceSeconds(2);
        String value = cache.get("a");
        assertNull(value, "Expired value should not be returned");
        assertEquals(0, cache.size(), "Expired entry should be removed from cache");
    }

    private static void testLRUEviction() {
        AdjustableTimeProvider time = new AdjustableTimeProvider();
        ExpiringLRUCache<String, String> cache = new ExpiringLRUCache<>(2, time);
        cache.put("a", "alpha", 10);
        cache.put("b", "bravo", 10);
        cache.get("a"); // make "a" most recently used
        cache.put("c", "charlie", 10); // should evict "b"
        assertNull(cache.get("b"), "Least recently used entry should be evicted");
        assertEquals("alpha", cache.get("a"), "Recently used entry should remain");
    }

    private static void testCleanExpiredRemovesEntries() {
        AdjustableTimeProvider time = new AdjustableTimeProvider();
        ExpiringLRUCache<String, String> cache = new ExpiringLRUCache<>(3, time);
        cache.put("a", "alpha", 1);
        cache.put("b", "bravo", 2);
        cache.put("c", "charlie", 3);
        time.advanceSeconds(3);
        assertEquals(3, cache.size(), "Cache retains expired entries until cleaned");
        cache.cleanExpired();
        assertEquals(0, cache.size(), "Expired entries should be removed after cleaning");
        assertNull(cache.get("c"), "Expired entry should not be accessible after cleaning");
    }

    private static void testUpdateRenewsExpiration() {
        AdjustableTimeProvider time = new AdjustableTimeProvider();
        ExpiringLRUCache<String, String> cache = new ExpiringLRUCache<>(2, time);
        cache.put("a", "alpha", 2);
        time.advanceSeconds(1);
        cache.put("a", "alpha-new", 2);
        time.advanceSeconds(1);
        assertEquals("alpha-new", cache.get("a"), "Updated value should be available before new expiry");
        time.advanceSeconds(2);
        assertNull(cache.get("a"), "Entry should expire after renewed duration");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message + " (expected null, actual: " + value + ")");
        }
    }

    private static final class AdjustableTimeProvider implements ExpiringLRUCache.TimeProvider {
        private long nowNanos = 0L;

        @Override
        public long nowNanos() {
            return nowNanos;
        }

        void advanceSeconds(long seconds) {
            nowNanos += Duration.ofSeconds(seconds).toNanos();
        }
    }
}
