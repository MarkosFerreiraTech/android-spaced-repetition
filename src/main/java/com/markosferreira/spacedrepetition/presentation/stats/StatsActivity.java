package com.markosferreira.spacedrepetition.presentation.stats;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ActivityStatsBinding;

/**
 * Estatisticas de desempenho (RF19 a RF21).
 *
 * <p>Casca fina sobre {@link StatsViewModel}. As linhas por baralho sao
 * construidas em codigo porque sao texto simples e nao tem interacao — usar um
 * {@code RecyclerView} aqui seria estrutura sem uso.
 */
public final class StatsActivity extends AppCompatActivity {

    private ActivityStatsBinding binding;
    private StatsViewModel viewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = ((CerebroApplication) getApplication()).container().statsViewModel();

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.buttonStartStudying.setOnClickListener(view -> finish());

        viewModel.state().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(final StatsState state) {
        final boolean empty = state.status() == StatsState.Status.EMPTY;
        binding.statsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.statsContent.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            return;
        }

        binding.valueAccuracy.setText(state.accuracyText());
        binding.valueTotalReviews.setText(String.valueOf(state.totalReviews()));
        binding.valueStreak.setText(getString(R.string.stats_streak_value, state.streakDays()));

        final String worst = state.worstDeckName();
        if (worst == null) {
            binding.worstDeck.setVisibility(View.GONE);
        } else {
            binding.worstDeck.setVisibility(View.VISIBLE);
            binding.worstDeck.setText(getString(R.string.stats_worst_deck, worst));
        }

        renderDeckRows(state);
    }

    private void renderDeckRows(final StatsState state) {
        binding.byDeckContainer.removeAllViews();
        for (final StatsState.DeckRow row : state.decks()) {
            final com.google.android.material.textview.MaterialTextView line =
                    new com.google.android.material.textview.MaterialTextView(this);
            line.setText(row.label());
            line.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            line.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_s), 0, 0);
            binding.byDeckContainer.addView(line);
        }
    }
}
