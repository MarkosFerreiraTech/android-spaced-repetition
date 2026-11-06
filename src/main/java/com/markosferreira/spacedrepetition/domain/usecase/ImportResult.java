package com.markosferreira.spacedrepetition.domain.usecase;

import java.util.Collections;
import java.util.List;

/**
 * Relatorio de uma importacao de planilha (RF07).
 */
public final class ImportResult {

    private final int imported;
    private final int updated;
    private final int skipped;
    private final List<LineError> errors;

    public ImportResult(final int imported, final int updated,
                        final int skipped, final List<LineError> errors) {
        this.imported = imported;
        this.updated = updated;
        this.skipped = skipped;
        this.errors = Collections.unmodifiableList(errors);
    }

    public int imported() {
        return imported;
    }

    public int updated() {
        return updated;
    }

    public int skipped() {
        return skipped;
    }

    public List<LineError> errors() {
        return errors;
    }

    public int totalAccepted() {
        return imported + updated;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        return "ImportResult{imported=" + imported + ", updated=" + updated
                + ", skipped=" + skipped + ", errors=" + errors.size() + '}';
    }

    /**
     * Erro de validacao de uma linha da planilha.
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
