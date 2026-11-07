package com.markosferreira.spacedrepetition.domain.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markosferreira.spacedrepetition.domain.model.CsvQuestion;
import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;
import com.markosferreira.spacedrepetition.fakes.InMemoryCardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("ImportDeckUseCase")
class ImportDeckUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private InMemoryCardRepository cards;
    private InMemoryDeckRepository decks;
    private StubCsvParser parser;
    private ImportDeckUseCase useCase;

    @BeforeEach
    void setUp() {
        cards = new InMemoryCardRepository();
        decks = new InMemoryDeckRepository();
        parser = new StubCsvParser();
        useCase = new ImportDeckUseCase(parser, decks, cards);
    }

    @Test
    @DisplayName("importa as questoes e cria o baralho com nome do arquivo, sem extensao")
    void importsQuestionsAndCreatesDeck() {
        parser.outcome = new ParseOutcome(List.of(
                new CsvQuestion(2, "Questao 1", List.of("A", "B"), 0, null),
                new CsvQuestion(3, "Questao 2", List.of("A", "B"), 1, "explicacao")),
                Collections.emptyList(), true, List.of("question", "option_a"));

        final ImportResult result = useCase.execute(
                new StringReader("csv"), "simulado_aws.csv", NOW);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.updated()).isZero();
        assertThat(cards.size()).isEqualTo(2);
        assertThat(decks.saved).hasSize(1);
        assertThat(decks.saved.get(0).name()).isEqualTo("simulado_aws");
    }

    @Test
    @DisplayName("reimportar o mesmo conteudo nao duplica questoes (upsert idempotente)")
    void reimportIsIdempotent() {
        parser.outcome = new ParseOutcome(List.of(
                new CsvQuestion(2, "Questao 1", List.of("A", "B"), 0, null)),
                Collections.emptyList(), true, List.of("question"));

        useCase.execute(new StringReader("csv"), "simulado.csv", NOW);
        final ImportResult second = useCase.execute(new StringReader("csv"), "simulado.csv", NOW);

        assertThat(cards.size()).isEqualTo(1);
        assertThat(second.imported()).isZero();
        assertThat(second.updated()).isEqualTo(1);
    }

    @Test
    @DisplayName("propaga erros de linha no relatorio sem abortar a importacao")
    void reportsLineErrors() {
        final List<ParseOutcome.LineError> errors = new ArrayList<>();
        errors.add(new ParseOutcome.LineError(10, "gabarito invalido"));
        parser.outcome = new ParseOutcome(List.of(
                new CsvQuestion(2, "Questao 1", List.of("A", "B"), 0, null)),
                errors, true, List.of("question"));

        final ImportResult result = useCase.execute(new StringReader("csv"), "s.csv", NOW);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).lineNumber()).isEqualTo(10);
        assertThat(result.errors().get(0).reason()).contains("gabarito");
    }

    @Test
    @DisplayName("cabecalho invalido lanca excecao com as colunas encontradas")
    void invalidHeaderThrows() {
        parser.outcome = ParseOutcome.invalidHeader(List.of("pergunta", "resposta"));

        assertThatThrownBy(() -> useCase.execute(new StringReader("csv"), "s.csv", NOW))
                .isInstanceOf(ImportDeckUseCase.InvalidHeaderException.class)
                .satisfies(thrown -> assertThat(
                        ((ImportDeckUseCase.InvalidHeaderException) thrown).foundColumns())
                        .containsExactly("pergunta", "resposta"));
        assertThat(cards.size()).isZero();
    }

    @Test
    @DisplayName("nome de baralho vazio recebe um nome padrao")
    void blankDeckNameGetsDefault() {
        parser.outcome = new ParseOutcome(Collections.emptyList(), Collections.emptyList(),
                true, List.of("question"));

        useCase.execute(new StringReader("csv"), "  ", NOW);

        assertThat(decks.saved.get(0).name()).isEqualTo("Simulado importado");
    }

    @Test
    @DisplayName("rejeita fonte nula")
    void rejectsNullReader() {
        assertThatThrownBy(() -> useCase.execute(null, "s.csv", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Parser stub: devolve o resultado pre-configurado. */
    private static final class StubCsvParser implements CsvParser {
        private ParseOutcome outcome = new ParseOutcome(Collections.emptyList(),
                Collections.emptyList(), true, Collections.emptyList());

        @Override
        public ParseOutcome parse(final java.io.Reader reader) {
            return outcome;
        }
    }

    /** Repositorio de baralhos em memoria. */
    private static final class InMemoryDeckRepository
            implements com.markosferreira.spacedrepetition.domain.port.DeckRepository {

        private final List<com.markosferreira.spacedrepetition.domain.model.Deck> saved =
                new ArrayList<>();
        private final Map<String, com.markosferreira.spacedrepetition.domain.model.Deck> byId =
                new HashMap<>();

        @Override
        public List<com.markosferreira.spacedrepetition.domain.model.DeckSummary>
                findAllWithCounts(final Instant now) {
            return Collections.emptyList();
        }

        @Override
        public com.markosferreira.spacedrepetition.domain.model.Deck findById(final String deckId) {
            return byId.get(deckId);
        }

        @Override
        public com.markosferreira.spacedrepetition.domain.model.Deck findByName(final String name) {
            return saved.stream()
                    .filter(deck -> deck.name().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void save(final com.markosferreira.spacedrepetition.domain.model.Deck deck) {
            saved.add(deck);
            byId.put(deck.id(), deck);
        }
    }
}
