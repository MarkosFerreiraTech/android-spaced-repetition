package com.markosferreira.spacedrepetition.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.markosferreira.spacedrepetition.domain.port.PreferencesRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;

/**
 * Preferencias do usuario cifradas com AES256-GCM (RF23, RNF05).
 *
 * <p>A chave mestra vive no Android Keystore (nao exportavel, hardware-backed
 * quando o aparelho suporta). Os valores no arquivo XML ficam cifrados e as
 * chaves, ofuscadas. Assim, um backup indevido do sandbox nao entrega as
 * preferencias em texto claro.
 *
 * <p>O dominio continua ignorando o mecanismo: ele so conhece a porta
 * {@link PreferencesRepository}.
 */
public final class EncryptedPreferencesRepository implements PreferencesRepository {

    /** Nome do arquivo de preferencias cifrado. */
    public static final String FILE_NAME = "cerebro_prefs";

    /** Chave do limite diario de questoes novas. */
    private static final String KEY_DAILY_LIMIT = "daily_new_limit";

    /** Chave do fuso horario. */
    private static final String KEY_ZONE_ID = "zone_id";

    /** Limite padrao de questoes novas por dia. */
    public static final int DEFAULT_DAILY_LIMIT = 20;

    private final SharedPreferences preferences;

    /**
     * Cria o repositorio sobre o arquivo cifrado.
     *
     * @param preferences preferencias ja cifradas
     */
    public EncryptedPreferencesRepository(final SharedPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Abre (ou cria) o armazenamento cifrado.
     *
     * <p>Se o Keystore nao estiver disponivel, falhar aqui e melhor do que
     * gravar em texto claro silenciosamente (RF24).
     *
     * @param context contexto da aplicacao
     * @return repositorio pronto para uso
     * @throws SecureStorageException se o armazenamento seguro falhar
     */
    public static EncryptedPreferencesRepository create(final Context context) {
        try {
            final MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            final SharedPreferences encrypted = EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return new EncryptedPreferencesRepository(encrypted);
        } catch (final GeneralSecurityException | IOException exception) {
            throw new SecureStorageException("Armazenamento seguro indisponivel", exception);
        }
    }

    @Override
    public int dailyNewLimit() {
        return preferences.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT);
    }

    @Override
    public ZoneId zoneId() {
        final String stored = preferences.getString(KEY_ZONE_ID, null);
        if (stored == null || stored.isEmpty()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(stored);
        } catch (final RuntimeException exception) {
            // Valor corrompido nao deve derrubar o app: volta ao fuso do aparelho.
            return ZoneId.systemDefault();
        }
    }

    @Override
    public void save(final int dailyNewLimit, final ZoneId zoneId) {
        if (dailyNewLimit <= 0) {
            throw new IllegalArgumentException("Limite diario deve ser positivo");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("Fuso horario obrigatorio");
        }
        try {
            preferences.edit()
                    .putInt(KEY_DAILY_LIMIT, dailyNewLimit)
                    .putString(KEY_ZONE_ID, zoneId.getId())
                    .apply();
        } catch (final RuntimeException exception) {
            throw new SecureStorageException("Falha ao gravar preferencias", exception);
        }
    }
}
