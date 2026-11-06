package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.StudySession;

/**
 * Porta de saida para sessoes de estudo.
 */
public interface SessionRepository {

    /**
     * Persiste ou atualiza uma sessao.
     *
     * @param session sessao a salvar
     */
    void save(StudySession session);

    /**
     * Recupera a sessao em andamento de um baralho, se existir.
     *
     * @param deckId identificador do baralho
     * @return sessao em andamento ou null
     */
    StudySession findInProgressByDeck(String deckId);

    /**
     * Busca uma sessao pelo identificador.
     *
     * @param sessionId identificador
     * @return sessao ou null
     */
    StudySession findById(String sessionId);
}
