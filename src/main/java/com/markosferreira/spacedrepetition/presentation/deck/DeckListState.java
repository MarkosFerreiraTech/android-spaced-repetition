package com.markosferreira.spacedrepetition.presentation.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado da tela de baralhos.
 *
 * <p>Modela os quatro estados possiveis como um objeto unico, em vez de tres
 * {@code LiveData} soltos ({@code isLoading}, {@code items}, {@code error}).
 * Assim e impossivel representar combinacoes invalidas — como "carregando" e
 * "com erro" ao mesmo tempo.
 */
public final class DeckListState {

    /** Situacao da tela. */
    public enum Status {
        /** Carregando os dados. */
        LOADING,
        /** Ha baralhos para exibir. */
        LOADED,
        /** Nenhum baralho: exibe a instrucao de importacao (RF11). */
        EMPTY,
        /** Falha ao carregar. */
        ERROR
    }

    private static final DeckListState LOADING = new DeckListState(Status.LOADING, List.of(), null);
    private static final DeckListState EMPTY = new DeckListState(Status.EMPTY, List.of(), null);

    private final Status status;
    private final List<DeckItem> items;
    private final String message;

    private DeckListState(final Status status, final List<DeckItem> items, final String message) {
        this.status = status;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.message = message;
    }

    /**
     * Estado de carregamento.
     *
     * @return estado
     */
    public static DeckListState loading() {
        return LOADING;
    }

    /**
     * Estado vazio (sem baralhos).
     *
     * @return estado
     */
    public static DeckListState empty() {
        return EMPTY;
    }

    /**
     * Estado com baralhos.
     *
     * @param items itens a exibir
     * @return estado
     */
    public static DeckListState loaded(final List<DeckItem> items) {
        return new DeckListState(Status.LOADED, items, null);
    }

    /**
     * Estado de falha.
     *
     * @param message mensagem exibida ao usuario
     * @return estado
     */
    public static DeckListState failed(final String message) {
        return new DeckListState(Status.ERROR, List.of(), message);
    }

    /**
     * Situacao atual.
     *
     * @return status
     */
    public Status status() {
        return status;
    }

    /**
     * Itens, vazios quando nao ha baralhos.
     *
     * @return lista imutavel
     */
    public List<DeckItem> items() {
        return items;
    }

    /**
     * Mensagem de erro, quando houver.
     *
     * @return mensagem ou null
     */
    public String message() {
        return message;
    }

    /**
     * Indica se a tela deve exibir a instrucao de importacao.
     *
     * @return verdadeiro no estado vazio
     */
    public boolean showsEmptyState() {
        return status == Status.EMPTY;
    }

    @Override
    public String toString() {
        return "DeckListState{" + status + ", items=" + items.size() + '}';
    }
}
