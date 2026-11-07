# Termo de Abertura do Projeto (Project Charter)

| Campo | Valor |
|---|---|
| **Projeto** | Cerebro — Repetição Espaçada Offline para Simulados AWS |
| **Código** | SRS-AWS-2026 |
| **Versão do documento** | 1.1 |
| **Autor** | Markos Ferreira |
| **Data de abertura** | 2026-11-06 |
| **Última revisão** | 2026-11-07 |
| **Issue de origem** | [#1](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues/1) |
| **Status** | Aprovado |

---

## 1. Justificativa

Quem se prepara para certificações AWS (Cloud Practitioner, Associate, Professional) depende massivamente de **simulados de múltipla escolha**. A prática comum é fazer o simulado, olhar a nota e seguir adiante. O resultado é um padrão clássico de esquecimento: o conteúdo estudado na segunda-feira desaparece antes da prova, e os mesmos erros se repetem no simulado seguinte.

As plataformas de simulado existentes resolvem mal esse problema por três motivos:

1. **Não priorizam o que você erra.** Entregam sempre o mesmo conjunto fixo de questões, ou uma seleção aleatória que ignora seu histórico.
2. **Não agendam revisão.** Não há mecanismo para reencontrar uma questão no momento em que você está prestes a esquecê-la.
3. **Exigem conexão e conta.** O estudante fica refém de nuvem, login e assinatura para revisar conteúdo que ele mesmo forneceu.

O **Cerebro** ataca exatamente esse ponto: o estudante importa o seu próprio banco de questões a partir de uma planilha CSV, e o aplicativo calcula localmente, no próprio celular, **quando cada questão deve reaparecer** — usando o algoritmo **FSRS** (*Free Spaced Repetition Scheduler*), o estado da arte em repetição espaçada. O ciclo é fechado: errar uma questão aumenta sua prioridade de revisão; acertar sucessivamente espaça o reencontro em semanas.

## 2. Visão do Produto

> Para **estudantes de certificação AWS** que **já possuem bancos de questões em planilhas** e **estudam em trânsito, com internet instável**, o **Cerebro** é um **aplicativo Android nativo** que **agenda e executa revisões espaçadas das questões que você erra**, diferente das **plataformas de simulado tradicionais** que **repetem o mesmo conjunto fixo de questões e exigem conexão permanente**, porque **o agendamento acontece 100% no dispositivo, sem servidor, sem conta e sem enviar seus dados para fora**.

### 2.1 Proposta de valor em uma frase

Transformar um simulado de 65 questões, com 20 erros, em uma fila de revisão inteligente que reencontra cada questão no momento certo — offline e sem cadastro.

### 2.2 Diferenciais

| Diferencial | Por que importa |
|---|---|
| FSRS calculado localmente | Agendamento personalizado, sem servidor e sem custo por usuário |
| Importação por CSV | O estudante usa o banco de questões que já tem; zero lock-in de conteúdo |
| Três modos de refazer simulado | Respeita a estratégia real do estudante: tudo, só erros, ou erros + marcadas |
| Offline-first de verdade | Funciona no metrô, no avião, no interior — não é "offline com cache" |
| Dados sob o controle do usuário | Nada sai do dispositivo; dados sensíveis cifrados |

## 3. Objetivos SMART

| # | Objetivo | Específico | Mensurável | Atingível | Relevante | Temporal |
|---|---|---|---|---|---|---|
| **O1** | Entregar o MVP funcional com o fluxo completo de importação e estudo | Importar CSV → baralho → sessão → estatísticas | Fluxo executado ponta a ponta em 1 dispositivo real | Escopo reduzido a 4 telas | É o produto mínimo que resolve a dor | Até 2026-11-13 |
| **O2** | Implementar o agendador FSRS isolado no domínio | Porta `ReviewScheduler` + implementação determinística | Cobertura de testes ≥ 95% no pacote `domain.fsrs` | Algoritmo documentado e sem dependência externa | É o núcleo do valor do produto | Até 2026-11-09 (marco v0.2.0) |
| **O3** | Garantir desempenho da importação | Leitura de CSV em background com OpenCSV | 10.000 linhas importadas em < 5 s em dispositivo de referência, sem ANR | ExecutorService + inserções em lote | Evita abandono na primeira importação real | Até 2026-11-11 (marco v0.3.0) |
| **O4** | Estabelecer portões de qualidade automatizados | CI com testes, lint, cobertura, análise estática | Cobertura global ≥ 80%, domínio ≥ 95%, 0 issues blocker/critical no Sonar, build bloqueante | JaCoCo + Checkstyle + SpotBugs + SonarCloud | Sustenta a evolução sem regressão | Ativo desde 2026-11-09, auditado em 2026-11-13 |
| **O5** | Aplicar DevSecOps em dados sensíveis | Classificação + cifra em repouso | 100% dos dados classificados como sensíveis cifrados via EncryptedSharedPreferences/Keystore; 0 segredos versionados | Android Keystore nativo | Protege o usuário e cumpre o requisito acadêmico | Auditado em 2026-11-13 |
| **O6** | Documentar com rastreabilidade completa | Requisitos → histórias → issues → testes | 100% dos RF de prioridade Must mapeados para ao menos uma issue fechada e um teste automatizado | Tabela de rastreabilidade no README | Prova a disciplina de engenharia exigida | Fechado em 2026-11-13, junto à release v1.0.0 |

### 3.1 Cronograma de marcos

Os objetivos acima são verificáveis em quatro marcos. Cada marco corresponde a
uma tag anotada SemVer e a um milestone no GitHub Issues.

| Marco | Objetivo | Data-alvo | Entregável verificável | Requisitos cobertos | Issues |
|---|---|---|---|---|---|
| **M1 — v0.1.0** | Documentação de planejamento | 2026-11-07 | Charter, requisitos, casos de uso, histórias e arquitetura publicados, com links internos válidos | Todos os RF/RNF especificados | #1–#5 |
| **M2 — v0.2.0** | Fundação técnica | 2026-11-09 | Build verde no CI com APK gerado; domínio FSRS com cobertura ≥ 95%; schema do Room versionado | RNF16–RNF19 | #6–#10 |
| **M3 — v0.3.0** | Núcleo funcional | 2026-11-11 | Importação de CSV de 10.000 linhas em background e sessão de estudo nos três modos, executáveis no app | RF01–RF21 | #11–#15 |
| **M4 — v1.0.0** | Qualidade e entrega | 2026-11-13 | Portões de qualidade ativos, dados sensíveis cifrados, README sincronizado e release com APK anexado | RNF08–RNF14, RNF20–RNF21 | #16–#20 |

## 4. Escopo Preliminar

### 4.1 Dentro do escopo (esta semana)

| Item | Descrição |
|---|---|
| Importação CSV | Seleção via Storage Access Framework, parsing com OpenCSV em `ExecutorService`, validação por linha, upsert idempotente por hash SHA-256 |
| Baralhos | Um baralho por arquivo importado; listagem com contagem de cartões e pendências do dia |
| Sessão de estudo | Múltipla escolha com 3 modos: completo, somente erros, erros + marcadas ("acertei com dúvida") |
| Agendamento FSRS | Cálculo local de estabilidade, dificuldade e próximo intervalo, com rating derivado do desempenho |
| Estatísticas | Acurácia global e por baralho, pendências do dia, sequência de dias estudados |
| Persistência | Room Database, schema versionado, offline-first |
| Segurança | EncryptedSharedPreferences para preferências e métricas sensíveis; classificação de dados documentada |
| Documentação | Charter, requisitos, casos de uso, user stories, arquitetura (C4 + DER + ADRs), README, VERSIONAMENTO |
| Processo | Git Flow, Conventional Commits, SemVer com tags anotadas, 20 issues, CI/CD no GitHub Actions |

### 4.2 Fora do escopo (expressamente)

| Item | Motivo da exclusão |
|---|---|
| Notificações push de revisão | Exige WorkManager + permissões de notificação + teste em background com restrições de bateria; incompatível com o prazo |
| Sincronização em nuvem / multiusuário | Contradiz a premissa offline-first e demanda backend, autenticação e LGPD |
| Editor de questões dentro do app | O fluxo de entrada é a planilha; um editor é um produto inteiro |
| Tipos de questão além de múltipla escolha | Verdadeiro/falso e dissertativa exigiriam outro modelo de correção |
| Exportação do progresso | Será tratado na v1.1 |
| Internacionalização | Interface pt-BR apenas nesta versão |
| Publicação na Play Store | Entrega é um APK de release assinado em debug, anexado à GitHub Release |
| SQLCipher no Room | Risco registrado e aceito no ADR-005; conteúdo de questões não é classificado como sensível |

### 4.3 Decisões de escopo aprovadas pelo Product Owner

Registradas na fase de concepção (2026-11-06), antes de qualquer linha de código.
Cada decisão rejeitou explicitamente uma alternativa, e a justificativa técnica
está no ADR correspondente.

| # | Decisão | Alternativas rejeitadas | ADR |
|---|---|---|---|
| D1 | Algoritmo **FSRS** | SM-2 clássico, SM-2 simplificado, FSRS com treino local | [ADR-001](./adr/ADR-001-fsrs-em-vez-de-sm2.md) |
| D2 | Sessão de **múltipla escolha** derivada do simulado, com três modos: completo, só erros, erros + marcadas | Flashcard com autoavaliação; tipo decidido pelo CSV | — |
| D3 | Reimportação por **upsert** com hash do enunciado, preservando o histórico | Ignorar duplicadas; perguntar ao usuário | — |
| D4 | **SonarCloud** condicional ao secret + portão local obrigatório (JaCoCo, lint, Checkstyle, SpotBugs) | Sonar self-hosted no runner; somente SonarCloud | — |
| D5 | Escopo de **4 telas**: importar, baralhos, sessão, estatísticas | 3 telas; 4 telas + configurações | — |
| D6 | **Git Flow** com `develop`, `feature/*`, `release/*` e tags anotadas | Commits apenas na branch principal | — |

**Nota sobre D5:** a tela de configurações, fora da escolha inicial do PO, foi
incorporada porque é o único lugar onde o requisito RNF12 (cifra em repouso) é
exercitado de forma observável. Sem ela, o requisito ficaria sem interface de
verificação.

## 5. Stakeholders e Público-Alvo

### 5.1 Stakeholders

| Stakeholder | Papel | Interesse | Influência |
|---|---|---|---|
| Markos Ferreira | Product Owner, desenvolvedor, usuário final | Aprovar escopo, validar requisitos e qualidade | Alta |
| Avaliador acadêmico | Auditor do processo | Verificar documentação, rastreabilidade, Git Flow e qualidade | Alta |
| Comunidade open source | Consumidores e contribuidores | Código legível, licença clara, contribuição padronizada | Baixa |

### 5.2 Personas

**Persona primária — "Ana, a concurseira de certificação"**
- 28 anos, analista de infraestrutura migrando para cloud.
- Estuda 40 minutos por dia, no ônibus, com internet intermitente.
- Já baixou o dump de questões em `.csv` de um curso pago.
- Dor: refaz o mesmo simulado inteiro, ou nada; não sabe o que priorizar.

**Persona secundária — "Rafael, o estudante auditável"**
- 35 anos, arquiteto de soluções preparando a Professional.
- Tem 2.400 questões acumuladas de vários cursos, em planilhas diferentes.
- Dor: quer saber, com número, se está melhorando por domínio, e não confia em plataformas que guardam seus dados.

## 6. Premissas

1. O usuário possui o arquivo CSV das questões e sabe selecioná-lo no dispositivo.
2. O CSV segue o contrato de colunas documentado em `docs/requisitos.md` (§Anexo A).
3. O dispositivo roda Android 7.0 (API 24) ou superior, com Google Play Services disponível para a biblioteca de segurança.
4. O estudante está disposto a responder a mesma questão ao longo de dias — a técnica só funciona com recorrência.
5. A máquina de desenvolvimento dispõe de JDK 17 e Android Studio; o build de referência roda também no GitHub Actions.

## 7. Restrições

| Restrição | Impacto | Mitigação |
|---|---|---|
| Prazo de uma semana | Impossibilita notificações, nuvem e editor de questões | Escopo fechado em 4 telas; itens fora do escopo declarados na §4.2 |
| Java (não Kotlin) | Ausência de corrotinas e de DSL para ViewBinding | Uso de `ExecutorService` injetado e `LiveData`; testes com executor síncrono |
| Sem backend | Agendamento precisa ser inteiramente local | FSRS determinístico no domínio, estado persistido no Room |
| Ambiente de execução do agente sem Android SDK | Impossível compilar o APK na sandbox | Build e testes validados no GitHub Actions; APK produzido e anexado pelo CI |
| Token de automação sem permissão de edição de issues | Labels e milestones não aplicáveis por API | Script idempotente `scripts/sync-backlog.sh` para execução local |

## 8. Riscos

| ID | Risco | Prob. | Impacto | Exposição | Mitigação | Contingência |
|---|---|---|---|---|---|---|
| R1 | Algoritmo FSRS implementado incorretamente, gerando intervalos sem sentido | Média | Alto | Crítico | Testes de propriedade e de casos-limite; parâmetros default da especificação FSRS | Congelar em SM-2 simplificado com a mesma porta de domínio |
| R2 | CSVs reais fora do contrato de colunas | Alta | Médio | Alto | Detecção de separador, BOM e cabeçalho; relatório de erros por linha | Assistente de mapeamento de colunas na v1.1 |
| R3 | ANR durante importação de arquivos grandes | Média | Alto | Alto | Parsing fora da main thread, inserção em lote, progresso incremental | Bloquear importações acima de 50 mil linhas com aviso |
| R4 | Perda da MasterKey do Keystore em backup/restauração | Baixa | Médio | Médio | Tratar `KeyStoreException` e recriar preferências com aviso ao usuário | Fallback explícito registrado em log, sem gravar em texto puro de forma silenciosa |
| R5 | Gate de qualidade bloqueando entrega por dívida acumulada | Média | Médio | Alto | Gate configurado desde o primeiro commit, com exceções documentadas | Sonar opcional via secret; JaCoCo e lint permanecem obrigatórios |
| R6 | Sobrecarga de escopo por novas ideias durante a semana | Alta | Alto | Alto | §4.2 explícita; toda ideia nova vira issue em milestone futuro | Product Owner (você) arbitra; nada entra em `release/1.0.0` |

## 9. Critérios de Sucesso

1. Fluxo completo (importar → estudar → reagendar → ver estatística) executável em um dispositivo Android real.
2. `./gradlew build` verde no CI, com cobertura ≥ 80% e domínio ≥ 95%.
3. As 20 issues criadas com critérios de aceite verificáveis e fechadas com referência `Closes #N`.
4. `git log --graph` exibindo o fluxo Git Flow com 4 tags anotadas SemVer.
5. Documentação navegável e sem links mortos, com tabela de rastreabilidade completa.

## 10. Aprovação

| Nome | Papel | Data | Decisão |
|---|---|---|---|
| Markos Ferreira | Product Owner | 2026-11-06 | Aprovado |
| Markos Ferreira | Tech Lead | 2026-11-06 | Aprovado |

## 11. Histórico de Revisões

| Versão | Data | Autor | Mudança |
|---|---|---|---|
| 1.0 | 2026-11-06 | Markos Ferreira | Versão inicial aprovada |
| 1.1 | 2026-11-07 | Markos Ferreira | Prazos dos objetivos convertidos em datas absolutas; cronograma de marcos (M1–M4) adicionado; decisões de escopo D1–D6 registradas; histórico de revisões incluído |
