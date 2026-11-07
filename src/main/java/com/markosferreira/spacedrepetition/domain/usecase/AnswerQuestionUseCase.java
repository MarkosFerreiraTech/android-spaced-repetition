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
    public AnswerResult execute(final AnswerCommand command) {
        if (command == null || command.card() == null) {
            throw new IllegalArgumentException("Questao obrigatoria");
        }
        final Card card = command.card();
        final int selectedIndex = command.selectedIndex();
        if (selectedIndex < 0 || selectedIndex >= card.options().size()) {
            throw new IllegalArgumentException("Alternativa fora da faixa: " + selectedIndex);
        }

        final boolean correct = selectedIndex == card.correctIndex();
        final Rating rating = RatingPolicy.derive(correct, command.flagged(),
                command.elapsedMillis(), command.averageMillis());
        final SchedulingInfo next = scheduler.schedule(card.scheduling(), rating, command.now());

        final ReviewLog log = ReviewLog.builder()
                .id(UUID.randomUUID().toString())
                .cardId(card.id())
                .sessionId(command.sessionId())
                .rating(rating)
                .correct(correct)
                .flagged(command.flagged())
                .elapsedMillis(command.elapsedMillis())
                .reviewedAt(command.now())
                .stabilityAfter(next.stability())
                .difficultyAfter(next.difficulty())
                .build();

        reviewRepository.record(log, next);

        return new AnswerResult(correct, card.correctIndex(), card.correctLetter(),
                card.explanation(), rating, next);
    }

    /**
     * Dados de uma resposta, agrupados para manter a assinatura do caso de uso
     * enxuta (regra ParameterNumber do Checkstyle).
     */
    public static final class AnswerCommand {
        private final Card card;
        private final String sessionId;
        private final int selectedIndex;
        private final boolean flagged;
        private final long elapsedMillis;
        private final long averageMillis;
        private final Instant now;

        private AnswerCommand(final Builder builder) {
            this.card = builder.card;
            this.sessionId = builder.sessionId;
            this.selectedIndex = builder.selectedIndex;
            this.flagged = builder.flagged;
            this.elapsedMillis = builder.elapsedMillis;
            this.averageMillis = builder.averageMillis;
            this.now = builder.now;
        }

        /**
         * Cria um builder de comando de resposta.
         *
         * @return builder
         */
        public static Builder builder() {
            return new Builder();
        }

        public Card card() {
            return card;
        }

        public String sessionId() {
            return sessionId;
        }

        public int selectedIndex() {
            return selectedIndex;
        }

        public boolean flagged() {
            return flagged;
        }

        public long elapsedMillis() {
            return elapsedMillis;
        }

        public long averageMillis() {
            return averageMillis;
        }

        public Instant now() {
            return now;
        }

        /**
         * Builder de {@link AnswerCommand}.
         */
        public static final class Builder {
            private Card card;
            private String sessionId;
            private int selectedIndex;
            private boolean flagged;
            private long elapsedMillis;
            private long averageMillis;
            private Instant now;

            public Builder card(final Card value) {
                this.card = value;
                return this;
            }

            public Builder sessionId(final String value) {
                this.sessionId = value;
                return this;
            }

            public Builder selectedIndex(final int value) {
                this.selectedIndex = value;
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

            public Builder averageMillis(final long value) {
                this.averageMillis = value;
                return this;
            }

            public Builder now(final Instant value) {
                this.now = value;
                return this;
            }

            public AnswerCommand build() {
                return new AnswerCommand(this);
            }
        }
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
