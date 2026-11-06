package com.markosferreira.spacedrepetition.domain.port;

import java.time.ZoneId;

/**
 * Porta de saida para preferencias do usuario.
 *
 * <p>A implementacao cifra os valores (RF23); o dominio nao sabe disso, o que
 * mantem o DevSecOps fora do nucleo de negocio.
 */
public interface PreferencesRepository {

    /**
     * Limite diario de questoes novas apresentado ao estudante.
     *
     * @return limite configurado
     */
    int dailyNewLimit();

    /**
     * Fuso horario usado no calculo de "pendente hoje".
     *
     * @return fuso configurado
     */
    ZoneId zoneId();

    /**
     * Persiste as preferencias.
     *
     * @param dailyNewLimit novo limite diario
     * @param zoneId novo fuso horario
     * @throws SecureStorageException se o armazenamento seguro nao estiver disponivel
     */
    void save(int dailyNewLimit, ZoneId zoneId);

    /**
     * Falha ao acessar o armazenamento seguro (RF24).
     */
    class SecureStorageException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SecureStorageException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
