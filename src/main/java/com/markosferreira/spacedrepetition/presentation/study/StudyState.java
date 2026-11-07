package com.markosferreira.spacedrepetition.presentation.study;

import com.markosferreira.spacedrepetition.domain.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado da sessao de estudo.
 *
 * <p>Carrega a questao atual, a alternativa escolhida e o feedback. Separar
 * "escolheu" de "confirmou" e o que permite ao usuario trocar de alternativa
 * antes de valer — e o que impede gravar duas revisoes para a mesma questao.
 */
public final class StudyState {

    /** Fase da questao atual. */
    public enum Phase {
        /** Aguardando escolha e confirmacao. */
        ANSWERING,
        /** Confirmada: existe feedback a exibir. */
        FEEDBACK,
        /** Sessao terminada. */
        FINISHED,
        /** Nenhuma questao a estudar. */
        EMPTY
    }

    private final Phase phase;
    private final Card card;
    private final int position;
    private final int total;
    private final int selectedIndex;
    private final boolean flagged;
    private final boolean correct;
    private final int correctCount;
    private final int wrongCount;
    private final int flaggedCount;

    private StudyState(final Builder builder) {
        this.phase = builder.phase;
        this.card = builder.card;
        this.position = builder.position;
        this.total = builder.total;
        this.selectedIndex = builder.selectedIndex;
        this.flagged = builder.flagged;
        this.correct = builder.correct;
        this.correctCount = builder.correctCount;
        this.wrongCount = builder.wrongCount;
        this.flaggedCount = builder.flaggedCount;
    }

    /**
     * Estado de questao em resposta.
     *
     * @param card questao atual
     * @param position posicao (base 1)
     * @param total total de questoes
     * @return estado
     */
    public static StudyState answering(final Card card, final int position, final int total) {
        return new Builder().phase(Phase.ANSWERING).card(card)
                .position(position).total(total).selectedIndex(-1).build();
    }

    /**
     * Estado apos confirmar a resposta.
     *
     * @param card questao respondida
     * @param position posicao (base 1)
     * @param total total de questoes
     * @param selectedIndex alternativa escolhida
     * @param flagged marcou "acertei com duvida"
     * @param correct acertou
     * @return estado com feedback
     */
    public static StudyState feedback(final Card card, final int position, final int total,
                                      final int selectedIndex, final boolean flagged,
                                      final boolean correct) {
        return new Builder().phase(Phase.FEEDBACK).card(card).position(position).total(total)
                .selectedIndex(selectedIndex).flagged(flagged).correct(correct).build();
    }

    /**
     * Estado de sessao concluida.
     *
     * @param correctCount acertos
     * @param wrongCount erros
     * @param flaggedCount marcadas
     * @return estado final
     */
    public static StudyState finished(final int correctCount, final int wrongCount,
                                      final int flaggedCount) {
        return new Builder().phase(Phase.FINISHED).correctCount(correctCount)
                .wrongCount(wrongCount).flaggedCount(flaggedCount).build();
    }

    /**
     * Estado sem questoes para estudar.
     *
     * @return estado vazio
     */
    public static StudyState empty() {
        return new Builder().phase(Phase.EMPTY).build();
    }

    /**
     * Fase atual.
     *
     * @return fase
     */
    public Phase phase() {
        return phase;
    }

    /**
     * Questao atual, quando houver.
     *
     * @return questao ou null
     */
    public Card card() {
        return card;
    }

    /**
     * Posicao da questao (base 1).
     *
     * @return posicao
     */
    public int position() {
        return position;
    }

    /**
     * Total de questoes da sessao.
     *
     * @return total
     */
    public int total() {
        return total;
    }

    /**
     * Alternativa escolhida, ou -1 antes da escolha.
     *
     * @return indice
     */
    public int selectedIndex() {
        return selectedIndex;
    }

    /**
     * Indica se o usuario marcou duvida.
     *
     * @return marcado
     */
    public boolean flagged() {
        return flagged;
    }

    /**
     * Indica se a resposta confirmada esta correta.
     *
     * @return acertou
     */
    public boolean correct() {
        return correct;
    }

    /**
     * Acertos na sessao.
     *
     * @return quantidade
     */
    public int correctCount() {
        return correctCount;
    }

    /**
     * Erros na sessao.
     *
     * @return quantidade
     */
    public int wrongCount() {
        return wrongCount;
    }

    /**
     * Questoes marcadas com duvida.
     *
     * @return quantidade
     */
    public int flaggedCount() {
        return flaggedCount;
    }

    /**
     * Percentual formatado de acerto da sessao.
     *
     * @return texto como "73%"
     */
    public String accuracyText() {
        final int answered = correctCount + wrongCount;
        final int percent = answered == 0 ? 0 : Math.round((correctCount * 100f) / answered);
        return percent + "%";
    }

    /**
     * Devolve uma copia com a alternativa escolhida, preservando a fase.
     *
     * <p>Permite trocar de alternativa antes de confirmar sem recriar o estado
     * inteiro a partir do cartao.
     *
     * @param index alternativa escolhida
     * @return novo estado
     */
    public StudyState withSelection(final int index) {
        return new Builder().phase(phase).card(card).position(position).total(total)
                .selectedIndex(index).flagged(flagged).correct(correct)
                .correctCount(correctCount).wrongCount(wrongCount)
                .flaggedCount(flaggedCount).build();
    }

    /**
     * Indica se ha alternativa escolhida.
     *
     * @return verdadeiro se selecionou
     */
    public boolean hasSelection() {
        return selectedIndex >= 0;
    }

    /**
     * Alternativas da questao.
     *
     * @return lista imutavel, vazia quando nao ha questao
     */
    public List<String> options() {
        return card == null ? Collections.emptyList() : new ArrayList<>(card.options());
    }

    /**
     * Construtor interno do estado.
     */
    private static final class Builder {
        private Phase phase = Phase.ANSWERING;
        private Card card;
        private int position;
        private int total;
        private int selectedIndex = -1;
        private boolean flagged;
        private boolean correct;
        private int correctCount;
        private int wrongCount;
        private int flaggedCount;

        Builder phase(final Phase value) {
            this.phase = value;
            return this;
        }

        Builder card(final Card value) {
            this.card = value;
            return this;
        }

        Builder position(final int value) {
            this.position = value;
            return this;
        }

        Builder total(final int value) {
            this.total = value;
            return this;
        }

        Builder selectedIndex(final int value) {
            this.selectedIndex = value;
            return this;
        }

        Builder flagged(final boolean value) {
            this.flagged = value;
            return this;
        }

        Builder correct(final boolean value) {
            this.correct = value;
            return this;
        }

        Builder correctCount(final int value) {
            this.correctCount = value;
            return this;
        }

        Builder wrongCount(final int value) {
            this.wrongCount = value;
            return this;
        }

        Builder flaggedCount(final int value) {
            this.flaggedCount = value;
            return this;
        }

        StudyState build() {
            return new StudyState(this);
        }
    }
}
