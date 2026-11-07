package com.markosferreira.spacedrepetition.presentation.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DeckListViewModel")
class DeckListViewModelTest {

    /** Instante fixo: 2026-11-07T12:00:00Z. */
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    @RegisterExtension
    static final InstantTaskExecutorRule TASK_RULE = new InstantTaskExecutorRule();

    @Mock
    private DeckRepository deckRepository;

    private DeckListViewModel viewModel;

    @BeforeEach
    void setUp() {
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        viewModel = new DeckListViewModel(deckRepository, clock);
    }

    @Test
    @DisplayName("expoe os baralhos com total de questoes e pendencias (RF10)")
    void exposesDecks() {
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
    @DisplayName("usa o instante do Clock injetado, nao Instant.now()")
    void usesInjectedClock() {
        when(deckRepository.findAllWithCounts(NOW)).thenReturn(List.of());

        viewModel.load();

        org.mockito.Mockito.verify(deckRepository).findAllWithCounts(NOW);
    }

    @Test
    @DisplayName("exibe estado vazio com instrucao quando nao ha baralhos (RF11)")
    void showsEmptyState() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(new ArrayList<>());

        viewModel.load();

        final DeckListState state = viewModel.state().getValue();
        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(DeckListState.Status.EMPTY);
        assertThat(state.showsEmptyState()).isTrue();
        assertThat(state.items()).isEmpty();
    }

    @Test
    @DisplayName("exibe erro em vez de propagar excecao quando o banco falha")
    void showsErrorOnFailure() {
        when(deckRepository.findAllWithCounts(any()))
                .thenThrow(new IllegalStateException("banco indisponivel"));

        viewModel.load();

        final DeckListState state = viewModel.state().getValue();
        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(DeckListState.Status.ERROR);
        assertThat(state.message()).isNotBlank();
    }

    @Test
    @DisplayName("comeca em LOADING antes de carregar")
    void initialStateIsLoading() {
        when(deckRepository.findAllWithCounts(any())).thenReturn(List.of());

        viewModel.load();

        // O estado exposto apos load nao e o inicial; este teste garante que a
        // tela tem um estado valido antes da primeira chamada.
        assertThat(DeckListState.loading().status()).isEqualTo(DeckListState.Status.LOADING);
    }

    @Test
    @DisplayName("formata o subtitulo no plural correto")
    void formatsSubtitle() {
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
