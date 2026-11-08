package com.markosferreira.spacedrepetition.presentation.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.presentation.LooperIdle;
import com.markosferreira.spacedrepetition.presentation.TestContainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

/**
 * Testes de inflacao e ciclo de vida da tela de baralhos (RF10, RF11).
 *
 * <p>Robolectric executa a Activity de verdade: o layout e inflado, o
 * {@code onCreate} roda e o {@code ViewBinding} e populado. E o que cobre a
 * classe de erro mais comum de telas — id inexistente, tipo de view trocado,
 * observer duplicado — sem precisar de emulador.
 *
 * <p>O container de teste entrega repositorios em memoria e um executor
 * sincrono, entao o carregamento e deterministico e nao depende do SQLite.
 */
@RunWith(RobolectricTestRunner.class)
public final class DeckListActivityTest {

    private TestContainer container;

    @Before
    public void setUp() {
        final CerebroApplication application = ApplicationProvider.getApplicationContext();
        container = new TestContainer();
        application.setContainer(container);
    }

    @Test
    public void inflatesAndShowsEmptyStateWithoutDecks() {
        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();
        LooperIdle.idle();

        assertThat(controller.get().findViewById(R.id.empty_state).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(controller.get().findViewById(R.id.deck_list).getVisibility())
                .isEqualTo(View.GONE);

        controller.pause().stop().destroy();
    }

    @Test
    public void rendersDecksWhenRepositoryHasData() {
        container.withDeck("d1", "Simulado AWS", 65, 20)
                .withDeck("d2", "Simulado Azure", 40, 0);

        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();
        LooperIdle.idle();

        final RecyclerView list = controller.get().findViewById(R.id.deck_list);
        assertThat(list.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(list.getAdapter()).isNotNull();
        assertThat(list.getAdapter().getItemCount()).isEqualTo(2);
        assertThat(controller.get().findViewById(R.id.empty_state).getVisibility())
                .isEqualTo(View.GONE);

        controller.pause().stop().destroy();
    }

    @Test
    public void fabOpensImportScreen() {
        container.withDeck("d1", "Simulado AWS", 10, 1);
        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();
        LooperIdle.idle();

        controller.get().findViewById(R.id.fab_import).performClick();

        final Intent started = shadowOf(controller.get()).getNextStartedActivity();
        assertThat(started).isNotNull();

        controller.pause().stop().destroy();
    }

    @Test
    public void emptystateImportButtonOpensImportScreen() {
        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();
        LooperIdle.idle();

        controller.get().findViewById(R.id.button_empty_import).performClick();

        assertThat(shadowOf(controller.get()).getNextStartedActivity()).isNotNull();

        controller.pause().stop().destroy();
    }

    @Test
    public void reloadsOnResumeAfterDataChanges() {
        container.withDeck("d1", "Simulado AWS", 10, 5);
        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();

        // Simula a volta de uma sessao que criou um baralho novo.
        container.withDeck("d2", "Novo", 3, 3);
        controller.pause().resume();
        // O carregamento do onResume vai para o executor e volta por postValue.
        LooperIdle.idle();

        final RecyclerView list = controller.get().findViewById(R.id.deck_list);
        assertThat(list.getAdapter().getItemCount()).isEqualTo(2);

        controller.pause().stop().destroy();
    }

    @Test
    public void hidesFabOnEmptyState() {
        final ActivityController<DeckListActivity> controller =
                Robolectric.buildActivity(DeckListActivity.class).setup();
        LooperIdle.idle();

        // Dois botoes identicos confundem: no vazio, so o do estado vazio aparece.
        assertThat(controller.get().findViewById(R.id.fab_import).getVisibility())
                .isEqualTo(View.GONE);

        controller.pause().stop().destroy();
    }
}
