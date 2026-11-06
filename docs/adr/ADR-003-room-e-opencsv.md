# ADR-003 — Room + OpenCSV para persistência e importação

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (Tech Lead)
- **Issue:** #10, #11

## Contexto

O app precisa (a) persistir decks, cartões, logs de revisão e sessões localmente e (b) ler planilhas CSV fornecidas pelo usuário, com aspas, separadores variados e acentuação.

## Decisão

- **Persistência:** **Room** sobre SQLite.
- **Importação:** **OpenCSV** para parsing.

## Justificativa — Room

| Alternativa | Veredito |
|---|---|
| SQLite bruto (`SQLiteOpenHelper`) | Rejeitado: SQL manual, sem verificação de queries em compilação |
| Realm | Rejeitado: dependência pesada, licença e modelo de threading próprios |
| ObjectBox | Rejeitado: dependência binária nativa, menos auditável no CI |
| Armazenar em JSON | Rejeitado: sem índices, agregação de estatísticas inviável |
| **Room** | **Aceito:** queries verificadas em compilação, migrações versionadas, integração de primeira classe com `LiveData`, teste em memória |

Room também fornece `exportSchema`, o que permite **versionar o schema no Git** (RNF06) e testar migrações com `MigrationTestHelper`.

## Justificativa — OpenCSV

| Alternativa | Veredito |
|---|---|
| `String.split(",")` | Rejeitado: quebra com aspas, campos multilinha e separador `;` |
| `android.util.JsonReader` | Rejeitado: não lê CSV |
| Parser próprio | Rejeitado: risco de bug em RFC 4180 sem ganho |
| **OpenCSV** | **Aceito:** maduro, API simples, suporta `CSVReaderBuilder` com separador configurável e `BeanToCsv` |

## Consequências

- O parser vive atrás da porta `CsvParser` no domínio, com implementação OpenCSV na camada de dados. Isso permite trocar de biblioteca sem afetar casos de uso e mantém o domínio testável com fixtures de texto.
- Room impõe entidades anotadas; por isso existem **entidades de dados separadas** dos **modelos de domínio**, com mappers dedicados. É custo de boilerplate aceito conscientemente em troca de isolamento.
