package org.bsc.langgraph4j.store;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StoreTest {

    private final Store store = new InMemoryStore();

    @Test
    public void putAndGet() {
        store.put("user:alice", "food", "I like spicy food", Map.of("source", "chat"));

        var item = store.get("user:alice", "food");

        assertTrue(item.isPresent());
        assertEquals("I like spicy food", item.get().value());
        assertEquals("chat", item.get().metadata().get("source"));
        assertTrue(item.get().createdAt() > 0);
        assertTrue(item.get().updatedAt() >= item.get().createdAt());
    }

    @Test
    public void getMissingReturnsEmpty() {
        var item = store.get("user:bob", "nonexistent");
        assertTrue(item.isEmpty());
    }

    @Test
    public void putOverwritesAndPreservesCreatedAt() throws InterruptedException {
        store.put("ns", "k", "original", null);
        var first = store.get("ns", "k").orElseThrow();

        Thread.sleep(5); // ensure timestamp differs

        store.put("ns", "k", "updated", Map.of("v", 2));
        var second = store.get("ns", "k").orElseThrow();

        assertEquals("updated", second.value());
        assertEquals(first.createdAt(), second.createdAt());
        assertTrue(second.updatedAt() > first.updatedAt());
    }

    @Test
    public void delete() {
        store.put("ns", "k", "value", null);
        store.delete("ns", "k");

        assertTrue(store.get("ns", "k").isEmpty());
    }

    @Test
    public void deleteMissingIsNoOp() {
        assertDoesNotThrow(() -> store.delete("ns", "nonexistent"));
    }

    @Test
    public void searchBySubstring() {
        store.put("user:alice", "m1", "I love spicy food", null);
        store.put("user:alice", "m2", "I prefer mild food", null);
        store.put("user:alice", "m3", "The weather is nice", null);

        var results = store.search("user:alice", "food", 10);

        assertEquals(2, results.items().size());
    }

    @Test
    public void searchIsCaseInsensitive() {
        store.put("ns", "k1", "Spicy FOOD", null);

        var results = store.search("ns", "spicy food", 10);

        assertEquals(1, results.items().size());
    }

    @Test
    public void searchRespectsLimit() {
        store.put("ns", "k1", "food", null);
        store.put("ns", "k2", "food", null);
        store.put("ns", "k3", "food", null);

        var results = store.search("ns", "food", 2);

        assertEquals(2, results.items().size());
    }

    @Test
    public void searchEmptyNamespaceReturnsEmpty() {
        var results = store.search("nonexistent", "query", 10);

        assertTrue(results.items().isEmpty());
    }

    @Test
    public void searchWithZeroLimitReturnsEmpty() {
        store.put("ns", "k", "food", null);

        var results = store.search("ns", "food", 0);

        assertTrue(results.items().isEmpty());
    }

    @Test
    public void searchReturnsByUpdatedAtDescending() throws InterruptedException {
        store.put("ns", "old", "food", null);
        Thread.sleep(5);
        store.put("ns", "new", "food", null);

        var results = store.search("ns", "food", 10);

        assertEquals("new", results.items().get(0).key());
    }

    @Test
    public void putWithNullMetadata() {
        store.put("ns", "k", "value", null);

        var item = store.get("ns", "k").orElseThrow();

        assertTrue(item.metadata().isEmpty());
    }

    @Test
    public void searchResultsAreImmutable() {
        store.put("ns", "k", "food", null);

        var results = store.search("ns", "food", 10);

        assertThrows(UnsupportedOperationException.class, () -> results.items().add(null));
    }
}
