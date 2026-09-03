package org.bsc.langgraph4j.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link Store} backed by nested
 * {@link ConcurrentHashMap}s. Search uses case-insensitive substring
 * matching on the item's string value. No persistence — data is lost
 * when the process exits.
 *
 * <p>Thread-safe: multiple threads can read and write concurrently.
 */
public class InMemoryStore implements Store {

    private final Map<String, Map<String, Item>> data = new ConcurrentHashMap<>();

    @Override
    public void put(String namespace, String key, Object value, Map<String, Object> metadata) {
        long now = System.currentTimeMillis();
        Map<String, Item> bucket = data.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
        Item existing = bucket.get(key);
        long createdAt = (existing != null) ? existing.createdAt() : now;

        Item item = new Item(
                key,
                value,
                metadata != null ? new HashMap<>(metadata) : Map.of(),
                createdAt,
                now
        );
        bucket.put(key, item);
    }

    @Override
    public Optional<Item> get(String namespace, String key) {
        Map<String, Item> bucket = data.get(namespace);
        if (bucket == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bucket.get(key));
    }

    @Override
    public void delete(String namespace, String key) {
        Map<String, Item> bucket = data.get(namespace);
        if (bucket != null) {
            bucket.remove(key);
        }
    }

    @Override
    public SearchResult search(String namespace, String query, int limit) {
        if (limit <= 0) {
            return new SearchResult(List.of());
        }

        Map<String, Item> bucket = data.get(namespace);
        if (bucket == null || bucket.isEmpty()) {
            return new SearchResult(List.of());
        }

        String q = (query != null) ? query.toLowerCase() : "";

        // Case-insensitive substring match on the item's string value.
        List<Item> results = bucket.values().stream()
                .filter(item -> {
                    String val = String.valueOf(item.value()).toLowerCase();
                    return val.contains(q);
                })
                .sorted(Comparator.comparingLong(Item::updatedAt).reversed())
                .limit(limit)
                .toList();

        return new SearchResult(results);
    }
}
