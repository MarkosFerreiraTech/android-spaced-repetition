package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
     * Atualiza o estado de agendamento de uma questao.
     *
     * <p>Os valores vem agrupados em {@link SchedulingUpdate} para manter a
     * assinatura dentro do limite de parametros do projeto.
     *
     * @param cardId identificador da questao
     * @param update  novo estado de agendamento
     */
    @Query("UPDATE cards SET stability = :#{#update.stability}, "
            + "difficulty = :#{#update.difficulty}, due_at = :#{#update.dueAt}, "
            + "last_review_at = :#{#update.lastReviewAt}, reps = :#{#update.reps}, "
            + "lapses = :#{#update.lapses}, state = :#{#update.state} WHERE id = :cardId")
    void updateScheduling(String cardId, SchedulingUpdate update);

    /**
     * Data media de resposta (ms) de uma questao, base para o rating EASY.
     *
     * @param cardId identificador
     * @return media ou 0 quando nao ha historico
     */
    @Query("SELECT AVG(elapsed_millis) FROM review_logs WHERE card_id = :cardId")
    double averageElapsedMillis(String cardId);

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

    /**
     * Valores de agendamento de uma questao, agrupados para a query de UPDATE.
     */
    final class SchedulingUpdate {

        private SchedulingUpdate() {
            // Use SchedulingUpdate.from(...)
        }

        public double stability;
        public double difficulty;
        public long dueAt;
        public long lastReviewAt;
        public int reps;
        public int lapses;
        public int state;

        /**
         * Cria os valores de agendamento.
         *
         * @param scheduling estado calculado pelo dominio
         * @return valores prontos para a query
         */
        public static SchedulingUpdate from(final SchedulingInfo scheduling,
                                            final java.util.function.LongSupplier dueAt,
                                            final java.util.function.LongSupplier lastReview) {
            final SchedulingUpdate update = new SchedulingUpdate();
            update.stability = scheduling.stability();
            update.difficulty = scheduling.difficulty();
            update.dueAt = dueAt.getAsLong();
            update.lastReviewAt = lastReview.getAsLong();
            update.reps = scheduling.repetitions();
            update.lapses = scheduling.lapses();
            update.state = scheduling.state().ordinal();
            return update;
        }
    }
}
