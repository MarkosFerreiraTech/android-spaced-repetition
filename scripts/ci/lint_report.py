#!/usr/bin/env python3
"""Converte o relatorio XML do Android Lint em anotacoes legiveis no GitHub Actions.

Motivacao: quando o gate de lint falha, o log bruto do Gradle fica extenso e de
dificil leitura no PR. Este script extrai apenas os problemas com severidade
Error/Fatal e os publica como anotacoes de arquivo e linha.

Uso: python3 scripts/ci/lint_report.py [caminho-do-xml]
"""
import os
import sys
import xml.etree.ElementTree as ET

DEFAULT_PATH = "build/reports/lint-results-debug.xml"
MAX_ANNOTATIONS = 50


def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PATH
    if not os.path.exists(path):
        print(f"::warning::Relatorio de lint nao encontrado em {path}")
        return 0

    root = ET.parse(path).getroot()
    blocking = 0
    emitted = 0

    for issue in root.findall("issue"):
        severity = issue.get("severity", "")
        if severity not in ("Error", "Fatal"):
            continue
        blocking += 1
        if emitted >= MAX_ANNOTATIONS:
            continue
        emitted += 1
        message = (issue.get("message") or "").replace("\n", " ")
        issue_id = issue.get("id", "Lint")
        location = issue.find("location")
        file_name = location.get("file", "?") if location is not None else "?"
        line = location.get("line", "1") if location is not None else "1"
        print(f"::error file={file_name},line={line}::{issue_id}: {message}")

    if blocking == 0:
        print("OK: nenhum problema de lint com severidade Error/Fatal.")
        return 0

    if blocking > emitted:
        print(f"::error::... e mais {blocking - emitted} problemas de lint nao exibidos.")
    print(f"::error::Android Lint encontrou {blocking} problema(s) bloqueante(s).")
    return 1


if __name__ == "__main__":
    sys.exit(main())
