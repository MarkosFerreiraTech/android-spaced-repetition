# ADR-001 — Adotar FSRS em vez de SM-2

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (PO), Markos Ferreira (Tech Lead)
- **Issue:** #5, #8

## Contexto

O produto precisa decidir *quando* cada questão deve reaparecer. Duas famílias de algoritmos são candidatas:

1. **SM-2** (SuperMemo 2, 1987) — o algoritmo do Anki clássico. Estado por cartão: `ease_factor`, `interval`, `repetitions`. A qualidade da resposta (0–5) multiplica o intervalo.
2. **FSRS** (Free Spaced Repetition Scheduler) — modelo de três componentes: **estabilidade** (dias até a retenção cair a 90%), **dificuldade** (intrínseca do cartão) e **retrievabilidade** (probabilidade de lembrar agora, decaindo com o tempo). Os parâmetros do modelo são ajustáveis e conhecidos publicamente.

O usuário final é alguém se preparando para uma prova com data marcada, onde subestimar o esquecimento custa caro.

## Decisão

Adotar **FSRS**, implementado do zero em Java puro no pacote `domain.fsrs`, com os parâmetros default da especificação pública.

## Justificativa

| Critério | SM-2 | FSRS |
|---|---|---|
| Modelo de esquecimento | Heurístico, sem curva explícita | Curva de esquecimento explícita (power-law) |
| Uso de histórico de revisão | Não | Sim (estabilidade e dificuldade evoluem) |
| Ajuste a desempenho real | Não | Sim, via parâmetros |
| Adequação a prova com data | Fraca | Boa (permite comprimir intervalos) |
| Complexidade de implementação | Baixa | Média, porém **contida** em uma classe |
| Testabilidade | Alta | Alta, porque o algoritmo é função pura de estado |

O ponto decisivo é o **determinismo**: `schedule(estado, rating, agora) → novo estado` é uma função pura sem aleatoriedade. Isso torna o FSRS **mais fácil de testar** do que a alternativa "realista" de amostragem estocástica, e permite testes de propriedade (ex.: `Again` nunca aumenta o intervalo).

## Consequências

**Positivas**
- Agendamento alinhado ao estado da arte, coerente com o objetivo do produto.
- Algoritmo isolado em uma única porta (`ReviewScheduler`), o que permite trocar de algoritmo sem tocar em UI, banco ou casos de uso.
- Parâmetros expostos permitem futura personalização (v2).

**Negativas**
- Mais campos no schema (`stability`, `difficulty`, `state`, `lapses`) do que o SM-2 exigiria.
- Risco de implementação incorreta das fórmulas (R1 do charter) — mitigado por testes de caso-limite e de propriedade.
- Sem dados históricos do usuário, os parâmetros são os default, não personalizados.

## Alternativas consideradas

- **SM-2 clássico:** rejeitado por não modelar a curva de esquecimento e por subestimar revisões em conteúdo difícil.
- **SM-2 simplificado (4 botões):** rejeitado porque a simplificação não traz ganho de simplicidade relevante e mantém a limitação de modelagem.
- **FSRS com treinamento de parâmetros no dispositivo:** adiado para v2; exigiria volume mínimo de histórico por usuário.
