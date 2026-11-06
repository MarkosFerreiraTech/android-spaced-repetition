package com.markosferreira.spacedrepetition.domain.usecase;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewScheduler;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra a resposta de uma questao e reagenda a proxima revisao (UC04).
 *
 * <p>Sequencia: deriva o rating, calcula o novo agendamento e grava log e
 * agendamento em uma unica transacao (RF16, UC04/E3).
 */
public final class AnswerQuestionUseCase {

    private final ReviewScheduler scheduler;
    private final ReviewRepository reviewRepository;

    public AnswerQuestionUseCase(final ReviewScheduler scheduler,
                                 final ReviewRepository reviewRepository) {
        this.scheduler = scheduler;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Processa uma resposta.
     *
     * @param card questao respondida
     * @param sessionId sessao em andamento
     * @param selectedIndex alternativa escolhida
     * @param flagged se marcou "acertei com duvida"
     * @param elapsedMillis tempo gasto
     * @param averageMillis tempo medio historico do estudante
     * @param now instante da resposta
     * @return resultado da resposta, com o feedback a exibir
     */
    public AnswerResult execute(final Card card,
                                final String sessionId,
                                final int selectedIndex,
                                final boolean flagged,
                                final long elapsedMillis,
                                final long averageMillis,
                                final Instant now) {
        if (card == null) {
            throw new IllegalArgumentException("Questao obrigatoria");
        }
        if (selectedIndex < 0 || selectedIndex >= card.options().size()) {
            throw new IllegalArgumentException("Alternativa fora da faixa: " + selectedIndex);
        }

        final boolean correct = selectedIndex == card.correctIndex();
        final Rating rating = RatingPolicy.derive(correct, flagged, elapsedMillis, averageMillis);
        final SchedulingInfo next = scheduler.schedule(card.scheduling(), rating, now);

        final ReviewLog log = ReviewLog.builder()
                .id(UUID.randomUUID().toString())
                .cardId(card.id())
                .sessionId(sessionId)
                .rating(rating)
                .correct(correct)
                .flagged(flagged)
                .elapsedMillis(elapsedMillis)
                .reviewedAt(now)
                .stabilityAfter(next.stability())
                .difficultyAfter(next.difficulty())
                .build();

        reviewRepository.record(log, next);

        return new AnswerResult(correct, card.correctIndex(), card.correctLetter(),
                card.explanation(), rating, next);
    }

    /**
     * Resultado de uma resposta, pronto para a camada de apresentacao.
     */
    public static final class AnswerResult {
        private final boolean correct;
        private final int correctIndex;
        private final String correctLetter;
        private final String explanation;
        private final Rating rating;
        private final SchedulingInfo nextScheduling;

        public AnswerResult(final boolean correct, final int correctIndex,
                            final String correctLetter, final String explanation,
                            final Rating rating, final SchedulingInfo nextScheduling) {
            this.correct = correct;
            this.correctIndex = correctIndex;
            this.correctLetter = correctLetter;
            this.explanation = explanation;
            this.rating = rating;
            this.nextScheduling = nextScheduling;
        }

        public boolean correct() {
            return correct;
        }

        public int correctIndex() {
            return correctIndex;
        }

        public String correctLetter() {
            return correctLetter;
        }

        public String explanation() {
            return explanation;
        }

        public Rating rating() {
            return rating;
        }

        public SchedulingInfo nextScheduling() {
            return nextScheduling;
        }
    }
}
