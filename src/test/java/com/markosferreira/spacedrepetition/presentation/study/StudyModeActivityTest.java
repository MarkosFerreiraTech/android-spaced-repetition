package com.markosferreira.spacedrepetition.presentation.study;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Intent;
import android.widget.TextView;

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
 * Testes da escolha de modo (RF12).
 *
 * <p>Esta tela derrubava o app logo apos a importacao: o {@code onCreate}
 * consultava o banco na thread principal e o Room lancava excecao. Os testes
 * abaixo fixam o comportamento esperado — abrir sem crash, mostrar as contagens
 * e so navegar quando ha questoes.
 */
@RunWith(RobolectricTestRunner.class)
public final class StudyModeActivityTest {

    private TestContainer container;

    @Before
    public void setUp() {
        final CerebroApplication application = ApplicationProvider.getApplicationContext();
        container = new TestContainer();
        application.setContainer(container);
    }

    @Test
    public void opensWithoutCrashAfterImport() {
        container.withStudyCards("d1", 4);

        final ActivityController<StudyModeActivity> controller = abrir("d1");

        assertThat(controller.get().isFinishing()).isFalse();
        assertThat(controller.get().findViewById(R.id.card_full)).isNotNull();

        controller.pause().stop().destroy();
    }

    @Test
    public void showsRealCountOfCards() {
        container.withStudyCards("d1", 4);

        final ActivityController<StudyModeActivity> controller = abrir("d1");

        // A descricao do modo completo carrega o total de questoes.
        assertThat(text(controller, R.id.desc_full)).contains("4");

        controller.pause().stop().destroy();
    }

    @Test
    public void fullModeStartsEvenWithoutWrongAnswers() {
        container.withStudyCards("d1", 4);

        final ActivityController<StudyModeActivity> controller = abrir("d1");
        controller.get().findViewById(R.id.card_full).performClick();

        assertThat(org.robolectric.Shadows.shadowOf(controller.get()).getNextStartedActivity())
                .isNotNull();

        controller.pause().stop().destroy();
    }

    @Test
    public void wrongModesDoNothingWhenNoReviewExists() {
        container.withStudyCards("d1", 4);

        final ActivityController<StudyModeActivity> controller = abrir("d1");
        controller.get().findViewById(R.id.card_wrong).performClick();

        // Sem erros registrados nao ha o que revisar: a tela nao navega.
        assertThat(org.robolectric.Shadows.shadowOf(controller.get()).getNextStartedActivity())
                .isNull();
        assertThat(controller.get().isFinishing()).isFalse();

        controller.pause().stop().destroy();
    }

    @Test
    public void finishesWithoutDeckExtra() {
        final ActivityController<StudyModeActivity> controller =
                Robolectric.buildActivity(StudyModeActivity.class).create();

        assertThat(controller.get().isFinishing()).isTrue();

        controller.destroy();
    }

    @Test
    public void intentCarriesDeckId() {
        final Intent intent = StudyModeActivity.intentFor(
                ApplicationProvider.getApplicationContext(), "d1");
        assertThat(intent.getStringExtra("deck_id")).isEqualTo("d1");
    }

    private ActivityController<StudyModeActivity> abrir(final String deckId) {
        final ActivityController<StudyModeActivity> controller = Robolectric
                .buildActivity(StudyModeActivity.class,
                        StudyModeActivity.intentFor(
                                ApplicationProvider.getApplicationContext(), deckId))
                .setup();
        LooperIdle.idle();
        return controller;
    }

    private String text(final ActivityController<StudyModeActivity> controller, final int id) {
        return ((TextView) controller.get().findViewById(id)).getText().toString();
    }
}
