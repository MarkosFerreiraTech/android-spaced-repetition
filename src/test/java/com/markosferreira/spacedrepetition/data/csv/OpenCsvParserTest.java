package com.markosferreira.spacedrepetition.data.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

/**
 * Testes do parser de CSV (RF04, RF05, Anexo A).
 *
 * <p>Cobrem exatamente os cenarios que a matriz de rastreabilidade exige:
 * validacao de cabecalho e erro de linha que nao aborta a importacao.
 */
@DisplayName("OpenCsvParser")
class OpenCsvParserTest {

    /** Limite usado nos testes: o mesmo do build de producao. */
    private static final int MAX_LINES = 50000;

    private static final String VALID_HEADER =
            "question,option_a,option_b,option_c,option_d,correct_answer,explanation";

    private OpenCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new OpenCsvParser(MAX_LINES);
    }

    @Nested
    @DisplayName("RF04: validacao de cabecalho")
    class HeaderValidation {

        @Test
        @DisplayName("aceita o cabecalho do contrato e le a primeira questao")
        void acceptsContractHeader() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"Qual servico oferece 11 noves?\",S3,EBS,EFS,Glacier,A,\"Object storage.\"\n"));

            assertThat(outcome.headerValid()).isTrue();
            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.questions().get(0).question())
                    .isEqualTo("Qual servico oferece 11 noves?");
            assertThat(outcome.questions().get(0).correctIndex()).isZero();
            assertThat(outcome.questions().get(0).options())
                    .containsExactly("S3", "EBS", "EFS", "Glacier");
        }

        @Test
        @DisplayName("rejeita o arquivo inteiro quando falta a coluna correct_answer")
        void rejectsHeaderWithoutCorrectAnswer() {
            final ParseOutcome outcome = parser.parse(new StringReader(
                    "question,option_a,option_b\n\"Pergunta\",A,B\n"));

            assertThat(outcome.headerValid()).isFalse();
            assertThat(outcome.questions()).isEmpty();
            assertThat(outcome.foundColumns()).containsExactly("question", "option_a", "option_b");
        }

        @Test
        @DisplayName("rejeita o arquivo inteiro quando falta a coluna question")
        void rejectsHeaderWithoutQuestion() {
            final ParseOutcome outcome = parser.parse(new StringReader(
                    "option_a,option_b,correct_answer\nA,B,A\n"));

            assertThat(outcome.headerValid()).isFalse();
            assertThat(outcome.questions()).isEmpty();
        }

        @Test
        @DisplayName("ignora diferenca de caixa e espacos no cabecalho")
        void headerIsCaseInsensitive() {
            final ParseOutcome outcome = parser.parse(new StringReader(
                    " Question , OPTION_A , option_b , Correct_Answer \n\"Pergunta\",A,B,B\n"));

            assertThat(outcome.headerValid()).isTrue();
            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.questions().get(0).correctIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("arquivo vazio invalida o cabecalho sem estourar excecao")
        void emptyFileIsInvalidHeader() {
            final ParseOutcome outcome = parser.parse(new StringReader(""));

            assertThat(outcome.headerValid()).isFalse();
            assertThat(outcome.questions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("RF05: uma linha ruim nao aborta a importacao")
    class LineValidation {

        @Test
        @DisplayName("isola a linha com gabarito invalido e mantem as validas")
        void invalidAnswerDoesNotAbort() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"Valida 1\",A,B,C,D,A,\"ok\"\n"
                    + "\"Invalida\",A,B,C,D,F,\"gabarito F nao existe\"\n"
                    + "\"Valida 2\",A,B,C,D,B,\"ok\"\n"));

            assertThat(outcome.headerValid()).isTrue();
            assertThat(outcome.questions()).hasSize(2);
            assertThat(outcome.errors()).hasSize(1);
            assertThat(outcome.errors().get(0).lineNumber()).isEqualTo(3);
            assertThat(outcome.errors().get(0).reason()).contains("gabarito");
        }

        @Test
        @DisplayName("rejeita enunciado vazio")
        void emptyQuestionIsRejected() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"\",A,B,C,D,A,\"sem enunciado\"\n"));

            assertThat(outcome.questions()).isEmpty();
            assertThat(outcome.errors()).hasSize(1);
            assertThat(outcome.errors().get(0).reason()).contains("enunciado");
        }

        @Test
        @DisplayName("rejeita linha com menos de duas alternativas")
        void singleOptionIsRejected() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"So uma alternativa\",A,,,,\"A\",\"\"\n"));

            assertThat(outcome.questions()).isEmpty();
            assertThat(outcome.errors()).hasSize(1);
            assertThat(outcome.errors().get(0).reason()).contains("alternativas");
        }

        @Test
        @DisplayName("gabarito que aponta para alternativa ausente e invalido")
        void answerBeyondPresentOptionsIsRejected() {
            // Duas alternativas presentes, gabarito "D" nao corresponde a nenhuma.
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"Pergunta\",A,B,,,D,\"\"\n"));

            assertThat(outcome.questions()).isEmpty();
            assertThat(outcome.errors()).hasSize(1);
            assertThat(outcome.errors().get(0).reason()).contains("gabarito");
        }

        @Test
        @DisplayName("linhas em branco sao ignoradas sem gerar erro")
        void blankLinesAreIgnored() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\n"
                    + "\"Valida\",A,B,,,A,\"\"\n"
                    + "\n"));

            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.errors()).isEmpty();
        }

        @Test
        @DisplayName("enunciado acima de 2000 caracteres e rejeitado")
        void tooLongQuestionIsRejected() {
            final String longQuestion = "x".repeat(2001);
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"" + longQuestion + "\",A,B,,,A,\"\"\n"));

            assertThat(outcome.questions()).isEmpty();
            assertThat(outcome.errors()).hasSize(1);
            assertThat(outcome.errors().get(0).reason()).contains("2000");
        }
    }

    @Nested
    @DisplayName("Anexo A: robustez do formato")
    class Robustness {

        @Test
        @DisplayName("campo entre aspas pode conter virgula (RFC 4180)")
        void quotedFieldMayContainSeparator() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"Qual destes, entre S3 e EBS, guarda objetos?\",S3,EBS,,,A,\"\"\n"));

            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.questions().get(0).question())
                    .isEqualTo("Qual destes, entre S3 e EBS, guarda objetos?");
        }

        @Test
        @DisplayName("campo entre aspas pode conter quebra de linha (RFC 4180)")
        void quotedFieldMayContainNewline() {
            final ParseOutcome outcome = parser.parse(new StringReader(VALID_HEADER + "\n"
                    + "\"Primeira linha\nsegunda linha\",A,B,,,A,\"\"\n"));

            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.questions().get(0).question()).contains("Primeira linha");
        }

        @Test
        @DisplayName("aceita option_e como quinta alternativa")
        void acceptsFifthOption() {
            final ParseOutcome outcome = parser.parse(new StringReader(
                    VALID_HEADER + ",option_e\n"
                            + "\"Cinco alternativas\",A,B,C,D,E,\"\",\"Alternativa E\"\n"));

            assertThat(outcome.questions()).hasSize(1);
            assertThat(outcome.questions().get(0).options()).hasSize(5);
            assertThat(outcome.questions().get(0).correctIndex()).isEqualTo(4);
        }

        @Test
        @DisplayName("limite operacional recusa o excedente sem estourar memoria")
        void respectsMaxLines() {
            final StringBuilder csv = new StringBuilder(VALID_HEADER).append('\n');
            for (int line = 0; line < 20; line++) {
                csv.append("\"Questao ").append(line).append("\",A,B,,,A,\"\"\n");
            }
            final OpenCsvParser limited = new OpenCsvParser(5);
            final ParseOutcome outcome = limited.parse(new StringReader(csv.toString()));

            assertThat(outcome.headerValid()).isTrue();
            assertThat(outcome.questions()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("falhas de leitura")
    class ReadFailures {

        @Test
        @DisplayName("leitor nulo e erro de contrato do chamador")
        void nullReaderFails() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(CsvParser.CsvReadException.class);
        }
    }
}
