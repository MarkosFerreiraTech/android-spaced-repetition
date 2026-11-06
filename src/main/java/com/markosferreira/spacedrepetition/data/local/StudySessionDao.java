package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Acesso a sessoes de estudo.
 */
@Dao
public interface StudySessionDao {

    /**
     * Insere ou substitui uma sessao.
     *
     * @param session sessao
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(StudySessionEntity session);

    /**
     * Sessao em andamento de um baralho (RF18).
     *
     * @param deckId identificador
     * @return sessao ou null
     */
    @Nullable
    @Query("SELECT * FROM study_sessions WHERE deck_id = :deckId AND status = 0 "
            + "ORDER BY started_at DESC LIMIT 1")
    StudySessionEntity findInProgressByDeck(String deckId);

    /**
     * Busca uma sessao.
     *
     * @param sessionId identificador
     * @return sessao ou null
     */
    @Nullable
    @Query("SELECT * FROM study_sessions WHERE id = :sessionId")
    StudySessionEntity findById(String sessionId);
}
