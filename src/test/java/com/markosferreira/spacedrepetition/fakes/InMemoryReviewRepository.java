package com.markosferreira.spacedrepetition.fakes;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dublê em memoria do historico de revisoes, para testes do dominio.
 */
public final class InMemoryReviewRepository implements ReviewRepository {

    private final List<ReviewLog> logs = new ArrayList<>();
    private final Map<String, List<Card>> deckCards = new HashMap<>();
    private final Map<String, SchedulingInfo> scheduling = new HashMap<>();

    /**
     * Registra as questoes de um baralho, para os modos de sessao.
     *
     * @param deckId baralho
     * @param cards questoes
     */
    public void registerCards(final String deckId, final List<Card> cards) {
        deckCards.put(deckId, new ArrayList<>(cards));
    }

    @Override
    public void record(final ReviewLog log, final SchedulingInfo nextScheduling) {
        logs.add(log);
        scheduling.put(log.cardId(), nextScheduling);
    }

    @Override
    public List<Card> findWrongByDeck(final String deckId) {
        return cardsByOutcome(deckId, false);
    }

    @Override
    public List<Card> findWrongOrFlaggedByDeck(final String deckId) {
        return cardsByOutcome(deckId, true);
    }

    private List<Card> cardsByOutcome(final String deckId, final boolean includeFlagged) {
        final Map<String, ReviewLog> lastByCard = new HashMap<>();
        for (final ReviewLog log : logs) {
            lastByCard.put(log.cardId(), log);
        }
        return deckCards.getOrDefault(deckId, new ArrayList<>()).stream()
                .filter(card -> {
                    final ReviewLog last = lastByCard.get(card.id());
                    if (last == null) {
                        return false;
                    }
                    return !last.correct() || (includeFlagged && last.flagged());
                })
                .collect(Collectors.toList());
    }

    public List<ReviewLog> logs() {
        return new ArrayList<>(logs);
    }

    public SchedulingInfo schedulingOf(final String cardId) {
        return scheduling.get(cardId);
    }
}
