package com.github.matei.sentinel.client;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory cache with time-based expiration.
 * <p>
 * This cache stores key-value pairs with a configurable Time-To-Live (TTL).
 * Entries are considered expired after the TTL duration has passed since they
 * were added to the cache.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Generic key-value storage</li>
 *   <li>Automatic expiration based on TTL</li>
 *   <li>Lazy eviction (expired entries removed on access)</li>
 *   <li>Thread-safe for single-threaded use</li>
 * </ul>
 *
 * <h2>Design Trade-offs</h2>
 * <ul>
 *   <li><b>Lazy eviction</b>: Expired entries remain in memory until accessed.
 *       This is acceptable for small caches with low entry count.</li>
 *   <li><b>No automatic cleanup</b>: No background thread removes expired entries.
 *       For production use with large caches, consider adding scheduled cleanup.</li>
 *   <li><b>Not thread-safe</b>: Designed for single-threaded polling use case.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SimpleCache<String, String> cache = new SimpleCache<>(Duration.ofSeconds(10));
 *
 * // Add entry
 * cache.put("key1", "value1");
 *
 * // Retrieve within TTL
 * String value = cache.get("key1"); // Returns "value1"
 *
 * // Wait 11 seconds
 * Thread.sleep(11000);
 *
 * // Retrieve after TTL
 * String expired = cache.get("key1"); // Returns null
 * }</pre>
 *
 * @param <K> the type of keys in this cache
 * @param <V> the type of values in this cache
 * @since 1.0
 * @see GitHubApiClientImpl
 */
class SimpleCache<K, V>
{
    /**
     * Time-To-Live duration for cache entries.
     * Entries expire this duration after being added.
     */
    private final Duration ttl;

    /**
     * Internal storage for cache entries with expiration metadata.
     */
    private final Map<K, CacheEntry<V>> cache = new HashMap<>();

    /**
     * Creates a new cache with the specified TTL.
     *
     * @param ttl the time-to-live for cache entries; must not be null or negative
     * @throws IllegalArgumentException if ttl is null or negative
     */
    public SimpleCache(Duration ttl)
    {
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL must be non-null and non-negative");
        }
        this.ttl = ttl;
    }

    /**
     * Retrieves a value from the cache.
     * <p>
     * If the entry has expired, it is treated as if it doesn't exist and
     * {@code null} is returned. The expired entry remains in memory until
     * explicitly removed or overwritten.
     * </p>
     *
     * @param key the key whose associated value is to be returned
     * @return the cached value, or {@code null} if the key is not present or has expired
     */
    public V get(K key)
    {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired())
        {
            return null;
        }

        return entry.value;
    }

    /**
     * Stores a key-value pair in the cache with the configured TTL.
     * <p>
     * If the key already exists, its value and expiration time are updated.
     * The entry will expire {@code ttl} duration after this method is called.
     * </p>
     *
     * @param key the key with which the value is to be associated
     * @param value the value to be associated with the key
     */
    public void put(K key, V value)
    {
        cache.put(key, new CacheEntry<>(value, Instant.now().plus(ttl)));
    }

    /**
     * Removes all entries from the cache.
     * <p>
     * This method clears both expired and non-expired entries. After calling
     * this method, the cache will be empty.
     * </p>
     */
    public void clear()
    {
        cache.clear();
    }

    /**
     * Internal storage class for cache entries with expiration metadata.
     * <p>
     * This class is immutable and package-private as it's only used internally
     * by {@link SimpleCache}.
     * </p>
     *
     * @param <V> the type of the cached value
     */
    private static class CacheEntry<V>
    {
        /**
         * The cached value.
         */
        final V value;

        /**
         * The instant at which this entry expires.
         */
        final Instant expiresAt;

        /**
         * Creates a new cache entry.
         *
         * @param value the value to cache
         * @param expiresAt the expiration timestamp
         */
        CacheEntry(V value, Instant expiresAt)
        {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        /**
         * Checks if this entry has expired.
         *
         * @return {@code true} if the current time is after the expiration time
         */
        boolean isExpired()
        {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
