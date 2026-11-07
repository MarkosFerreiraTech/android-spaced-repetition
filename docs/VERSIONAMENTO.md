# Versionamento

Convenções de commit, fluxo de branches e política de versões deste repositório.
Complementa o `CONTRIBUTING.md` e o `CHANGELOG.md`.

---

## 1. Conventional Commits

Toda mensagem de commit segue o formato:

```
tipo(escopo): descrição curta no imperativo

Corpo opcional explicando o porquê da mudança.

Rodapé opcional: Closes #12
```

### 1.1 Tipos permitidos

| Tipo | Uso |
|---|---|
| `feat` | Nova funcionalidade visível ao usuário |
| `fix` | Correção de defeito |
| `docs` | Apenas documentação |
| `style` | Formatação, sem mudança de comportamento |
| `refactor` | Reorganização sem alterar comportamento observável |
| `perf` | Melhoria de desempenho |
| `test` | Criação ou ajuste de testes |
| `build` | Build, Gradle, dependências, ferramentas |
| `ci` | Pipeline, workflows, scripts de CI |
| `chore` | Manutenção que não se encaixa acima |
| `revert` | Reversão de um commit anterior |

### 1.2 Escopo obrigatório

O escopo identifica a área tocada, em minúsculas. Escopos em uso:

`domain` · `data` · `csv` · `fsrs` · `ui` · `stats` · `study` · `di` ·
`build` · `ci` · `docs` · `readme` · `repo` · `release`

O escopo é opcional **apenas** em `revert`.

### 1.3 Regras do assunto

- Máximo **72 caracteres**.
- Imperativo, em português, sem ponto final.
- Descreve o **resultado**, não o processo: `fix(build): ligar o excludeFilter` e
  não `build: mexendo no gradle`.

### 1.4 Validação automática

O hook em `scripts/git-hooks/commit-msg` rejeita mensagens fora do padrão:

```bash
git config core.hooksPath scripts/git-hooks
```

A partir daí, um commit `ajustes` é recusado com a lista de tipos aceitos e o
formato esperado. O hook permite `Merge`, `Revert`, `fixup!` e `squash!`, que o
próprio Git gera.

### 1.5 Rastreabilidade

Commits que implementam uma issue citam o número no assunto:

```
feat(csv): adicionar parser OpenCSV com validacao por linha (#11)
```

O PR usa `Closes #N` no corpo para fechar automaticamente no merge.

---

## 2. Git Flow

```
main ─────●───────────────●────────────────●──
           \             /                /
develop     ●───●───●───●────●───●───●───●──
             \ /   \ /       \ /   \ /
              feature/issue-11 …
```

### 2.1 Branches

| Branch | Origem | Destino | Papel |
|---|---|---|---|
| `main` | — | — | Código em produção. Só recebe `release/*` e `hotfix/*`. |
| `develop` | `main` | — | Integração contínua das features. |
| `feature/issue-NN-slug` | `develop` | `develop` | Uma issue por branch. |
| `release/vX.Y.Z` | `develop` | `main` + `develop` | Congelamento antes da tag. |
| `hotfix/vX.Y.Z` | `main` | `main` + `develop` | Correção urgente em produção. |

### 2.2 Merges

Sempre `--no-ff`, para que o grafo preserve a existência da branch:

```bash
git switch develop
git merge --no-ff feature/issue-11-csv -m "merge: integrar importador CSV (#11)"
```

Sem `--no-ff` o histórico linear esconde o trabalho por issue e o
`git log --graph` deixa de ser auditável — que é exatamente o que a issue #17
exige.

---

## 3. Versionamento Semântico

`MAJOR.MINOR.PATCH`, com tags anotadas.

| Componente | Incrementa quando |
|---|---|
| `MAJOR` | Quebra de compatibilidade: formato de CSV aceito muda, banco exige migração destrutiva, schema de banco incompatível |
| `MINOR` | Funcionalidade nova compatível |
| `PATCH` | Correção compatível |

### 3.1 Tags anotadas

Tags anotadas (`git tag -a`) carregam autor, data e mensagem, e são assináveis.
Tags leves não servem para auditoria.

```bash
git tag -a v0.2.0 -m "v0.2.0: scaffold Android e dominio com FSRS"
git push origin v0.2.0
```

### 3.2 Marcos

| Versão | Conteúdo | Estado |
|---|---|---|
| `v0.1.0` | Documentação: requisitos, casos de uso, user stories, ADRs | ✅ publicada |
| `v0.2.0` | Scaffold Android + domínio com FSRS | planejada |
| `v0.3.0` | Importador CSV + sessão de estudo | planejada |
| `v1.0.0` | MVP completo | planejada |

### 3.3 Regra do 0.x

Enquanto a versão for `0.y.z`, a API é considerada instável e mudanças
incompatíveis podem ocorrer em `MINOR`. A partir de `1.0.0`, vale a regra
estrita acima.

---

## 4. Changelog

O `CHANGELOG.md` segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e é atualizado **no merge da release**, não a cada commit. Seções possíveis:
`Adicionado`, `Alterado`, `Corrigido`, `Removido`, `Segurança`.

Cada versão publicada deve ter entrada correspondente. O padrão de entrada cita
as issues quando aplicável.

---

## 5. Checklist de release

1. `git switch develop && git pull`
2. `git switch -c release/vX.Y.Z`
3. Atualizar `versionName`/`versionCode` em `build.gradle` e o `CHANGELOG.md`
4. Rodar a pipeline completa e confirmar os quatro jobs verdes
5. `git switch main && git merge --no-ff release/vX.Y.Z`
6. `git tag -a vX.Y.Z -m "..."` e `git push origin vX.Y.Z`
7. Merge de volta em `develop`
8. Publicar a release no GitHub com o texto do `CHANGELOG.md`
