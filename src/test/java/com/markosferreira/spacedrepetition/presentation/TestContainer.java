package com.markosferreira.spacedrepetition.presentation;

import com.markosferreira.spacedrepetition.di.AppContainer;
import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;
import com.markosferreira.spacedrepetition.domain.port.SessionRepository;
import com.markosferreira.spacedrepetition.domain.port.StatisticsRepository;
import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;
import com.markosferreira.spacedrepetition.domain.model.StudySession;
import com.markosferreira.spacedrepetition.domain.usecase.AnswerQuestionUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.BuildStudyQueueUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.StartStudySessionUseCase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Container de teste para os testes de tela.
 *
 * <p>Resolve dois problemas de uma vez: substitui os repositorios Room por
 * dublês em memoria (o banco real exigiria device ou Robolectric com SQLite) e
 * troca o executor de IO por um sincrono, que roda na mesma thread e torna o
 * carregamento deterministico — sem {@code Thread.sleep} nos testes.
 */
public final class TestContainer extends AppContainer {

    private final List<DeckSummary> decks = new ArrayList<>();
    private final List<StatisticsRepository.DeckAccuracy> accuracies = new ArrayList<>();

    private int totalReviews;
    private int correctReviews;
    private int streak;

    /**
     * Cria o container de teste.
     *
     * <p>O {@code Context} e nulo porque todos os metodos que o usariam
     * (banco, preferencias) sao sobrescritos aqui; nenhum teste de tela toca o
     * armazenamento real.
     */
    public TestContainer() {
        super(null);
    }

    /**
     * Executor sincrono: roda imediatamente, na thread do chamador.
     */
    private static final Executor SYNC = Runnable::run;

    /**
     * Adiciona um baralho ao dublê.
     *
     * @param id identificador
     * @param name nome
     * @param total total de questoes
     * @param due pendentes hoje
     * @return este container
     */
    public TestContainer withDeck(final String id, final String name,
                                  final int total, final int due) {
        decks.add(new DeckSummary(new Deck(id, name,
                Instant.parse("2026-11-07T12:00:00Z"), null), total, due));
        return this;
    }

    /**
     * Define os numeros de estatistica.
     *
     * @param total revisoes
     * @param correct acertos
     * @param streakDays sequencia de dias
     * @return este container
     */
    public TestContainer withStats(final int total, final int correct, final int streakDays) {
        this.totalReviews = total;
        this.correctReviews = correct;
        this.streak = streakDays;
        return this;
    }

    /**
     * Adiciona uma linha de acuracia por baralho.
     *
     * @param deckId identificador
     * @param deckName nome
     * @param total respondidas
     * @param correct acertos
     * @return este container
     */
    public TestContainer withDeckAccuracy(final String deckId, final String deckName,
                                          final int total, final int correct) {
        accuracies.add(new StatisticsRepository.DeckAccuracy(deckId, deckName,
                new StatisticsRepository.Accuracy(correct, total)));
        return this;
    }

    @Override
    public Executor io() {
        return SYNC;
    }

    /**
     * ViewModel de estudo com dublês em memoria.
     *
     * <p>Sobrescrito para que o container base nao tente abrir o Room com
     * contexto nulo — foi exatamente o que o Robolectric apontou.
     */
    @Override
    public com.markosferreira.spacedrepetition.presentation.study.StudyViewModel studyViewModel() {
        final CardRepository cards = cardRepository();
        final SessionRepository sessions = sessionRepository();
        return new com.markosferreira.spacedrepetition.presentation.study.StudyViewModel(
                new StartStudySessionUseCase(
                        new BuildStudyQueueUseCase(cards, reviewRepository()), sessions),
                new AnswerQuestionUseCase(scheduler(), reviewRepository()),
                cards, sessions, SYNC, java.time.Clock.systemDefaultZone());
    }

    @Override
    public synchronized CardRepository cardRepository() {
        return new CardRepository() {
            @Override
            public List<Card> findByDeck(final String deckId) {
                return new ArrayList<>();
            }

            @Override
            public Card findById(final String cardId) {
                return null;
            }

            @Override
            public UpsertResult upsertAll(final List<Card> cards) {
                return new UpsertResult(0, 0);
            }

            @Override
            public java.util.Set<String> findHashesByDeck(final String deckId) {
                return new java.util.HashSet<>();
            }
        };
    }

    @Override
    public synchronized ReviewRepository reviewRepository() {
        return new ReviewRepository() {
            @Override
            public void record(final ReviewLog log, final SchedulingInfo nextScheduling) {
                // Dublê: sem persistencia.
            }

            @Override
            public List<Card> findWrongByDeck(final String deckId) {
                return new ArrayList<>();
            }

            @Override
            public List<Card> findWrongOrFlaggedByDeck(final String deckId) {
                return new ArrayList<>();
            }
        };
    }

    @Override
    public synchronized SessionRepository sessionRepository() {
        return new SessionRepository() {
            @Override
            public void save(final StudySession session) {
                // Dublê: sem persistencia.
            }

            @Override
            public StudySession findInProgressByDeck(final String deckId) {
                return null;
            }

            @Override
            public StudySession findById(final String sessionId) {
                return null;
            }
        };
    }

    @Override
    public synchronized DeckRepository deckRepository() {
        return new DeckRepository() {
            @Override
            public List<DeckSummary> findAllWithCounts(final Instant now) {
                return new ArrayList<>(decks);
            }

            @Override
            public Deck findByName(final String name) {
                return null;
            }

            @Override
            public Deck findById(final String deckId) {
                return null;
            }

            @Override
            public void save(final Deck deck) {
                // Dublê: sem persistencia.
            }
        };
    }

    @Override
    public synchronized StatisticsRepository statisticsRepository() {
        return new StatisticsRepository() {
            @Override
            public Accuracy globalAccuracy() {
                return new Accuracy(correctReviews, totalReviews);
            }

            @Override
            public int totalReviews() {
                return totalReviews;
            }

            @Override
            public int currentStreakDays() {
                return streak;
            }

            @Override
            public List<DeckAccuracy> accuracyByDeck() {
                return new ArrayList<>(accuracies);
            }
        };
    }
}
