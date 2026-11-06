package com.markosferreira.spacedrepetition.domain.fsrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

/**
 * Testes do algoritmo FSRS.
 *
 * <p>Cobre as quatro garantias declaradas no javadoc de {@link FsrsScheduler}
 * e os casos-limite do modelo: primeira revisao, lapso, saturacao de limite e
 * comportamento deterministico. E o pacote com maior exigencia de cobertura do
 * projeto (RNF17: >= 95%).
 */
@DisplayName("FsrsScheduler")
class FsrsSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private final FsrsScheduler scheduler = FsrsScheduler.withDefaults();

    @Nested
    @DisplayName("primeira revisao")
    class FirstReview {

        @ParameterizedTest(name = "rating {0} produz estabilidade positiva e vencimento futuro")
        @EnumSource(Rating.class)
        void producesPositiveStability(final Rating rating) {
            final SchedulingInfo result = scheduler.schedule(
                    SchedulingInfo.newCard(), rating, NOW);

            assertThat(result.stability()).isPositive();
            assertThat(result.difficulty())
                    .isBetween(SchedulingInfo.MIN_DIFFICULTY, SchedulingInfo.MAX_DIFFICULTY);
            assertThat(result.dueAt()).isAfter(NOW);
            assertThat(result.repetitions()).isEqualTo(1);
            assertThat(result.lastReviewAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("acerto na primeira revisao entra em REVIEW")
        void goodEntersReviewState() {
            final SchedulingInfo result = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.GOOD, NOW);

            assertThat(result.state()).isEqualTo(CardState.REVIEW);
        }

        @Test
        @DisplayName("erro na primeira revisao entra em LEARNING e conta lapso")
        void againEntersLearningState() {
            final SchedulingInfo result = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.AGAIN, NOW);

            assertThat(result.state()).isEqualTo(CardState.LEARNING);
            assertThat(result.lapses()).isEqualTo(1);
        }

        @Test
        @DisplayName("estabilidade inicial cresce com a facilidade do rating")
        void initialStabilityIsMonotonic() {
            final double again = scheduler.schedule(SchedulingInfo.newCard(), Rating.AGAIN, NOW)
                    .stability();
            final double hard = scheduler.schedule(SchedulingInfo.newCard(), Rating.HARD, NOW)
                    .stability();
            final double good = scheduler.schedule(SchedulingInfo.newCard(), Rating.GOOD, NOW)
                    .stability();
            final double easy = scheduler.schedule(SchedulingInfo.newCard(), Rating.EASY, NOW)
                    .stability();

            assertThat(again).isLessThan(hard);
            assertThat(hard).isLessThan(good);
            assertThat(good).isLessThan(easy);
        }

        @Test
        @DisplayName("dificuldade inicial decresce com a facilidade do rating")
        void initialDifficultyIsAntiMonotonic() {
            final double again = scheduler.schedule(SchedulingInfo.newCard(), Rating.AGAIN, NOW)
                    .difficulty();
            final double easy = scheduler.schedule(SchedulingInfo.newCard(), Rating.EASY, NOW)
                    .difficulty();

            assertThat(again).isGreaterThan(easy);
        }
    }

    @Nested
    @DisplayName("revisoes subsequentes")
    class SubsequentReviews {

        @Test
        @DisplayName("GOOD espaça a revisao de uma questao ja em revisao")
        void goodIncreasesInterval() {
            final SchedulingInfo first = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.GOOD, NOW);
            final Instant nextReview = NOW.plus(Duration.ofDays(4));

            final SchedulingInfo second = scheduler.schedule(first, Rating.GOOD, nextReview);

            assertThat(second.stability()).isGreaterThan(first.stability());
            assertThat(second.dueAt()).isAfter(second.lastReviewAt());
        }

        @Test
        @DisplayName("AGAIN nunca aumenta o intervalo de uma questao em revisao")
        void againNeverIncreasesStability() {
            SchedulingInfo card = scheduler.schedule(SchedulingInfo.newCard(), Rating.GOOD, NOW);
            for (int day = 1; day <= 5; day++) {
                card = scheduler.schedule(card, Rating.GOOD, NOW.plus(Duration.ofDays(day * 10L)));
            }
            final double stabilityBefore = card.stability();

            final SchedulingInfo lapsed = scheduler.schedule(
                    card, Rating.AGAIN, NOW.plus(Duration.ofDays(60)));

            assertThat(lapsed.stability()).isLessThanOrEqualTo(stabilityBefore);
            assertThat(lapsed.state()).isEqualTo(CardState.RELEARNING);
            assertThat(lapsed.lapses()).isEqualTo(card.lapses() + 1);
        }

        @Test
        @DisplayName("EASY gera intervalo maior que GOOD, e GOOD maior que HARD")
        void intervalsAreOrderedByRating() {
            final SchedulingInfo base = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.GOOD, NOW);
            final Instant reviewAt = NOW.plus(Duration.ofDays(3));

            final long hard = daysUntil(base, Rating.HARD, reviewAt);
            final long good = daysUntil(base, Rating.GOOD, reviewAt);
            final long easy = daysUntil(base, Rating.EASY, reviewAt);

            assertThat(hard).isLessThanOrEqualTo(good);
            assertThat(good).isLessThanOrEqualTo(easy);
        }

        @Test
        @DisplayName("RELEARNING volta para REVIEW ao acertar")
        void relearningReturnsToReview() {
            final SchedulingInfo first = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.GOOD, NOW);
            final SchedulingInfo lapsed = scheduler.schedule(
                    first, Rating.AGAIN, NOW.plus(Duration.ofDays(2)));

            final SchedulingInfo recovered = scheduler.schedule(
                    lapsed, Rating.GOOD, NOW.plus(Duration.ofDays(3)));

            assertThat(lapsed.state()).isEqualTo(CardState.RELEARNING);
            assertThat(recovered.state()).isEqualTo(CardState.REVIEW);
        }

        private long daysUntil(final SchedulingInfo base, final Rating rating,
                               final Instant at) {
            final SchedulingInfo result = scheduler.schedule(base, rating, at);
            return Duration.between(at, result.dueAt()).toDays();
        }
    }

    @Nested
    @DisplayName("propriedades do modelo")
    class ModelProperties {

        @Test
        @DisplayName("e deterministico: mesma entrada, mesma saida")
        void isDeterministic() {
            final SchedulingInfo card = SchedulingInfo.newCard().toBuilder()
                    .stability(12d)
                    .difficulty(5d)
                    .repetitions(3)
                    .state(CardState.REVIEW)
                    .lastReviewAt(NOW.minus(Duration.ofDays(5)))
                    .build();

            final SchedulingInfo first = scheduler.schedule(card, Rating.GOOD, NOW);
            for (int i = 0; i < 100; i++) {
                assertThat(scheduler.schedule(card, Rating.GOOD, NOW)).isEqualTo(first);
            }
        }

        @ParameterizedTest(name = "estabilidade {0} dias permanece dentro dos limites")
        @ValueSource(doubles = {0d, -5d, 0.001d, 1d, 365d, 1_000_000d, Double.NaN})
        void stabilityStaysWithinBounds(final double stability) {
            final SchedulingInfo card = SchedulingInfo.newCard().toBuilder()
                    .stability(stability)
                    .difficulty(5d)
                    .repetitions(2)
                    .state(CardState.REVIEW)
                    .lastReviewAt(NOW.minus(Duration.ofDays(3)))
                    .build();

            final SchedulingInfo result = scheduler.schedule(card, Rating.GOOD, NOW);

            assertThat(result.stability())
                    .isBetween(SchedulingInfo.MIN_STABILITY, SchedulingInfo.MAX_STABILITY);
        }

        @Test
        @DisplayName("a dificuldade respeita a escala 1..10 mesmo apos muitos erros")
        void difficultySaturatesAtMaximum() {
            SchedulingInfo card = SchedulingInfo.newCard();
            for (int i = 0; i < 40; i++) {
                card = scheduler.schedule(card, Rating.AGAIN, NOW.plus(Duration.ofHours(i)));
            }

            assertThat(card.difficulty())
                    .isBetween(SchedulingInfo.MIN_DIFFICULTY, SchedulingInfo.MAX_DIFFICULTY);
            assertThat(card.difficulty()).isEqualTo(SchedulingInfo.MAX_DIFFICULTY);
        }

        @Test
        @DisplayName("o intervalo e sempre de pelo menos um dia")
        void intervalIsAtLeastOneDay() {
            final SchedulingInfo result = scheduler.schedule(
                    SchedulingInfo.newCard(), Rating.AGAIN, NOW);

            assertThat(Duration.between(NOW, result.dueAt()).toDays()).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("retrievabilidade cai com o tempo e fica entre 0 e 1")
        void retrievabilityDecays() {
            final double today = scheduler.retrievability(10d, 0L);
            final double later = scheduler.retrievability(10d, 10L);
            final double muchLater = scheduler.retrievability(10d, 365L);

            assertThat(today).isCloseTo(1d, org.assertj.core.data.Offset.offset(0.001d));
            assertThat(later).isLessThan(today);
            assertThat(muchLater).isLessThan(later).isGreaterThan(0d);
        }

        @Test
        @DisplayName("retrievabilidade de estabilidade nula e zero, sem NaN")
        void retrievabilityOfZeroStabilityIsZero() {
            assertThat(scheduler.retrievability(0d, 5L)).isZero();
        }

        @Test
        @DisplayName("intervalo satura no maximo permitido")
        void intervalSaturates() {
            assertThat(scheduler.intervalFromStability(1_000_000d))
                    .isEqualTo(FsrsParameters.MAXIMUM_INTERVAL_DAYS);
            assertThat(scheduler.intervalFromStability(0d)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("contrato de entrada")
    class InputContract {

        @Test
        @DisplayName("rejeita estado nulo")
        void rejectsNullState() {
            assertThatThrownBy(() -> scheduler.schedule(null, Rating.GOOD, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita rating nulo")
        void rejectsNullRating() {
            assertThatThrownBy(() -> scheduler.schedule(SchedulingInfo.newCard(), null, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita instante nulo")
        void rejectsNullInstant() {
            assertThatThrownBy(() -> scheduler.schedule(SchedulingInfo.newCard(), Rating.GOOD, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parametros exigem exatamente 18 pesos")
        void parametersRequireEighteenWeights() {
            assertThatThrownBy(() -> new FsrsParameters(new double[] {1d, 2d}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("18");
        }
    }
}
