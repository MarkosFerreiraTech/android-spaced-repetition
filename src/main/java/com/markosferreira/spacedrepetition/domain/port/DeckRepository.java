package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.DeckSummary;

import java.time.Instant;
import java.util.List;

/**
 * Porta de saida para persistencia de baralhos.
 */
public interface DeckRepository {

    /**
     * Lista os baralhos com contadores agregados.
     *
     * @param now instante de referencia para o calculo de pendencias
     * @return baralhos ordenados pela ultima atividade
     */
    List<DeckSummary> findAllWithCounts(Instant now);

    /**
     * Busca um baralho pelo nome exato.
     *
     * <p>Usado na importacao para que reimportar a mesma planilha caia no mesmo
     * baralho — pre-requisito para o upsert idempotente por content hash (RF06).
     *
     * @param name nome do baralho
     * @return baralho ou null se nao existir
     */
    Deck findByName(String name);

    /**
     * Busca um baralho pelo identificador.
     *
     * @param deckId identificador
     * @return baralho ou null se nao existir
     */
    Deck findById(String deckId);

    /**
     * Persiste um baralho (insercao ou atualizacao).
     *
     * @param deck baralho a persistir
     */
    void save(Deck deck);
}
