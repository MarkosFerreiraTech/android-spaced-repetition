# ADR-006 — Camadas como pacotes, não como módulos Gradle

- **Status:** Aceito com risco documentado
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (Tech Lead)
- **Issue:** #5, #10

## Contexto

Clean Architecture pode ser imposta fisicamente (módulos Gradle separados: `:app`, `:domain`, `:data`) ou logicamente (pacotes dentro de um único módulo).

## Decisão

Implementar **camadas como pacotes** no módulo `app`, com a regra de dependência garantida por **testes de ArchUnit** no CI.

## Justificativa

- **Prazo:** múltiplos módulos exigem configuração de build, publicação local e ajustes de variantes — esforço que não agrega nota nem valor funcional nesta semana.
- **Build mais rápido** um único módulo compila em menos tempo no runner do CI.
- **A regra de dependência ainda é verificada**, e de forma mais explícita do que a convenção social: um teste falha se `domain` importar `android.*`.

## Consequências

**Positivas**
- Setup simples, build rápido.
- A garantia arquitetural é executável (`./gradlew test`), não apenas documentada.

**Negativas / Risco aceito**
- Nada impede tecnicamente um import indevido em tempo de escrita — apenas o CI impede o merge. Dívida documentada: migrar para módulos `:domain` e `:data` na v1.1, quando a base de código justificar.

## Alternativas consideradas

- **Módulos Gradle separados:** correto a longo prazo, mas custo desproporcional ao escopo.
- **Sem verificação automatizada:** rejeitado — a regra de dependência viraria folclore.
