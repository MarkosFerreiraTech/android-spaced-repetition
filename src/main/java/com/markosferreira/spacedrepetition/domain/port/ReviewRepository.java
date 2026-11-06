package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.ReviewLog;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

import java.util.List;

/**
 * Porta de saida para o historico de revisoes.
 */
public interface ReviewRepository {

    /**
     * Grava o log e atualiza o agendamento da questao em uma unica transacao.
     *
     * <p>Atomicidade e obrigatoria: um log sem atualizacao de agendamento
     * corromperia a estatistica e o algoritmo (UC04/E3).
     *
     * @param log registro imutavel da resposta
     * @param nextScheduling novo estado de agendamento
     */
    void record(ReviewLog log, SchedulingInfo nextScheduling);

    /**
     * Questoes erradas na ultima tentativa de um baralho (modo WRONG_ONLY).
     *
     * @param deckId identificador do baralho
     * @return questoes erradas
     */
    List<Card> findWrongByDeck(String deckId);

    /**
     * Questoes erradas ou marcadas com duvida na ultima tentativa
     * (modo WRONG_PLUS_FLAGGED).
     *
     * @param deckId identificador do baralho
     * @return questoes erradas ou marcadas
     */
    List<Card> findWrongOrFlaggedByDeck(String deckId);
}
