package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Acesso a baralhos.
 */
@Dao
public interface DeckDao {

    /**
     * Insere ou substitui um baralho.
     *
     * @param deck baralho
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DeckEntity deck);

    /**
     * Busca um baralho por identificador.
     *
     * @param deckId identificador
     * @return entidade ou null
     */
    @Nullable
    @Query("SELECT * FROM decks WHERE id = :deckId")
    DeckEntity findById(String deckId);

    /**
     * Lista os baralhos ordenados pela ultima atividade, com contadores
     * agregados calculados no banco (RF10, RNF03).
     *
     * @param now instante de referencia, em epoch millis
     * @return projecoes de baralho
     */
    @Query("SELECT d.id AS id, d.name AS name, d.created_at AS createdAt, "
            + "d.last_studied_at AS lastStudiedAt, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deck_id = d.id) AS totalCards, "
            + "(SELECT COUNT(*) FROM cards c2 WHERE c2.deck_id = d.id "
            + "   AND (c2.due_at IS NULL OR c2.due_at <= :now)) AS dueToday "
            + "FROM decks d "
            + "ORDER BY COALESCE(d.last_studied_at, d.created_at) DESC")
    List<DeckSummaryRow> findAllWithCounts(long now);

    /**
     * Atualiza a data da ultima atividade do baralho.
     *
     * @param deckId identificador
     * @param instant instante da atividade, em epoch millis
     */
    @Query("UPDATE decks SET last_studied_at = :instant WHERE id = :deckId")
    void touchLastStudied(String deckId, long instant);
}
