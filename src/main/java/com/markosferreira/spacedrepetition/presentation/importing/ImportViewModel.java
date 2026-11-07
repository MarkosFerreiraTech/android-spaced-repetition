package com.markosferreira.spacedrepetition.presentation.importing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.markosferreira.spacedrepetition.data.concurrent.ImportExecutor;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;
import com.markosferreira.spacedrepetition.domain.usecase.ImportDeckUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.ImportResult;

import java.io.IOException;
import java.io.Reader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel da importacao (RF01, RF07, RF08).
 *
 * <p>Leitura e persistencia rodam em {@link ImportExecutor}, nunca na thread de
 * UI. O {@link Future} devolvido permite cancelar (RF08) e o resultado volta
 * por {@code postValue}.
 *
 * <p>Todo o fluxo e testavel em JVM pura: o leitor entra por
 * {@link ReaderSource}, o tempo por {@link Clock} e o executor aceita um pool
 * de uma thread nos testes.
 */
public final class ImportViewModel extends ViewModel {

    /** Limite operacional de linhas, espelhando o BuildConfig. */
    public static final int MAX_LINES = 50000;

    private final ImportDeckUseCase importDeckUseCase;
    private final ImportExecutor executor;
    private final Clock clock;

    private final MutableLiveData<ImportState> state =
            new MutableLiveData<>(ImportState.idle());

    private Future<ImportResult> runningTask;

    /**
     * Cria o ViewModel.
     *
     * @param importDeckUseCase caso de uso de importacao
     * @param executor executor dedicado
     * @param clock fonte de tempo
     */
    public ImportViewModel(final ImportDeckUseCase importDeckUseCase,
                           final ImportExecutor executor, final Clock clock) {
        this.importDeckUseCase = importDeckUseCase;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Estado observavel.
     *
     * @return estado atual
     */
    public LiveData<ImportState> state() {
        return state;
    }

    /**
     * Inicia a importacao de um arquivo.
     *
     * @param source fornecedor do conteudo
     * @param fileName nome exibido ao usuario
     */
    public void importFrom(final ReaderSource source, final String fileName) {
        if (executor.isRunning()) {
            return;
        }
        state.setValue(ImportState.running(fileName));
        runningTask = executor.submit(() -> {
            try (Reader reader = source.open()) {
                return importDeckUseCase.execute(reader, fileName, clock.instant());
            } finally {
                executor.markIdle();
            }
        });
        publishResult(fileName, runningTask);
    }

    /**
     * Cancela a importacao em andamento (RF08).
     *
     * <p>O lote corrente e transacional: o que ja foi gravado permanece, e
     * nada fica pela metade.
     */
    public void cancel() {
        if (runningTask != null) {
            runningTask.cancel(true);
            runningTask = null;
        }
        executor.markIdle();
        state.setValue(ImportState.idle());
    }

    private void publishResult(final String fileName, final Future<ImportResult> task) {
        try {
            final ImportResult result = task.get();
            final List<ImportState.LineError> errors = new ArrayList<>(result.errors().size());
            for (final ImportResult.LineError error : result.errors()) {
                errors.add(new ImportState.LineError(error.lineNumber(), error.reason()));
            }
            state.postValue(ImportState.report(fileName, result.imported(),
                    result.updated(), result.skipped(), errors));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            state.postValue(ImportState.idle());
        } catch (final ExecutionException exception) {
            state.postValue(toFailure(exception.getCause()));
        } catch (final java.util.concurrent.CancellationException exception) {
            state.postValue(ImportState.idle());
        }
    }

    private ImportState toFailure(final Throwable cause) {
        if (cause instanceof ImportDeckUseCase.InvalidHeaderException) {
            final ImportDeckUseCase.InvalidHeaderException invalidHeader =
                    (ImportDeckUseCase.InvalidHeaderException) cause;
            return new ImportStateBuilder().headerInvalid(invalidHeader.foundColumns());
        }
        if (cause instanceof CsvParser.CsvReadException) {
            return ImportState.failed("Arquivo indisponivel",
                    "Nao foi possivel ler o conteudo do arquivo. Selecione novamente.");
        }
        if (cause instanceof IOException) {
            return ImportState.failed("Arquivo indisponivel",
                    "Nao foi possivel abrir o arquivo. Selecione novamente.");
        }
        return ImportState.failed("Importacao nao concluida",
                "Ocorreu um erro inesperado ao ler a planilha.");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (runningTask != null) {
            runningTask.cancel(true);
            runningTask = null;
        }
    }

    /**
     * Construtor de estados de falha especificos.
     *
     * <p>Existe para manter {@link #toFailure} curto e sem concatenacao de texto
     * espalhada pelo metodo.
     */
    private static final class ImportStateBuilder {

        ImportState headerInvalid(final List<String> foundColumns) {
            final String expected = "question, option_a, option_b, option_c, option_d, "
                    + "correct_answer";
            final String found = foundColumns.isEmpty() ? "(nenhuma)" : String.join(", ", foundColumns);
            return ImportState.failed("Cabecalho invalido",
                    "Colunas esperadas: " + expected + "\nColunas encontradas: " + found);
        }
    }
}
