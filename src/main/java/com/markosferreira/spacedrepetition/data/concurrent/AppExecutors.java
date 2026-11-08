package com.markosferreira.spacedrepetition.data.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executores do processo.
 *
 * <p>Existe por um motivo concreto: o Room **lanca excecao** se o banco for
 * acessado na thread principal. Todo ViewModel recebe este executor e faz a
 * consulta em background, publicando o resultado com {@code postValue}, que ja
 * entrega na thread de UI.
 *
 * <p>Nos testes, o executor e trocado por {@code Runnable::run}, o que torna o
 * carregamento sincrono e deterministico.
 */
public final class AppExecutors {

    private final ExecutorService background;

    /**
     * Cria a instancia com um pool de uma thread.
     */
    public AppExecutors() {
        final AtomicInteger counter = new AtomicInteger();
        final ThreadFactory factory = task -> {
            final Thread thread = new Thread(task, "cerebro-io-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.background = Executors.newSingleThreadExecutor(factory);
    }

    /**
     * Executor para trabalho de entrada e saida.
     *
     * @return executor de background
     */
    public Executor io() {
        return background;
    }

    /**
     * Encerra o pool.
     */
    public void shutdown() {
        background.shutdown();
    }
}
