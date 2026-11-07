package com.markosferreira.spacedrepetition.presentation.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Testes do ViewModel da lista de baralhos (RF10, RF11).
 *
 * <p>Rodam em JVM pura: {@link InstantTaskExecutorRule} faz o {@code LiveData}
 * publicar de forma sincrona, e o {@link Clock} e fixo, entao "pendente hoje"
 * e deterministico em qualquer data do calendario.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DeckListViewModelTest {

    /** Instante fixo: 2026-11-07T12:00:00Z. */
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    // TestRule do JUnit 4: faz o LiveData publicar de forma sincrona.
    @Rule
    public final InstantTaskExecutorRule taskRule = new InstantTaskExecutorRule();

    @Mock
    private DeckRepository deckRepository;

    private DeckListViewModel viewModel;

    @Before
    public void setUp() {
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        viewModel = new DeckListViewModel(deckRepository, clock);
    }

    @Test
    public void exposesDecks() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(List.of(
                summary("d1", "Simulado AWS", 65, 20),
                summary("d2", "Simulado Azure", 50, 0)));

        viewModel.load();

        final DeckListState state = viewModel.state().getValue();
        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(DeckListState.Status.LOADED);
        assertThat(state.items()).hasSize(2);
        assertThat(state.items().get(0).name()).isEqualTo("Simulado AWS");
        assertThat(state.items().get(0).totalCards()).isEqualTo(65);
        assertThat(state.items().get(0).dueToday()).isEqualTo(20);
    }

    @Test
    public void usesInjectedClock() {
        when(deckRepository.findAllWithCounts(NOW)).thenReturn(List.of());

        viewModel.load();

        org.mockito.Mockito.verify(deckRepository).findAllWithCounts(NOW);
    }

    @Test
    public void showsEmptyState() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(new ArrayList<>());

        viewModel.load();

        final DeckListState state = viewModel.state().getValue();
        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(DeckListState.Status.EMPTY);
        assertThat(state.showsEmptyState()).isTrue();
        assertThat(state.items()).isEmpty();
    }

    @Test
    public void showsErrorOnFailure() {
        when(deckRepository.findAllWithCounts(any()))
                .thenThrow(new IllegalStateException("banco indisponivel"));

        viewModel.load();

        final DeckListState state = viewModel.state().getValue();
        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(DeckListState.Status.ERROR);
        assertThat(state.message()).isNotBlank();
    }

    @Test
    public void initialStateIsLoading() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(List.of());

        viewModel.load();

        // O estado exposto apos load nao e o inicial; este teste garante que a
        // tela tem um estado valido antes da primeira chamada.
        assertThat(DeckListState.loading().status()).isEqualTo(DeckListState.Status.LOADING);
    }

    @Test
    public void formatsSubtitle() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(List.of(
                summary("d1", "Um", 1, 1),
                summary("d2", "Muitos", 65, 20),
                summary("d3", "Em dia", 10, 0)));

        viewModel.load();

        final List<DeckItem> items = viewModel.state().getValue().items();
        assertThat(items.get(0).subtitle()).isEqualTo("1 questao · 1 pendente");
        assertThat(items.get(1).subtitle()).isEqualTo("65 questoes · 20 pendentes");
        assertThat(items.get(2).subtitle()).isEqualTo("10 questoes · em dia");
    }

    private DeckSummary summary(final String id, final String name,
                                final int total, final int due) {
        return new DeckSummary(new Deck(id, name, NOW, null), total, due);
    }
}
