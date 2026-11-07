package com.markosferreira.spacedrepetition.data.repository;

import com.markosferreira.spacedrepetition.data.local.CardMapper;
import com.markosferreira.spacedrepetition.data.local.StudySessionDao;
import com.markosferreira.spacedrepetition.data.local.StudySessionEntity;
import com.markosferreira.spacedrepetition.domain.model.StudySession;
import com.markosferreira.spacedrepetition.domain.port.SessionRepository;

/**
 * Adaptador Room da porta {@link SessionRepository}.
 *
 * <p>Permite retomar uma sessao interrompida (RF14): a sessao em andamento e
 * recuperada pelo deck e reconstruida com a mesma fila de questoes.
 */
public final class RoomSessionRepository implements SessionRepository {

    private final StudySessionDao sessionDao;

    /**
     * Cria o repositorio.
     *
     * @param sessionDao DAO de sessoes
     */
    public RoomSessionRepository(final StudySessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void save(final StudySession session) {
        sessionDao.upsert(CardMapper.toEntity(session));
    }

    @Override
    public StudySession findInProgressByDeck(final String deckId) {
        final StudySessionEntity entity = sessionDao.findInProgressByDeck(deckId);
        return entity == null ? null : CardMapper.toDomain(entity);
    }

    @Override
    public StudySession findById(final String sessionId) {
        final StudySessionEntity entity = sessionDao.findById(sessionId);
        return entity == null ? null : CardMapper.toDomain(entity);
    }
}
