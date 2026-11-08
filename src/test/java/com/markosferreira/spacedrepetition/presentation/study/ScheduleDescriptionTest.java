package com.markosferreira.spacedrepetition.presentation.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Testes do texto de agendamento (RF25).
 *
 * <p>A comparacao e por dia de calendario, e nao por diferenca de horas. Esses
 * testes fixam justamente os casos de fronteira que quebram essa distincao:
 * revisao que vence daqui a poucas horas mas em outro dia, e revisao atrasada.
 */
@DisplayName("ScheduleDescription (RF25)")
class ScheduleDescriptionTest {

    /** Fuso fixo: a regra nao pode depender do fuso da maquina de CI. */
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    /** Referencia: 8 de novembro de 2026, 10h em Sao Paulo (UTC-3). */
    private static final Instant NOW = Instant.parse("2026-11-08T13:00:00Z");

    @Test
    @DisplayName("questao nunca respondida e NEW")
    void newCardIsNew() {
        final ScheduleDescription schedule =
                ScheduleDescription.of(SchedulingInfo.newCard(), NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.NEW);
        assertThat(schedule.state()).isEqualTo(CardState.NEW);
    }

    @Test
    @DisplayName("vence daqui a poucas horas, no mesmo dia, e HOJE")
    void dueLaterTodayIsToday() {
        // 22h de hoje em Sao Paulo (01h UTC do dia seguinte).
        final ScheduleDescription schedule = ScheduleDescription.of(
                comAgendamento("2026-11-09T01:00:00Z", CardState.LEARNING), NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.TODAY);
    }

    @Test
    @DisplayName("vence amanha cedo e AMANHA, mesmo com menos de 24 horas")
    void earlyTomorrowIsTomorrow() {
        // 8h de amanha em Sao Paulo (11h UTC): 22 horas depois de NOW.
        final ScheduleDescription schedule = ScheduleDescription.of(
                comAgendamento("2026-11-09T11:00:00Z", CardState.LEARNING), NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.TOMORROW);
        assertThat(schedule.days()).isEqualTo(1);
    }

    @Test
    @DisplayName("revisao atrasada conta como HOJE, nao como dias negativos")
    void overdueIsToday() {
        final ScheduleDescription schedule = ScheduleDescription.of(
                comAgendamento("2026-11-01T13:00:00Z", CardState.REVIEW), NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.TODAY);
        assertThat(schedule.days()).isZero();
    }

    @Test
    @DisplayName("vence em cinco dias informa o numero de dias")
    void fiveDaysAhead() {
        final ScheduleDescription schedule = ScheduleDescription.of(
                comAgendamento("2026-11-13T13:00:00Z", CardState.REVIEW), NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.IN_DAYS);
        assertThat(schedule.days()).isEqualTo(5);
        assertThat(schedule.state()).isEqualTo(CardState.REVIEW);
    }

    @Test
    @DisplayName("agendamento nulo nao lanca excecao")
    void nullSchedulingIsSafe() {
        final ScheduleDescription schedule = ScheduleDescription.of(null, NOW, ZONE);

        assertThat(schedule.dueKind()).isEqualTo(ScheduleDescription.DueKind.NEW);
        assertThat(schedule.state()).isEqualTo(CardState.NEW);
    }

    /**
     * Monta um agendamento vencendo no instante informado.
     *
     * @param dueAt instante de vencimento
     * @param state estagio
     * @return agendamento pronto
     */
    private static SchedulingInfo comAgendamento(final String dueAt, final CardState state) {
        return SchedulingInfo.builder()
                .stability(1d)
                .difficulty(5d)
                .dueAt(Instant.parse(dueAt))
                .lastReviewAt(NOW)
                .repetitions(1)
                .lapses(0)
                .state(state)
                .build();
    }
}
