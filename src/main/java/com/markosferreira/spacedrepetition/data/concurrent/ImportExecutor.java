package com.markosferreira.spacedrepetition.data.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executor dedicado a leitura e persistencia do CSV (RF02, RNF07).
 *
 * <p>Pool de uma unica thread, de proposito: duas importacoes simultaneas
 * disputariam escrita no mesmo baralho e no SQLite. Uma fila serial e
 * previsivel e mantem a thread de UI livre.
 *
 * <p>Cada tarefa devolve um {@link Future}, o que da ao chamador duas coisas
 * que o RF08 exige: acompanhar a conclusao e cancelar o trabalho em andamento.
 */
public final class ImportExecutor {

    /** Nome da thread, visivel em stack traces e no profiler. */
    private static final String THREAD_NAME = "csv-import";

    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Cria o executor.
     *
     * @param poolSize tamanho do pool (vem de {@code BuildConfig.CSV_IMPORT_POOL_SIZE})
     */
    public ImportExecutor(final int poolSize) {
        final ThreadFactory factory = task -> {
            final Thread thread = new Thread(task, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(Math.max(1, poolSize), factory);
    }

    /**
     * Agenda uma tarefa de importacao.
     *
     * @param task trabalho a executar em background
     * @param <T> tipo do resultado
     * @return futuro que permite aguardar ou cancelar
     */
    public <T> Future<T> submit(final Callable<T> task) {
        running.set(true);
        return executor.submit(task);
    }

    /**
     * Indica se ha importacao em curso.
     *
     * @return verdadeiro enquanto uma tarefa nao terminar
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Marca o ciclo como encerrado (chamado no fim da tarefa).
     */
    public void markIdle() {
        running.set(false);
    }

    /**
     * Encerra o pool.
     *
     * <p>Chamado quando a tela de importacao e destruida de forma definitiva.
     * Tarefas ja submetidas terminam; novas sao rejeitadas.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
