package com.markosferreira.spacedrepetition.presentation.study;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.ZoneId;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ActivityStudyBinding;
import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.StudyMode;

/**
 * Sessao de estudo (RF13 a RF18).
 *
 * <p>Casca fina: apenas liga o adaptador de alternativas e os botoes ao
 * ViewModel. Toda a regra — selecionar, confirmar, contar, encerrar — vive em
 * {@link StudyViewModel} e e testada sem Android.
 */
public final class StudyActivity extends AppCompatActivity {

    private static final String EXTRA_DECK_ID = "deck_id";
    private static final String EXTRA_MODE = "mode";

    private ActivityStudyBinding binding;
    private StudyViewModel viewModel;
    private OptionAdapter optionAdapter;
    private StudyMode mode;
    private String deckId;
    private java.util.List<String> lastOptions = new java.util.ArrayList<>();

    /**
     * Cria o intent da sessao.
     *
     * @param context contexto
     * @param deckId baralho
     * @param mode modo escolhido
     * @return intent pronto
     */
    public static Intent intentFor(final Context context, final String deckId,
                                   final StudyMode mode) {
        final Intent intent = new Intent(context, StudyActivity.class);
        intent.putExtra(EXTRA_DECK_ID, deckId);
        intent.putExtra(EXTRA_MODE, mode.name());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        final String modeName = getIntent().getStringExtra(EXTRA_MODE);
        if (deckId == null || modeName == null) {
            finish();
            return;
        }
        mode = StudyMode.valueOf(modeName);

        viewModel = ((CerebroApplication) getApplication()).container().studyViewModel();

        optionAdapter = new OptionAdapter(viewModel::selectOption);
        binding.optionsContainer.setLayoutManager(new LinearLayoutManager(this));
        binding.optionsContainer.setAdapter(optionAdapter);

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.buttonPrimary.setOnClickListener(view -> onPrimaryAction());
        binding.checkboxFlagged.setOnCheckedChangeListener(
                (button, checked) -> viewModel.setFlagged(checked));

        viewModel.state().observe(this, this::render);

        if (savedInstanceState == null) {
            viewModel.start(deckId, mode);
        }
    }

    /**
     * O botao unico muda de papel: confirma a resposta, avanca, ou encerra.
     */
    private void onPrimaryAction() {
        final StudyState current = viewModel.state().getValue();
        if (current == null) {
            return;
        }
        switch (current.phase()) {
            case ANSWERING:
                viewModel.confirm();
                break;
            case FEEDBACK:
                viewModel.next();
                break;
            case FINISHED:
                finish();
                break;
            default:
                finish();
                break;
        }
    }

    private void render(final StudyState state) {
        binding.sessionProgress.setMax(Math.max(1, state.total()));
        binding.sessionProgress.setProgress(state.position());

        switch (state.phase()) {
            case ANSWERING:
            case FEEDBACK:
                renderQuestion(state);
                break;
            case FINISHED:
                renderSummary(state);
                break;
            default:
                // Sem questoes: volta para a escolha de modo.
                finish();
                break;
        }
    }

    private void renderQuestion(final StudyState state) {
        final boolean answering = state.phase() == StudyState.Phase.ANSWERING;

        binding.questionText.setText(state.card().question());
        binding.progressLabel.setText(getString(R.string.study_progress,
                state.position(), state.total()));
        syncOptions(state);

        binding.checkboxFlagged.setChecked(state.flagged());
        binding.checkboxFlagged.setEnabled(answering);
        binding.buttonPrimary.setText(answering
                ? R.string.study_confirm : R.string.study_next);
        binding.buttonPrimary.setEnabled(!answering || state.hasSelection());

        if (answering) {
            hideFeedback();
            return;
        }
        showFeedback(state);
    }

    /**
     * Sincroniza o adaptador apenas quando a questao muda.
     *
     * <p>Resubmeter a mesma lista a cada redesenho perderia a selecao e
     * reiniciaria o estado do adaptador.
     */
    private void syncOptions(final StudyState state) {
        final boolean changed = !state.options().equals(lastOptions);
        if (changed) {
            optionAdapter.submit(state.options());
            lastOptions = state.options();
        }
        if (state.hasSelection()) {
            optionAdapter.select(state.selectedIndex());
        }
    }

    private void hideFeedback() {
        binding.feedbackCard.setVisibility(View.GONE);
        binding.optionsContainer.setVisibility(View.VISIBLE);
    }

    private void showFeedback(final StudyState state) {
        optionAdapter.reveal(state.card().correctIndex());
        binding.feedbackCard.setVisibility(View.VISIBLE);
        binding.feedbackTitle.setText(state.correct()
                ? R.string.study_feedback_correct : R.string.study_feedback_wrong);
        binding.feedbackTitle.setTextColor(getColor(state.correct()
                ? R.color.feedback_correct : R.color.feedback_wrong));
        binding.feedbackCorrectAnswer.setText(getString(
                R.string.study_feedback_correct_is, state.card().correctLetter()));
        bindExplanation(state.card().explanation());
        bindSchedule(state);
    }

    /**
     * Mostra quando a questao volta e em que estagio ela esta (RF25).
     *
     * <p>O texto so aparece no feedback: antes de responder, o agendamento da
     * rodada ainda nao foi calculado.
     *
     * @param state estado com o agendamento
     */
    private void bindSchedule(final StudyState state) {
        if (state.nextScheduling() == null) {
            binding.feedbackSchedule.setVisibility(View.GONE);
            return;
        }
        final ScheduleDescription schedule = ScheduleDescription.of(state.nextScheduling(),
                java.time.Instant.now(), ZoneId.systemDefault());

        binding.feedbackSchedule.setVisibility(View.VISIBLE);
        binding.feedbackSchedule.setText(getString(R.string.study_schedule_line,
                dueText(schedule), stageText(schedule.state())));
    }

    /**
     * Texto do prazo de revisao.
     *
     * @param schedule descricao calculada
     * @return frase do prazo
     */
    private String dueText(final ScheduleDescription schedule) {
        switch (schedule.dueKind()) {
            case TODAY:
                return getString(R.string.schedule_today);
            case TOMORROW:
                return getString(R.string.schedule_tomorrow);
            case IN_DAYS:
                return getString(R.string.schedule_in_days, schedule.days());
            default:
                return getString(R.string.schedule_new);
        }
    }

    /**
     * Texto do estagio de aprendizado.
     *
     * @param state estagio da questao
     * @return frase do estagio
     */
    private String stageText(final CardState state) {
        switch (state) {
            case LEARNING:
                return getString(R.string.schedule_stage_learning);
            case REVIEW:
                return getString(R.string.schedule_stage_review);
            case RELEARNING:
                return getString(R.string.schedule_stage_relearning);
            default:
                return getString(R.string.schedule_stage_new);
        }
    }

    private void bindExplanation(final String explanation) {
        if (explanation == null || explanation.isEmpty()) {
            binding.feedbackExplanation.setVisibility(View.GONE);
            return;
        }
        binding.feedbackExplanation.setVisibility(View.VISIBLE);
        binding.feedbackExplanation.setText(explanation);
    }

    private void renderSummary(final StudyState state) {
        binding.optionsContainer.setVisibility(View.GONE);
        binding.feedbackCard.setVisibility(View.VISIBLE);
        binding.questionText.setText(R.string.study_summary_title);
        binding.progressLabel.setText(getString(R.string.study_summary_body,
                state.correctCount(), state.wrongCount(), state.flaggedCount()));

        binding.feedbackTitle.setText(getString(R.string.study_summary_accuracy,
                state.accuracyText()));
        binding.feedbackTitle.setTextColor(getColor(R.color.brand_primary));
        binding.feedbackCorrectAnswer.setText(getString(R.string.study_summary_body,
                state.correctCount(), state.wrongCount(), state.flaggedCount()));
        binding.feedbackExplanation.setVisibility(View.VISIBLE);
        binding.feedbackExplanation.setText(R.string.study_back_to_decks);

        binding.checkboxFlagged.setVisibility(View.GONE);
        binding.buttonPrimary.setText(R.string.study_back_to_decks);
        binding.buttonPrimary.setEnabled(true);
    }
}
