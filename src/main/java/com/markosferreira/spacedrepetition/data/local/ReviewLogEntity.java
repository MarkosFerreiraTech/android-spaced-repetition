package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidade Room do historico de revisoes.
 *
 * <p>Apenas insercao; nunca atualizacao. E a fonte de verdade das estatisticas.
 */
@Entity(
        tableName = "review_logs",
        foreignKeys = @ForeignKey(
                entity = CardEntity.class,
                parentColumns = "id",
                childColumns = "card_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("card_id"), @Index("reviewed_at"), @Index("session_id")})
public class ReviewLogEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "card_id")
    public String cardId;

    @Nullable
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "rating")
    public int rating;

    @ColumnInfo(name = "correct")
    public boolean correct;

    @ColumnInfo(name = "flagged")
    public boolean flagged;

    @ColumnInfo(name = "elapsed_millis")
    public long elapsedMillis;

    @ColumnInfo(name = "reviewed_at")
    public long reviewedAt;

    @ColumnInfo(name = "stability_after")
    public double stabilityAfter;

    @ColumnInfo(name = "difficulty_after")
    public double difficultyAfter;

    /** Construtor vazio exigido pelo Room. As colunas sao atribuidas por campo. */
    public ReviewLogEntity() {
        this.id = "";
        this.cardId = "";
    }
}
