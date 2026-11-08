# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o
projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).
A convenção de commits está em [`docs/VERSIONAMENTO.md`](docs/VERSIONAMENTO.md).

## [Não publicado]

_Nada pendente no momento._

## [1.0.0] — 2026-11-08

Primeira versão completa: o ciclo inteiro funciona no aparelho, do CSV à revisão
agendada.

### Adicionado

- **Tela de importação** com o seletor de arquivos do sistema (sem permissão de
  armazenamento), relatório de linhas aceitas e rejeitadas e cancelamento em
  andamento (#12)
- **Lista de baralhos** com contagem de pendentes do dia, estado vazio que
  orienta importar e recarga ao voltar de uma sessão (#12)
- **Escolha de modo** com a contagem real de cada modalidade: tudo, só os erros,
  erros mais marcadas (#13)
- **Sessão de estudo** com alternativas coloridas por estado, flag "acertei com
  dúvida", feedback com gabarito e explicação (#13)
- **Tela de estatísticas** com acurácia global, total de revisões, sequência de
  dias, baralho que precisa de atenção e acurácia por baralho (#14)
- **Data da próxima revisão e estágio atual** no feedback de cada questão, com
  comparação por dia de calendário em vez de diferença de horas (#14, RF25)
- Testes de tela com **Robolectric**: inflação, ciclo de vida e população de
  adaptadores
- **12 regras ArchUnit**, incluindo a proibição de Activity acessar repositório e
  a de registrar log com dado do usuário (RF26)
- Arquivo de exemplo `docs/exemplos/aws-cloud-practitioner.csv`, verificado por
  teste que roda o parser de produção

### Corrigido

- **Fechamento do app ao iniciar a sessão de estudo.** A tela de escolha de modo
  consultava o Room na thread principal, o que lança exceção. As consultas
  passaram a rodar em um `Executor` injetado, e uma regra ArchUnit impede que
  qualquer Activity volte a acessar repositório diretamente
- **Determinismo dos testes de lista:** o cálculo de diferenças do `ListAdapter`
  roda em thread própria; o executor passou a ser injetável e os testes drenam o
  looper principal

### Segurança

- Atualização do `assertj-core` de 3.26.3 para 3.27.7, corrigindo vulnerabilidade de XXE
- Nenhuma chamada a `android.util.Log` no código de produção, verificado em teste
- Permissão `INTERNET` ausente do manifesto; nenhum dado sai do aparelho

### Alterado

- `OpenCSV` 5.9 → 5.12.0
- Grupo de análise estática e grupo de GitHub Actions atualizados

### Notas de upgrade

Sem migração de dados pendente: o esquema Room desta versão é o mesmo da `0.2.0`
e não há `fallbackToDestructiveMigration`.


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
