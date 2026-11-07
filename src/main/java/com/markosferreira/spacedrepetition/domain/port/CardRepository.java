package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.Card;

import java.util.List;
import java.util.Set;

/**
 * Porta de saida para persistencia de questoes.
 */
public interface CardRepository {

    /**
     * Lista as questoes de um baralho.
     *
     * @param deckId identificador do baralho
     * @return questoes do baralho
     */
    List<Card> findByDeck(String deckId);

    /**
     * Busca uma questao pelo identificador.
     *
     * @param cardId identificador
     * @return questao ou null
     */
    Card findById(String cardId);

    /**
     * Persiste um lote de questoes, aplicando upsert por {@code contentHash}.
     *
     * <p>Deve ser atomico: ou o lote inteiro entra, ou nada entra (RF08, RNF07).
     *
     * @param cards questoes a persistir
     * @return resultado da operacao por questao
     */
    UpsertResult upsertAll(List<Card> cards);

    /**
     * Identificadores de questoes que ja existem no baralho, por hash de conteudo.
     *
     * @param deckId identificador do baralho
     * @return conjunto de hashes existentes
     */
    Set<String> findHashesByDeck(String deckId);

    /**
     * Resultado de um upsert em lote.
     */
    final class UpsertResult {
        private final int inserted;
        private final int updated;

        public UpsertResult(final int inserted, final int updated) {
            this.inserted = inserted;
            this.updated = updated;
        }

        public int inserted() {
            return inserted;
        }

        public int updated() {
            return updated;
        }
    }
}
