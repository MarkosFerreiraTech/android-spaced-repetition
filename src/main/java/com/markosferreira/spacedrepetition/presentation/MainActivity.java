package com.markosferreira.spacedrepetition.presentation;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.markosferreira.spacedrepetition.R;
import com.markosferreira.spacedrepetition.databinding.ActivityMainBinding;

/**
 * Ponto de entrada temporario do aplicativo.
 *
 * <p>Sera substituido pela tela de simulados na issue #12. Existe para que o
 * projeto compile e possua um APK executavel desde o primeiro marco tecnico.
 */
public final class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.textPlaceholder.setText(getString(R.string.app_name));
    }
}
