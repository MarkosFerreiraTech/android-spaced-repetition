package com.markosferreira.spacedrepetition.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Gera a identidade de conteudo de uma questao, usada no upsert (RF06).
 *
 * <p>O hash e calculado sobre o texto normalizado do enunciado dentro do
 * baralho. Reimportar a mesma questao produz o mesmo hash, o que torna a
 * importacao idempotente sem depender de identificadores externos na planilha.
 */
public final class ContentHasher {

    private ContentHasher() {
        // Classe utilitaria: nao instanciavel.
    }

    /**
     * Calcula o hash SHA-256 de uma questao em um baralho.
     *
     * @param deckId identificador do baralho
     * @param question enunciado
     * @return hash em hexadecimal, 64 caracteres
     */
    public static String hash(final String deckId, final String question) {
        final String payload = normalize(deckId) + '|' + normalize(question);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }

    /**
     * Normaliza o texto removendo espacos redundantes e diferencas de caixa.
     *
     * @param text texto bruto
     * @return texto normalizado, vazio quando nulo
     */
    public static String normalize(final String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String toHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (final byte value : bytes) {
            final String hex = Integer.toHexString(value & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }
}
