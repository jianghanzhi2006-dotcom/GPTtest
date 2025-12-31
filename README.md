# Expiring LRU Cache

This project implements a thread-safe LRU (Least Recently Used) cache with per-entry expiration in Java.

## Features
- **LRU eviction:** Fixed-capacity cache that evicts the least recently used entry when capacity is exceeded.
- **Per-entry TTL:** Each item has its own expiration time; expired items are never returned by `get`.
- **Manual cleanup:** `cleanExpired()` removes all expired entries without requiring a full scan on every operation.
- **Thread-safe O(1) get/put:** `ReentrantLock` guards internal state while hash map + doubly linked list keep operations O(1) on average.

## Project structure
- `src/main/java/com/example/cache/ExpiringLRUCache.java`: Cache implementation with pluggable time provider for testing.
- `src/test/java/com/example/cache/ExpiringLRUCacheTest.java`: Lightweight test harness using assertions and a controllable clock.

## Running the tests
Use the JDK tools directly (no external dependencies):

```bash
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.example.cache.ExpiringLRUCacheTest
```
