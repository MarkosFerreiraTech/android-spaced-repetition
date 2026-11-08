package com.markosferreira.spacedrepetition.presentation.study;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;

import java.util.concurrent.Executor;

/**
 * Contagens da tela de escolha de modo (RF12).
 *
 * <p>Existe porque a tela precisa de tres consultas ao banco antes de desenhar,
 * e o Room <b>lanca excecao</b> quando isso acontece na thread principal — era
 * exatamente o que derrubava o app ao tocar em "estudar". As consultas rodam no
 * executor injetado e o resultado volta por {@code postValue}.
 */
public final class StudyModeViewModel extends ViewModel {

    /**
     * Contagens por modalidade.
     */
    public static final class Counts {

        private final int total;
        private final int wrong;
        private final int wrongOrFlagged;

        /**
         * Cria o conjunto de contagens.
         *
         * @param total questoes do baralho
         * @param wrong questoes erradas na ultima tentativa
         * @param wrongOrFlagged erradas mais as marcadas com duvida
         */
        public Counts(final int total, final int wrong, final int wrongOrFlagged) {
            this.total = total;
            this.wrong = wrong;
            this.wrongOrFlagged = wrongOrFlagged;
        }

        /**
         * @return total de questoes do baralho
         */
        public int total() {
            return total;
        }

        /**
         * @return questoes erradas na ultima tentativa
         */
        public int wrong() {
            return wrong;
        }

        /**
         * @return erradas mais as marcadas com duvida
         */
        public int wrongOrFlagged() {
            return wrongOrFlagged;
        }
    }

    private final CardRepository cardRepository;
    private final ReviewRepository reviewRepository;
    private final Executor io;
    private final MutableLiveData<Counts> counts = new MutableLiveData<>();

    /**
     * Cria o ViewModel.
     *
     * @param cardRepository porta de questoes
     * @param reviewRepository porta de revisoes
     * @param io executor de background (o Room proibe acesso na thread de UI)
     */
    public StudyModeViewModel(final CardRepository cardRepository,
                              final ReviewRepository reviewRepository,
                              final Executor io) {
        this.cardRepository = cardRepository;
        this.reviewRepository = reviewRepository;
        this.io = io;
    }

    /**
     * Contagens observaveis; nulo enquanto o calculo nao terminou.
     *
     * @return estado observavel
     */
    public LiveData<Counts> counts() {
        return counts;
    }

    /**
     * Calcula as contagens em background.
     *
     * @param deckId baralho escolhido
     */
    public void load(@Nullable final String deckId) {
        if (deckId == null) {
            return;
        }
        io.execute(() -> counts.postValue(compute(deckId)));
    }

    private Counts compute(final String deckId) {
        try {
            return new Counts(cardRepository.findByDeck(deckId).size(),
                    reviewRepository.findWrongByDeck(deckId).size(),
                    reviewRepository.findWrongOrFlaggedByDeck(deckId).size());
        } catch (final RuntimeException exception) {
            // Sem contagens a tela continua util: mostra os modos sem numero.
            return new Counts(0, 0, 0);
        }
    }
}
