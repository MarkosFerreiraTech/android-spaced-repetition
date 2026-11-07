package com.markosferreira.spacedrepetition.data.repository;

import com.markosferreira.spacedrepetition.data.local.DeckAccuracyRow;
import com.markosferreira.spacedrepetition.data.local.ReviewLogDao;
import com.markosferreira.spacedrepetition.domain.port.StatisticsRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador Room da porta {@link StatisticsRepository}.
 *
 * <p>Toda agregacao acontece no SQL (RF19): trazer 65 mil linhas de review para
 * somar em memoria seria desperdicio de CPU e de memoria no celular.
 */
public final class RoomStatisticsRepository implements StatisticsRepository {

    /** Segundos em um dia; usado para converter epoch millis em dia local. */
    private static final long SECONDS_PER_DAY = 86400L;

    private final ReviewLogDao reviewLogDao;
    private final ZoneId zoneId;

    /**
     * Cria o repositorio.
     *
     * @param reviewLogDao DAO do historico
     * @param zoneId fuso usado no calculo da sequencia de dias
     */
    public RoomStatisticsRepository(final ReviewLogDao reviewLogDao, final ZoneId zoneId) {
        this.reviewLogDao = reviewLogDao;
        this.zoneId = zoneId;
    }

    @Override
    public Accuracy globalAccuracy() {
        return new Accuracy(reviewLogDao.countCorrect(), reviewLogDao.countAll());
    }

    @Override
    public int totalReviews() {
        return reviewLogDao.countAll();
    }

    @Override
    public int currentStreakDays() {
        final int offset = ZonedDateTime.now(zoneId).getOffset().getTotalSeconds();
        final List<Long> days = reviewLogDao.distinctStudyDays(offset);
        if (days.isEmpty()) {
            return 0;
        }
        final long today = LocalDate.now(zoneId).toEpochDay();
        int streak = 0;
        long expected = today;
        for (final Long day : days) {
            // A sequencia so conta dias consecutivos terminando hoje.
            if (day != null && day == expected) {
                streak = streak + 1;
                expected = expected - 1;
            } else {
                break;
            }
        }
        return streak;
    }

    @Override
    public List<DeckAccuracy> accuracyByDeck() {
        final List<DeckAccuracyRow> rows = reviewLogDao.accuracyByDeck();
        final List<DeckAccuracy> result = new ArrayList<>(rows.size());
        for (final DeckAccuracyRow row : rows) {
            result.add(new DeckAccuracy(row.deckId, row.deckName,
                    new Accuracy(row.correct, row.total)));
        }
        return result;
    }

    /**
     * Quantidade de segundos em um dia, exposta para os testes de fronteira.
     *
     * @return segundos por dia
     */
    static long secondsPerDay() {
        return SECONDS_PER_DAY;
    }
}
