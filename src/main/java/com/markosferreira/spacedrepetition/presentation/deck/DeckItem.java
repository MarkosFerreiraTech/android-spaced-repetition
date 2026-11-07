package com.markosferreira.spacedrepetition.presentation.deck;

import java.util.Objects;

/**
 * Item exibido na lista de baralhos.
 *
 * <p>E um modelo de apresentacao, nao de dominio: ja traz os textos formatados
 * que a tela mostra, o que mantem o layout burro e o ViewModel testavel.
 */
public final class DeckItem {

    private final String deckId;
    private final String name;
    private final int totalCards;
    private final int dueToday;

    /**
     * Cria o item.
     *
     * @param deckId identificador do baralho
     * @param name nome do baralho
     * @param totalCards total de questoes
     * @param dueToday questoes pendentes hoje
     */
    public DeckItem(final String deckId, final String name,
                    final int totalCards, final int dueToday) {
        this.deckId = deckId;
        this.name = name;
        this.totalCards = totalCards;
        this.dueToday = dueToday;
    }

    /**
     * Identificador do baralho.
     *
     * @return id
     */
    public String deckId() {
        return deckId;
    }

    /**
     * Nome do baralho.
     *
     * @return nome
     */
    public String name() {
        return name;
    }

    /**
     * Total de questoes do baralho.
     *
     * @return quantidade
     */
    public int totalCards() {
        return totalCards;
    }

    /**
     * Questoes pendentes hoje.
     *
     * @return quantidade
     */
    public int dueToday() {
        return dueToday;
    }

    /**
     * Texto secundario do item, no plural correto.
     *
     * @return descricao legivel
     */
    public String subtitle() {
        final String questoes = totalCards == 1 ? "1 questao" : totalCards + " questoes";
        if (dueToday == 0) {
            return questoes + " · em dia";
        }
        final String pendentes = dueToday == 1 ? "1 pendente" : dueToday + " pendentes";
        return questoes + " · " + pendentes;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DeckItem)) {
            return false;
        }
        final DeckItem that = (DeckItem) other;
        return totalCards == that.totalCards
                && dueToday == that.dueToday
                && Objects.equals(deckId, that.deckId)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckId, name, totalCards, dueToday);
    }

    @Override
    public String toString() {
        return "DeckItem{" + name + ", total=" + totalCards + ", due=" + dueToday + '}';
    }
}
