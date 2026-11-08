package com.markosferreira.spacedrepetition.presentation;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

/**
 * Utilitario de sincronizacao para os testes de tela.
 *
 * <p>Existe porque duas coisas acontecem fora da chamada do teste:
 * <ol>
 *   <li>o diff do {@code ListAdapter} roda em background e publica o resultado
 *       postando na main thread;</li>
 *   <li>os {@code LiveData} alimentados por {@code postValue} tambem entregam o
 *       valor pela main thread.</li>
 * </ol>
 *
 * <p>Sem drenar o looper, o teste le o estado anterior e falha de forma
 * intermitente — foi o que aconteceu antes de centralizar isso aqui.
 */
public final class LooperIdle {

    private LooperIdle() {
        // Classe utilitaria.
    }

    /**
     * Executa as tarefas pendentes da thread principal ate a fila esvaziar.
     */
    public static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }
}
