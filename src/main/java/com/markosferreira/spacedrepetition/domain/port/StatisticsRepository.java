package com.markosferreira.spacedrepetition.domain.port;

import java.util.List;

/**
 * Porta de saida para agregados de desempenho.
 *
 * <p>As agregacoes ocorrem no banco (SQL), nunca em memoria (RF19).
 */
public interface StatisticsRepository {

    /**
     * Acurácia global do estudante.
     *
     * @return objeto com acertos, total e percentual
     */
    Accuracy globalAccuracy();

    /**
     * Total de revisoes registradas.
     *
     * @return quantidade de revisoes
     */
    int totalReviews();

    /**
     * Dias consecutivos com pelo menos uma revisao.
     *
     * @return tamanho da sequencia atual
     */
    int currentStreakDays();

    /**
     * Acurácia por baralho.
     *
     * @return lista de acuracias por baralho
     */
    List<DeckAccuracy> accuracyByDeck();

    /**
     * Acurácia agregada.
     */
    final class Accuracy {
        private final int correct;
        private final int total;

        public Accuracy(final int correct, final int total) {
            this.correct = correct;
            this.total = total;
        }

        public int correct() {
            return correct;
        }

        public int total() {
            return total;
        }

        /**
         * Percentual de acerto, tolerante a base vazia.
         *
         * @return 0 quando nao ha revisoes
         */
        public double percentage() {
            return total == 0 ? 0d : (correct * 100d) / total;
        }
    }

    /**
     * Acurácia de um baralho especifico.
     */
    final class DeckAccuracy {
        private final String deckId;
        private final String deckName;
        private final Accuracy accuracy;

        public DeckAccuracy(final String deckId, final String deckName, final Accuracy accuracy) {
            this.deckId = deckId;
            this.deckName = deckName;
            this.accuracy = accuracy;
        }

        public String deckId() {
            return deckId;
        }

        public String deckName() {
            return deckName;
        }

        public Accuracy accuracy() {
            return accuracy;
        }
    }
}
