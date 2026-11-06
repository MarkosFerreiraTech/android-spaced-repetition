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

    public StudySession(final String id, final String deckId, final StudyMode mode,
                        final SessionStatus status, final Instant startedAt,
                        final Instant finishedAt, final List<String> cardIds,
                        final int answeredCount) {
        this.id = id;
        this.deckId = deckId;
        this.mode = mode;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.cardIds = Collections.unmodifiableList(cardIds);
        this.answeredCount = answeredCount;
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
        return new StudySession(id, deckId, mode, SessionStatus.IN_PROGRESS,
                startedAt, null, cardIds, 0);
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
        return new StudySession(id, deckId, mode,
                last ? SessionStatus.FINISHED : status,
                startedAt, last ? Instant.now() : null, cardIds, nextCount);
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
}
