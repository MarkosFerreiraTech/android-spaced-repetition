# ADR-004 — Usar ExecutorService em vez de WorkManager para a importação

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (Tech Lead)
- **Issue:** #11

## Contexto

A importação de CSV pode levar segundos e precisa ocorrer fora da thread principal. As opções usuais são `ExecutorService` (thread de background gerenciada pelo app) e `WorkManager` (execução diferida e persistente, gerenciada pelo sistema).

## Decisão

Usar **`ExecutorService`** (`newSingleThreadExecutor`) com thread nomeada `csv-import`, injetado via construtor na camada de dados.

## Justificativa

1. **A importação é interativa, não diferível.** O usuário está olhando para a tela e esperando o resultado; `WorkManager` é projetado para trabalho que sobrevive ao app e pode rodar em qualquer momento.
2. **Requisito explícito do enunciado:** "o app fará a leitura em background (usando `ExecutorService`)".
3. **Testabilidade:** injetar o executor permite substituir por um executor direto nos testes, eliminando flakiness.
4. **Custo:** `WorkManager` adiciona `androidx.work`, restrições de execução e um modelo assíncrono baseado em `ListenableWorker` que não traz benefício no fluxo atual.

## Consequências

**Positivas**
- Feedback imediato de progresso, com controle fino de cancelamento (`shutdownNow`).
- Nenhuma dependência adicional no grafo de build.

**Negativas**
- Se o processo for morto durante a importação, o trabalho é interrompido. Mitigado por: transação por lote de 500 (nada fica parcial), marcação do baralho como `incompleto` e oferta de reimportação (UC01/E7).
- O app é responsável pela gestão do ciclo de vida do executor; `shutdown()` no `onCleared` do ViewModel e no `Application.onTerminate`.

**Revisão futura:** quando notificações de revisão (fora do escopo atual) forem implementadas, `WorkManager` passa a ser a escolha correta para o agendamento periódico.
