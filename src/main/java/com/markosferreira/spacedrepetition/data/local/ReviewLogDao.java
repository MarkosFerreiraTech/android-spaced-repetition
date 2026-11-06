package com.markosferreira.spacedrepetition.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Acesso ao historico de revisoes e aos agregados de estatistica.
 *
 * <p>Todas as agregacoes sao feitas em SQL: nenhum calculo de estatistica
 * percorre listas em memoria (RF19).
 */
@Dao
public interface ReviewLogDao {

    /**
     * Insere um registro de revisao.
     *
     * @param log registro
     */
    @Insert
    void insert(ReviewLogEntity log);

    /**
     * Total de revisoes registradas.
     *
     * @return contagem
     */
    @Query("SELECT COUNT(*) FROM review_logs")
    int countAll();

    /**
     * Total de acertos.
     *
     * @return contagem
     */
    @Query("SELECT COUNT(*) FROM review_logs WHERE correct = 1")
    int countCorrect();

    /**
     * Dias distintos com pelo menos uma revisao, do mais recente ao mais antigo.
     *
     * @param zoneOffsetSeconds deslocamento do fuso do usuario, em segundos
     * @return dias em epoch day, ordem decrescente
     */
    @Query("SELECT DISTINCT CAST((reviewed_at / 1000 + :zoneOffsetSeconds) / 86400 AS INTEGER) "
            + "AS day FROM review_logs ORDER BY day DESC")
    List<Long> distinctStudyDays(int zoneOffsetSeconds);

    /**
     * Acurácia por baralho.
     *
     * @return projecoes por baralho
     */
    @Query("SELECT d.id AS deckId, d.name AS deckName, COUNT(rl.id) AS total, "
            + "SUM(CASE WHEN rl.correct = 1 THEN 1 ELSE 0 END) AS correct "
            + "FROM decks d JOIN cards c ON c.deck_id = d.id "
            + "JOIN review_logs rl ON rl.card_id = c.id "
            + "GROUP BY d.id, d.name ORDER BY d.id")
    List<DeckAccuracyRow> accuracyByDeck();
}
