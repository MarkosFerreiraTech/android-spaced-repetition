package com.markosferreira.spacedrepetition.presentation.study;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.markosferreira.spacedrepetition.data.concurrent.AppExecutors;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.domain.model.StudySession;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.SessionRepository;
import com.markosferreira.spacedrepetition.domain.usecase.AnswerQuestionUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.StartStudySessionUseCase;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * ViewModel da sessao de estudo (RF12 a RF18).
 *
 * <p>Orquestra os casos de uso de inicio de sessao e de resposta, mantendo a
 * fila de questoes em memoria. O {@link Clock} e injetado para que a duracao
 * da resposta e o reagendamento sejam deterministicos nos testes.
 */
public final class StudyViewModel extends ViewModel {

    private final StartStudySessionUseCase startSession;
    private final AnswerQuestionUseCase answerQuestion;
    private final CardRepository cardRepository;
    private final SessionRepository sessionRepository;
    private final Executor io;
    private final Clock clock;

    private final MutableLiveData<StudyState> state =
            new MutableLiveData<>(StudyState.empty());

    private StudySession session;
    private List<Card> queue = new ArrayList<>();
    private int position;
    private int selectedIndex = -1;
    private boolean flagged;
    private int correctCount;
    private int wrongCount;
    private int flaggedCount;
    private long questionStartedAt;

    /**
     * Cria o ViewModel.
     *
     * @param startSession caso de uso de inicio
     * @param answerQuestion caso de uso de resposta
     * @param cardRepository porta de questoes
     * @param sessionRepository porta de sessoes
     * @param io executor de background
     * @param clock fonte de tempo
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public StudyViewModel(final StartStudySessionUseCase startSession,
                          final AnswerQuestionUseCase answerQuestion,
                          final CardRepository cardRepository,
                          final SessionRepository sessionRepository,
                          final Executor io, final Clock clock) {
        this.startSession = startSession;
        this.answerQuestion = answerQuestion;
        this.cardRepository = cardRepository;
        this.sessionRepository = sessionRepository;
        this.io = io;
        this.clock = clock;
    }

    /**
     * Estado observavel.
     *
     * @return estado atual
     */
    public LiveData<StudyState> state() {
        return state;
    }

    /**
     * Inicia uma sessao em um modo.
     *
     * @param deckId baralho
     * @param mode modo de estudo
     */
    public void start(final String deckId, final StudyMode mode) {
        // Montar a fila consulta o banco: precisa sair da thread de UI.
        io.execute(() -> startBlocking(deckId, mode));
    }

    private void startBlocking(final String deckId, final StudyMode mode) {
        session = startSession.execute(deckId, mode, clock.instant());
        queue = resolveQueue(session);
        position = 0;
        correctCount = 0;
        wrongCount = 0;
        flaggedCount = 0;
        if (queue.isEmpty()) {
            state.postValue(StudyState.empty());
            return;
        }
        showCurrent();
    }

    /**
     * Seleciona uma alternativa (ainda sem gravar).
     *
     * @param index indice escolhido
     */
    public void selectOption(final int index) {
        final StudyState current = state.getValue();
        if (current == null || current.phase() != StudyState.Phase.ANSWERING) {
            return;
        }
        if (index < 0 || index >= current.options().size()) {
            return;
        }
        selectedIndex = index;
        state.setValue(current.withSelection(index));
    }

    /**
     * Marca a questao com duvida (RF14).
     *
     * @param value marcado
     */
    public void setFlagged(final boolean value) {
        this.flagged = value;
    }

    /**
     * Confirma a resposta, grava a revisao e mostra o feedback.
     */
    public void confirm() {
        final StudyState current = state.getValue();
        if (current == null || current.phase() != StudyState.Phase.ANSWERING
                || !current.hasSelection()) {
            return;
        }
        io.execute(this::confirmBlocking);
    }

    private void confirmBlocking() {
        final StudyState current = state.getValue();
        if (current == null || current.phase() != StudyState.Phase.ANSWERING
                || !current.hasSelection()) {
            return;
        }
        final Card card = current.card();
        final long elapsed = elapsedSince(questionStartedAt);

        final AnswerQuestionUseCase.AnswerResult result =
                answerQuestion.execute(AnswerQuestionUseCase.AnswerCommand.builder()
                        .card(card)
                        .sessionId(session.id())
                        .selectedIndex(selectedIndex)
                        .flagged(flagged)
                        .elapsedMillis(elapsed)
                        .averageMillis(elapsed)
                        .now(clock.instant())
                        .build());

        if (result.correct()) {
            correctCount = correctCount + 1;
        } else {
            wrongCount = wrongCount + 1;
        }
        if (flagged) {
            flaggedCount = flaggedCount + 1;
        }
        state.postValue(StudyState.feedback(card, position + 1, queue.size(),
                selectedIndex, flagged, result.correct(), result.nextScheduling()));
    }

    /**
     * Avanca para a proxima questao, ou encerra a sessao.
     */
    public void next() {
        position = position + 1;
        selectedIndex = -1;
        flagged = false;
        if (position >= queue.size()) {
            finishSession();
            return;
        }
        showCurrent();
    }

    private void showCurrent() {
        questionStartedAt = clock.millis();
        state.postValue(StudyState.answering(queue.get(position), position + 1, queue.size()));
    }

    private void finishSession() {
        final StudySession finished = session.toBuilder()
                .status(com.markosferreira.spacedrepetition.domain.model.SessionStatus.FINISHED)
                .finishedAt(clock.instant())
                .answeredCount(correctCount + wrongCount)
                .build();
        sessionRepository.save(finished);
        state.postValue(StudyState.finished(correctCount, wrongCount, flaggedCount));
    }

    /**
     * Resolve a fila da sessao a partir dos identificadores persistidos.
     *
     * <p>A sessao guarda apenas ids; as questoes vem do repositorio para que um
     * agendamento atualizado durante a sessao seja refletido.
     */
    private List<Card> resolveQueue(final StudySession started) {
        final List<Card> cards = new ArrayList<>(started.cardIds().size());
        for (final String cardId : started.cardIds()) {
            final Card card = cardRepository.findById(cardId);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    private long elapsedSince(final long startMillis) {
        return Math.max(0L, clock.millis() - startMillis);
    }
}
