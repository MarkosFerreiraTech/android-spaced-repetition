package com.markosferreira.spacedrepetition.domain.model;

import java.util.Objects;

/**
 * Projecao de leitura exibida na lista de simulados (RF10).
 *
 * <p>Existe para evitar carregar todas as questoes na memoria apenas para contar
 * quantas estao pendentes hoje.
 */
public final class DeckSummary {

    private final Deck deck;
    private final int totalCards;
    private final int dueToday;

    public DeckSummary(final Deck deck, final int totalCards, final int dueToday) {
        this.deck = deck;
        this.totalCards = totalCards;
        this.dueToday = dueToday;
    }

    public Deck deck() {
        return deck;
    }

    public int totalCards() {
        return totalCards;
    }

    public int dueToday() {
        return dueToday;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DeckSummary)) {
            return false;
        }
        final DeckSummary that = (DeckSummary) other;
        return totalCards == that.totalCards
                && dueToday == that.dueToday
                && Objects.equals(deck, that.deck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deck, totalCards, dueToday);
    }

    @Override
    public String toString() {
        return "DeckSummary{deck=" + deck.name()
                + ", total=" + totalCards + ", due=" + dueToday + '}';
    }
}
