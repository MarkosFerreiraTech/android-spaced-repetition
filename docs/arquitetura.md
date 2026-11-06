# Arquitetura do Sistema

| Campo | Valor |
|---|---|
| Versão | 1.0 |
| Issue de origem | [#5](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues/5) |
| ADRs | [adr/](./adr/) |
| Última revisão | 2026-11-06 |

---

## 1. Visão Geral

O **Cerebro** é um aplicativo **Android nativo em Java**, **offline-first**, organizado segundo **Clean Architecture** com o padrão de apresentação **MVVM**. Toda a inteligência de agendamento vive na **camada de domínio**, que é Java puro e não conhece Android.

O princípio estrutural é a **Regra de Dependência**: as setas apontam sempre para dentro. Domínio não importa nada de dados, apresentação ou framework. Isso é verificado automaticamente por testes de ArchUnit, não apenas por convenção.

## 2. C4 — Nível 1: Contexto

```mermaid
C4Context
  title Cerebro — Diagrama de Contexto (C4 Nível 1)

  Person(estudante, "Estudante de certificação AWS", "Pessoa que importa bancos de questões e realiza revisões espaçadas")

  System(cerebro, "Cerebro", "App Android nativo que agenda e executa revisões espaçadas de questões de simulado, 100% offline")

  System_Ext(saf, "Storage Access Framework", "Provedor de documentos do Android, de onde o CSV é lido")
  System_Ext(keystore, "Android Keystore", "Armazena a chave mestra para cifra das preferências")

  Rel(estudante, cerebro, "Importa CSV, estuda, consulta progresso", "Toque")
  Rel(cerebro, saf, "Solicita abertura de arquivo", "Intent / ContentResolver")
  Rel(cerebro, keystore, "Solicita chave AES-256-GCM", "AndroidKeyStore")

  UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

**Observação deliberada:** não existe nenhum sistema externo de backend, autenticação ou telemetria. A ausência é um requisito (RNF05), não uma omissão.

## 3. C4 — Nível 2: Containers

```mermaid
C4Container
  title Cerebro — Diagrama de Containers (C4 Nível 2)

  Person(estudante, "Estudante")

  Container_Boundary(app, "Aplicativo Android (APK)") {
    Container(ui, "Camada de Apresentação", "Activities, Fragments, ViewModels, XML + ViewBinding", "Exibe telas, traduz eventos em intenções e mantém UiState imutável")
    Container(domain, "Camada de Domínio", "Java puro", "Modelos, casos de uso, FSRS. Sem Android, sem Room, sem UI")
    Container(data, "Camada de Dados", "Room, OpenCSV, EncryptedSharedPreferences", "Repositórios, DAOs, parser CSV, preferências cifradas, mappers")
    ContainerDb(room, "Banco local", "SQLite / Room", "Decks, Cards, ReviewLogs, StudySessions")
    ContainerDb(prefs, "Preferências cifradas", "EncryptedSharedPreferences", "Limite diário, fuso, métricas agregadas")
  }

  System_Ext(saf, "SAF")
  System_Ext(keystore, "Android Keystore")

  Rel(estudante, ui, "Interage", "Touch")
  Rel(ui, domain, "Invoca casos de uso", "Chamada Java")
  Rel(data, domain, "Implementa portas de repositório", "Interface")
  Rel(domain, data, "Depende de abstrações", "Inversão de dependência")
  Rel(data, room, "Lê e escreve", "JDBC/SQLite")
  Rel(data, prefs, "Lê e escreve cifrado", "AES-256-GCM")
  Rel(data, saf, "Abre stream do CSV", "ContentResolver")
  Rel(prefs, keystore, "Obtém chave", "Keystore")
```

## 4. Camadas e Regra de Dependência

```mermaid
flowchart TB
  subgraph P["presentation"]
    direction TB
    A1[Activity / Fragment] --> A2[ViewModel]
  end
  subgraph D["domain — Java puro"]
    direction TB
    B1[Modelos: Card, Deck, ReviewLog]
    B2[Portas: ReviewScheduler, CardRepository]
    B3[Casos de uso: ImportDeck, StartSession, AnswerQuestion, GetStatistics]
  end
  subgraph DA["data"]
    direction TB
    C1[Repositórios impl.]
    C2[DAOs Room + Entidades]
    C3[CsvParser OpenCSV]
    C4[SecurePreferences]
    C5[Mappers Entity ↔ Domain]
  end

  A2 --> B3
  C1 -.->|implementa| B2
  C5 --> B1
  B3 --> B1
  B3 --> B2

  classDef inner fill:#dcfce7,stroke:#16a34a,stroke-width:2px
  classDef outer fill:#e0f2fe,stroke:#0284c7
  class B1,B2,B3 inner
  class A1,A2,C1,C2,C3,C4,C5 outer
```

**Regras verificadas por ArchUnit:**

| Regra | Teste |
|---|---|
| `domain` não importa `android.*` | `ArchitectureTest.domainHasNoAndroidDependency` |
| `domain` não importa `androidx.room.*` | `ArchitectureTest.domainIsFrameworkFree` |
| `presentation` não importa `data` | `ArchitectureTest.presentationDoesNotDependOnData` |
| Nenhuma classe de domínio tem `@Entity` | `ArchitectureTest.domainModelsAreNotEntities` |
| Nenhum ViewModel instancia repositório | `ArchitectureTest.viewModelsReceiveCollaborators` |

## 5. Diagrama de Classes do Domínio

```mermaid
classDiagram
  direction RL

  class Card {
    +String id
    +String deckId
    +String question
    +List~String~ options
    +int correctIndex
    +String explanation
    +String contentHash
    +SchedulingInfo scheduling
  }

  class SchedulingInfo {
    +double stability
    +double difficulty
    +Instant dueAt
    +Instant lastReviewAt
    +int reps
    +int lapses
    +CardState state
  }

  class Deck {
    +String id
    +String name
    +Instant createdAt
    +Instant lastStudiedAt
  }

  class ReviewLog {
    +String id
    +String cardId
    +String sessionId
    +Rating rating
    +boolean correct
    +boolean flagged
    +long elapsedMillis
    +Instant reviewedAt
    +double stabilityAfter
    +double difficultyAfter
  }

  class StudySession {
    +String id
    +String deckId
    +StudyMode mode
    +SessionStatus status
    +Instant startedAt
    +Instant finishedAt
    +List~String~ cardIds
  }

  class Rating {
    <<enumeration>>
    AGAIN
    HARD
    GOOD
    EASY
  }

  class CardState {
    <<enumeration>>
    NEW
    LEARNING
    REVIEW
    RELEARNING
  }

  class StudyMode {
    <<enumeration>>
    FULL
    WRONG_ONLY
    WRONG_PLUS_FLAGGED
  }

  class ReviewScheduler {
    <<interface>>
    +SchedulingInfo schedule(SchedulingInfo current, Rating rating, Instant now)
    +boolean isDue(SchedulingInfo info, Instant now)
  }

  class FsrsScheduler {
    -FsrsParameters params
    +SchedulingInfo schedule(...)
    +boolean isDue(...)
  }

  class CardRepository {
    <<interface>>
    +List~Card~ findByDeck(String deckId)
    +void upsertAll(List~Card~ cards)
    +Card findById(String id)
  }

  class DeckRepository {
    <<interface>>
    +List~DeckSummary~ findAllWithCounts(Instant now)
    +Deck create(String name)
    +void delete(String id)
  }

  class ReviewRepository {
    <<interface>>
    +void record(ReviewLog log, SchedulingInfo next)
    +List~Card~ findWrongByDeck(String deckId)
    +List~Card~ findFlaggedByDeck(String deckId)
  }

  Card "1" *-- "1" SchedulingInfo
  Deck "1" o-- "many" Card
  Card "1" o-- "many" ReviewLog
  StudySession "1" o-- "many" ReviewLog
  ReviewLog --> Rating
  SchedulingInfo --> CardState
  StudySession --> StudyMode
  FsrsScheduler ..|> ReviewScheduler
```

## 6. Modelo Entidade-Relacionamento (Room)

```mermaid
erDiagram
  DECKS ||--o{ CARDS : contains
  CARDS ||--o{ REVIEW_LOGS : generates
  DECKS ||--o{ STUDY_SESSIONS : groups
  STUDY_SESSIONS ||--o{ STUDY_SESSION_ITEMS : has
  CARDS ||--o{ STUDY_SESSION_ITEMS : appears_in

  DECKS {
    TEXT id PK
    TEXT name
    INTEGER created_at
    INTEGER last_studied_at
  }

  CARDS {
    TEXT id PK
    TEXT deck_id FK
    TEXT question
    TEXT options_json
    INTEGER correct_index
    TEXT explanation
    TEXT content_hash UK
    REAL stability
    REAL difficulty
    INTEGER due_at
    INTEGER last_review_at
    INTEGER reps
    INTEGER lapses
    INTEGER state
  }

  REVIEW_LOGS {
    TEXT id PK
    TEXT card_id FK
    TEXT session_id FK
    INTEGER rating
    INTEGER correct
    INTEGER flagged
    INTEGER elapsed_millis
    INTEGER reviewed_at
    REAL stability_after
    REAL difficulty_after
  }

  STUDY_SESSIONS {
    TEXT id PK
    TEXT deck_id FK
    INTEGER mode
    INTEGER status
    INTEGER started_at
    INTEGER finished_at
  }

  STUDY_SESSION_ITEMS {
    TEXT id PK
    TEXT session_id FK
    TEXT card_id FK
    INTEGER position
    INTEGER answered_at
    INTEGER selected_index
    INTEGER correct
    INTEGER flagged
  }
```

**Índices:** `CARDS(deck_id)`, `CARDS(due_at)`, `CARDS(content_hash)` UNIQUE, `REVIEW_LOGS(card_id)`, `REVIEW_LOGS(reviewed_at)`, `STUDY_SESSION_ITEMS(session_id, position)` UNIQUE.

## 7. Fluxo de Dados — Importação

```mermaid
sequenceDiagram
  autonumber
  actor E as Estudante
  participant A as ImportActivity
  participant VM as ImportViewModel
  participant UC as ImportDeckUseCase
  participant P as CsvParser
  participant EX as ExecutorService
  participant R as CardRepository
  participant DB as Room

  E->>A: toca "Selecionar CSV"
  A->>A: ActivityResultContracts.OpenDocument
  A->>VM: onFileSelected(uri)
  VM->>UC: execute(inputStream, deckName)
  UC->>EX: submit(worker)
  Note over EX: Parsing fora da main thread (RNF01)
  EX->>P: parse(reader)
  P-->>EX: Fluxo de linhas válidas + erros
  loop a cada 500 linhas
    EX->>R: upsertAll(batch)
    R->>DB: transação com REPLACE por content_hash
    EX-->>VM: onProgress(n / total)
    VM-->>A: LiveData<UiState>
  end
  EX-->>VM: ImportReport(imported, updated, skipped, errors)
  VM-->>A: Success(report)
  A->>E: Snackbar/Ecrã de relatório
```

## 8. Fluxo de Dados — Estudo e Agendamento

```mermaid
sequenceDiagram
  autonumber
  actor E as Estudante
  participant A as StudyActivity
  participant VM as StudyViewModel
  participant UC as AnswerQuestionUseCase
  participant SCH as ReviewScheduler
  participant RR as ReviewRepository
  participant DB as Room

  E->>A: seleciona alternativa + confirma
  A->>VM: answer(selectedIndex, flagged, elapsed)
  VM->>UC: execute(card, answer)
  UC->>UC: deriveRating(correct, flagged, elapsed)
  UC->>SCH: schedule(current, rating, now)
  SCH-->>UC: SchedulingInfo (stability, difficulty, dueAt)
  UC->>RR: record(reviewLog, nextScheduling)
  RR->>DB: transação: INSERT log + UPDATE card
  UC-->>VM: AnswerResult(correct, correctIndex, explanation)
  VM-->>A: Feedback
  A->>E: correto/incorreto + próxima questão
```

## 9. Arquitetura de Testes

| Nível | Ferramenta | Alvo | Execução |
|---|---|---|---|
| Unitário rápido | JUnit 5 + AssertJ + Mockito | Domínio, FSRS, casos de uso, ViewModels | `./gradlew test` (JVM, sem Android) |
| ViewModel | Robolectric + InstantTaskExecutorRule | `LiveData`, ciclo de vida | `./gradlew testDebugUnitTest` |
| Contrato de dados | Room in-memory | DAOs, mappers, migrações | `./gradlew connectedDebugAndroidTest` |
| UI | Espresso | Fluxos de importação, estudo, estatísticas | Instrumentado |
| Arquitetura | ArchUnit | Regra de dependência | `./gradlew test` |
| Cobertura | JaCoCo | Global ≥ 80%, domínio ≥ 95% | Job de CI |

## 10. Decisões Arquiteturais (ADRs)

| ADR | Decisão | Status |
|---|---|---|
| [ADR-001](./adr/ADR-001-fsrs-em-vez-de-sm2.md) | FSRS em vez de SM-2 | Aceito |
| [ADR-002](./adr/ADR-002-java-em-vez-de-kotlin.md) | Java em vez de Kotlin | Aceito |
| [ADR-003](./adr/ADR-003-room-e-opencsv.md) | Room + OpenCSV para persistência e importação | Aceito |
| [ADR-004](./adr/ADR-004-executorservice-em-vez-de-workmanager.md) | ExecutorService em vez de WorkManager | Aceito |
| [ADR-005](./adr/ADR-005-classificacao-de-dados-e-criptografia.md) | Classificação de dados e escopo da criptografia | Aceito com risco |
| [ADR-006](./adr/ADR-006-arquitetura-em-camadas-fisicas.md) | Camadas como pacotes, não módulos Gradle | Aceito com risco |
