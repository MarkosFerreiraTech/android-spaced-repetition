package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidade Room de questao.
 *
 * <p>O indice unico em {@code content_hash} e o que garante tecnicamente o
 * upsert idempotente (RF06): o banco recusa duplicata, e o repositorio usa
 * {@code REPLACE} para atualizar mantendo o agendamento.
 */
@Entity(
        tableName = "cards",
        foreignKeys = @ForeignKey(
                entity = DeckEntity.class,
                parentColumns = "id",
                childColumns = "deck_id",
                onDelete = ForeignKey.CASCADE),
        indices = {
            @Index("deck_id"),
            @Index("due_at"),
            @Index(value = "content_hash", unique = true)
        })
public class CardEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "deck_id")
    public String deckId;

    @NonNull
    @ColumnInfo(name = "question")
    public String question;

    /** Alternativas serializadas em JSON, na ordem A..E. */
    @NonNull
    @ColumnInfo(name = "options_json")
    public String optionsJson;

    @ColumnInfo(name = "correct_index")
    public int correctIndex;

    @Nullable
    @ColumnInfo(name = "explanation")
    public String explanation;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash;

    @ColumnInfo(name = "stability")
    public double stability;

    @ColumnInfo(name = "difficulty")
    public double difficulty;

    @Nullable
    @ColumnInfo(name = "due_at")
    public Long dueAt;

    @Nullable
    @ColumnInfo(name = "last_review_at")
    public Long lastReviewAt;

    @ColumnInfo(name = "reps")
    public int repetitions;

    @ColumnInfo(name = "lapses")
    public int lapses;

    /** Ordinal de {@code CardState}. */
    @ColumnInfo(name = "state")
    public int state;

    public CardEntity(@NonNull final String id, @NonNull final String deckId,
                      @NonNull final String question, @NonNull final String optionsJson,
                      final int correctIndex, @Nullable final String explanation,
                      @NonNull final String contentHash, final double stability,
                      final double difficulty, @Nullable final Long dueAt,
                      @Nullable final Long lastReviewAt, final int repetitions,
                      final int lapses, final int state) {
        this.id = id;
        this.deckId = deckId;
        this.question = question;
        this.optionsJson = optionsJson;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        this.contentHash = contentHash;
        this.stability = stability;
        this.difficulty = difficulty;
        this.dueAt = dueAt;
        this.lastReviewAt = lastReviewAt;
        this.repetitions = repetitions;
        this.lapses = lapses;
        this.state = state;
    }
}
