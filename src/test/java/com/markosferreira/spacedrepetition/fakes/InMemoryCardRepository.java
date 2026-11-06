package com.markosferreira.spacedrepetition.fakes;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dublê em memoria da porta de questoes, para testes do dominio.
 */
public final class InMemoryCardRepository implements CardRepository {

    private final Map<String, Card> byHash = new LinkedHashMap<>();

    @Override
    public List<Card> findByDeck(final String deckId) {
        return byHash.values().stream()
                .filter(card -> card.deckId().equals(deckId))
                .collect(Collectors.toList());
    }

    @Override
    public Card findById(final String cardId) {
        return byHash.values().stream()
                .filter(card -> card.id().equals(cardId))
                .findFirst().orElse(null);
    }

    @Override
    public UpsertResult upsertAll(final List<Card> cards) {
        int inserted = 0;
        int updated = 0;
        for (final Card card : cards) {
            if (byHash.containsKey(card.contentHash())) {
                updated++;
            } else {
                inserted++;
            }
            byHash.put(card.contentHash(), card);
        }
        return new UpsertResult(inserted, updated);
    }

    @Override
    public Set<String> findHashesByDeck(final String deckId) {
        return byHash.values().stream()
                .filter(card -> card.deckId().equals(deckId))
                .map(Card::contentHash)
                .collect(Collectors.toSet());
    }

    /**
     * Quantidade de questoes armazenadas.
     *
     * @return total
     */
    public int size() {
        return byHash.size();
    }

    /**
     * Questoes armazenadas.
     *
     * @return lista
     */
    public List<Card> all() {
        return new ArrayList<>(byHash.values());
    }
}
