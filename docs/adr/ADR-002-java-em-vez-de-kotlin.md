# ADR-002 — Implementar em Java, não em Kotlin

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (PO)
- **Issue:** #5, #7

## Contexto

O padrão atual de mercado para Android é Kotlin. Porém o escopo acadêmico exige **Java** explicitamente, e o autor tem maior fluência em Java.

## Decisão

Implementar toda a aplicação em **Java 17**, com `sourceCompatibility` e `targetCompatibility` fixados em 17.

## Justificativa

1. **Requisito explícito** do trabalho: "MVP simples de um aplicativo mobile nativo (Android/Java)".
2. Fluência do autor reduz retrabalho em um prazo de uma semana.
3. Java com `ExecutorService`, `LiveData` e ViewBinding atende integralmente aos requisitos funcionais.

## Consequências

**Positivas**
- Curva de aprendizado zero para o autor.
- Interoperabilidade total com bibliotecas Android, que expõem API Java.

**Negativas**
- Ausência de corrotinas obriga a um modelo explícito de threading (`ExecutorService` injetado) e de cancelamento.
- Ausência de `data class`, `sealed` e `when` exaustivo reduz a expressividade de modelos e `UiState`; mitigado com classes imutáveis geradas manualmente e enums.
- Sem *null safety* nativa; mitigado com `@NonNull`/`@Nullable` e NullAway/SpotBugs no gate.

**Mitigação de threading:** todo trabalho assíncrono passa por um `Executor` injetado na camada de dados, o que também permite substituir por um executor síncrono nos testes (`DirectExecutor`), tornando os testes determinísticos.
