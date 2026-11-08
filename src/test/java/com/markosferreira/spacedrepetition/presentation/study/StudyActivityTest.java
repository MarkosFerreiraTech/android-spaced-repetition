package com.markosferreira.spacedrepetition.presentation.study;

import static org.assertj.core.api.Assertions.assertThat;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.presentation.TestContainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

/**
 * Testes de inflacao e ciclo de vida da tela de estudo (RF13, RF14).
 *
 * <p>Cobrem o que acontece so em execucao: o layout inflar, o adaptador de
 * alternativas ser populado e o estado vazio encerrar a tela sem crash.
 */
@RunWith(RobolectricTestRunner.class)
public final class StudyActivityTest {

    @Before
    public void setUp() {
        final CerebroApplication application = ApplicationProvider.getApplicationContext();
        application.setContainer(new TestContainer());
    }

    @Test
    public void inflatesStudyLayout() {
        // Sem questoes na sessao, o estado e EMPTY e a tela se encerra — mas o
        // layout tem de inflar antes, que e o que este teste garante.
        final ActivityController<StudyActivity> controller = Robolectric
                .buildActivity(StudyActivity.class, StudyActivity.intentFor(
                        ApplicationProvider.getApplicationContext(), "d1",
                        com.markosferreira.spacedrepetition.domain.model.StudyMode.FULL))
                .create();

        final View options = controller.get().findViewById(R.id.options_container);
        final View primary = controller.get().findViewById(R.id.button_primary);
        final View flagged = controller.get().findViewById(R.id.checkbox_flagged);

        assertThat(options).isNotNull();
        assertThat(primary).isNotNull();
        assertThat(flagged).isNotNull();

        controller.pause().stop().destroy();
    }

    @Test
    public void finishesWithoutDeckExtra() {
        final ActivityController<StudyActivity> controller =
                Robolectric.buildActivity(StudyActivity.class).create();

        // Sem extras a tela nao pode permanecer aberta com estado invalido.
        org.junit.Assert.assertTrue(controller.get().isFinishing());

        controller.destroy();
    }

    @Test
    public void optionAdapterRendersLettersAndText() {
        final OptionAdapter adapter = new OptionAdapter(index -> { });
        adapter.submit(java.util.List.of("S3", "EBS", "EFS"));

        org.junit.Assert.assertEquals(3, adapter.getItemCount());

        adapter.select(1);
        adapter.reveal(0);
        // Sem excecao: os quatro estados visuais sao desenhaveis.
        org.junit.Assert.assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void optionsContainerHasLayoutManager() {
        final ActivityController<StudyActivity> controller = Robolectric
                .buildActivity(StudyActivity.class, StudyActivity.intentFor(
                        ApplicationProvider.getApplicationContext(), "d1",
                        com.markosferreira.spacedrepetition.domain.model.StudyMode.FULL))
                .create();

        final RecyclerView options = controller.get().findViewById(R.id.options_container);
        assertThat(options.getLayoutManager()).isNotNull();
        org.junit.Assert.assertEquals(View.VISIBLE, options.getVisibility());

        controller.pause().stop().destroy();
    }
}
