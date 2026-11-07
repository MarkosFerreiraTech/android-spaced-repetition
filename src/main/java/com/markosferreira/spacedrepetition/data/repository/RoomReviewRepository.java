package com.markosferreira.spacedrepetition.data.repository;

import com.markosferreira.spacedrepetition.data.local.AppDatabase;
import com.markosferreira.spacedrepetition.data.local.CardDao;
import com.markosferreira.spacedrepetition.data.local.CardEntity;
import com.markosferreira.spacedrepetition.data.local.CardMapper;
import com.markosferreira.spacedrepetition.data.local.ReviewLogDao;
import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador Room da porta {@link ReviewRepository}.
 *
 * <p>{@link #record(ReviewLog, SchedulingInfo)} executa em transacao unica
 * (RF16): gravar o log sem atualizar o agendamento corromperia tanto a
 * estatistica quanto o proprio algoritmo de repeticao espacada.
 */
public final class RoomReviewRepository implements ReviewRepository {

    private final AppDatabase database;
    private final CardDao cardDao;
    private final ReviewLogDao reviewLogDao;

    /**
     * Cria o repositorio.
     *
     * @param database banco aberto, usado para delimitar a transacao
     * @param cardDao DAO de questoes
     * @param reviewLogDao DAO do historico
     */
    public RoomReviewRepository(final AppDatabase database, final CardDao cardDao,
                                final ReviewLogDao reviewLogDao) {
        this.database = database;
        this.cardDao = cardDao;
        this.reviewLogDao = reviewLogDao;
    }

    @Override
    public void record(final ReviewLog log, final SchedulingInfo nextScheduling) {
        database.runInTransaction(() -> {
            reviewLogDao.insert(CardMapper.toEntity(log));
            final CardEntity entity = cardDao.findById(log.cardId());
            if (entity == null) {
                // A questao sumiu (baralho excluido durante a sessao): o log ja
                // foi gravado, mas nao ha agendamento a atualizar.
                return;
            }
            final Card updated = CardMapper.toDomain(entity).withScheduling(nextScheduling);
            cardDao.updateCard(CardMapper.toEntity(updated));
        });
    }

    @Override
    public List<Card> findWrongByDeck(final String deckId) {
        return toDomain(cardDao.findWrongByDeck(deckId));
    }

    @Override
    public List<Card> findWrongOrFlaggedByDeck(final String deckId) {
        return toDomain(cardDao.findWrongOrFlaggedByDeck(deckId));
    }

    private List<Card> toDomain(final List<CardEntity> entities) {
        final List<Card> cards = new ArrayList<>(entities.size());
        for (final CardEntity entity : entities) {
            cards.add(CardMapper.toDomain(entity));
        }
        return cards;
    }
}
