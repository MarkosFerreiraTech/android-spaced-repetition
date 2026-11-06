package com.markosferreira.spacedrepetition.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro imutavel de uma resposta dada a uma questao (RF16).
 *
 * <p>E a fonte de verdade das estatisticas: nunca e atualizado, apenas inserido.
 */
public final class ReviewLog {

    private final String id;
    private final String cardId;
    private final String sessionId;
    private final Rating rating;
    private final boolean correct;
    private final boolean flagged;
    private final long elapsedMillis;
    private final Instant reviewedAt;
    private final double stabilityAfter;
    private final double difficultyAfter;

    private ReviewLog(final Builder builder) {
        this.id = builder.id;
        this.cardId = builder.cardId;
        this.sessionId = builder.sessionId;
        this.rating = builder.rating;
        this.correct = builder.correct;
        this.flagged = builder.flagged;
        this.elapsedMillis = builder.elapsedMillis;
        this.reviewedAt = builder.reviewedAt;
        this.stabilityAfter = builder.stabilityAfter;
        this.difficultyAfter = builder.difficultyAfter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public String cardId() {
        return cardId;
    }

    public String sessionId() {
        return sessionId;
    }

    public Rating rating() {
        return rating;
    }

    public boolean correct() {
        return correct;
    }

    public boolean flagged() {
        return flagged;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    public Instant reviewedAt() {
        return reviewedAt;
    }

    public double stabilityAfter() {
        return stabilityAfter;
    }

    public double difficultyAfter() {
        return difficultyAfter;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReviewLog)) {
            return false;
        }
        final ReviewLog that = (ReviewLog) other;
        return correct == that.correct
                && flagged == that.flagged
                && elapsedMillis == that.elapsedMillis
                && Double.compare(that.stabilityAfter, stabilityAfter) == 0
                && Double.compare(that.difficultyAfter, difficultyAfter) == 0
                && Objects.equals(id, that.id)
                && Objects.equals(cardId, that.cardId)
                && Objects.equals(sessionId, that.sessionId)
                && rating == that.rating
                && Objects.equals(reviewedAt, that.reviewedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cardId, sessionId, rating, correct, flagged,
                elapsedMillis, reviewedAt, stabilityAfter, difficultyAfter);
    }

    @Override
    public String toString() {
        return "ReviewLog{card=" + cardId + ", rating=" + rating
                + ", correct=" + correct + ", flagged=" + flagged + '}';
    }

    /**
     * Builder de {@link ReviewLog}.
     */
    public static final class Builder {
        private String id;
        private String cardId;
        private String sessionId;
        private Rating rating;
        private boolean correct;
        private boolean flagged;
        private long elapsedMillis;
        private Instant reviewedAt;
        private double stabilityAfter;
        private double difficultyAfter;

        public Builder id(final String value) {
            this.id = value;
            return this;
        }

        public Builder cardId(final String value) {
            this.cardId = value;
            return this;
        }

        public Builder sessionId(final String value) {
            this.sessionId = value;
            return this;
        }

        public Builder rating(final Rating value) {
            this.rating = value;
            return this;
        }

        public Builder correct(final boolean value) {
            this.correct = value;
            return this;
        }

        public Builder flagged(final boolean value) {
            this.flagged = value;
            return this;
        }

        public Builder elapsedMillis(final long value) {
            this.elapsedMillis = value;
            return this;
        }

        public Builder reviewedAt(final Instant value) {
            this.reviewedAt = value;
            return this;
        }

        public Builder stabilityAfter(final double value) {
            this.stabilityAfter = value;
            return this;
        }

        public Builder difficultyAfter(final double value) {
            this.difficultyAfter = value;
            return this;
        }

        public ReviewLog build() {
            return new ReviewLog(this);
        }
    }
}
