package com.markosferreira.spacedrepetition.domain.usecase;

import com.markosferreira.spacedrepetition.domain.model.Rating;

/**
 * Traduz o resultado de uma questao de multipla escolha em um {@link Rating} do FSRS.
 *
 * <p>Esta e a ponte entre a UX de simulado e o modelo de repeticao espacada, e
 * por isso vive no dominio: e regra de negocio, nao detalhe de tela.
 *
 * <table>
 *   <caption>Mapeamento</caption>
 *   <tr><th>Resultado</th><th>Rating</th></tr>
 *   <tr><td>Errou</td><td>AGAIN</td></tr>
 *   <tr><td>Acertou e marcou "acertei com duvida"</td><td>HARD</td></tr>
 *   <tr><td>Acertou</td><td>GOOD</td></tr>
 *   <tr><td>Acertou sem marcacao e rapido</td><td>EASY</td></tr>
 * </table>
 */
public final class RatingPolicy {

    /** Fracao do tempo medio abaixo da qual um acerto conta como EASY. */
    public static final double FAST_THRESHOLD = 0.4d;

    /** Tempo medio considerado quando ainda nao ha historico, em milissegundos. */
    public static final long DEFAULT_AVERAGE_MILLIS = 20_000L;

    private RatingPolicy() {
        // Classe utilitaria: nao instanciavel.
    }

    /**
     * Deriva o rating da resposta.
     *
     * @param correct se o estudante acertou
     * @param flagged se marcou "acertei com duvida"
     * @param elapsedMillis tempo gasto na questao
     * @param averageMillis tempo medio historico do estudante
     * @return rating FSRS correspondente
     */
    public static Rating derive(final boolean correct,
                                final boolean flagged,
                                final long elapsedMillis,
                                final long averageMillis) {
        if (!correct) {
            return Rating.AGAIN;
        }
        if (flagged) {
            return Rating.HARD;
        }
        final long baseline = averageMillis > 0L ? averageMillis : DEFAULT_AVERAGE_MILLIS;
        final boolean fast = elapsedMillis > 0L && elapsedMillis < baseline * FAST_THRESHOLD;
        return fast ? Rating.EASY : Rating.GOOD;
    }
}
