package com.markosferreira.spacedrepetition.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Banco local do aplicativo.
 *
 * <p>{@code exportSchema = true} e {@code schemas/} versionado: o schema faz parte
 * do contrato de dados e migracoes sao testadas (RNF06).
 */
@Database(
        entities = {
            DeckEntity.class,
            CardEntity.class,
            ReviewLogEntity.class,
            StudySessionEntity.class
        },
        version = 1,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * DAO de baralhos.
     *
     * @return dao
     */
    public abstract DeckDao deckDao();

    /**
     * DAO de questoes.
     *
     * @return dao
     */
    public abstract CardDao cardDao();

    /**
     * DAO de revisoes.
     *
     * @return dao
     */
    public abstract ReviewLogDao reviewLogDao();

    /**
     * DAO de sessoes.
     *
     * @return dao
     */
    public abstract StudySessionDao studySessionDao();

    /** Nome do arquivo do banco. */
    public static final String NAME = "cerebro.db";
}
