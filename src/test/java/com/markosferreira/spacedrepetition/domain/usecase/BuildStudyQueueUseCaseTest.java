package com.markosferreira.spacedrepetition.domain.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.fakes.InMemoryCardRepository;
import com.markosferreira.spacedrepetition.fakes.InMemoryReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Testes do modo de sessao: cenario central do produto (US04).
 *
 * <p>Reproduz o caso real descrito pelo Product Owner: um simulado de 65
 * questoes com 20 erros e 7 acertos marcados como duvidosos.
 */
@DisplayName("BuildStudyQueueUseCase")
class BuildStudyQueueUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final String DECK = "deck-aws";

    private InMemoryCardRepository cards;
    private InMemoryReviewRepository reviews;
    private BuildStudyQueueUseCase useCase;

    @BeforeEach
    void setUp() {
        cards = new InMemoryCardRepository();
        reviews = new InMemoryReviewRepository();
        useCase = new BuildStudyQueueUseCase(cards, reviews);
    }

    /**
     * Cria um simulado com 65 questoes em que as 20 primeiras foram erradas
     * e as questoes 20..26 foram acertadas com a marcacao de duvida.
     *
     * @return lista das 65 questoes
     */
    private List<Card> givenSimuladoWith65Questions() {
        final List<Card> all = new ArrayList<>();
        final List<Card> batch = new ArrayList<>();
        for (int i = 1; i <= 65; i++) {
            final Card card = Card.builder()
                    .id("card-" + i)
                    .deckId(DECK)
                    .question("Questao " + i)
                    .options(List.of("A", "B", "C", "D"))
                    .correctIndex(0)
                    .contentHash("hash-" + i)
                    .build();
            all.add(card);
            batch.add(card);
        }
        cards.upsertAll(batch);
        reviews.registerCards(DECK, all);

        for (int i = 1; i <= 20; i++) {
            recordOutcome("card-" + i, false, false);
        }
        for (int i = 21; i <= 27; i++) {
            recordOutcome("card-" + i, true, true);
        }
        for (int i = 28; i <= 65; i++) {
            recordOutcome("card-" + i, true, false);
        }
        return all;
    }

    private void recordOutcome(final String cardId, final boolean correct, final boolean flagged) {
        reviews.record(
                ReviewLog.builder()
                        .id("log-" + cardId)
                        .cardId(cardId)
                        .sessionId("session-1")
                        .rating(correct ? Rating.GOOD : Rating.AGAIN)
                        .correct(correct)
                        .flagged(flagged)
                        .elapsedMillis(10_000L)
                        .reviewedAt(NOW.minus(Duration.ofDays(1)))
                        .stabilityAfter(2d)
                        .difficultyAfter(5d)
                        .build(),
                SchedulingInfo.newCard());
    }

    @Test
    @DisplayName("modo COMPLETO devolve as 65 questoes")
    void fullModeReturnsEverything() {
        givenSimuladoWith65Questions();

        final List<Card> queue = useCase.execute(DECK, StudyMode.FULL, NOW);

        assertThat(queue).hasSize(65);
    }

    @Test
    @DisplayName("modo WRONG_ONLY devolve exatamente os 20 erros")
    void wrongOnlyReturnsTheTwentyWrongOnes() {
        givenSimuladoWith65Questions();

        final List<Card> queue = useCase.execute(DECK, StudyMode.WRONG_ONLY, NOW);

        assertThat(queue).hasSize(20);
        assertThat(queue).allMatch(card -> card.id().matches("card-([1-9]|1[0-9]|20)"));
    }

    @Test
    @DisplayName("modo WRONG_PLUS_FLAGGED devolve 20 erros mais 7 marcadas")
    void wrongPlusFlaggedReturnsTwentySeven() {
        givenSimuladoWith65Questions();

        final List<Card> queue = useCase.execute(DECK, StudyMode.WRONG_PLUS_FLAGGED, NOW);

        assertThat(queue).hasSize(27);
    }

    @Test
    @DisplayName("modo WRONG_ONLY sem erros devolve fila vazia")
    void wrongOnlyWithoutErrorsIsEmpty() {
        final List<Card> all = givenSimuladoWith65Questions();
        final InMemoryCardRepository cleanCards = new InMemoryCardRepository();
        cleanCards.upsertAll(all);
        final InMemoryReviewRepository cleanReviews = new InMemoryReviewRepository();
        cleanReviews.registerCards(DECK, all);
        for (final Card card : all) {
            cleanReviews.record(
                    ReviewLog.builder().id("ok-" + card.id()).cardId(card.id())
                            .sessionId("s").rating(Rating.EASY).correct(true).flagged(false)
                            .elapsedMillis(5_000L).reviewedAt(NOW).build(),
                    SchedulingInfo.newCard());
        }
        final BuildStudyQueueUseCase clean = new BuildStudyQueueUseCase(cleanCards, cleanReviews);

        assertThat(clean.execute(DECK, StudyMode.WRONG_ONLY, NOW)).isEmpty();
    }

    @Test
    @DisplayName("modo COMPLETO prioriza as questoes vencidas")
    void fullModePrioritisesDueCards() {
        final Card due = Card.builder().id("due").deckId(DECK).question("Vencida")
                .options(List.of("A", "B")).correctIndex(0).contentHash("h1")
                .scheduling(SchedulingInfo.newCard().toBuilder()
                        .stability(2d).difficulty(5d).repetitions(2)
                        .dueAt(NOW.minus(Duration.ofDays(1))).build())
                .build();
        final Card future = Card.builder().id("future").deckId(DECK).question("Futura")
                .options(List.of("A", "B")).correctIndex(0).contentHash("h2")
                .scheduling(SchedulingInfo.newCard().toBuilder()
                        .stability(30d).difficulty(5d).repetitions(2)
                        .dueAt(NOW.plus(Duration.ofDays(20))).build())
                .build();
        cards.upsertAll(List.of(future, due));

        final List<Card> queue = useCase.execute(DECK, StudyMode.FULL, NOW);

        assertThat(queue).extracting(Card::id).containsExactly("due", "future");
    }

    @Test
    @DisplayName("rejeita baralho vazio")
    void rejectsEmptyDeckId() {
        assertThatThrownBy(() -> useCase.execute("", StudyMode.FULL, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejeita modo nulo")
    void rejectsNullMode() {
        assertThatThrownBy(() -> useCase.execute(DECK, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("baralho sem questoes devolve fila vazia, nao nulo")
    void unknownDeckReturnsEmptyList() {
        assertThat(useCase.execute("inexistente", StudyMode.FULL, NOW)).isEmpty();
    }
}
