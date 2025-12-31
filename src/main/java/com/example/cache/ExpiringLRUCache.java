package com.example.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU cache with per-entry expiration.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ExpiringLRUCache<K, V> {
    private final Map<K, Node<K, V>> map;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final TimeProvider timeProvider;

    private final Node<K, V> head;
    private final Node<K, V> tail;

    /**
     * Create a cache with the given capacity using system time.
     */
    public ExpiringLRUCache(int capacity) {
        this(capacity, new SystemTimeProvider());
    }

    /**
     * Create a cache with the given capacity and custom time provider (useful for testing).
     */
    public ExpiringLRUCache(int capacity, TimeProvider timeProvider) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.map = new HashMap<>(capacity * 2);
        this.head = new Node<>(null, null, 0L);
        this.tail = new Node<>(null, null, 0L);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Retrieve a value by key if present and not expired.
     *
     * @param key cache key
     * @return value when available; otherwise {@code null}
     */
    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                return null;
            }
            if (isExpired(node)) {
                removeNode(node);
                map.remove(key);
                return null;
            }
            moveToFront(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Put a value into the cache with an expiration delay.
     *
     * @param key            cache key
     * @param value          value to store
     * @param expireSeconds  seconds before the entry expires
     */
    public void put(K key, V value, long expireSeconds) {
        if (expireSeconds < 0) {
            throw new IllegalArgumentException("expireSeconds must be non-negative");
        }
        long expireAt = timeProvider.nowNanos() + toNanos(expireSeconds);
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node != null) {
                node.value = value;
                node.expireAtNanos = expireAt;
                moveToFront(node);
                return;
            }
            Node<K, V> newNode = new Node<>(key, value, expireAt);
            map.put(key, newNode);
            addToFront(newNode);
            if (map.size() > capacity) {
                evictLeastRecentlyUsed();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove all expired entries.
     */
    public void cleanExpired() {
        lock.lock();
        try {
            Node<K, V> current = head.next;
            while (current != tail) {
                Node<K, V> next = current.next;
                if (isExpired(current)) {
                    removeNode(current);
                    map.remove(current.key);
                }
                current = next;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Current number of items in the cache (includes expired items until they are accessed or cleaned).
     */
    public int size() {
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    private void evictLeastRecentlyUsed() {
        Node<K, V> lru = tail.prev;
        if (lru != null && lru != head) {
            removeNode(lru);
            map.remove(lru.key);
        }
    }

    private void moveToFront(Node<K, V> node) {
        removeNode(node);
        addToFront(node);
    }

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        Node<K, V> prevNode = node.prev;
        Node<K, V> nextNode = node.next;
        if (prevNode != null) {
            prevNode.next = nextNode;
        }
        if (nextNode != null) {
            nextNode.prev = prevNode;
        }
        node.prev = null;
        node.next = null;
    }

    private boolean isExpired(Node<K, V> node) {
        return timeProvider.nowNanos() >= node.expireAtNanos;
    }

    private long toNanos(long expireSeconds) {
        return Duration.ofSeconds(expireSeconds).toNanos();
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private long expireAtNanos;
        private Node<K, V> prev;
        private Node<K, V> next;

        private Node(K key, V value, long expireAtNanos) {
            this.key = key;
            this.value = value;
            this.expireAtNanos = expireAtNanos;
        }
    }

    /**
     * Abstraction to provide time in nanoseconds for testing and production.
     */
    public interface TimeProvider {
        long nowNanos();
    }

    private static final class SystemTimeProvider implements TimeProvider {
        @Override
        public long nowNanos() {
            return System.nanoTime();
        }
    }
}
