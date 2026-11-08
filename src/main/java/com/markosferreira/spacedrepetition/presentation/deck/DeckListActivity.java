package com.markosferreira.spacedrepetition.presentation.deck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.di.AppContainer;
import com.markosferreira.spacedrepetition.databinding.ActivityDeckListBinding;
import com.markosferreira.spacedrepetition.presentation.importing.ImportActivity;
import com.markosferreira.spacedrepetition.presentation.stats.StatsActivity;
import com.markosferreira.spacedrepetition.presentation.study.StudyModeActivity;

/**
 * Tela inicial: lista de baralhos (RF10, RF11).
 *
 * <p>A Activity e uma casca fina: cria o adaptador, observa o estado do
 * ViewModel e delega toques. Nenhuma regra de negocio vive aqui — o que
 * permite testar toda a logica de apresentacao sem inflar a tela.
 */
public final class DeckListActivity extends AppCompatActivity {

    private ActivityDeckListBinding binding;
    private DeckListViewModel viewModel;
    private DeckAdapter adapter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeckListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final AppContainer container = ((CerebroApplication) getApplication()).container();
        viewModel = container.deckListViewModel();

        // O diff roda no executor do container: em producao um pool, nos testes
        // o mesmo executor sincrono dos ViewModels.
        adapter = new DeckAdapter(this::openStudyModes, container.io());
        binding.deckList.setLayoutManager(new LinearLayoutManager(this));
        binding.deckList.setAdapter(adapter);

        binding.fabImport.setOnClickListener(view -> openImport());
        binding.buttonEmptyImport.setOnClickListener(view -> openImport());
        binding.buttonRetry.setOnClickListener(view -> viewModel.load());
        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_stats) {
                startActivity(new Intent(this, StatsActivity.class));
                return true;
            }
            return false;
        });

        viewModel.state().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega ao voltar de uma sessao ou importacao: os contadores de
        // pendencia mudaram sem que esta tela soubesse.
        viewModel.load();
    }

    private void render(final DeckListState state) {
        binding.progress.setVisibility(state.status() == DeckListState.Status.LOADING
                ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        binding.emptyState.setVisibility(state.showsEmptyState() ? View.VISIBLE : View.GONE);
        binding.errorState.setVisibility(state.status() == DeckListState.Status.ERROR
                ? View.VISIBLE : View.GONE);
        binding.deckList.setVisibility(state.status() == DeckListState.Status.LOADED
                ? View.VISIBLE : View.GONE);

        // O botao flutuante some no estado vazio: la o botao do estado vazio e o
        // caminho principal, e dois botoes iguais confundem.
        binding.fabImport.setVisibility(state.showsEmptyState() ? View.GONE : View.VISIBLE);

        if (state.status() == DeckListState.Status.ERROR) {
            binding.errorMessage.setText(state.message());
        }
        adapter.submitList(state.items());
    }

    private void openImport() {
        startActivity(new Intent(this, ImportActivity.class));
    }

    private void openStudyModes(final String deckId) {
        startActivity(StudyModeActivity.intentFor(this, deckId));
    }
}
