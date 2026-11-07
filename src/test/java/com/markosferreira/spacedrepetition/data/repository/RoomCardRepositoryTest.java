package com.markosferreira.spacedrepetition.data.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.markosferreira.spacedrepetition.data.local.AppDatabase;
import com.markosferreira.spacedrepetition.data.local.CardDao;
import com.markosferreira.spacedrepetition.data.local.CardEntity;
import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;



/**
 * Testes do adaptador de questoes.
 *
 * <p>O foco e o contrato que o RF06 e o RF08 exigem: o upsert roda em
 * transacao e devolve as contagens corretas de inseridos e atualizados.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoomCardRepository")
class RoomCardRepositoryTest {

    private static final String DECK_ID = "deck-1";

    @Mock
    private AppDatabase database;

    @Mock
    private CardDao cardDao;

    private RoomCardRepository repository;

    @BeforeEach
    void setUp() {
        // A transacao do Room nao e um no-op: executa o bloco para que o teste
        // observe as chamadas ao DAO feitas dentro dela.
        doAnswer(invocation -> {
            final Runnable block = invocation.getArgument(0);
            block.run();
            return null;
        }).when(database).runInTransaction(any(Runnable.class));
        repository = new RoomCardRepository(database, cardDao);
    }

    @Test
    @DisplayName("persiste o lote dentro de uma transacao")
    void upsertRunsInTransaction() {
        when(cardDao.findHashesByDeck(DECK_ID)).thenReturn(new ArrayList<>());

        repository.upsertAll(List.of(card("hash-1"), card("hash-2")));

        verify(database).runInTransaction(any(Runnable.class));
        verify(cardDao).upsertAll(anyList());
    }

    @Test
    @DisplayName("conta como inseridas as questoes cujo hash ainda nao existe")
    void countsInserts() {
        when(cardDao.findHashesByDeck(DECK_ID)).thenReturn(new ArrayList<>());

        final CardRepository.UpsertResult result =
                repository.upsertAll(List.of(card("hash-1"), card("hash-2")));

        assertThat(result.inserted()).isEqualTo(2);
        assertThat(result.updated()).isZero();
    }

    @Test
    @DisplayName("conta como atualizadas as questoes cujo hash ja existe")
    void countsUpdates() {
        when(cardDao.findHashesByDeck(DECK_ID)).thenReturn(new ArrayList<>(List.of("hash-1")));

        final CardRepository.UpsertResult result =
                repository.upsertAll(List.of(card("hash-1"), card("hash-2")));

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
    }

    @Test
    @DisplayName("lote vazio nao abre transacao")
    void emptyBatchIsNoop() {
        final CardRepository.UpsertResult result = repository.upsertAll(List.of());

        assertThat(result.inserted()).isZero();
        assertThat(result.updated()).isZero();
        verify(database, org.mockito.Mockito.never()).runInTransaction(any(Runnable.class));
    }

    @Test
    @DisplayName("mapeia a questao para entidade preservando o hash de conteudo")
    void mapsHashToEntity() {
        when(cardDao.findHashesByDeck(DECK_ID)).thenReturn(new ArrayList<>());

        repository.upsertAll(List.of(card("hash-1")));

        final org.mockito.ArgumentCaptor<List<CardEntity>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(cardDao).upsertAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).contentHash).isEqualTo("hash-1");
        assertThat(captor.getValue().get(0).deckId).isEqualTo(DECK_ID);
    }

    private Card card(final String hash) {
        return Card.builder()
                .id("card-" + hash)
                .deckId(DECK_ID)
                .question("Enunciado de " + hash)
                .options(List.of("A", "B"))
                .correctIndex(0)
                .contentHash(hash)
                .build();
    }
}
