package com.markosferreira.spacedrepetition.domain.model;

/**
 * Avaliacao da lembranca de uma questao, na escala do FSRS.
 *
 * <p>A sessao de simulado e de multipla escolha, portanto o rating e derivado do
 * desempenho (ver {@link com.markosferreira.spacedrepetition.domain.usecase.RatingPolicy}),
 * e nao escolhido manualmente pelo estudante.
 */
public enum Rating {

    /** Errou a questao. */
    AGAIN(1),

    /** Acertou, porem marcou "acertei com duvida". */
    HARD(2),

    /** Acertou. */
    GOOD(3),

    /** Acertou com folga: sem marcacao e muito mais rapido que o historico. */
    EASY(4);

    private final int weight;

    Rating(final int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /**
     * Converte o peso numerico (1..4) de volta para o enum.
     *
     * @param weight peso do rating
     * @return rating correspondente
     * @throws IllegalArgumentException se o peso estiver fora da faixa
     */
    public static Rating fromWeight(final int weight) {
        for (final Rating rating : values()) {
            if (rating.weight == weight) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Rating invalido: " + weight);
    }
}
