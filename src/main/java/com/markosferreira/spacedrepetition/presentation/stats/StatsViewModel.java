package com.markosferreira.spacedrepetition.presentation.stats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.markosferreira.spacedrepetition.domain.port.StatisticsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel da tela de estatisticas (RF19 a RF21).
 *
 * <p>Toda agregacao ja vem pronta do repositorio, calculada em SQL. Aqui apenas
 * formatamos — nenhuma soma percorre listas de revisoes.
 */
public final class StatsViewModel extends ViewModel {

    private final StatisticsRepository statisticsRepository;
    private final java.util.concurrent.Executor io;

    private final MutableLiveData<StatsState> state =
            new MutableLiveData<>(StatsState.loading());

    /**
     * Cria o ViewModel.
     *
     * @param statisticsRepository porta de agregados
     * @param io executor de background
     */
    public StatsViewModel(final StatisticsRepository statisticsRepository,
                          final java.util.concurrent.Executor io) {
        this.statisticsRepository = statisticsRepository;
        this.io = io;
    }

    /**
     * Estado observavel.
     *
     * @return estado atual
     */
    public LiveData<StatsState> state() {
        return state;
    }

    /**
     * Carrega os agregados.
     */
    public void load() {
        // As agregacoes consultam o banco: fora da thread de UI.
        io.execute(this::loadBlocking);
    }

    private void loadBlocking() {
        final int total = statisticsRepository.totalReviews();
        if (total == 0) {
            state.postValue(StatsState.empty());
            return;
        }
        final StatisticsRepository.Accuracy global = statisticsRepository.globalAccuracy();
        final List<StatsState.DeckRow> rows = new ArrayList<>();
        for (final StatisticsRepository.DeckAccuracy deck : statisticsRepository.accuracyByDeck()) {
            rows.add(new StatsState.DeckRow(deck.deckName(),
                    deck.accuracy().correct(), deck.accuracy().total()));
        }
        state.postValue(StatsState.loaded(
                formatPercent(global.percentage()),
                total,
                statisticsRepository.currentStreakDays(),
                rows));
    }

    private String formatPercent(final double value) {
        return String.format(Locale.ROOT, "%.0f%%", value);
    }
}
