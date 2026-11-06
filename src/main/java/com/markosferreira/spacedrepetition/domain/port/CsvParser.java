package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;

import java.io.Reader;

/**
 * Porta de entrada para leitura de planilhas CSV.
 *
 * <p>A implementacao concreta (OpenCSV) vive na camada de dados. O dominio
 * conhece apenas a promessa: dado um leitor, devolver o que e valido e o que
 * nao e, sem lancar excecao por linha (ADR-003).
 */
public interface CsvParser {

    /**
     * Le e valida a planilha inteira.
     *
     * @param reader fonte de caracteres
     * @return questoes aceitas, erros por linha e validade do cabecalho
     * @throws CsvReadException se o arquivo nao puder ser lido
     */
    ParseOutcome parse(Reader reader);

    /**
     * Falha de leitura do arquivo como um todo (nao de uma linha).
     */
    class CsvReadException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CsvReadException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
