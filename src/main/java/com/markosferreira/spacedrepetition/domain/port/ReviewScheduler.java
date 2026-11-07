package com.markosferreira.spacedrepetition.domain.port;

import com.markosferreira.spacedrepetition.domain.model.Rating;
import com.markosferreira.spacedrepetition.domain.model.SchedulingInfo;

import java.time.Instant;

/**
 * Porta de saida do dominio para o algoritmo de repeticao espacada.
 *
 * <p>Existe para que o caso de uso nunca dependa de uma implementacao concreta
 * (Dependency Inversion). Trocar FSRS por SM-2, ou por um dublê de teste, e uma
 * questao de injecao de dependencia — nenhuma linha de caso de uso muda.
 */
public interface ReviewScheduler {

    /**
     * Calcula o proximo agendamento de uma questao.
     *
     * <p>Funcao pura: dado o mesmo estado, o mesmo rating e o mesmo instante, o
     * resultado e sempre identico. Nao usa aleatoriedade.
     *
     * @param current estado atual do agendamento
     * @param rating avaliacao da resposta
     * @param now instante da resposta
     * @return novo estado de agendamento
     */
    SchedulingInfo schedule(SchedulingInfo current, Rating rating, Instant now);
}
