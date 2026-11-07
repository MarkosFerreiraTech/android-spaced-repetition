package com.markosferreira.spacedrepetition.data.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Serializa as alternativas de uma questao para uma coluna de texto.
 *
 * <p>Formato: alternativas separadas por {@code \u001F} (unit separator), com
 * caracteres de controle removidos do conteudo. E deliberadamente simples e
 * livre de dependencia de biblioteca JSON: o conteudo e texto de uma planilha,
 * nao estrutura arbitraria, e o formato e estavel e inspecionavel no banco.
 */
public final class OptionCodec {

    private static final char SEPARATOR = '\u001F';

    private OptionCodec() {
        // Classe utilitaria: nao instanciavel.
    }

    /**
     * Codifica alternativas.
     *
     * @param options alternativas
     * @return texto codificado
     */
    public static String encode(final List<String> options) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(sanitize(options.get(i)));
        }
        return builder.toString();
    }

    /**
     * Decodifica alternativas.
     *
     * @param encoded texto codificado
     * @return lista de alternativas, nunca nula
     */
    public static List<String> decode(final String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(encoded.split(String.valueOf(SEPARATOR), -1)));
    }

    private static String sanitize(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace(SEPARATOR, ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
