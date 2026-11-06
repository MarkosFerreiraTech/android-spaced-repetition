package com.markosferreira.spacedrepetition.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContentHasher")
class ContentHasherTest {

    @Test
    @DisplayName("o mesmo enunciado produz sempre o mesmo hash")
    void isStable() {
        final String first = ContentHasher.hash("deck-1", "O que e S3?");
        final String second = ContentHasher.hash("deck-1", "  O que e   S3?  ");

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    @DisplayName("ignora diferencas de caixa")
    void ignoresCase() {
        assertThat(ContentHasher.hash("deck-1", "O QUE E S3?"))
                .isEqualTo(ContentHasher.hash("deck-1", "o que e s3?"));
    }

    @Test
    @DisplayName("enunciados diferentes produzem hashes diferentes")
    void differsByQuestion() {
        assertThat(ContentHasher.hash("deck-1", "O que e S3?"))
                .isNotEqualTo(ContentHasher.hash("deck-1", "O que e EC2?"));
    }

    @Test
    @DisplayName("o mesmo enunciado em baralhos diferentes gera hashes diferentes")
    void differsByDeck() {
        assertThat(ContentHasher.hash("deck-1", "O que e S3?"))
                .isNotEqualTo(ContentHasher.hash("deck-2", "O que e S3?"));
    }

    @Test
    @DisplayName("nulo e tratado como texto vazio, sem excecao")
    void handlesNull() {
        assertThat(ContentHasher.normalize(null)).isEmpty();
        assertThat(ContentHasher.hash(null, null)).hasSize(64);
    }
}
