package com.markosferreira.spacedrepetition.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Valor lido de uma linha da planilha, ja validado.
 *
 * <p>E deliberadamente separado de {@link Card}: este objeto representa o
 * <em>contrato de entrada</em> (o CSV do usuario) e nao possui identidade,
 * agendamento nem baralho. A conversao para entidade acontece no caso de uso de
 * importacao, o que mantem a fronteira entre DTO de CSV e entidade de dominio.
 */
public final class CsvQuestion {

    private final int lineNumber;
    private final String question;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;

    public CsvQuestion(final int lineNumber, final String question,
                       final List<String> options, final int correctIndex,
                       final String explanation) {
        this.lineNumber = lineNumber;
        this.question = question;
        this.options = Collections.unmodifiableList(options);
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String question() {
        return question;
    }

    public List<String> options() {
        return options;
    }

    public int correctIndex() {
        return correctIndex;
    }

    public String explanation() {
        return explanation;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CsvQuestion)) {
            return false;
        }
        final CsvQuestion that = (CsvQuestion) other;
        return lineNumber == that.lineNumber
                && correctIndex == that.correctIndex
                && Objects.equals(question, that.question)
                && Objects.equals(options, that.options)
                && Objects.equals(explanation, that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, question, options, correctIndex, explanation);
    }

    @Override
    public String toString() {
        return "CsvQuestion{linha=" + lineNumber + ", alternativas=" + options.size() + '}';
    }
}
