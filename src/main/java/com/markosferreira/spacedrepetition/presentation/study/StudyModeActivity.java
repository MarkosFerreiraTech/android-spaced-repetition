package com.markosferreira.spacedrepetition.presentation.study;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ActivityStudyModeBinding;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;

/**
 * Escolha do modo de sessao (RF12).
 *
 * <p>Mostra as tres opcoes com a contagem real de cada uma, para que o estudante
 * decida com informacao: "so os erros (20)" e diferente de "todos (65)".
 */
public final class StudyModeActivity extends AppCompatActivity {

    private static final String EXTRA_DECK_ID = "deck_id";

    private String deckId;

    /**
     * Cria o intent da tela.
     *
     * @param context contexto
     * @param deckId baralho escolhido
     * @return intent pronto
     */
    public static Intent intentFor(final Context context, final String deckId) {
        final Intent intent = new Intent(context, StudyModeActivity.class);
        intent.putExtra(EXTRA_DECK_ID, deckId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityStudyModeBinding binding =
                ActivityStudyModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        if (deckId == null) {
            finish();
            return;
        }

        final CerebroApplication application = (CerebroApplication) getApplication();
        final CardRepository cards = application.container().cardRepository();
        final ReviewRepository reviews = application.container().reviewRepository();

        final int total = cards.findByDeck(deckId).size();
        final int wrong = reviews.findWrongByDeck(deckId).size();
        final int wrongOrFlagged = reviews.findWrongOrFlaggedByDeck(deckId).size();

        binding.descFull.setText(getString(R.string.study_mode_full_desc, total));
        binding.descWrong.setText(getString(R.string.study_mode_wrong_desc, wrong));
        binding.descWrongFlagged.setText(
                getString(R.string.study_mode_wrong_flagged_desc, wrongOrFlagged));

        binding.toolbar.setNavigationOnClickListener(view -> finish());

        binding.cardFull.setOnClickListener(view -> startSession(StudyMode.FULL, total));
        binding.cardWrong.setOnClickListener(view -> startSession(StudyMode.WRONG_ONLY, wrong));
        binding.cardWrongFlagged.setOnClickListener(
                view -> startSession(StudyMode.WRONG_PLUS_FLAGGED, wrongOrFlagged));

        // Sem erros registrados, os dois modos de revisao nao tem o que mostrar.
        binding.modeEmptyMessage.setVisibility(wrong == 0 && wrongOrFlagged == 0
                ? View.VISIBLE : View.GONE);
    }

    private void startSession(final StudyMode mode, final int count) {
        if (count == 0) {
            return;
        }
        startActivity(StudyActivity.intentFor(this, deckId, mode));
        finish();
    }
}
