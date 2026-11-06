package com.markosferreira.spacedrepetition.domain.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.markosferreira.spacedrepetition.domain.model.Rating;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RatingPolicy")
class RatingPolicyTest {

    private static final long AVERAGE = 20_000L;

    @Test
    @DisplayName("erro sempre vira AGAIN, mesmo marcado")
    void wrongIsAlwaysAgain() {
        assertThat(RatingPolicy.derive(false, false, 1_000L, AVERAGE)).isEqualTo(Rating.AGAIN);
        assertThat(RatingPolicy.derive(false, true, 30_000L, AVERAGE)).isEqualTo(Rating.AGAIN);
    }

    @Test
    @DisplayName("acerto marcado como duvida vira HARD")
    void flaggedCorrectIsHard() {
        assertThat(RatingPolicy.derive(true, true, 500L, AVERAGE)).isEqualTo(Rating.HARD);
    }

    @Test
    @DisplayName("acerto normal vira GOOD")
    void normalCorrectIsGood() {
        assertThat(RatingPolicy.derive(true, false, 15_000L, AVERAGE)).isEqualTo(Rating.GOOD);
    }

    @Test
    @DisplayName("acerto muito rapido e sem marcacao vira EASY")
    void fastCorrectIsEasy() {
        assertThat(RatingPolicy.derive(true, false, 2_000L, AVERAGE)).isEqualTo(Rating.EASY);
    }

    @Test
    @DisplayName("sem historico de tempo, usa a media padrao")
    void usesDefaultAverageWhenUnknown() {
        assertThat(RatingPolicy.derive(true, false, 1_000L, 0L)).isEqualTo(Rating.EASY);
        assertThat(RatingPolicy.derive(true, false,
                RatingPolicy.DEFAULT_AVERAGE_MILLIS, 0L)).isEqualTo(Rating.GOOD);
    }

    @Test
    @DisplayName("tempo zero nao conta como rapidez")
    void zeroElapsedIsNotFast() {
        assertThat(RatingPolicy.derive(true, false, 0L, AVERAGE)).isEqualTo(Rating.GOOD);
    }
}
