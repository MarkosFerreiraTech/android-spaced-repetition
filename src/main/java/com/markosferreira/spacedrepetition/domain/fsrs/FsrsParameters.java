package com.markosferreira.spacedrepetition.domain.fsrs;

/**
 * Parametros do modelo FSRS.
 *
 * <p>Os valores default seguem a especificacao publica do FSRS (versao 4.5) e
 * foram mantidos como constantes nomeadas, e nao como numeros soltos, para que
 * o algoritmo seja auditavel: cada simbolo da especificacao aparece aqui com o
 * mesmo nome.
 *
 * <p>As dezoito constantes possuem o seguinte papel:
 * <ul>
 *   <li>{@code W[0..3]} — estabilidade inicial para Again, Hard, Good, Easy.</li>
 *   <li>{@code W[4..5]} — dificuldade inicial e sua taxa de mudanca.</li>
 *   <li>{@code W[6]} — reversao da dificuldade em caso de acerto.</li>
 *   <li>{@code W[7]} — incremento de dificuldade em caso de erro.</li>
 *   <li>{@code W[8..10]} — fatores de incremento de estabilidade em revisao.</li>
 *   <li>{@code W[11..14]} — parametros da curva de esquecimento.</li>
 *   <li>{@code W[15..16]} — estabilidade apos erro (reaprendizado).</li>
 *   <li>{@code W[17]} — fator de curto prazo.</li>
 * </ul>
 *
 * <p>Os valores sao fixos nesta versao. O ajuste automatico a partir do historico
 * do usuario fica para a v2 (ADR-001), o que exige volume minimo de revisoes.
 */
public final class FsrsParameters {

    /** Fator de retencao desejada: 90% de chance de lembrar no vencimento. */
    public static final double REQUEST_RETENTION = 0.9d;

    /** Fator de decaimento da curva de esquecimento. */
    public static final double DECAY = -0.5d;

    /** Expoente do fator de estabilidade, derivado de {@link #DECAY}. */
    public static final double FACTOR = 19d / 81d;

    /** Intervalo maximo permitido, em dias. */
    public static final int MAXIMUM_INTERVAL_DAYS = 36500;

    private static final double[] DEFAULT_WEIGHTS = {
        // w0..w3: estabilidade inicial por rating (Again, Hard, Good, Easy)
        0.4072d, 1.1829d, 3.1262d, 15.4722d,
        // w4..w5: dificuldade inicial e taxa de mudanca
        7.2102d, 0.5316d,
        // w6..w7: reversao e incremento de dificuldade
        1.0651d, 0.0234d,
        // w8..w10: incrementos de estabilidade em revisao
        1.616d, 0.1544d, 1.0824d,
        // w11..w14: curva de esquecimento
        1.9813d, 0.0953d, 0.2975d, 2.2042d,
        // w15..w16: estabilidade apos lapso
        0.2407d, 2.9466d,
        // w17: fator de curto prazo
        0.5034d
    };

    private final double[] weights;

    /**
     * Cria o conjunto de parametros default.
     *
     * @return parametros do FSRS
     */
    public static FsrsParameters defaults() {
        return new FsrsParameters(DEFAULT_WEIGHTS.clone());
    }

    /**
     * Cria parametros customizados.
     *
     * @param weights vetor w0..w17
     * @throws IllegalArgumentException se o vetor nao tiver 18 posicoes
     */
    public FsrsParameters(final double[] weights) {
        if (weights.length != 18) {
            throw new IllegalArgumentException(
                    "FSRS exige exatamente 18 pesos, recebido: " + weights.length);
        }
        this.weights = weights.clone();
    }

    /**
     * Peso do modelo na posicao indicada.
     *
     * @param index posicao 0..17
     * @return peso
     */
    public double weight(final int index) {
        return weights[index];
    }

    /**
     * Quantidade de pesos do modelo.
     *
     * @return 18
     */
    public int size() {
        return weights.length;
    }
}
