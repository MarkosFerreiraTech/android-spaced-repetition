#!/usr/bin/env bash
#
# sync-backlog.sh — aplica labels, milestones e fecha issues entregues do backlog.
#
# POR QUE ESTE SCRIPT EXISTE
# --------------------------
# As 20 issues foram criadas por automacao cujo token tem permissao de LEITURA e
# de CRIACAO em issues, mas nao de EDICAO (HTTP 403 em PATCH /issues/N). Como
# labels, milestone e fechamento sao operacoes de edicao, elas precisam ser
# aplicadas por alguem com permissao de escrita no repositorio.
#
# O script e IDEMPOTENTE: rodar duas vezes nao gera erro nem duplica estado.
#
# USO
# ---
#   gh auth status                 # confirme que esta autenticado com escrita
#   ./scripts/sync-backlog.sh            # aplica labels/milestones e fecha o entregue
#   ./scripts/sync-backlog.sh --dry-run  # mostra o que faria, sem alterar nada
#
# REQUISITOS: gh >= 2.40, autenticado via `gh auth login`.

set -euo pipefail

REPO="${REPO:-MarkosFerreiraTech/android-spaced-repetition}"
DRY_RUN="false"

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN="true"
fi

run() {
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "  [dry-run] $*"
  else
    "$@" >/dev/null
  fi
}

ensure_label() {
  local name="$1" color="$2" description="$3"
  if gh label list --repo "$REPO" --json name -q '.[].name' | grep -qx "$name"; then
    echo "  label ok: $name"
  else
    echo "  criando label: $name"
    run gh label create "$name" --repo "$REPO" --color "$color" --description "$description"
  fi
}

ensure_milestone() {
  local title="$1" description="$2"
  if gh api "repos/$REPO/milestones?state=all" -q '.[].title' | grep -qx "$title"; then
    echo "  milestone ok: $title"
  else
    echo "  criando milestone: $title"
    if [[ "$DRY_RUN" == "true" ]]; then
      echo "  [dry-run] gh api repos/$REPO/milestones -f title='$title'"
    else
      gh api "repos/$REPO/milestones" -f title="$title" -f description="$description" >/dev/null
    fi
  fi
}

apply() {
  local issue="$1" milestone="$2" labels="$3"
  echo "issue #$issue -> '$milestone' | labels: $labels"
  run gh issue edit "$issue" --repo "$REPO" --milestone "$milestone" --add-label "$labels"
}

echo "== Labels =="
ensure_label "documentation" "0075ca" "Melhorias ou adicoes a documentacao"
ensure_label "feature"       "0e8a16" "Nova funcionalidade"
ensure_label "enhancement"   "a2eeef" "Melhoria em funcionalidade existente"
ensure_label "bug"           "d73a4a" "Algo nao funciona"
ensure_label "refactor"      "5319e7" "Mudanca interna sem alterar comportamento"
ensure_label "test"          "fbca04" "Testes e cobertura"
ensure_label "ci"            "1d76db" "Pipeline e automacao"
ensure_label "chore"         "ededed" "Manutencao e configuracao"
ensure_label "security"      "b60205" "Seguranca e privacidade"

echo "== Milestones =="
ensure_milestone "M1 - Documentação e Planejamento" "Charter, requisitos, casos de uso, historias e arquitetura. Marco v0.1.0"
ensure_milestone "M2 - Fundação Técnica"           "Scaffold, dominio FSRS, Room e CI. Marco v0.2.0"
ensure_milestone "M3 - Núcleo Funcional"           "Importacao CSV, sessao de estudo e estatisticas. Marco v0.3.0"
ensure_milestone "M4 - Qualidade e Entrega"        "Portoes de qualidade, DevSecOps e release v1.0.0"

echo "== Issues: labels e milestones =="
apply 1  "M1 - Documentação e Planejamento" "documentation"
apply 2  "M1 - Documentação e Planejamento" "documentation"
apply 3  "M1 - Documentação e Planejamento" "documentation"
apply 4  "M1 - Documentação e Planejamento" "documentation"
apply 5  "M1 - Documentação e Planejamento" "documentation"
apply 6  "M1 - Documentação e Planejamento" "chore,documentation"
apply 7  "M2 - Fundação Técnica"            "chore"
apply 8  "M2 - Fundação Técnica"            "feature"
apply 9  "M2 - Fundação Técnica"            "ci"
apply 10 "M2 - Fundação Técnica"            "feature"
apply 11 "M2 - Fundação Técnica"            "refactor"
apply 12 "M3 - Núcleo Funcional"            "feature"
apply 13 "M3 - Núcleo Funcional"            "feature"
apply 14 "M3 - Núcleo Funcional"            "feature"
apply 15 "M3 - Núcleo Funcional"            "feature"
apply 16 "M4 - Qualidade e Entrega"         "security"
apply 17 "M4 - Qualidade e Entrega"         "test"
apply 18 "M4 - Qualidade e Entrega"         "documentation"
apply 19 "M4 - Qualidade e Entrega"         "documentation"
apply 20 "M4 - Qualidade e Entrega"         "chore"

# ---------------------------------------------------------------------------
# Fechamento das issues ja entregues.
# Criterio objetivo: o artefato existe no repositorio E o CI que o valida esta
# verde. Atualize a lista conforme cada entrega for verificada.
# ---------------------------------------------------------------------------
echo "== Fechamento das issues entregues =="
DELIVERED=(1 2 3 4 5 8 10)

for issue in "${DELIVERED[@]}"; do
  state="$(gh issue view "$issue" --repo "$REPO" --json state -q '.state')"
  if [[ "$state" == "CLOSED" ]]; then
    echo "  #$issue ja esta fechada"
  else
    echo "  fechando #$issue"
    run gh issue close "$issue" --repo "$REPO" --reason completed
  fi
done

echo
echo "Concluido. Confira com: gh issue list --repo $REPO"
echo "As issues 6, 7 e 9 seguem abertas ate o job de analise estatica ficar verde."
