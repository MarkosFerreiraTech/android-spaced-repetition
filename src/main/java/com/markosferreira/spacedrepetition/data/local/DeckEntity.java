package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room de baralho.
 *
 * <p>Separada do modelo de dominio {@code Deck} de proposito: o schema pode
 * evoluir sem obrigar o dominio a mudar (ADR-003).
 */
@Entity(tableName = "decks")
public class DeckEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @Nullable
    @ColumnInfo(name = "last_studied_at")
    public Long lastStudiedAt;

    public DeckEntity(@NonNull final String id, @NonNull final String name,
                      final long createdAt, @Nullable final Long lastStudiedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastStudiedAt = lastStudiedAt;
    }
}
