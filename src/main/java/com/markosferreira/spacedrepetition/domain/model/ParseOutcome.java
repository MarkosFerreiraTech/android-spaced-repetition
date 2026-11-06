package com.markosferreira.spacedrepetition.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Resultado do parsing de uma planilha: o que foi aceito e o que foi rejeitado.
 *
 * <p>O parsing nunca lanca excecao por linha invalida: acumula o erro e segue
 * adiante, para que uma linha ruim nao invalide um arquivo inteiro (RF05).
 */
public final class ParseOutcome {

    private final List<CsvQuestion> questions;
    private final List<LineError> errors;
    private final boolean headerValid;
    private final List<String> foundColumns;

    public ParseOutcome(final List<CsvQuestion> questions,
                        final List<LineError> errors,
                        final boolean headerValid,
                        final List<String> foundColumns) {
        this.questions = Collections.unmodifiableList(questions);
        this.errors = Collections.unmodifiableList(errors);
        this.headerValid = headerValid;
        this.foundColumns = Collections.unmodifiableList(foundColumns);
    }

    /**
     * Resultado de cabecalho invalido.
     *
     * @param foundColumns colunas efetivamente encontradas
     * @return resultado sem questoes e com headerValid falso
     */
    public static ParseOutcome invalidHeader(final List<String> foundColumns) {
        return new ParseOutcome(Collections.emptyList(), Collections.emptyList(),
                false, foundColumns);
    }

    public List<CsvQuestion> questions() {
        return questions;
    }

    public List<LineError> errors() {
        return errors;
    }

    public boolean headerValid() {
        return headerValid;
    }

    public List<String> foundColumns() {
        return foundColumns;
    }

    /**
     * Erro de uma linha especifica.
     */
    public static final class LineError {
        private final int lineNumber;
        private final String reason;

        public LineError(final int lineNumber, final String reason) {
            this.lineNumber = lineNumber;
            this.reason = reason;
        }

        public int lineNumber() {
            return lineNumber;
        }

        public String reason() {
            return reason;
        }

        @Override
        public String toString() {
            return "linha " + lineNumber + ": " + reason;
        }
    }
}
