package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidade Room de sessao de estudo.
 */
@Entity(
        tableName = "study_sessions",
        foreignKeys = @ForeignKey(
                entity = DeckEntity.class,
                parentColumns = "id",
                childColumns = "deck_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("deck_id"), @Index("status")})
public class StudySessionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "deck_id")
    public String deckId;

    @ColumnInfo(name = "mode")
    public int mode;

    @ColumnInfo(name = "status")
    public int status;

    @ColumnInfo(name = "started_at")
    public long startedAt;

    @Nullable
    @ColumnInfo(name = "finished_at")
    public Long finishedAt;

    /** Identificadores das questoes, em ordem de apresentacao, separados por virgula. */
    @NonNull
    @ColumnInfo(name = "card_ids")
    public String cardIds;

    @ColumnInfo(name = "answered_count")
    public int answeredCount;

    public StudySessionEntity(@NonNull final String id, @NonNull final String deckId,
                              final int mode, final int status, final long startedAt,
                              @Nullable final Long finishedAt, @NonNull final String cardIds,
                              final int answeredCount) {
        this.id = id;
        this.deckId = deckId;
        this.mode = mode;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.cardIds = cardIds;
        this.answeredCount = answeredCount;
    }
}
