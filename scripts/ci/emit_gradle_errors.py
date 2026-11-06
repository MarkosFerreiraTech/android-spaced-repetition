#!/usr/bin/env python3
"""Publica como anotacoes as linhas relevantes da saida do Gradle.

Necessario porque os logs de job do GitHub Actions nao sao acessiveis por todos
os clientes (ficam em storage externo), enquanto as anotacoes de check-run vem
pela API do GitHub. Assim, uma falha de build fica legivel direto no PR.

Uso: python3 scripts/ci/emit_gradle_errors.py <arquivo-de-log>
"""
import re
import sys

MAX_LINES = 25
KEYWORDS = (
    "error:",
    "Caused by:",
    "What went wrong",
    "Execution failed for task",
    "> Could not",
    "> Failed to",
    "FAILURE:",
    "Unsupported class file",
    "Could not resolve",
    "Could not find",
    "No matching",
    "A problem occurred",
)


def main() -> int:
    if len(sys.argv) < 2:
        print("::warning::Nenhum log informado para analise.")
        return 0
    try:
        with open(sys.argv[1], encoding="utf-8", errors="replace") as handle:
            lines = [line.rstrip("\n") for line in handle]
    except FileNotFoundError:
        print(f"::warning::Log nao encontrado: {sys.argv[1]}")
        return 0

    relevant = []
    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue
        if any(keyword.lower() in stripped.lower() for keyword in KEYWORDS):
            relevant.append(stripped)

    for line in relevant[:MAX_LINES]:
        safe = re.sub(r"::", ":", line)[:400]
        print(f"::error::{safe}")

    if not relevant:
        tail = [line.strip() for line in lines if line.strip()][-MAX_LINES:]
        for line in tail:
            safe = re.sub(r"::", ":", line)[:400]
            print(f"::error::{safe}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
