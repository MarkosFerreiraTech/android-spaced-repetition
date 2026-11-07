package com.markosferreira.spacedrepetition.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Questao de multipla escolha pertencente a um baralho.
 *
 * <p>Modelo de dominio puro: nao conhece Room, Android nem CSV.
 */
public final class Card {

    /** Quantidade minima de alternativas aceita. */
    public static final int MIN_OPTIONS = 2;

    /** Quantidade maxima de alternativas aceita. */
    public static final int MAX_OPTIONS = 5;

    private final String id;
    private final String deckId;
    private final String question;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;
    private final String contentHash;
    private final SchedulingInfo scheduling;

    private Card(final Builder builder) {
        this.id = builder.id;
        this.deckId = builder.deckId;
        this.question = builder.question;
        this.options = Collections.unmodifiableList(builder.options);
        this.correctIndex = builder.correctIndex;
        this.explanation = builder.explanation;
        this.contentHash = builder.contentHash;
        this.scheduling = builder.scheduling == null ? SchedulingInfo.newCard() : builder.scheduling;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cria uma copia com o agendamento atualizado.
     *
     * @param newScheduling novo estado de agendamento
     * @return nova instancia de Card
     */
    public Card withScheduling(final SchedulingInfo newScheduling) {
        return toBuilder().scheduling(newScheduling).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .deckId(deckId)
                .question(question)
                .options(options)
                .correctIndex(correctIndex)
                .explanation(explanation)
                .contentHash(contentHash)
                .scheduling(scheduling);
    }

    public String id() {
        return id;
    }

    public String deckId() {
        return deckId;
    }

    public String question() {
        return question;
    }

    public List<String> options() {
        return options;
    }

    public int correctIndex() {
        return correctIndex;
    }

    /**
     * Letra da alternativa correta (A, B, C, D ou E).
     *
     * @return letra correspondente ao indice correto
     */
    public String correctLetter() {
        return String.valueOf((char) ('A' + correctIndex));
    }

    public String explanation() {
        return explanation;
    }

    public String contentHash() {
        return contentHash;
    }

    public SchedulingInfo scheduling() {
        return scheduling;
    }

    /**
     * Indica se a questao esta vencida para revisao.
     *
     * @param now instante de referencia
     * @return true se nunca foi respondida ou se o vencimento ja passou
     */
    public boolean isDue(final java.time.Instant now) {
        return scheduling.dueAt() == null || !scheduling.dueAt().isAfter(now);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Card)) {
            return false;
        }
        final Card that = (Card) other;
        return correctIndex == that.correctIndex
                && Objects.equals(id, that.id)
                && Objects.equals(deckId, that.deckId)
                && Objects.equals(question, that.question)
                && Objects.equals(options, that.options)
                && Objects.equals(explanation, that.explanation)
                && Objects.equals(contentHash, that.contentHash)
                && Objects.equals(scheduling, that.scheduling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deckId, question, options, correctIndex,
                explanation, contentHash, scheduling);
    }

    @Override
    public String toString() {
        return "Card{id=" + id + ", deckId=" + deckId + ", hash=" + contentHash + '}';
    }

    /**
     * Builder de {@link Card}.
     */
    public static final class Builder {
        private String id;
        private String deckId;
        private String question;
        private List<String> options = Collections.emptyList();
        private int correctIndex;
        private String explanation;
        private String contentHash;
        private SchedulingInfo scheduling;

        public Builder id(final String value) {
            this.id = value;
            return this;
        }

        public Builder deckId(final String value) {
            this.deckId = value;
            return this;
        }

        public Builder question(final String value) {
            this.question = value;
            return this;
        }

        public Builder options(final List<String> value) {
            this.options = value;
            return this;
        }

        public Builder correctIndex(final int value) {
            this.correctIndex = value;
            return this;
        }

        public Builder explanation(final String value) {
            this.explanation = value;
            return this;
        }

        public Builder contentHash(final String value) {
            this.contentHash = value;
            return this;
        }

        public Builder scheduling(final SchedulingInfo value) {
            this.scheduling = value;
            return this;
        }

        public Card build() {
            return new Card(this);
        }
    }
}
