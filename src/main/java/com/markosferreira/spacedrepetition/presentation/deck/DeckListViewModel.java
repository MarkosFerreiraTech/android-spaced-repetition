package com.markosferreira.spacedrepetition.presentation.deck;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.markosferreira.spacedrepetition.data.concurrent.AppExecutors;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * ViewModel da lista de baralhos (RF10, RF11).
 *
 * <p>Nao conhece Activity, ViewBinding nem Android View: apenas expoe estado
 * observavel. Isso permite testar a regra de apresentacao em JVM pura, com
 * {@code InstantTaskExecutorRule}, sem Robolectric.
 *
 * <p>O {@link Clock} e injetado para que "pendente hoje" seja deterministico —
 * um teste que depende de {@code Instant.now()} falha em datas de virada de
 * fuso, que e exatamente o tipo de teste fragil que a issue #16 proibe.
 */
public final class DeckListViewModel extends ViewModel {

    private final DeckRepository deckRepository;
    private final Executor io;
    private final Clock clock;

    private final MutableLiveData<DeckListState> state = new MutableLiveData<>();

    /**
     * Cria o ViewModel.
     *
     * @param deckRepository porta de baralhos
     * @param io executor de background (o Room proibe acesso na thread de UI)
     * @param clock fonte de tempo, injetavel para teste
     */
    public DeckListViewModel(final DeckRepository deckRepository, final Executor io,
                             final Clock clock) {
        this.deckRepository = deckRepository;
        this.io = io;
        this.clock = clock;
    }

    /**
     * Estado observavel da tela.
     *
     * @return estado atual
     */
    public LiveData<DeckListState> state() {
        return state;
    }

    /**
     * Carrega os baralhos com os contadores de pendencias.
     *
     * <p>Chamado pelo {@code onResume} da tela, para que um baralho estudado em
     * outra tela apareca atualizado ao voltar.
     */
    public void load() {
        state.setValue(DeckListState.loading());
        // Room lanca excecao na thread principal: a consulta vai para background
        // e o resultado volta por postValue, que publica na thread de UI.
        io.execute(this::loadBlocking);
    }

    private void loadBlocking() {
        try {
            final Instant now = clock.instant();
            final List<DeckSummary> summaries = deckRepository.findAllWithCounts(now);
            if (summaries.isEmpty()) {
                // RF11: estado vazio com instrucao de importacao.
                state.postValue(DeckListState.empty());
                return;
            }
            final List<DeckItem> items = new ArrayList<>(summaries.size());
            for (final DeckSummary summary : summaries) {
                items.add(toItem(summary));
            }
            state.postValue(DeckListState.loaded(items));
        } catch (final RuntimeException exception) {
            state.postValue(DeckListState.failed("Nao foi possivel carregar os baralhos"));
        }
    }

    private DeckItem toItem(final DeckSummary summary) {
        return new DeckItem(
                summary.deck().id(),
                summary.deck().name(),
                summary.totalCards(),
                summary.dueToday());
    }
}
