package com.markosferreira.spacedrepetition.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Baralho de questoes, criado a partir de uma planilha importada.
 */
public final class Deck {

    private final String id;
    private final String name;
    private final Instant createdAt;
    private final Instant lastStudiedAt;

    public Deck(final String id, final String name,
                final Instant createdAt, final Instant lastStudiedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastStudiedAt = lastStudiedAt;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastStudiedAt() {
        return lastStudiedAt;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Deck)) {
            return false;
        }
        final Deck that = (Deck) other;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(lastStudiedAt, that.lastStudiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, createdAt, lastStudiedAt);
    }

    @Override
    public String toString() {
        return "Deck{id=" + id + ", name=" + name + '}';
    }
}
