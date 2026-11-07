package com.markosferreira.spacedrepetition.data.csv;

import com.markosferreira.spacedrepetition.domain.model.CsvQuestion;
import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementacao do parser de CSV sobre o OpenCSV (RF04, RF05, Anexo A).
 *
 * <p>O arquivo e lido em uma unica passada. Cada linha e validada de forma
 * independente: uma linha ruim e anotada no relatorio e as demais seguem
 * (ADR-003 — erro de linha nao aborta a importacao). Somente um cabecalho fora
 * do contrato invalida o arquivo inteiro.
 *
 * <p>A leitura respeita RFC 4180: campos entre aspas podem conter o separador
 * e quebras de linha.
 */
public final class OpenCsvParser implements CsvParser {

    /** Separador padrao do contrato (Anexo A). */
    private static final char SEPARATOR = ',';

    /** Coluna obrigatoria com o enunciado. */
    private static final String COL_QUESTION = "question";

    /** Coluna obrigatoria com o gabarito. */
    private static final String COL_CORRECT = "correct_answer";

    /** Coluna opcional com a justificativa. */
    private static final String COL_EXPLANATION = "explanation";

    /** Prefixo das colunas de alternativa. */
    private static final String COL_OPTION_PREFIX = "option_";

    /** Maior alternativa suportada: option_a ate option_e. */
    private static final int MAX_OPTIONS = 5;

    /** Limite de caracteres do enunciado (Anexo A). */
    private static final int MAX_QUESTION_LENGTH = 2000;

    /** Minimo de alternativas para uma questao ser valida (RF05). */
    private static final int MIN_OPTIONS = 2;

    /** Evita laco infinito caso o OpenCSV devolva linhas vazias em sequencia. */
    private static final int MAX_CONSECUTIVE_BLANK = 10000;

    private final int maxLines;

    /**
     * Cria o parser.
     *
     * @param maxLines limite operacional de linhas do arquivo (RF — Anexo A)
     */
    public OpenCsvParser(final int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public ParseOutcome parse(final Reader reader) {
        if (reader == null) {
            throw new CsvReadException("Leitor do CSV obrigatorio", null);
        }
        try {
            return read(reader);
        } catch (final CsvReadException exception) {
            // Ja e a falha de contrato da porta: propaga sem reembrulhar.
            throw exception;
        } catch (final RuntimeException exception) {
            throw new CsvReadException("Falha ao ler o CSV", exception);
        }
    }

    private ParseOutcome read(final Reader reader) {
        final com.opencsv.CSVReader csv = new com.opencsv.CSVReaderBuilder(reader)
                .withCSVParser(new com.opencsv.RFC4180Parser())
                .build();
        final String[] header = readHeader(csv);
        if (header == null) {
            return ParseOutcome.invalidHeader(new ArrayList<>());
        }
        final Map<String, Integer> columns = indexOf(header);
        if (!columns.containsKey(COL_QUESTION) || !columns.containsKey(COL_CORRECT)) {
            return ParseOutcome.invalidHeader(normalizeHeader(header));
        }
        final Questions accumulator = new Questions();
        int lineNumber = 1;
        int blankRun = 0;
        String[] row = nextRow(csv);
        while (row != null && lineNumber < maxLines) {
            lineNumber = lineNumber + 1;
            if (isBlank(row)) {
                blankRun = blankRun + 1;
                if (blankRun > MAX_CONSECUTIVE_BLANK) {
                    break;
                }
                row = nextRow(csv);
                continue;
            }
            blankRun = 0;
            accumulate(accumulator, columns, row, lineNumber);
            row = nextRow(csv);
        }
        return accumulator.toOutcome(header);
    }

    private String[] readHeader(final com.opencsv.CSVReader csv) {
        final String[] header = nextRow(csv);
        if (header == null) {
            return null;
        }
        for (final String column : header) {
            if (column == null) {
                return null;
            }
        }
        return header;
    }

    private String[] nextRow(final com.opencsv.CSVReader csv) {
        try {
            return csv.readNext();
        } catch (final IOException | com.opencsv.exceptions.CsvValidationException exception) {
            throw new CsvReadException("Linha malformada no CSV", exception);
        }
    }

    private Map<String, Integer> indexOf(final String[] header) {
        final Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.length; index++) {
            columns.put(normalize(header[index]), index);
        }
        return columns;
    }

    private void accumulate(final Questions accumulator, final Map<String, Integer> columns,
                            final String[] row, final int lineNumber) {
        final String question = value(columns, row, COL_QUESTION);
        if (question == null || question.trim().isEmpty()) {
            accumulator.error(lineNumber, "enunciado vazio");
            return;
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            accumulator.error(lineNumber, "enunciado excede " + MAX_QUESTION_LENGTH + " caracteres");
            return;
        }
        final List<String> options = readOptions(columns, row);
        if (options.size() < MIN_OPTIONS) {
            accumulator.error(lineNumber, "menos de " + MIN_OPTIONS + " alternativas validas");
            return;
        }
        final int correctIndex = readCorrectIndex(columns, row, options);
        if (correctIndex < 0) {
            accumulator.error(lineNumber, "gabarito invalido");
            return;
        }
        accumulator.accept(new CsvQuestion(lineNumber, question.trim(), options, correctIndex,
                value(columns, row, COL_EXPLANATION)));
    }

    private List<String> readOptions(final Map<String, Integer> columns, final String[] row) {
        final List<String> options = new ArrayList<>();
        for (int position = 0; position < MAX_OPTIONS; position++) {
            final char letter = (char) ('a' + position);
            final String raw = value(columns, row, COL_OPTION_PREFIX + letter);
            if (raw == null || raw.trim().isEmpty()) {
                // Alternativas contiguas: a primeira ausente encerra a lista.
                break;
            }
            options.add(raw.trim());
        }
        return options;
    }

    private int readCorrectIndex(final Map<String, Integer> columns, final String[] row,
                                 final List<String> options) {
        final String raw = value(columns, row, COL_CORRECT);
        if (raw == null || raw.trim().isEmpty()) {
            return -1;
        }
        final String letter = raw.trim().toUpperCase(Locale.ROOT);
        if (letter.length() != 1) {
            return -1;
        }
        final int index = letter.charAt(0) - 'A';
        // O gabarito deve apontar para uma alternativa realmente presente.
        return index >= 0 && index < options.size() ? index : -1;
    }

    private String value(final Map<String, Integer> columns, final String[] row,
                         final String column) {
        final Integer position = columns.get(column);
        if (position == null || position >= row.length) {
            return null;
        }
        return row[position];
    }

    private boolean isBlank(final String[] row) {
        for (final String cell : row) {
            if (cell != null && !cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<String> normalizeHeader(final String[] header) {
        final List<String> columns = new ArrayList<>();
        for (final String column : header) {
            columns.add(column == null ? "" : column.trim());
        }
        return columns;
    }

    private String normalize(final String column) {
        return column == null ? "" : column.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Acumulador mutavel usado durante a leitura; evita listas recriadas por linha.
     */
    private static final class Questions {
        private final List<CsvQuestion> questions = new ArrayList<>();
        private final List<ParseOutcome.LineError> errors = new ArrayList<>();

        void accept(final CsvQuestion question) {
            questions.add(question);
        }

        void error(final int lineNumber, final String reason) {
            errors.add(new ParseOutcome.LineError(lineNumber, reason));
        }

        ParseOutcome toOutcome(final String[] header) {
            return new ParseOutcome(questions, errors, true, headerList(header));
        }

        private List<String> headerList(final String[] header) {
            final List<String> columns = new ArrayList<>();
            for (final String column : header) {
                columns.add(column == null ? "" : column.trim());
            }
            return columns;
        }
    }
}
