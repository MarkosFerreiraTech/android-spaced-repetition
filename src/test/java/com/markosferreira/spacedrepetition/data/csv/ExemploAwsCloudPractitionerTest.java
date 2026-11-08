package com.markosferreira.spacedrepetition.data.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.markosferreira.spacedrepetition.domain.model.CsvQuestion;
import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Impede que o arquivo de exemplo quebre.
 *
 * <p>O CSV de {@code docs/exemplos} e o que o usuario importa no aparelho para
 * conferir as telas com dados reais. Ele ja precisou ser corrigido uma vez
 * (cabecalho em portugues, gabarito por extenso, coluna extra), entao este teste
 * fixa o contrato: o exemplo tem de continuar importavel pelo parser de
 * producao, e nao apenas parecer um CSV valido.
 */
@DisplayName("Exemplo importavel")
class ExemploAwsCloudPractitionerTest {

    /** Mesmo limite do build de producao. */
    private static final int MAX_LINES = 50000;

    /** Caminho do exemplo publicado junto com o projeto. */
    private static final String RECURSO = "/exemplos/aws-cloud-practitioner.csv";

    @Test
    @DisplayName("o exemplo e lido sem nenhuma linha rejeitada")
    void exampleIsFullyImported() throws IOException {
        final ParseOutcome outcome = new OpenCsvParser(MAX_LINES).parse(ler());

        assertThat(outcome.errors())
                .as("linha rejeitada no exemplo entregue ao usuario")
                .isEmpty();
        assertThat(outcome.headerValid()).isTrue();
        assertThat(outcome.questions()).hasSize(4);
    }

    @Test
    @DisplayName("cada questao tem gabarito apontando para alternativa existente")
    void everyQuestionHasValidAnswer() throws IOException {
        final ParseOutcome outcome = new OpenCsvParser(MAX_LINES).parse(ler());

        for (final CsvQuestion question : outcome.questions()) {
            assertThat(question.options())
                    .as("alternativas da linha %d", question.lineNumber())
                    .hasSizeGreaterThanOrEqualTo(2);
            assertThat(question.correctIndex())
                    .as("indice do gabarito da linha %d", question.lineNumber())
                    .isBetween(0, question.options().size() - 1);
            assertThat(question.explanation())
                    .as("explicacao da linha %d", question.lineNumber())
                    .isNotBlank();
        }
    }

    /**
     * Abre o exemplo como o app faz no aparelho: UTF-8.
     *
     * <p>Ao contrario do {@code ImportActivity}, aqui o arquivo vem do classpath
     * (resources de teste) e nao do {@code ContentResolver}.
     *
     * @return leitor do exemplo
     * @throws IOException se o recurso nao estiver no classpath
     */
    private Reader ler() throws IOException {
        final InputStream stream = getClass().getResourceAsStream(RECURSO);
        if (stream == null) {
            throw new IOException("Recurso ausente no classpath: " + RECURSO);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
