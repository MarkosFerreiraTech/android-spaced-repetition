package com.markosferreira.spacedrepetition.domain.model;

/**
 * Estagio de aprendizado de uma questao, conforme o modelo FSRS.
 */
public enum CardState {

    /** Nunca respondida. */
    NEW,

    /** Em aprendizado inicial, com intervalos curtos. */
    LEARNING,

    /** Em revisao de longo prazo. */
    REVIEW,

    /** Reaprendizado apos um erro. */
    RELEARNING
}
