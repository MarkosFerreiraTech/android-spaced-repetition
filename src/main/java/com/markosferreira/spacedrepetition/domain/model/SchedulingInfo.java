package com.markosferreira.spacedrepetition.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Estado de agendamento de uma questao, no modelo FSRS.
 *
 * <p>Imutavel: toda transicao produz uma nova instancia. Isso torna o algoritmo
 * deterministico e trivialmente testavel (ADR-001).
 */
public final class SchedulingInfo {

    /** Estabilidade minima aceita, em dias. */
    public static final double MIN_STABILITY = 0.01d;

    /** Estabilidade maxima aceita, em dias (100 anos). */
    public static final double MAX_STABILITY = 36500d;

    /** Dificuldade minima da escala FSRS. */
    public static final double MIN_DIFFICULTY = 1d;

    /** Dificuldade maxima da escala FSRS. */
    public static final double MAX_DIFFICULTY = 10d;

    private final double stability;
    private final double difficulty;
    private final Instant dueAt;
    private final Instant lastReviewAt;
    private final int repetitions;
    private final int lapses;
    private final CardState state;

    private SchedulingInfo(final Builder builder) {
        this.stability = clamp(builder.stability, MIN_STABILITY, MAX_STABILITY);
        this.difficulty = clamp(builder.difficulty, MIN_DIFFICULTY, MAX_DIFFICULTY);
        this.dueAt = builder.dueAt;
        this.lastReviewAt = builder.lastReviewAt;
        this.repetitions = builder.repetitions;
        this.lapses = builder.lapses;
        this.state = builder.state;
    }

    /**
     * Cria o estado inicial de uma questao nunca respondida.
     *
     * @return estado NEW
     */
    public static SchedulingInfo newCard() {
        return builder()
                .stability(0d)
                .difficulty(0d)
                .dueAt(null)
                .lastReviewAt(null)
                .repetitions(0)
                .lapses(0)
                .state(CardState.NEW)
                .build();
    }

    /**
     * Cria um builder vazio.
     *
     * @return builder de {@link SchedulingInfo}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cria uma copia deste estado com os campos alterados.
     *
     * @return builder preenchido com os valores atuais
     */
    public Builder toBuilder() {
        return new Builder()
                .stability(stability)
                .difficulty(difficulty)
                .dueAt(dueAt)
                .lastReviewAt(lastReviewAt)
                .repetitions(repetitions)
                .lapses(lapses)
                .state(state);
    }

    private static double clamp(final double value, final double min, final double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public double stability() {
        return stability;
    }

    public double difficulty() {
        return difficulty;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public Instant lastReviewAt() {
        return lastReviewAt;
    }

    public int repetitions() {
        return repetitions;
    }

    public int lapses() {
        return lapses;
    }

    public CardState state() {
        return state;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SchedulingInfo)) {
            return false;
        }
        final SchedulingInfo that = (SchedulingInfo) other;
        return Double.compare(that.stability, stability) == 0
                && Double.compare(that.difficulty, difficulty) == 0
                && repetitions == that.repetitions
                && lapses == that.lapses
                && Objects.equals(dueAt, that.dueAt)
                && Objects.equals(lastReviewAt, that.lastReviewAt)
                && state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stability, difficulty, dueAt, lastReviewAt,
                repetitions, lapses, state);
    }

    @Override
    public String toString() {
        return "SchedulingInfo{stability=" + stability
                + ", difficulty=" + difficulty
                + ", dueAt=" + dueAt
                + ", reps=" + repetitions
                + ", lapses=" + lapses
                + ", state=" + state + '}';
    }

    /**
     * Builder de {@link SchedulingInfo}.
     */
    public static final class Builder {
        private double stability;
        private double difficulty;
        private Instant dueAt;
        private Instant lastReviewAt;
        private int repetitions;
        private int lapses;
        private CardState state = CardState.NEW;

        public Builder stability(final double value) {
            this.stability = value;
            return this;
        }

        public Builder difficulty(final double value) {
            this.difficulty = value;
            return this;
        }

        public Builder dueAt(final Instant value) {
            this.dueAt = value;
            return this;
        }

        public Builder lastReviewAt(final Instant value) {
            this.lastReviewAt = value;
            return this;
        }

        public Builder repetitions(final int value) {
            this.repetitions = value;
            return this;
        }

        public Builder lapses(final int value) {
            this.lapses = value;
            return this;
        }

        public Builder state(final CardState value) {
            this.state = value;
            return this;
        }

        /**
         * Constroi o estado aplicando os limites do modelo.
         *
         * @return estado imutavel
         */
        public SchedulingInfo build() {
            return new SchedulingInfo(this);
        }
    }
}
