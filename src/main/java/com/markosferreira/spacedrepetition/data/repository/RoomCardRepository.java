package com.markosferreira.spacedrepetition.data.repository;

import com.markosferreira.spacedrepetition.data.local.AppDatabase;
import com.markosferreira.spacedrepetition.data.local.CardDao;
import com.markosferreira.spacedrepetition.data.local.CardEntity;
import com.markosferreira.spacedrepetition.data.local.CardMapper;
import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptador Room da porta {@link CardRepository}.
 *
 * <p>O upsert em lote e transacional (RF08): ou o lote inteiro entra, ou nada
 * entra. Isso impede que uma falha no meio de uma importacao de 65 questoes
 * deixe o baralho em estado parcial.
 */
public final class RoomCardRepository implements CardRepository {

    private final AppDatabase database;
    private final CardDao cardDao;

    /**
     * Cria o repositorio.
     *
     * @param database banco aberto, usado para delimitar a transacao
     * @param cardDao DAO de questoes
     */
    public RoomCardRepository(final AppDatabase database, final CardDao cardDao) {
        this.database = database;
        this.cardDao = cardDao;
    }

    @Override
    public List<Card> findByDeck(final String deckId) {
        final List<CardEntity> entities = cardDao.findByDeck(deckId);
        final List<Card> cards = new ArrayList<>(entities.size());
        for (final CardEntity entity : entities) {
            cards.add(CardMapper.toDomain(entity));
        }
        return cards;
    }

    @Override
    public Card findById(final String cardId) {
        final CardEntity entity = cardDao.findById(cardId);
        return entity == null ? null : CardMapper.toDomain(entity);
    }

    @Override
    public UpsertResult upsertAll(final List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return new UpsertResult(0, 0);
        }
        final List<CardEntity> entities = new ArrayList<>(cards.size());
        final Set<String> hashes = new HashSet<>();
        for (final Card card : cards) {
            entities.add(CardMapper.toEntity(card));
            if (card.contentHash() != null) {
                hashes.add(card.contentHash());
            }
        }
        final Set<String> existing = existingHashes(cards.get(0).deckId(), hashes);
        database.runInTransaction(() -> cardDao.upsertAll(entities));
        final int updated = countExisting(cards, existing);
        return new UpsertResult(cards.size() - updated, updated);
    }

    @Override
    public Set<String> findHashesByDeck(final String deckId) {
        return new HashSet<>(cardDao.findHashesByDeck(deckId));
    }

    private Set<String> existingHashes(final String deckId, final Set<String> candidates) {
        if (candidates.isEmpty()) {
            return new HashSet<>();
        }
        final Set<String> stored = findHashesByDeck(deckId);
        stored.retainAll(candidates);
        return stored;
    }

    private int countExisting(final List<Card> cards, final Set<String> existing) {
        int updated = 0;
        for (final Card card : cards) {
            if (card.contentHash() != null && existing.contains(card.contentHash())) {
                updated = updated + 1;
            }
        }
        return updated;
    }
}
