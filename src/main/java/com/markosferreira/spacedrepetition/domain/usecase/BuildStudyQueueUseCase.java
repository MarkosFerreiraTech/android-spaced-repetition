package com.markosferreira.spacedrepetition.domain.usecase;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta a fila de questoes de uma sessao de simulado (RF12).
 *
 * <p>Implementa os tres modos pedidos pelo usuario:
 * <ul>
 *   <li>{@link StudyMode#FULL} — todas as questoes, priorizando as vencidas;</li>
 *   <li>{@link StudyMode#WRONG_ONLY} — apenas as erradas na ultima tentativa;</li>
 *   <li>{@link StudyMode#WRONG_PLUS_FLAGGED} — erradas mais as marcadas com duvida.</li>
 * </ul>
 */
public final class BuildStudyQueueUseCase {

    private final CardRepository cardRepository;
    private final ReviewRepository reviewRepository;

    public BuildStudyQueueUseCase(final CardRepository cardRepository,
                                  final ReviewRepository reviewRepository) {
        this.cardRepository = cardRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Monta a fila de questoes.
     *
     * @param deckId baralho selecionado
     * @param mode modo de sessao
     * @param now instante de referencia para ordenacao das vencidas
     * @return lista de questoes na ordem de apresentacao
     */
    public List<Card> execute(final String deckId, final StudyMode mode, final Instant now) {
        if (deckId == null || deckId.isEmpty()) {
            throw new IllegalArgumentException("Baralho obrigatorio para montar a fila");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Modo de estudo obrigatorio");
        }

        final List<Card> selected;
        switch (mode) {
            case WRONG_ONLY:
                selected = reviewRepository.findWrongByDeck(deckId);
                break;
            case WRONG_PLUS_FLAGGED:
                selected = dedupe(reviewRepository.findWrongOrFlaggedByDeck(deckId));
                break;
            case FULL:
            default:
                selected = sortByDueDate(cardRepository.findByDeck(deckId), now);
                break;
        }
        return selected;
    }

    /**
     * Ordena priorizando vencidas e, entre elas, as de menor estabilidade
     * (as mais frageis primeiro).
     *
     * @param cards questoes a ordenar
     * @param now instante de referencia
     * @return lista ordenada
     */
    List<Card> sortByDueDate(final List<Card> cards, final Instant now) {
        final List<Card> ordered = new ArrayList<>(cards);
        ordered.sort(Comparator
                .comparing((Card card) -> !card.isDue(now))
                .thenComparingDouble(card -> card.scheduling().stability())
                .thenComparing(Card::id));
        return ordered;
    }

    private List<Card> dedupe(final List<Card> cards) {
        final Map<String, Card> unique = new LinkedHashMap<>();
        for (final Card card : cards) {
            unique.put(card.id(), card);
        }
        return new ArrayList<>(unique.values());
    }
}
