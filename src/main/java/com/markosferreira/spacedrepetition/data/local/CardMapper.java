package com.markosferreira.spacedrepetition.data.local;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.model.SessionStatus;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.domain.model.StudySession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converte entre entidades do Room e modelos de dominio.
 *
 * <p>Concentrar a conversao aqui garante que {@code @Entity} nunca apareca fora
 * da camada de dados — regra verificada por ArchUnit.
 */
public final class CardMapper {

    private CardMapper() {
        // Classe utilitaria: nao instanciavel.
    }

    /**
     * Converte entidade em modelo de dominio.
     *
     * @param entity entidade
     * @return questao de dominio
     */
    public static Card toDomain(final CardEntity entity) {
        return Card.builder()
                .id(entity.id)
                .deckId(entity.deckId)
                .question(entity.question)
                .options(OptionCodec.decode(entity.optionsJson))
                .correctIndex(entity.correctIndex)
                .explanation(entity.explanation)
                .contentHash(entity.contentHash)
                .scheduling(SchedulingInfo.builder()
                        .stability(entity.stability)
                        .difficulty(entity.difficulty)
                        .dueAt(toInstant(entity.dueAt))
                        .lastReviewAt(toInstant(entity.lastReviewAt))
                        .repetitions(entity.repetitions)
                        .lapses(entity.lapses)
                        .state(toCardState(entity.state))
                        .build())
                .build();
    }

    /**
     * Converte modelo de dominio em entidade.
     *
     * @param card questao
     * @return entidade
     */
    public static CardEntity toEntity(final Card card) {
        final SchedulingInfo scheduling = card.scheduling();
        return new CardEntity(card.id(), card.deckId(), card.question(),
                OptionCodec.encode(card.options()), card.correctIndex(), card.explanation(),
                card.contentHash(), scheduling.stability(), scheduling.difficulty(),
                toEpochMillis(scheduling.dueAt()), toEpochMillis(scheduling.lastReviewAt()),
                scheduling.repetitions(), scheduling.lapses(), scheduling.state().ordinal());
    }

    /**
     * Converte entidade de baralho em modelo de dominio.
     *
     * @param entity entidade
     * @return baralho
     */
    public static Deck toDomain(final DeckEntity entity) {
        return new Deck(entity.id, entity.name, Instant.ofEpochMilli(entity.createdAt),
                toInstant(entity.lastStudiedAt));
    }

    /**
     * Converte baralho em entidade.
     *
     * @param deck baralho
     * @return entidade
     */
    public static DeckEntity toEntity(final Deck deck) {
        return new DeckEntity(deck.id(), deck.name(), deck.createdAt().toEpochMilli(),
                toEpochMillis(deck.lastStudiedAt()));
    }

    /**
     * Converte a projecao de lista em resumo de dominio.
     *
     * @param row projecao
     * @return resumo
     */
    public static DeckSummary toSummary(final DeckSummaryRow row) {
        final Deck deck = new Deck(row.id, row.name, Instant.ofEpochMilli(row.createdAt),
                toInstant(row.lastStudiedAt));
        return new DeckSummary(deck, row.totalCards, row.dueToday);
    }

    /**
     * Converte log de dominio em entidade.
     *
     * @param log registro
     * @return entidade
     */
    public static ReviewLogEntity toEntity(final ReviewLog log) {
        return new ReviewLogEntity(log.id(), log.cardId(), log.sessionId(),
                log.rating().weight(), log.correct(), log.flagged(), log.elapsedMillis(),
                log.reviewedAt().toEpochMilli(), log.stabilityAfter(), log.difficultyAfter());
    }

    /**
     * Converte entidade de log em dominio.
     *
     * @param entity entidade
     * @return registro
     */
    public static ReviewLog toDomain(final ReviewLogEntity entity) {
        return ReviewLog.builder()
                .id(entity.id)
                .cardId(entity.cardId)
                .sessionId(entity.sessionId)
                .rating(Rating.fromWeight(entity.rating))
                .correct(entity.correct)
                .flagged(entity.flagged)
                .elapsedMillis(entity.elapsedMillis)
                .reviewedAt(Instant.ofEpochMilli(entity.reviewedAt))
                .stabilityAfter(entity.stabilityAfter)
                .difficultyAfter(entity.difficultyAfter)
                .build();
    }

    /**
     * Converte sessao de dominio em entidade.
     *
     * @param session sessao
     * @return entidade
     */
    public static StudySessionEntity toEntity(final StudySession session) {
        return new StudySessionEntity(session.id(), session.deckId(), session.mode().ordinal(),
                session.status().ordinal(), session.startedAt().toEpochMilli(),
                toEpochMillis(session.finishedAt()), String.join(",", session.cardIds()),
                session.answeredCount());
    }

    /**
     * Converte entidade de sessao em dominio.
     *
     * @param entity entidade
     * @return sessao
     */
    public static StudySession toDomain(final StudySessionEntity entity) {
        final List<String> ids = entity.cardIds.isEmpty()
                ? new ArrayList<>() : Arrays.asList(entity.cardIds.split(","));
        return new StudySession(entity.id, entity.deckId,
                StudyMode.values()[entity.mode], SessionStatus.values()[entity.status],
                Instant.ofEpochMilli(entity.startedAt), toInstant(entity.finishedAt),
                ids, entity.answeredCount);
    }

    /**
     * Converte epoch millis em instante, tolerando nulo.
     *
     * @param value millis
     * @return instante ou null
     */
    public static Instant toInstant(final Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    /**
     * Converte instante em epoch millis, tolerando nulo.
     *
     * @param value instante
     * @return millis ou null
     */
    public static Long toEpochMillis(final Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    private static CardState toCardState(final int ordinal) {
        final CardState[] values = CardState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CardState.NEW;
    }
}
