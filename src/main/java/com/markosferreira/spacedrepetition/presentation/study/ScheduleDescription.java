package com.markosferreira.spacedrepetition.presentation.study;

import com.markosferreira.spacedrepetition.domain.model.CardState;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Traduz o agendamento FSRS em texto para o estudante (RF25).
 *
 * <p>O {@link SchedulingInfo} guarda estabilidade e dificuldade, que sao uteis
 * para o algoritmo e inuteis para quem esta estudando. Aqui a conversao e para
 * duas perguntas que a pessoa realmente faz: "quando isso volta?" e "em que pe
 * estou?".
 *
 * <p>Sem dependencia de Android de proposito: a formatacao de data varia por
 * locale, entao o texto final e montado na Activity com recursos; esta classe
 * apenas decide <b>qual</b> caso se aplica. Assim a regra pode ser testada com
 * relogio fixo.
 */
public final class ScheduleDescription {

    /**
     * Casos de proxima revisao, na ordem em que sao avaliados.
     */
    public enum DueKind {

        /** Questao nunca respondida. */
        NEW,

        /** Vence hoje (ou ja venceu). */
        TODAY,

        /** Vence amanha. */
        TOMORROW,

        /** Vence em dois dias ou mais. */
        IN_DAYS
    }

    private final DueKind dueKind;
    private final int days;
    private final CardState state;

    private ScheduleDescription(final DueKind dueKind, final int days, final CardState state) {
        this.dueKind = dueKind;
        this.days = days;
        this.state = state;
    }

    /**
     * Descreve o agendamento.
     *
     * @param scheduling agendamento calculado
     * @param now instante de referencia
     * @param zone fuso do aparelho
     * @return descricao pronta para virar texto
     */
    public static ScheduleDescription of(final SchedulingInfo scheduling, final Instant now,
                                         final ZoneId zone) {
        if (scheduling == null || scheduling.state() == null) {
            return new ScheduleDescription(DueKind.NEW, 0, CardState.NEW);
        }
        final Instant due = scheduling.dueAt();
        if (due == null || scheduling.state() == CardState.NEW) {
            return new ScheduleDescription(DueKind.NEW, 0, scheduling.state());
        }

        // A comparacao e por DIA de calendario, nao por diferenca de horas: uma
        // revisao as 23h de hoje e as 8h de amanha sao "hoje" e "amanha" para
        // quem esta estudando, mesmo com 9 horas de diferenca.
        final ZonedDateTime dueDay = due.atZone(zone).toLocalDate().atStartOfDay(zone);
        final ZonedDateTime today = now.atZone(zone).toLocalDate().atStartOfDay(zone);
        final long days = Duration.between(today, dueDay).toDays();

        if (days <= 0) {
            return new ScheduleDescription(DueKind.TODAY, 0, scheduling.state());
        }
        if (days == 1) {
            return new ScheduleDescription(DueKind.TOMORROW, 1, scheduling.state());
        }
        return new ScheduleDescription(DueKind.IN_DAYS, (int) days, scheduling.state());
    }

    /**
     * @return caso da proxima revisao
     */
    public DueKind dueKind() {
        return dueKind;
    }

    /**
     * @return dias ate a revisao; zero quando vence hoje ou e nova
     */
    public int days() {
        return days;
    }

    /**
     * @return estagio de aprendizado da questao
     */
    public CardState state() {
        return state;
    }
}
