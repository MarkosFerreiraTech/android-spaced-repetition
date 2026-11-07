# Requisitos — RF e RNF

| Campo | Valor |
|---|---|
| Versão | 1.0 |
| Issue de origem | [#2](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues/2) |
| Rastreia | [Charter §4](./project-charter.md#4-escopo-preliminar) → [Casos de Uso](./casos-de-uso.md) → [User Stories](./user-stories.md) → Issues |
| Última revisão | 2026-11-06 |

**Convenções**

- Prioridade aplica **MoSCoW**: `Must`, `Should`, `Could`.
- Toda linha declara **como será verificada** (teste automatizado, inspeção ou medição).
- Identificadores são estáveis: um RF nunca é renumerado; se for descontinuado, fica marcado como `OBSOLETO`.

---

## 1. Requisitos Funcionais

### 1.1 Importação

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF01** | O sistema deve permitir ao usuário selecionar um arquivo CSV do armazenamento do dispositivo usando o Storage Access Framework, sem exigir permissão de leitura de armazenamento. | Must | Espresso: `ImportActivityTest.selectCsv` | #11, #12 |
| **RF02** | O sistema deve ler o CSV em thread de background (`ExecutorService`), mantendo a interface responsiva e publicando progresso incremental a cada 500 linhas. | Must | JUnit: `CsvImportDataSourceTest.reportsProgress` + inspeção de thread | #11 |
| **RF03** | O sistema deve aceitar separador vírgula (`,`) ou ponto e vírgula (`;`), detectando-o automaticamente, e ignorar BOM UTF-8 no início do arquivo. | Must | JUnit parametrizado: `CsvParserTest.separatorDetection` | #11 |
| **RF04** | O sistema deve exigir o cabeçalho `question,option_a,option_b,option_c,option_d,correct_answer`, permitindo `option_e` como coluna opcional, e rejeitar o arquivo inteiro com mensagem clara caso o cabeçalho não esteja conforme. | Must | JUnit: `CsvParserTest.headerValidation` | #11 |
| **RF05** | O sistema deve validar cada linha individualmente e, em caso de erro (gabarito inválido, enunciado vazio, menos de 2 alternativas), ignorar apenas aquela linha e acumulá-la no relatório de erros com número da linha e motivo. | Must | JUnit: `CsvParserTest.invalidLineDoesNotAbort` | #11 |
| **RF06** | O sistema deve impedir duplicidade por conteúdo: o mesmo enunciado normalizado, no mesmo baralho, gera o mesmo `content_hash` SHA-256; reimportar atualiza o registro existente e preserva o histórico de revisões. | Must | JUnit: `CardRepositoryTest.upsertIsIdempotent` | #10, #11 |
| **RF07** | O sistema deve exibir, ao final da importação, um relatório com as contagens de importados, atualizados, ignorados e a lista de linhas com erro. | Must | Espresso: `ImportActivityTest.showsReport` | #12 |
| **RF08** | O sistema deve permitir cancelar uma importação em andamento, encerrando o executor e sem deixar registros parciais inconsistentes (transação por lote). | Should | JUnit: `ImportUseCaseTest.cancelRollsBackBatch` | #11 |

### 1.2 Baralhos

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF09** | O sistema deve criar um baralho por arquivo importado, nomeado a partir do nome do arquivo e editável pelo usuário. | Must | JUnit + Espresso | #10, #12 |
| **RF10** | O sistema deve listar os baralhos exibindo nome, total de questões e quantidade de revisões pendentes no dia. | Must | Espresso: `DeckListActivityTest.showsPendingCount` | #12 |
| **RF11** | O sistema deve exibir estado vazio com instrução de importação quando não houver baralhos. | Must | Espresso: `DeckListActivityTest.emptyState` | #12 |

### 1.3 Sessão de estudo

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF12** | O sistema deve oferecer três modos de sessão: `COMPLETO` (todas as questões do baralho), `SOMENTE_ERROS` (questões erradas na última tentativa) e `ERROS_MAIS_MARCADAS` (erradas + acertadas com a marcação "acertei com dúvida"). | Must | JUnit: `StudySessionBuilderTest` (3 cenários) | #13 |
| **RF13** | O sistema deve apresentar uma questão por vez, com enunciado, alternativas e indicador de progresso `n/total`. | Must | Espresso: `StudyActivityTest.rendersQuestion` | #13 |
| **RF14** | O sistema deve permitir marcar uma questão com o flag "acertei com dúvida" antes de confirmar a resposta. | Must | Espresso | #13 |
| **RF15** | O sistema deve dar feedback imediato após a confirmação, indicando acerto ou erro e qual era a alternativa correta. | Must | Espresso | #13 |
| **RF16** | O sistema deve registrar cada resposta como um `ReviewLog` imutável e reagendar a questão aplicando o algoritmo FSRS. | Must | JUnit: `StudyViewModelTest.schedulesAfterAnswer` | #8, #13 |
| **RF17** | O sistema deve exibir, ao final da sessão, um resumo com acertos, erros e marcadas, e oferecer a ação de refazer somente os erros. | Must | Espresso | #13 |
| **RF18** | O sistema deve persistir o estado da sessão em andamento, permitindo retomada no ponto exato após o app ser encerrado. | Should | JUnit: `StudyViewModelTest.restoresSession` | #13 |

### 1.4 Estatísticas

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF19** | O sistema deve calcular e exibir a acurácia global e por baralho, a partir de agregação SQL sobre `ReviewLog` (nunca em memória). | Must | JUnit DAO com fixtures | #14 |
| **RF20** | O sistema deve exibir a sequência de dias consecutivos estudados e o total de questões revisadas. | Should | JUnit DAO | #14 |
| **RF21** | O sistema deve exibir estado vazio nas estatísticas, sem divisão por zero, quando não houver revisões. | Must | Espresso | #14 |

### 1.5 Preferências e segurança

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF22** | O sistema deve permitir configurar o limite diário de questões novas e o fuso horário usado no cálculo de "pendente hoje". | Should | Espresso + JUnit | #15, #13 |
| **RF23** | O sistema deve armazenar preferências e métricas agregadas em `EncryptedSharedPreferences` com chave mestra no Android Keystore. | Must | Teste instrumentado: valor em claro ≠ valor persistido | #15 |
| **RF24** | O sistema deve tratar falha de acesso ao Keystore sem gravar dados sensíveis em texto puro silenciosamente, informando o usuário. | Must | JUnit: `SecurePreferencesTest.keystoreFailure` | #15 |

### 1.6 Rastreabilidade e transparência

| ID | Requisito | Prioridade | Verificação | Issue |
|---|---|---|---|---|
| **RF25** | O sistema deve exibir, para cada questão, a data da próxima revisão e o estágio atual de aprendizado. | Could | Espresso | #14 |
| **RF26** | O sistema deve registrar logs de aplicação sem qualquer dado sensível do usuário (enunciado, resposta ou preferência). | Must | Regra de lint customizada + inspeção | #15 |

---

## 2. Requisitos Não Funcionais

### 2.1 Desempenho

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF01** | A importação de um CSV não deve bloquear a thread principal. | 0 ANRs; parsing em thread nomeada `csv-import` | Inspeção + teste de thread |
| **RNF02** | A importação de 10.000 linhas deve concluir em tempo aceitável em dispositivo de referência. | < 5 s; inserção em lote de 500 registros por transação | Teste de benchmark instrumentado |
| **RNF03** | A abertura de um baralho com até 5.000 questões deve renderizar rapidamente. | < 300 ms na primeira renderização | Macrobenchmark |
| **RNF04** | O cálculo de reagendamento deve ser desprezível frente à interação do usuário. | < 5 ms por questão | JUnit com medição |

### 2.2 Persistência e confiabilidade

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF05** | O aplicativo deve ser 100% funcional sem conexão de rede. | 0 chamadas de rede no manifesto (0 permissão `INTERNET`) | Inspeção do `AndroidManifest.xml` como teste |
| **RNF06** | O schema do Room deve ser versionado e migrável sem perda de dados. | `exportSchema = true`, JSON em `schemas/` versionado, migração testada | `MigrationTestHelper` |
| **RNF07** | Nenhuma operação de escrita pode deixar estado parcial em caso de cancelamento. | Atomicidade por transação de lote | Teste de rollback |

### 2.3 Usabilidade e acessibilidade

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF08** | A interface deve seguir o Material Design 3 com componentes oficiais do Android. | 0 componentes customizados de terceiros; uso de `MaterialToolbar`, `MaterialCardView`, `MaterialButton` | Inspeção + revisão de PR |
| **RNF09** | Todos os elementos interativos devem ter rótulo acessível e alvo de toque mínimo. | `contentDescription` em 100% dos ícones; alvos ≥ 48dp | `lint` (Accessibility) + Espresso |
| **RNF10** | A interface deve suportar rotação e não perder o estado da sessão. | Estado preservado via ViewModel | Teste de configuração |
| **RNF11** | O app deve suportar tema claro e escuro. | Paleta declarada em `values/themes.xml` e `values-night/themes.xml` | Inspeção visual |

### 2.4 Segurança (DevSecOps)

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF12** | Dados classificados como sensíveis devem ser cifrados em repouso. | AES-256-GCM via `EncryptedSharedPreferences`; chave no Keystore | Teste instrumentado |
| **RNF13** | Nenhum segredo, chave ou credencial pode ser versionado. | 0 ocorrências em `git log`; `gitleaks` no CI | Varredura automatizada |
| **RNF14** | Dependências devem ser monitoradas contra vulnerabilidades conhecidas. | Dependabot semanal + `dependency-check` no CI | Relatório de CI |
| **RNF15** | A superfície de permissões deve ser mínima. | Apenas permissões indispensáveis (nenhuma de armazenamento, nenhuma de rede) | Inspeção do manifesto |

### 2.5 Manutenibilidade e qualidade

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF16** | O domínio não pode depender de Android, Room ou UI. | 0 imports `android.*` no pacote `domain` | ArchUnit |
| **RNF17** | A cobertura de testes deve ser mensurável e mínima. | Global ≥ 80%; `domain.fsrs` ≥ 95% | JaCoCo com `jacocoTestCoverageVerification` |
| **RNF18** | O código não pode conter code smells que ultrapassem os limites do gate. | 0 blocker, 0 critical, duplicação < 3%, complexidade cognitiva por método ≤ 15 | SonarCloud + Checkstyle + SpotBugs |
| **RNF19** | A build deve funcionar em qualquer máquina com JDK 17. | Gradle Wrapper + version catalog fixo | CI em runner limpo |

### 2.6 Portabilidade e compatibilidade

| ID | Requisito | Métrica | Verificação |
|---|---|---|---|
| **RNF20** | O app deve rodar em Android 7.0 ou superior. | `minSdk 24`, `targetSdk 35` | Inspeção + emulador API 24 no CI |
| **RNF21** | O APK de debug deve ser produzido pelo CI a cada push em `develop`. | Artifact publicado em 100% das execuções verdes | Workflow |

---

## Anexo A — Contrato do arquivo CSV

**Formato:** texto delimitado, UTF-8 (BOM opcional), separador `,` ou `;`.

**Cabeçalho obrigatório (nesta ordem ou com nomes equivalentes, em qualquer ordem):**

| Coluna | Obrigatória | Regra |
|---|---|---|
| `question` | Sim | Não vazio, até 2.000 caracteres |
| `option_a` | Sim | Não vazio |
| `option_b` | Sim | Não vazio |
| `option_c` | Não | Opcional em questões de duas alternativas |
| `option_d` | Não | Idem |
| `option_e` | Não | Idem |
| `correct_answer` | Sim | Um entre `A`,`B`,`C`,`D`,`E`, sem distinção de caixa; deve referenciar uma alternativa presente |
| `explanation` | Não | Texto de justificativa exibido no feedback |

**Exemplo mínimo:**

```csv
question,option_a,option_b,option_c,option_d,correct_answer,explanation
"Qual serviço oferece armazenamento de objetos com durabilidade de 11 noves?",S3,EBS,EFS,Glacier,A,"S3 é o serviço de object storage da AWS."
```

**Regras de robustez**

1. Campos entre aspas podem conter o separador e quebras de linha (RFC 4180).
2. Linhas com número de colunas diferente do cabeçalho são inválidas, não fatais.
3. Linhas em branco são ignoradas sem gerar erro.
4. Limite operacional: 50.000 linhas por arquivo (acima disso, o app recusa com aviso).

## Anexo B — Mapa de rastreabilidade resumido

| RF | Caso de uso | User story | Issue |
|---|---|---|---|
| RF01–RF08 | UC01 | US01, US02 | #11, #12 |
| RF09–RF11 | UC02 | US03 | #10, #12 |
| RF12–RF18 | UC03, UC04 | US04, US05, US06 | #13 |
| RF19–RF21 | UC05 | US07 | #14 |
| RF22–RF24 | UC06 | US08 | #15 |
| RF25–RF26 | UC05 | US09 | #14, #15 |
