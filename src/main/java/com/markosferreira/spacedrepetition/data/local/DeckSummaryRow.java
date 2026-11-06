package com.markosferreira.spacedrepetition.data.local;

import androidx.annotation.Nullable;

/**
 * Projecao de leitura da lista de baralhos (RF10).
 */
public class DeckSummaryRow {

    public String id;
    public String name;
    public long createdAt;

    @Nullable
    public Long lastStudiedAt;

    public int totalCards;
    public int dueToday;
}
