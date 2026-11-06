package com.markosferreira.spacedrepetition.domain.usecase;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.domain.model.StudySession;
import com.markosferreira.spacedrepetition.domain.port.SessionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cria e persiste uma sessao de estudo (UC03).
 *
 * <p>A sessao nasce persistida, e nao em memoria, para que o progresso sobreviva
 * ao encerramento do aplicativo (RF18).
 */
public final class StartStudySessionUseCase {

    private final BuildStudyQueueUseCase buildQueue;
    private final SessionRepository sessionRepository;

    public StartStudySessionUseCase(final BuildStudyQueueUseCase buildQueue,
                                    final SessionRepository sessionRepository) {
        this.buildQueue = buildQueue;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Inicia uma sessao.
     *
     * @param deckId baralho
     * @param mode modo de estudo
     * @param now instante de inicio
     * @return sessao persistida, possivelmente vazia quando nao ha questoes elegiveis
     */
    public StudySession execute(final String deckId, final StudyMode mode, final Instant now) {
        final List<Card> queue = buildQueue.execute(deckId, mode, now);
        final List<String> ids = new ArrayList<>(queue.size());
        for (final Card card : queue) {
            ids.add(card.id());
        }
        final StudySession session = StudySession.start(
                UUID.randomUUID().toString(), deckId, mode, now, ids);
        sessionRepository.save(session);
        return session;
    }

    /**
     * Recupera a sessao em andamento de um baralho, se houver.
     *
     * @param deckId baralho
     * @return sessao em andamento ou null
     */
    public StudySession findResumable(final String deckId) {
        return sessionRepository.findInProgressByDeck(deckId);
    }
}
