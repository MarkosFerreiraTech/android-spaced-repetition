package com.markosferreira.spacedrepetition.presentation.importing;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.markosferreira.spacedrepetition.CerebroApplication;
import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ActivityImportBinding;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Tela de importacao (RF01, UC01).
 *
 * <p>O acesso ao arquivo usa o Storage Access Framework
 * ({@link ActivityResultContracts.OpenDocument}), que nao exige permissao de
 * armazenamento e devolve acesso a um documento escolhido explicitamente.
 *
 * <p>A Activity converte o {@link Uri} em um {@link ReaderSource} e entrega ao
 * ViewModel; nenhuma leitura acontece na thread principal.
 */
public final class ImportActivity extends AppCompatActivity {

    private ActivityImportBinding binding;
    private ImportViewModel viewModel;
    private ActivityResultLauncher<String[]> filePicker;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = ((CerebroApplication) getApplication()).container().importViewModel();

        filePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onFileChosen);

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.buttonSelectFile.setOnClickListener(view -> pickFile());
        binding.buttonTryAgain.setOnClickListener(view -> showInitial());
        binding.buttonChooseAnother.setOnClickListener(view -> {
            showInitial();
            pickFile();
        });
        binding.buttonBackToDecks.setOnClickListener(view -> finish());
        binding.buttonCancel.setOnClickListener(view -> viewModel.cancel());

        viewModel.state().observe(this, this::render);
    }

    private void pickFile() {
        filePicker.launch(new String[]{"text/csv", "text/comma-separated-values",
                "text/plain", "application/csv"});
    }

    private void onFileChosen(@Nullable final Uri uri) {
        if (uri == null) {
            // Usuario desistiu da escolha: permanece na tela inicial.
            return;
        }
        final String fileName = readDisplayName(uri);
        viewModel.importFrom(resolver(uri), fileName);
    }

    /**
     * Extrai um nome legivel do documento escolhido.
     *
     * @param uri documento
     * @return nome do arquivo, ou o ultimo segmento do uri
     */
    private String readDisplayName(final Uri uri) {
        return uri.getLastPathSegment() == null ? "planilha.csv" : uri.getLastPathSegment();
    }

    /**
     * Cria a fonte de leitura sobre o documento.
     *
     * @param uri documento escolhido
     * @return fornecedor de leitor
     */
    private ReaderSource resolver(final Uri uri) {
        return () -> {
            if (getContentResolver() == null) {
                throw new IOException("ContentResolver indisponivel");
            }
            final Reader reader = new InputStreamReader(
                    getContentResolver().openInputStream(uri), StandardCharsets.UTF_8);
            return reader;
        };
    }

    private void render(final ImportState state) {
        showOnly(state.phase());
        switch (state.phase()) {
            case RUNNING:
                binding.progressLabel.setText(getString(R.string.import_progress_indeterminate));
                break;
            case REPORT:
                renderReport(state);
                break;
            case ERROR:
                binding.errorTitle.setText(state.errorTitle());
                binding.errorBody.setText(state.errorDetail());
                break;
            default:
                break;
        }
    }

    private void renderReport(final ImportState state) {
        binding.reportSummary.setText(getString(R.string.import_report_summary,
                state.imported(), state.updated(), state.skipped(), state.errors().size()));

        if (!state.hasErrors()) {
            binding.reportErrorsTitle.setVisibility(View.GONE);
            binding.reportErrors.setVisibility(View.GONE);
            return;
        }
        final StringBuilder text = new StringBuilder();
        for (final ImportState.LineError error : state.errors()) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(getString(R.string.import_error_line, error.lineNumber(), error.reason()));
        }
        binding.reportErrorsTitle.setVisibility(View.VISIBLE);
        binding.reportErrors.setVisibility(View.VISIBLE);
        binding.reportErrors.setText(text.toString());
    }

    private void showOnly(final ImportState.Phase phase) {
        binding.stateInitial.setVisibility(phase == ImportState.Phase.IDLE
                ? View.VISIBLE : View.GONE);
        binding.stateProgress.setVisibility(phase == ImportState.Phase.RUNNING
                ? View.VISIBLE : View.GONE);
        binding.stateReport.setVisibility(phase == ImportState.Phase.REPORT
                ? View.VISIBLE : View.GONE);
        binding.stateError.setVisibility(phase == ImportState.Phase.ERROR
                ? View.VISIBLE : View.GONE);
    }

    private void showInitial() {
        binding.stateInitial.setVisibility(View.VISIBLE);
        binding.stateReport.setVisibility(View.GONE);
        binding.stateError.setVisibility(View.GONE);
    }
}
