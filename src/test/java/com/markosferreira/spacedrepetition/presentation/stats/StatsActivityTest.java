package com.markosferreira.spacedrepetition.presentation.stats;

import static org.assertj.core.api.Assertions.assertThat;

import android.view.View;
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
 * Testes de inflacao e conteudo da tela de estatisticas (RF19 a RF21).
 */
@RunWith(RobolectricTestRunner.class)
public final class StatsActivityTest {

    private TestContainer container;

    @Before
    public void setUp() {
        final CerebroApplication application = ApplicationProvider.getApplicationContext();
        container = new TestContainer();
        application.setContainer(container);
    }

    @Test
    public void inflatesAndShowsEmptyStateWithoutReviews() {
        final ActivityController<StatsActivity> controller =
                Robolectric.buildActivity(StatsActivity.class).setup();
        LooperIdle.idle();

        assertThat(controller.get().findViewById(R.id.stats_empty).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(controller.get().findViewById(R.id.stats_content).getVisibility())
                .isEqualTo(View.GONE);

        controller.pause().stop().destroy();
    }

    @Test
    public void rendersAccuracyAndCounters() {
        container.withStats(200, 150, 4)
                .withDeckAccuracy("d1", "Simulado AWS", 100, 80)
                .withDeckAccuracy("d2", "Simulado Azure", 100, 70);

        final ActivityController<StatsActivity> controller =
                Robolectric.buildActivity(StatsActivity.class).setup();
        LooperIdle.idle();

        assertThat(text(controller, R.id.value_accuracy)).isEqualTo("75%");
        assertThat(text(controller, R.id.value_total_reviews)).isEqualTo("200");
        assertThat(text(controller, R.id.value_streak)).contains("4");

        controller.pause().stop().destroy();
    }

    @Test
    public void pointsOutTheWorstDeck() {
        container.withStats(200, 150, 1)
                .withDeckAccuracy("d1", "Simulado AWS", 100, 80)
                .withDeckAccuracy("d2", "Simulado Azure", 100, 70);

        final ActivityController<StatsActivity> controller =
                Robolectric.buildActivity(StatsActivity.class).setup();
        LooperIdle.idle();

        assertThat(controller.get().findViewById(R.id.worst_deck).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(text(controller, R.id.worst_deck)).contains("Simulado Azure");

        controller.pause().stop().destroy();
    }

    @Test
    public void buildsOneRowPerDeck() {
        container.withStats(50, 40, 2)
                .withDeckAccuracy("d1", "Um", 10, 9)
                .withDeckAccuracy("d2", "Dois", 10, 8)
                .withDeckAccuracy("d3", "Tres", 10, 7);

        final ActivityController<StatsActivity> controller =
                Robolectric.buildActivity(StatsActivity.class).setup();
        LooperIdle.idle();

        final android.widget.LinearLayout rows =
                controller.get().findViewById(R.id.by_deck_container);
        assertThat(rows.getChildCount()).isEqualTo(3);

        controller.pause().stop().destroy();
    }

    private String text(final ActivityController<StatsActivity> controller, final int id) {
        return ((TextView) controller.get().findViewById(id)).getText().toString();
    }
}
