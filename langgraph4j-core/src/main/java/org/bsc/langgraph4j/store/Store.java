package org.bsc.langgraph4j.store;

import java.util.*;

/**
 * A namespaced key-value store for long-term memory that persists across
 * threads and invocations. Intentionally kept as a thin "dumb pipe" —
 * all memory-specific intelligence (decay, merge, compression) lives
 * in middleware layers built on top of this interface.
 *
 * <p>Mirrors the contract of Python LangGraph's {@code BaseStore}:
 * <ul>
 *   <li>{@link #put} — write or overwrite an item</li>
 *   <li>{@link #get} — retrieve a single item by key</li>
 *   <li>{@link #delete} — remove an item</li>
 *   <li>{@link #search} — retrieve items by query (implementation-defined)</li>
 * </ul>
 *
 * <p>Namespaces are opaque strings that callers use to partition items
 * (e.g. {@code "user:alice:food"}). The store does not impose any
 * semantics on namespace structure.
 */
public interface Store {

    /**
     * Write or overwrite an item identified by {@code (namespace, key)}.
     *
     * @param namespace partition for the item
     * @param key       unique key within the namespace
     * @param value     the item's payload
     * @param metadata  optional metadata (may be {@code null})
     */
    void put(String namespace, String key, Object value, Map<String, Object> metadata);

    /**
     * Retrieve a single item by key within a namespace.
     *
     * @param namespace partition for the item
     * @param key       unique key within the namespace
     * @return the item, or empty if not found
     */
    Optional<Item> get(String namespace, String key);

    /**
     * Remove an item from the store. No-op if the item does not exist.
     *
     * @param namespace partition for the item
     * @param key       unique key within the namespace
     */
    void delete(String namespace, String key);

    /**
     * Search for items within a namespace. The query semantics are
     * implementation-defined — an in-memory store may use substring
     * matching, while a vector-backed store may use cosine similarity.
     *
     * @param namespace partition to search within
     * @param query     the search query
     * @param limit     maximum number of results
     * @return search results, possibly empty
     */
    SearchResult search(String namespace, String query, int limit);

    /**
     * A stored item with its payload and timestamps.
     */
    record Item(
            String key,
            Object value,
            Map<String, Object> metadata,
            long createdAt,
            long updatedAt
    ) {
        public Item {
            if (metadata == null) {
                metadata = Map.of();
            }
        }
    }

    /**
     * Result of a {@link #search} operation.
     */
    record SearchResult(List<Item> items) {
        public SearchResult {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
