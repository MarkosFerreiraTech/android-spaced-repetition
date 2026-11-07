## O que esta PR faz

<!-- Uma ou duas frases. O "porquê" importa mais que a lista de arquivos. -->

## Issue relacionada

Closes #

## Tipo de mudança

- [ ] `feat` — nova funcionalidade
- [ ] `fix` — correção de defeito
- [ ] `docs` — documentação
- [ ] `refactor` — reorganização sem mudança de comportamento
- [ ] `test` — testes
- [ ] `build` / `ci` — build ou pipeline
- [ ] `chore` — manutenção

## Checklist

### Código
- [ ] Segue a arquitetura em camadas (`domain` não importa `android`, `data` ou `presentation`)
- [ ] Nenhum segredo, chave ou `local.properties` versionado
- [ ] Não introduziu a permissão de `INTERNET` no manifest (o app é offline-first)

### Testes
- [ ] Testes unitários cobrem o caminho principal e os casos de erro
- [ ] `./gradlew test` passa localmente
- [ ] Cobertura mantida: domínio ≥ 95%, global ≥ 80%

### Pipeline
- [ ] Os quatro jobs do CI passaram (build, análise estática, segurança, Sonar)
- [ ] Nenhum achado novo de Checkstyle ou SpotBugs

### Documentação
- [ ] `CHANGELOG.md` atualizado, se a mudança for visível ao usuário
- [ ] Documentação em `docs/` atualizada, se contrato ou arquitetura mudou
- [ ] `README.md` atualizado, se o setup ou as funcionalidades mudaram

### Git Flow
- [ ] Branch nomeada no padrão `feature/issue-NN-slug`
- [ ] Commits seguem Conventional Commits com escopo obrigatório
- [ ] Merge para `develop` (ou `main` no caso de `release/*` e `hotfix/*`)
