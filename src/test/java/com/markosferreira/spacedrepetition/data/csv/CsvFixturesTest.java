package com.markosferreira.spacedrepetition.data.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Testes do parser contra planilhas reais versionadas em {@code src/test/resources/fixtures}.
 *
 * <p>Os fixtures existem para que os cenarios de aceite da matriz de
 * rastreabilidade sejam reproduziveis: arquivo valido, arquivo com linhas
 * ruins, cabecalho fora do contrato e robustez RFC 4180.
 */
@DisplayName("OpenCsvParser com fixtures")
class CsvFixturesTest {

    private final OpenCsvParser parser = new OpenCsvParser(50000);

    @Test
    @DisplayName("importa o simulado valido inteiro, sem erros")
    void parsesValidFixture() throws IOException {
        final ParseOutcome outcome = parser.parse(reader("simulado_valido.csv"));

        assertThat(outcome.headerValid()).isTrue();
        assertThat(outcome.questions()).hasSize(5);
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.questions().get(0).correctIndex()).isZero();
        assertThat(outcome.questions().get(0).options()).hasSize(4);
    }

    @Test
    @DisplayName("isola as linhas ruins e mantem as 3 validas")
    void parsesFixtureWithErrors() throws IOException {
        final ParseOutcome outcome = parser.parse(reader("simulado_com_erros.csv"));

        assertThat(outcome.headerValid()).isTrue();
        assertThat(outcome.questions()).hasSize(3);
        assertThat(outcome.errors()).hasSize(4);

        final List<String> reasons = new ArrayList<>();
        for (final ParseOutcome.LineError error : outcome.errors()) {
            reasons.add(error.reason());
        }
        assertThat(reasons).anyMatch(reason -> reason.contains("gabarito"));
        assertThat(reasons).anyMatch(reason -> reason.contains("enunciado"));
        assertThat(reasons).anyMatch(reason -> reason.contains("alternativas"));
    }

    @Test
    @DisplayName("rejeita o fixture com cabecalho fora do contrato")
    void rejectsInvalidHeaderFixture() throws IOException {
        final ParseOutcome outcome = parser.parse(reader("simulado_cabecalho_invalido.csv"));

        assertThat(outcome.headerValid()).isFalse();
        assertThat(outcome.questions()).isEmpty();
        assertThat(outcome.foundColumns()).contains("pergunta", "resposta_certa");
    }

    @Test
    @DisplayName("preserva virgula e quebra de linha dentro de aspas (RFC 4180)")
    void parsesRfc4180Fixture() throws IOException {
        final ParseOutcome outcome = parser.parse(reader("simulado_rfc4180.csv"));

        assertThat(outcome.headerValid()).isTrue();
        assertThat(outcome.questions()).hasSize(2);
        assertThat(outcome.questions().get(0).question())
                .isEqualTo("Qual destes servicos, entre os listados, guarda objetos?");
        assertThat(outcome.questions().get(0).explanation()).contains("Segunda linha");
    }

    private Reader reader(final String fixture) throws IOException {
        final String path = "/fixtures/" + fixture;
        final InputStream stream = getClass().getResourceAsStream(path);
        assertThat(stream).as("fixture %s no classpath de teste", path).isNotNull();
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
