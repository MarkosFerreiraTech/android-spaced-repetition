package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Upsert;
import androidx.room.Query;
import androidx.room.Transaction;


import java.util.List;

/**
 * Acesso a questoes.
 */
@Dao
public interface CardDao {

    /**
     * Insere ou substitui um lote de questoes em uma unica transacao (RNF07).
     *
     * @param cards questoes
     */
    /**
     * Insere ou atualiza um lote de questoes.
     *
     * <p>Deliberadamente {@link Upsert} e nao {@code @Insert(REPLACE)}: o
     * REPLACE executa DELETE seguido de INSERT, e como {@code review_logs}
     * referencia {@code cards} em {@code ON DELETE CASCADE}, reimportar uma
     * planilha apagaria todo o historico de revisoes (violacao do RF06).
     *
     * @param cards questoes a persistir
     */
    @Upsert
    @Transaction
    void upsertAll(List<CardEntity> cards);

    /**
     * Lista as questoes de um baralho.
     *
     * @param deckId identificador
     * @return questoes
     */
    @Query("SELECT * FROM cards WHERE deck_id = :deckId ORDER BY due_at IS NULL DESC, due_at ASC")
    List<CardEntity> findByDeck(String deckId);

    /**
     * Busca uma questao.
     *
     * @param cardId identificador
     * @return entidade ou null
     */
    @Nullable
    @Query("SELECT * FROM cards WHERE id = :cardId")
    CardEntity findById(String cardId);

    /**
     * Hashes de conteudo ja existentes no baralho, para o upsert (RF06).
     *
     * @param deckId identificador
     * @return hashes
     */
    @Query("SELECT content_hash FROM cards WHERE deck_id = :deckId")
    List<String> findHashesByDeck(String deckId);

    /**
     * Atualiza a questao a partir da entidade ja modificada.
     *
     * <p>Usa {@code @Update} do Room em vez de uma query com sete parametros:
     * e o idioma nativo do framework, nao exige mapear coluna a coluna e evita
     * assinaturas longas (regra ParameterNumber do Checkstyle).
     *
     * @param card entidade com o agendamento atualizado
     */
    @Update
    void updateCard(CardEntity card);

    /**
     * Data media de resposta (ms) de uma questao, base para o rating EASY.
     *
     * @param cardId identificador
     * @return media ou 0 quando nao ha historico
     */
    /**
     * Questoes erradas na ultima tentativa (modo WRONG_ONLY).
     *
     * @param deckId identificador
     * @return questoes
     */
    @Query("SELECT c.* FROM cards c WHERE c.deck_id = :deckId AND c.id IN ("
            + "  SELECT rl.card_id FROM review_logs rl WHERE rl.correct = 0 "
            + "  AND rl.reviewed_at = (SELECT MAX(r2.reviewed_at) FROM review_logs r2 "
            + "                         WHERE r2.card_id = rl.card_id))")
    List<CardEntity> findWrongByDeck(String deckId);

    /**
     * Questoes erradas ou marcadas na ultima tentativa (modo WRONG_PLUS_FLAGGED).
     *
     * @param deckId identificador
     * @return questoes
     */
    @Query("SELECT c.* FROM cards c WHERE c.deck_id = :deckId AND c.id IN ("
            + "  SELECT rl.card_id FROM review_logs rl WHERE (rl.correct = 0 OR rl.flagged = 1) "
            + "  AND rl.reviewed_at = (SELECT MAX(r2.reviewed_at) FROM review_logs r2 "
            + "                         WHERE r2.card_id = rl.card_id))")
    List<CardEntity> findWrongOrFlaggedByDeck(String deckId);
}
