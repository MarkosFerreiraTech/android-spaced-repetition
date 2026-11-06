package com.markosferreira.spacedrepetition.domain.model;

/**
 * Modo de montagem da fila de uma sessao de simulado.
 *
 * <p>Espelha a estrategia real do estudante ao refazer um simulado: tudo,
 * somente o que errou, ou o que errou mais o que acertou com duvida.
 */
public enum StudyMode {

    /** Todas as questoes do baralho. */
    FULL,

    /** Somente questoes erradas na ultima tentativa. */
    WRONG_ONLY,

    /** Questoes erradas mais as acertadas com a marcacao de duvida. */
    WRONG_PLUS_FLAGGED
}
