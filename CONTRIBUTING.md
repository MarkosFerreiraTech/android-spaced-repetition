# Como contribuir

Obrigado pelo interesse. Este documento descreve o fluxo de trabalho obrigatório
do projeto. Ele não é burocracia: cada regra existe para manter a rastreabilidade
entre requisito, issue, código e teste.

## Antes de começar

Leia, nesta ordem:

1. [`docs/project-charter.md`](docs/project-charter.md) — o que o produto resolve e o que está fora do escopo.
2. [`docs/requisitos.md`](docs/requisitos.md) — os RFs e RNFs numerados.
3. [`docs/arquitetura.md`](docs/arquitetura.md) — as camadas e a regra de dependência.

## 1. Abra ou localize uma issue

Toda contribuição começa em uma issue. Se não existir, crie usando os templates em
`.github/ISSUE_TEMPLATE/`. A issue precisa ter **critérios de aceite verificáveis**
em formato Gherkin.

## 2. Crie a branch a partir de `develop`

```bash
git switch develop
git pull origin develop
git switch -c feature/issue-42-descricao-curta
```

| Prefixo | Uso |
|---|---|
| `feature/issue-NN-*` | Nova funcionalidade |
| `fix/issue-NN-*` | Correção de bug |
| `refactor/issue-NN-*` | Mudança interna sem alterar comportamento |
| `docs/issue-NN-*` | Documentação |
| `release/X.Y.Z` | Congelamento de release |
| `hotfix/X.Y.Z` | Correção urgente em produção |

**Nunca** faça commit direto em `main` ou `develop`.

## 3. Commits atômicos

Formato: **Conventional Commits** com escopo e referência à issue.

```
<tipo>(<escopo>): <descrição no imperativo> (#N)
```

Tipos aceitos: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
Escopos: `domain`, `data`, `csv`, `fsrs`, `ui`, `study`, `stats`, `di`, `build`, `ci`, `docs`, `readme`, `repo`, `release`.

O assunto tem no máximo 72 caracteres e não termina com ponto final. O escopo é
obrigatório (exceto em `revert`).

Exemplos reais do histórico:

```
feat(domain): implementar FSRS com estabilidade, dificuldade e curva de esquecimento (#8)
fix(domain): corrigir curva de esquecimento, intervalo e dificuldade do FSRS (#8)
ci(observabilidade): emitir erros do Gradle como anotacoes de check-run (#9)
```

**Um commit = uma intenção.** Não misture correção de bug com formatação.

O padrão é verificado automaticamente. Ative o hook uma vez por clone:

```bash
git config core.hooksPath scripts/git-hooks
```

A partir daí, uma mensagem como `ajustes` é recusada com a lista de tipos e o
formato esperado. A convenção completa, incluindo a política SemVer e o
checklist de release, está em [`docs/VERSIONAMENTO.md`](docs/VERSIONAMENTO.md).

## 4. Antes de abrir o Pull Request

Rode localmente os mesmos portões do CI:

```bash
./gradlew testDebugUnitTest
./gradlew jacocoTestCoverageVerification
./gradlew checkstyleMain
./gradlew lintDebug
```

| Portão | Limite |
|---|---|
| Testes | 100% verde |
| Cobertura global | ≥ 80% |
| Cobertura de `domain.*` | ≥ 95% |
| Checkstyle | 0 violações |
| Android Lint | 0 erros |

## 5. Abra o Pull Request

- Base: `develop` (nunca `main`, exceto `release/*` e `hotfix/*`).
- Título no padrão Conventional Commits.
- Descrição: o que mudou, por quê, como testar e **`Closes #N`**.
- Preencha o checklist do template.

## 6. Regras de código

| Regra | Consequência |
|---|---|
| `domain` não importa `android.*` nem `androidx.room.*` | Build quebra (ArchUnit) |
| Nenhum modelo de domínio tem `@Entity` | Build quebra |
| ViewModel não instancia repositório (`new XRepository()`) | Build quebra |
| Nenhuma chamada de rede | O app não tem permissão `INTERNET` |
| Nenhum segredo no código | gitleaks falha o build |
| Nenhum `System.out` ou `printStackTrace` | Checkstyle falha o build |

## 7. Dúvidas sobre escopo

Se a ideia não está coberta por um RF, o caminho é: abra uma issue em um milestone
futuro. Não amplie o escopo da entrega atual — está documentado no charter §4.2 o
que ficou expressamente de fora e por quê.

## Código de conduta

Seja direto e respeitoso nas revisões. Critique o código, não a pessoa.
