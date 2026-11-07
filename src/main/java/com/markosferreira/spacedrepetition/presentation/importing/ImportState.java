package com.markosferreira.spacedrepetition.presentation.importing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado da tela de importacao.
 *
 * <p>Objeto unico com as quatro fases, pelo mesmo motivo de
 * {@code DeckListState}: estados invalidos devem ser irrepresentaveis.
 */
public final class ImportState {

    /** Fase da importacao. */
    public enum Phase {
        /** Escolha do arquivo e instrucoes. */
        IDLE,
        /** Leitura e persistencia em andamento. */
        RUNNING,
        /** Concluida: relatorio disponivel. */
        REPORT,
        /** Falhou: mensagem e possivel nova tentativa. */
        ERROR
    }

    private final Phase phase;
    private final String fileName;
    private final int imported;
    private final int updated;
    private final int skipped;
    private final List<LineError> errors;
    private final String errorTitle;
    private final String errorDetail;

    private ImportState(final Phase phase, final String fileName, final Counts counts,
                        final List<LineError> errors, final String errorTitle,
                        final String errorDetail) {
        this.phase = phase;
        this.fileName = fileName;
        this.imported = counts.imported;
        this.updated = counts.updated;
        this.skipped = counts.skipped;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.errorTitle = errorTitle;
        this.errorDetail = errorDetail;
    }

    /**
     * Estado inicial.
     *
     * @return estado ocioso
     */
    public static ImportState idle() {
        return new ImportState(Phase.IDLE, null, new Counts(), List.of(), null, null);
    }

    /**
     * Importacao em andamento.
     *
     * @param fileName nome do arquivo escolhido
     * @return estado em execucao
     */
    public static ImportState running(final String fileName) {
        return new ImportState(Phase.RUNNING, fileName, new Counts(), List.of(), null, null);
    }

    /**
     * Relatorio final.
     *
     * @param fileName nome do arquivo
     * @param imported questoes novas
     * @param updated questoes atualizadas
     * @param skipped linhas ignoradas
     * @param errors linhas com erro
     * @return estado com relatorio
     */
    public static ImportState report(final String fileName, final int imported, final int updated,
                                     final int skipped, final List<LineError> errors) {
        return new ImportState(Phase.REPORT, fileName,
                new Counts(imported, updated, skipped), errors, null, null);
    }

    /**
     * Falha.
     *
     * @param title titulo curto
     * @param detail explicacao ao usuario
     * @return estado de erro
     */
    public static ImportState failed(final String title, final String detail) {
        return new ImportState(Phase.ERROR, null, new Counts(), List.of(), title, detail);
    }

    /**
     * Fase atual.
     *
     * @return fase
     */
    public Phase phase() {
        return phase;
    }

    /**
     * Nome do arquivo em processamento ou processado.
     *
     * @return nome ou null
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Questoes novas.
     *
     * @return quantidade
     */
    public int imported() {
        return imported;
    }

    /**
     * Questoes atualizadas.
     *
     * @return quantidade
     */
    public int updated() {
        return updated;
    }

    /**
     * Linhas ignoradas.
     *
     * @return quantidade
     */
    public int skipped() {
        return skipped;
    }

    /**
     * Linhas com erro.
     *
     * @return lista imutavel
     */
    public List<LineError> errors() {
        return errors;
    }

    /**
     * Indica se ha linhas com erro a exibir.
     *
     * @return verdadeiro se a lista nao estiver vazia
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Titulo da falha.
     *
     * @return titulo ou null
     */
    public String errorTitle() {
        return errorTitle;
    }

    /**
     * Detalhe da falha.
     *
     * @return detalhe ou null
     */
    public String errorDetail() {
        return errorDetail;
    }

    /**
     * Linha que nao pode ser importada.
     */
    public static final class LineError {
        private final int lineNumber;
        private final String reason;

        /**
         * Cria o erro de linha.
         *
         * @param lineNumber numero da linha no arquivo
         * @param reason motivo
         */
        public LineError(final int lineNumber, final String reason) {
            this.lineNumber = lineNumber;
            this.reason = reason;
        }

        /**
         * Numero da linha.
         *
         * @return linha
         */
        public int lineNumber() {
            return lineNumber;
        }

        /**
         * Motivo.
         *
         * @return motivo
         */
        public String reason() {
            return reason;
        }
    }

    /**
     * Agrupador das tres contagens do relatorio.
     */
    private static final class Counts {
        private final int imported;
        private final int updated;
        private final int skipped;

        Counts() {
            this(0, 0, 0);
        }

        Counts(final int imported, final int updated, final int skipped) {
            this.imported = imported;
            this.updated = updated;
            this.skipped = skipped;
        }
    }
}
