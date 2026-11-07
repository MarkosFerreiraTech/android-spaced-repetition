# Checklist de release

Procedimento para publicar uma versão. Cada item tem comando de verificação,
para que "está pronto" seja uma afirmação checável, não uma impressão.

Referências: [`VERSIONAMENTO.md`](VERSIONAMENTO.md) §5 e [`../CHANGELOG.md`](../CHANGELOG.md).

---

## 1. Congelamento

- [ ] Todas as issues do marco estão fechadas ou explicitamente adiadas
- [ ] Não há PR aberto apontando para `main`
- [ ] `develop` está sincronizada com `main`

```bash
gh pr list --state open
git switch develop && git pull
git log --oneline main..develop | wc -l
```

## 2. Versão

- [ ] `versionName` corresponde à tag que será criada
- [ ] `versionCode` foi incrementado em relação à release anterior
- [ ] Badge de versão no `README.md` atualizado

```bash
grep -n 'versionName\|versionCode' build.gradle
grep -n 'vers%C3%A3o' README.md
```

Convenção: `MAJOR.MINOR.PATCH`. Detalhes da política em
[`VERSIONAMENTO.md` §3](VERSIONAMENTO.md).

## 3. Qualidade

- [ ] Build, testes e cobertura verdes
- [ ] Análise estática verde (Checkstyle + SpotBugs)
- [ ] Varredura de segurança verde (gitleaks + ausência de `INTERNET`)
- [ ] SonarCloud verde **ou** skip justificado por ausência de token
- [ ] Cobertura: domínio ≥ 95%, global ≥ 80%

```bash
./gradlew testDebugUnitTest jacocoTestReport jacocoTestCoverageVerification
./gradlew checkstyleMain spotbugsMain
./gradlew assembleDebug
```

O mesmo conjunto roda no CI. Se o CI estiver verde no commit da tag, os portões
estão satisfeitos:

```bash
gh api repos/:owner/:repo/commits/<sha>/check-runs \
  -q '.check_runs[] | "\(.name): \(.conclusion)"'
```

## 4. Documentação

- [ ] `CHANGELOG.md` tem seção para a versão, com data
- [ ] `README.md` reflete as funcionalidades reais (implementado × planejado)
- [ ] Nenhum link morto nos documentos

```bash
grep -oE '\]\(([^)]+\.md)\)' README.md \
  | sed 's/](//;s/)//' | sort -u \
  | while read -r f; do [ -e "$f" ] || echo "LINK MORTO: $f"; done
```

## 5. Publicação

- [ ] Merge `release/vX.Y.Z` → `main` com `--no-ff`
- [ ] Tag **anotada** criada e enviada
- [ ] Back-merge `main` → `develop`
- [ ] GitHub Release publicada com notas do CHANGELOG

```bash
git switch main && git merge --no-ff release/vX.Y.Z
git tag -a vX.Y.Z -m "vX.Y.Z: <resumo>"
git push origin main vX.Y.Z

git switch develop && git merge --no-ff main && git push origin develop

gh release create vX.Y.Z --title "vX.Y.Z — <nome>" --notes-file /tmp/notas.md
```

## 6. Pós-release

- [ ] APK anexado como asset, quando houver interface gráfica
- [ ] Tag visível em `git tag -n`
- [ ] `develop` contém o merge de volta

```bash
git tag -n
git log --oneline --graph --all -15
```

---

## Anexar o APK

O APK é produzido pelo CI e publicado como artifact. Para anexá-lo a uma
release:

```bash
# Baixar o artifact do run verde
gh run download <run-id> -n cerebro-debug-apk -D /tmp/apk

# Anexar a uma release existente
gh release upload vX.Y.Z /tmp/apk/app-debug.apk
```

O APK de debug é o artefato de entrega declarado no charter (§4.2). Assinatura
de release está fora do escopo.

---

## Registro de releases

| Versão | Data | Conteúdo | Release |
|---|---|---|---|
| `v0.1.0` | 2026-11-07 | Documentação e domínio FSRS | [publicada](https://github.com/MarkosFerreiraTech/android-spaced-repetition/releases/tag/v0.1.0) |
| `v0.2.0` | 2026-11-07 | Persistência, CSV, preferências cifradas, DI, ArchUnit | pendente de tag |
| `v1.0.0` | — | MVP com telas | bloqueada por #12–#14 |
