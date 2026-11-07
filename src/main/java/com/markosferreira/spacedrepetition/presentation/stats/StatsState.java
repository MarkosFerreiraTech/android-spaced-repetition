package com.markosferreira.spacedrepetition.presentation.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado da tela de estatisticas (RF19 a RF21).
 */
public final class StatsState {

    /** Situacao da tela. */
    public enum Status {
        /** Carregando. */
        LOADING,
        /** Ha revisoes registradas. */
        LOADED,
        /** Nenhuma revisao: a tela convida a estudar. */
        EMPTY
    }

    private final Status status;
    private final String accuracyText;
    private final int totalReviews;
    private final int streakDays;
    private final List<DeckRow> decks;

    private StatsState(final Status status, final String accuracyText, final int totalReviews,
                       final int streakDays, final List<DeckRow> decks) {
        this.status = status;
        this.accuracyText = accuracyText;
        this.totalReviews = totalReviews;
        this.streakDays = streakDays;
        this.decks = Collections.unmodifiableList(new ArrayList<>(decks));
    }

    /**
     * Estado de carregamento.
     *
     * @return estado
     */
    public static StatsState loading() {
        return new StatsState(Status.LOADING, "0%", 0, 0, List.of());
    }

    /**
     * Estado sem revisoes.
     *
     * @return estado vazio
     */
    public static StatsState empty() {
        return new StatsState(Status.EMPTY, "0%", 0, 0, List.of());
    }

    /**
     * Estado com dados.
     *
     * @param accuracy percentual ja formatado
     * @param totalReviews revisoes registradas
     * @param streakDays sequencia de dias
     * @param decks acuracia por baralho
     * @return estado carregado
     */
    public static StatsState loaded(final String accuracy, final int totalReviews,
                                    final int streakDays, final List<DeckRow> decks) {
        return new StatsState(Status.LOADED, accuracy, totalReviews, streakDays, decks);
    }

    /**
     * Situacao atual.
     *
     * @return status
     */
    public Status status() {
        return status;
    }

    /**
     * Percentual de acerto formatado.
     *
     * @return texto
     */
    public String accuracyText() {
        return accuracyText;
    }

    /**
     * Total de revisoes.
     *
     * @return quantidade
     */
    public int totalReviews() {
        return totalReviews;
    }

    /**
     * Sequencia de dias estudados.
     *
     * @return dias
     */
    public int streakDays() {
        return streakDays;
    }

    /**
     * Acuracia por baralho.
     *
     * @return lista imutavel
     */
    public List<DeckRow> decks() {
        return decks;
    }

    /**
     * Baralho com pior acuracia, quando houver mais de um.
     *
     * @return nome do baralho ou null
     */
    public String worstDeckName() {
        DeckRow worst = null;
        for (final DeckRow row : decks) {
            if (row.answered() == 0) {
                continue;
            }
            if (worst == null || row.accuracyPercent() < worst.accuracyPercent()) {
                worst = row;
            }
        }
        // Sem dado suficiente para apontar um ponto fraco.
        if (worst == null || decks.size() < 2) {
            return null;
        }
        return worst.deckName();
    }

    /**
     * Acuracia de um baralho.
     */
    public static final class DeckRow {
        private final String deckName;
        private final int correct;
        private final int answered;

        /**
         * Cria a linha.
         *
         * @param deckName nome do baralho
         * @param correct acertos
         * @param answered total respondido
         */
        public DeckRow(final String deckName, final int correct, final int answered) {
            this.deckName = deckName;
            this.correct = correct;
            this.answered = answered;
        }

        /**
         * Nome do baralho.
         *
         * @return nome
         */
        public String deckName() {
            return deckName;
        }

        /**
         * Total respondido.
         *
         * @return quantidade
         */
        public int answered() {
            return answered;
        }

        /**
         * Percentual de acerto arredondado.
         *
         * @return 0 a 100
         */
        public int accuracyPercent() {
            return answered == 0 ? 0 : Math.round((correct * 100f) / answered);
        }

        /**
         * Texto exibido na linha.
         *
         * @return descricao
         */
        public String label() {
            return deckName + " · " + accuracyPercent() + "%";
        }
    }
}
