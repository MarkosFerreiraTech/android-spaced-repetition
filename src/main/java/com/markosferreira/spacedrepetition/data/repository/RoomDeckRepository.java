package com.markosferreira.spacedrepetition.data.repository;

import com.markosferreira.spacedrepetition.data.local.DeckDao;
import com.markosferreira.spacedrepetition.data.local.DeckEntity;
import com.markosferreira.spacedrepetition.data.local.DeckSummaryRow;
import com.markosferreira.spacedrepetition.data.local.CardMapper;
import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador Room da porta {@link DeckRepository}.
 *
 * <p>Traduz o modelo de dominio para a entidade e devolve o agregado ordenado
 * pela ultima atividade, com os contadores calculados no SQL (RF10).
 */
public final class RoomDeckRepository implements DeckRepository {

    private final DeckDao deckDao;

    /**
     * Cria o repositorio.
     *
     * @param deckDao DAO de baralhos
     */
    public RoomDeckRepository(final DeckDao deckDao) {
        this.deckDao = deckDao;
    }

    @Override
    public List<DeckSummary> findAllWithCounts(final Instant now) {
        final List<DeckSummaryRow> rows = deckDao.findAllWithCounts(now.toEpochMilli());
        final List<DeckSummary> summaries = new ArrayList<>(rows.size());
        for (final DeckSummaryRow row : rows) {
            summaries.add(CardMapper.toSummary(row));
        }
        return summaries;
    }

    @Override
    public Deck findByName(final String name) {
        final DeckEntity entity = deckDao.findByName(name);
        return entity == null ? null : CardMapper.toDomain(entity);
    }

    @Override
    public Deck findById(final String deckId) {
        final DeckEntity entity = deckDao.findById(deckId);
        return entity == null ? null : CardMapper.toDomain(entity);
    }

    @Override
    public void save(final Deck deck) {
        deckDao.upsert(CardMapper.toEntity(deck));
    }
}
