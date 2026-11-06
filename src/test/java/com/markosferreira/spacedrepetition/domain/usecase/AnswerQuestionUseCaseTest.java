package com.markosferreira.spacedrepetition.domain.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markosferreira.spacedrepetition.domain.fsrs.FsrsScheduler;
import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.fakes.InMemoryReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

@DisplayName("AnswerQuestionUseCase")
class AnswerQuestionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private InMemoryReviewRepository reviews;
    private AnswerQuestionUseCase useCase;
    private Card card;

    @BeforeEach
    void setUp() {
        reviews = new InMemoryReviewRepository();
        useCase = new AnswerQuestionUseCase(FsrsScheduler.withDefaults(), reviews);
        card = Card.builder()
                .id("card-1")
                .deckId("deck-1")
                .question("Qual servico oferece object storage?")
                .options(List.of("S3", "EBS", "EFS", "Glacier"))
                .correctIndex(0)
                .explanation("S3 e o servico de object storage.")
                .contentHash("hash-1")
                .build();
    }

    @Test
    @DisplayName("acerto grava log com rating GOOD e reagenda")
    void correctAnswerRecordsLog() {
        final AnswerQuestionUseCase.AnswerResult result = useCase.execute(
                card, "session-1", 0, false, 12_000L, 20_000L, NOW);

        assertThat(result.correct()).isTrue();
        assertThat(result.rating()).isEqualTo(Rating.GOOD);
        assertThat(result.correctLetter()).isEqualTo("A");
        assertThat(result.explanation()).contains("object storage");
        assertThat(result.nextScheduling().stability()).isPositive();
        assertThat(reviews.logs()).hasSize(1);
        assertThat(reviews.logs().get(0).correct()).isTrue();
        assertThat(reviews.schedulingOf("card-1")).isEqualTo(result.nextScheduling());
    }

    @Test
    @DisplayName("erro grava AGAIN, incrementa lapso e aponta a alternativa correta")
    void wrongAnswerRecordsAgain() {
        final SchedulingInfo before = card.scheduling();

        final AnswerQuestionUseCase.AnswerResult result = useCase.execute(
                card, "session-1", 2, false, 30_000L, 20_000L, NOW);

        assertThat(result.correct()).isFalse();
        assertThat(result.rating()).isEqualTo(Rating.AGAIN);
        assertThat(result.correctIndex()).isZero();
        assertThat(result.nextScheduling().lapses()).isEqualTo(before.lapses() + 1);
        assertThat(reviews.logs().get(0).flagged()).isFalse();
    }

    @Test
    @DisplayName("acerto marcado como duvida grava HARD e preserva o flag")
    void flaggedAnswerRecordsHard() {
        final AnswerQuestionUseCase.AnswerResult result = useCase.execute(
                card, "session-1", 0, true, 8_000L, 20_000L, NOW);

        assertThat(result.rating()).isEqualTo(Rating.HARD);
        assertThat(reviews.logs().get(0).flagged()).isTrue();
    }

    @Test
    @DisplayName("acerto muito rapido grava EASY")
    void fastAnswerRecordsEasy() {
        final AnswerQuestionUseCase.AnswerResult result = useCase.execute(
                card, "session-1", 0, false, 1_500L, 20_000L, NOW);

        assertThat(result.rating()).isEqualTo(Rating.EASY);
    }

    @Test
    @DisplayName("rejeita questao nula")
    void rejectsNullCard() {
        assertThatThrownBy(() -> useCase.execute(
                null, "session-1", 0, false, 1_000L, 20_000L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejeita alternativa fora da faixa")
    void rejectsOutOfRangeOption() {
        assertThatThrownBy(() -> useCase.execute(
                card, "session-1", 9, false, 1_000L, 20_000L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(
                card, "session-1", -1, false, 1_000L, 20_000L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
