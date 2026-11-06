package com.markosferreira.spacedrepetition.domain.fsrs;

import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.port.ReviewScheduler;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementacao do algoritmo FSRS (Free Spaced Repetition Scheduler).
 *
 * <p>O modelo e composto por tres variaveis:
 * <ul>
 *   <li><b>estabilidade</b> — dias ate a probabilidade de lembrar cair a 90%;</li>
 *   <li><b>dificuldade</b> — quao intrinsecamente dificil a questao e, na escala 1..10;</li>
 *   <li><b>retrievabilidade</b> — probabilidade de lembrar agora, derivada do tempo
 *       decorrido e da estabilidade.</li>
 * </ul>
 *
 * <p>Garantias verificadas por teste ({@code FsrsSchedulerTest}):
 * <ol>
 *   <li>Determinismo absoluto: mesma entrada, mesma saida.</li>
 *   <li>Monotonicidade: {@code EASY > GOOD > HARD > AGAIN} no intervalo gerado.</li>
 *   <li>Sanidade: {@code AGAIN} nunca aumenta o intervalo de uma questao em revisao.</li>
 *   <li>Limites: estabilidade e dificuldade ficam sempre dentro da faixa do modelo.</li>
 * </ol>
 */
public final class FsrsScheduler implements ReviewScheduler {

    private static final int MIN_DURATION_MINUTES = 1;

    private final FsrsParameters parameters;

    /**
     * Cria o agendador com os parametros informados.
     *
     * @param parameters parametros do modelo
     */
    public FsrsScheduler(final FsrsParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Cria o agendador com os parametros default da especificacao.
     *
     * @return agendador pronto para uso
     */
    public static FsrsScheduler withDefaults() {
        return new FsrsScheduler(FsrsParameters.defaults());
    }

    @Override
    public SchedulingInfo schedule(final SchedulingInfo current,
                                   final Rating rating,
                                   final Instant now) {
        if (current == null) {
            throw new IllegalArgumentException("Estado atual nao pode ser nulo");
        }
        if (rating == null) {
            throw new IllegalArgumentException("Rating nao pode ser nulo");
        }
        if (now == null) {
            throw new IllegalArgumentException("Instante da resposta nao pode ser nulo");
        }

        final boolean firstReview = current.state() == CardState.NEW || current.repetitions() == 0;
        final double stability = firstReview
                ? initialStability(rating)
                : nextStability(current, rating, now);
        final double difficulty = current.repetitions() == 0
                ? initialDifficulty(rating)
                : nextDifficulty(current.difficulty(), rating);

        final int intervalDays = intervalFromStability(stability);
        final CardState nextState = nextState(current.state(), rating, firstReview);

        return current.toBuilder()
                .stability(stability)
                .difficulty(difficulty)
                .dueAt(now.plus(Duration.ofDays(intervalDays)))
                .lastReviewAt(now)
                .repetitions(current.repetitions() + 1)
                .lapses(current.lapses() + (rating == Rating.AGAIN ? 1 : 0))
                .state(nextState)
                .build();
    }

    // ---------------------------------------------------------------- estabilidade

    /**
     * Estabilidade inicial, conforme w0..w3 da especificacao.
     *
     * @param rating avaliacao da primeira resposta
     * @return estabilidade em dias
     */
    double initialStability(final Rating rating) {
        return parameters.weight(rating.weight() - 1);
    }

    /**
     * Estabilidade apos uma revisao, no caso de acerto ou de erro.
     *
     * @param current estado atual
     * @param rating avaliacao
     * @param now instante da resposta
     * @return nova estabilidade, limitada a faixa do modelo
     */
    double nextStability(final SchedulingInfo current, final Rating rating, final Instant now) {
        final double retrievability = retrievability(current.stability(),
                elapsedDays(current.lastReviewAt(), now));

        if (rating == Rating.AGAIN) {
            return clampStability(stabilityAfterLapse(current, retrievability));
        }
        return clampStability(stabilityAfterRecall(current, rating, retrievability));
    }

    private double stabilityAfterRecall(final SchedulingInfo current,
                                        final Rating rating,
                                        final double retrievability) {
        final double w8 = parameters.weight(8);
        final double w9 = parameters.weight(9);
        final double w10 = parameters.weight(10);
        final double w15 = parameters.weight(15);

        final double hardPenalty = rating == Rating.HARD ? w15 : 1d;
        final double easyBonus = rating == Rating.EASY ? parameters.weight(16) : 1d;

        final double growth = Math.exp(w8)
                * (11d - current.difficulty())
                * Math.pow(current.stability(), -w9)
                * (Math.exp((1d - retrievability) * w10) - 1d)
                * hardPenalty
                * easyBonus;

        return current.stability() * (1d + growth);
    }

    private double stabilityAfterLapse(final SchedulingInfo current, final double retrievability) {
        final double w11 = parameters.weight(11);
        final double w12 = parameters.weight(12);
        final double w13 = parameters.weight(13);
        final double w14 = parameters.weight(14);

        final double newStability = w11
                * Math.pow(current.difficulty(), -w12)
                * (Math.pow(current.stability() + 1d, w13) - 1d)
                * Math.exp(w14 * (1d - retrievability));

        // Um erro nao pode aumentar a estabilidade (garantia de monotonicidade)
        return Math.min(newStability, current.stability());
    }

    // ---------------------------------------------------------------- dificuldade

    /**
     * Dificuldade inicial, conforme w4.
     *
     * @param rating avaliacao da primeira resposta
     * @return dificuldade na escala 1..10
     */
    double initialDifficulty(final Rating rating) {
        final double raw = parameters.weight(4) - (rating.weight() - 3) * parameters.weight(5);
        return clampDifficulty(raw, parameters.weight(4));
    }

    /**
     * Dificuldade apos a revisao, com amortecimento em direcao ao valor medio.
     *
     * @param currentDifficulty dificuldade atual
     * @param rating avaliacao
     * @return nova dificuldade na escala 1..10
     */
    double nextDifficulty(final double currentDifficulty, final Rating rating) {
        final double delta = rating == Rating.AGAIN ? parameters.weight(7) : -parameters.weight(6)
                * (rating.weight() - 3);
        final double damped = currentDifficulty + delta;
        final double meanReversion = parameters.weight(4);
        return clampDifficulty(
                meanReversion * (1d - parameters.weight(5)) + parameters.weight(5) * damped,
                currentDifficulty);
    }

    // ---------------------------------------------------------------- curva e intervalo

    /**
     * Probabilidade de lembrar, dado o tempo decorrido em dias.
     *
     * @param stability estabilidade em dias
     * @param elapsedDays dias desde a ultima revisao
     * @return probabilidade entre 0 e 1
     */
    double retrievability(final double stability, final long elapsedDays) {
        if (stability <= 0d) {
            return 0d;
        }
        final double factor = 0.9d / (1d / FsrsParameters.FACTOR) - 1d;
        return Math.pow(1d + factor * (elapsedDays / stability), FsrsParameters.DECAY);
    }

    /**
     * Converte estabilidade em intervalo de dias, para 90% de retencao.
     *
     * @param stability estabilidade em dias
     * @return intervalo em dias, entre 1 e o maximo permitido
     */
    int intervalFromStability(final double stability) {
        if (stability <= 0d) {
            return 1;
        }
        final double factor = 0.9d / (1d / FsrsParameters.FACTOR) - 1d;
        final double interval = stability / factor
                * (Math.pow(FsrsParameters.REQUEST_RETENTION, 1d / FsrsParameters.DECAY) - 1d);
        final long rounded = Math.round(interval);
        return (int) Math.max(1L, Math.min(FsrsParameters.MAXIMUM_INTERVAL_DAYS, rounded));
    }

    private long elapsedDays(final Instant lastReviewAt, final Instant now) {
        if (lastReviewAt == null) {
            return 0L;
        }
        final long minutes = Math.max(MIN_DURATION_MINUTES,
                Duration.between(lastReviewAt, now).toMinutes());
        return minutes / (24L * 60L);
    }

    // ---------------------------------------------------------------- estado

    private CardState nextState(final CardState current, final Rating rating,
                                final boolean firstReview) {
        if (firstReview) {
            return rating == Rating.AGAIN ? CardState.LEARNING : CardState.REVIEW;
        }
        if (rating == Rating.AGAIN) {
            return CardState.RELEARNING;
        }
        return current == CardState.RELEARNING ? CardState.REVIEW : current;
    }

    private double clampStability(final double value) {
        if (Double.isNaN(value) || value <= 0d) {
            return SchedulingInfo.MIN_STABILITY;
        }
        return Math.max(SchedulingInfo.MIN_STABILITY,
                Math.min(SchedulingInfo.MAX_STABILITY, value));
    }

    private double clampDifficulty(final double value, final double fallback) {
        if (Double.isNaN(value)) {
            return fallback;
        }
        return Math.max(SchedulingInfo.MIN_DIFFICULTY,
                Math.min(SchedulingInfo.MAX_DIFFICULTY, value));
    }
}
