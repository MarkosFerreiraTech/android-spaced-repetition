package com.markosferreira.spacedrepetition.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sessao de estudo sobre um baralho, com os itens na ordem em que serao
 * apresentados.
 *
 * <p>A sessao e imutavel. O avanco produz uma nova instancia, o que permite
 * persistir o progresso sem risco de mutacao acidental (RF18).
 */
public final class StudySession {

    private final String id;
    private final String deckId;
    private final StudyMode mode;
    private final SessionStatus status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final List<String> cardIds;
    private final int answeredCount;

    private StudySession(final Builder builder) {
        this.id = builder.id;
        this.deckId = builder.deckId;
        this.mode = builder.mode;
        this.status = builder.status;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
        this.cardIds = Collections.unmodifiableList(builder.cardIds);
        this.answeredCount = builder.answeredCount;
    }

    /**
     * Cria um builder vazio.
     *
     * @return builder de {@link StudySession}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cria uma nova sessao em andamento.
     *
     * @param id identificador da sessao
     * @param deckId baralho estudado
     * @param mode modo de montagem da fila
     * @param startedAt inicio
     * @param cardIds fila de questoes
     * @return sessao IN_PROGRESS
     */
    public static StudySession start(final String id, final String deckId,
                                     final StudyMode mode, final Instant startedAt,
                                     final List<String> cardIds) {
        return builder()
                .id(id)
                .deckId(deckId)
                .mode(mode)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(startedAt)
                .cardIds(cardIds)
                .answeredCount(0)
                .build();
    }

    /**
     * Avanca a sessao em um item.
     *
     * @return nova sessao com o contador incrementado; se era o ultimo item,
     *     a sessao passa a FINISHED
     */
    public StudySession advance() {
        final int nextCount = Math.min(answeredCount + 1, cardIds.size());
        final boolean last = nextCount >= cardIds.size();
        return toBuilder()
                .status(last ? SessionStatus.FINISHED : status)
                .finishedAt(last ? Instant.now() : null)
                .answeredCount(nextCount)
                .build();
    }

    /**
     * Cria uma copia deste estado com os campos alterados.
     *
     * @return builder preenchido com os valores atuais
     */
    public Builder toBuilder() {
        return builder()
                .id(id)
                .deckId(deckId)
                .mode(mode)
                .status(status)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .cardIds(new java.util.ArrayList<>(cardIds))
                .answeredCount(answeredCount);
    }

    public String id() {
        return id;
    }

    public String deckId() {
        return deckId;
    }

    public StudyMode mode() {
        return mode;
    }

    public SessionStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public List<String> cardIds() {
        return cardIds;
    }

    public int answeredCount() {
        return answeredCount;
    }

    public int totalCount() {
        return cardIds.size();
    }

    public boolean isFinished() {
        return status == SessionStatus.FINISHED;
    }

    /**
     * Identificador da questao da posicao indicada.
     *
     * @param position indice baseado em zero
     * @return id da questao
     */
    public String cardIdAt(final int position) {
        return cardIds.get(position);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudySession)) {
            return false;
        }
        final StudySession that = (StudySession) other;
        return answeredCount == that.answeredCount
                && Objects.equals(id, that.id)
                && Objects.equals(deckId, that.deckId)
                && mode == that.mode
                && status == that.status
                && Objects.equals(startedAt, that.startedAt)
                && Objects.equals(finishedAt, that.finishedAt)
                && Objects.equals(cardIds, that.cardIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deckId, mode, status, startedAt, finishedAt, cardIds, answeredCount);
    }

    @Override
    public String toString() {
        return "StudySession{id=" + id + ", mode=" + mode + ", status=" + status
                + ", answered=" + answeredCount + '/' + cardIds.size() + '}';
    }

    /**
     * Builder de {@link StudySession} (evita construtor com muitos parametros).
     */
    public static final class Builder {
        private String id;
        private String deckId;
        private StudyMode mode;
        private SessionStatus status = SessionStatus.IN_PROGRESS;
        private Instant startedAt;
        private Instant finishedAt;
        private List<String> cardIds = new java.util.ArrayList<>();
        private int answeredCount;

        public Builder id(final String value) {
            this.id = value;
            return this;
        }

        public Builder deckId(final String value) {
            this.deckId = value;
            return this;
        }

        public Builder mode(final StudyMode value) {
            this.mode = value;
            return this;
        }

        public Builder status(final SessionStatus value) {
            this.status = value;
            return this;
        }

        public Builder startedAt(final Instant value) {
            this.startedAt = value;
            return this;
        }

        public Builder finishedAt(final Instant value) {
            this.finishedAt = value;
            return this;
        }

        public Builder cardIds(final List<String> value) {
            this.cardIds = value;
            return this;
        }

        public Builder answeredCount(final int value) {
            this.answeredCount = value;
            return this;
        }

        public StudySession build() {
            return new StudySession(this);
        }
    }
}
