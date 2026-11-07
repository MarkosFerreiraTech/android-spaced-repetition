# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o
projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).
A convenção de commits está em [`docs/VERSIONAMENTO.md`](docs/VERSIONAMENTO.md).

## [Não publicado]

### Adicionado

- Telas de importação, listagem de baralhos, sessão de estudo e estatísticas (#12, #13, #14)
- Testes ArchUnit cobrindo as regras de dependência entre camadas (#16)

### Segurança

- Atualização do `assertj-core` de 3.26.3 para 3.27.7, corrigindo vulnerabilidade de XXE

## [0.2.0] — 2026-11-07

### Adicionado

- Parser de CSV sobre OpenCSV com leitura RFC 4180, validação de cabeçalho conforme
  o contrato (RF04) e validação por linha que não interrompe a importação (RF05) (#11)
- Seis adaptadores Room implementando as portas de saída da camada de domínio:
  baralhos, questões, revisões, sessões e estatísticas (#10)
- Preferências cifradas com `EncryptedSharedPreferences` e `MasterKey` AES256-GCM no
  Android Keystore, com degradação segura (#15)
- Container de injeção de dependência manual (`AppContainer`) e `CerebroApplication`
  como composition root
- `ImportExecutor`: pool serial com `Future` para leitura e persistência em background
- Testes de fixtures com planilhas versionadas em `src/test/resources/fixtures`
- Workflow do Qodana como análise complementar não bloqueante
- `docs/VERSIONAMENTO.md`, `CHANGELOG.md` e hook de validação de mensagem de commit

### Corrigido

- **`CardDao` apagaria todo o histórico de revisões ao reimportar um CSV.** O método
  usava `@Insert(onConflict = REPLACE)`, que no SQLite executa `DELETE` seguido de
  `INSERT`; como `review_logs` referencia `cards` com `ON DELETE CASCADE`, a
  reimportação destruía o histórico. Trocado por `@Upsert`
- `DeckDao` não possuía `findByName`, exigido pelo caso de uso de importação para que
  a reimportação caia no mesmo baralho
- O `excludeFilter` do SpotBugs nunca era aplicado: o `config/spotbugs-exclude.xml`
  existia mas era ignorado pela tarefa `spotbugsMain`
- Tarefas `checkstyleMain` e `spotbugsMain` não eram criadas, porque os plugins
  geram tarefas por source set e o código Android vive em `src/main/java` na raiz
- Builders de `Card` e `StudySession` retinham referência a listas mutáveis
  (`EI_EXPOSE_REP2`), corrigido com cópia defensiva

### Removido

- `CardDao.averageElapsedMillis`, consulta sem nenhum consumidor

## [0.1.0] — 2026-11-07

### Adicionado

- Especificação de requisitos funcionais e não funcionais com matriz de
  rastreabilidade
- Casos de uso com fluxos principal, alternativo e de exceção
- Histórias de usuário com critérios de aceite em BDD
- Seis ADRs: FSRS em vez de SM-2, Java em vez de Kotlin, Room e OpenCSV,
  `ExecutorService` em vez de `WorkManager`, classificação de dados e criptografia,
  e arquitetura em camadas físicas
- Diagrama de arquitetura C4 e DER
- Algoritmo FSRS implementado na camada de domínio, com testes determinísticos
- Pipeline de CI/CD com quatro portões: build e testes, análise estática, varredura
  de segurança e SonarCloud
- `README.md`, `LICENSE` (MIT), `CONTRIBUTING.md` e `docs/seguranca.md`
