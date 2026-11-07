package com.markosferreira.spacedrepetition.data.prefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import android.content.SharedPreferences;

import com.markosferreira.spacedrepetition.domain.port.PreferencesRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do repositorio de preferencias cifradas.
 *
 * <p>O {@code SharedPreferences} injetado aqui e o arquivo cifrado pelo
 * EncryptedSharedPreferences em producao; o teste cobre a logica de leitura,
 * escrita, validacao e degradacao segura (RF23, RF24).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EncryptedPreferencesRepository")
class EncryptedPreferencesRepositoryTest {

    @Mock
    private SharedPreferences preferences;

    @Mock
    private SharedPreferences.Editor editor;

    private EncryptedPreferencesRepository repository;

    @BeforeEach
    void setUp() {
        when(preferences.edit()).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        repository = new EncryptedPreferencesRepository(preferences);
    }

    @Test
    @DisplayName("usa o limite padrao quando nada foi configurado")
    void defaultDailyLimit() {
        when(preferences.getInt("daily_new_limit",
                EncryptedPreferencesRepository.DEFAULT_DAILY_LIMIT)).thenReturn(20);

        assertThat(repository.dailyNewLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("devolve o limite persistido")
    void persistedDailyLimit() {
        when(preferences.getInt("daily_new_limit",
                EncryptedPreferencesRepository.DEFAULT_DAILY_LIMIT)).thenReturn(65);

        assertThat(repository.dailyNewLimit()).isEqualTo(65);
    }

    @Test
    @DisplayName("usa o fuso do aparelho quando nada foi configurado")
    void defaultZone() {
        when(preferences.getString("zone_id", null)).thenReturn(null);

        assertThat(repository.zoneId()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    @DisplayName("le o fuso persistido")
    void persistedZone() {
        when(preferences.getString("zone_id", null)).thenReturn("America/Sao_Paulo");

        assertThat(repository.zoneId()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    @DisplayName("fuso corrompido degrada para o fuso do aparelho em vez de quebrar")
    void corruptedZoneFallsBack() {
        when(preferences.getString("zone_id", null)).thenReturn("Marte/Olympus");

        assertThat(repository.zoneId()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    @DisplayName("grava limite e fuso no arquivo cifrado")
    void savesValues() {
        repository.save(30, ZoneId.of("America/Sao_Paulo"));

        verify(editor).putInt("daily_new_limit", 30);
        verify(editor).putString("zone_id", "America/Sao_Paulo");
        verify(editor).apply();
    }

    @Test
    @DisplayName("recusa limite nao positivo")
    void rejectsNonPositiveLimit() {
        assertThatThrownBy(() -> repository.save(0, ZoneId.of("UTC")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("falha de escrita no armazenamento seguro vira SecureStorageException (RF24)")
    void writeFailureIsWrapped() {
        when(editor.putInt(anyString(), anyInt()))
                .thenThrow(new IllegalStateException("keystore indisponivel"));

        assertThatThrownBy(() -> repository.save(30, ZoneId.of("UTC")))
                .isInstanceOf(PreferencesRepository.SecureStorageException.class);
    }
}
