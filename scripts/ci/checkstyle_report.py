#!/usr/bin/env python3
"""Converte o relatorio XML do Checkstyle em anotacoes legiveis no GitHub Actions.

Uso: python3 scripts/ci/checkstyle_report.py [caminho-do-xml]
"""
import os
import sys
import xml.etree.ElementTree as ET

DEFAULT_PATH = "build/reports/checkstyle/main.xml"
MAX_ANNOTATIONS = 50


def _relative(path: str) -> str:
    """Converte o caminho absoluto do runner em caminho relativo ao repositorio."""
    normalizado = path.replace("\\", "/")
    marcador = "/src/"
    indice = normalizado.find(marcador)
    if indice >= 0:
        return "src/" + normalizado[indice + len(marcador):]
    return os.path.basename(normalizado)


def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PATH
    if not os.path.exists(path):
        print(f"::warning::Relatorio do Checkstyle nao encontrado em {path}")
        return 0

    root = ET.parse(path).getroot()
    violations = 0
    emitted = 0

    for file_element in root.findall("file"):
        file_name = file_element.get("name", "?")
        for error in file_element.findall("error"):
            violations += 1
            if emitted >= MAX_ANNOTATIONS:
                continue
            emitted += 1
            line = error.get("line", "1")
            message = (error.get("message") or "").replace("\n", " ")
            source = error.get("source", "").split(".")[-1]
            # A anotacao precisa dizer QUAL arquivo e QUAL linha. Sem isso o
            # GitHub agrupa a mensagem em .github e nao da para localizar a
            # violacao em uma sessao sem acesso ao artefato.
            relativo = _relative(file_name)
            print(f"::error file={relativo},line={line}::{source}: {message}")

    if violations == 0:
        print("OK: nenhuma violacao de Checkstyle.")
        return 0

    if violations > emitted:
        print(f"::error::... e mais {violations - emitted} violacoes nao exibidas.")
    print(f"::error::Checkstyle encontrou {violations} violacao(oes).")
    return 1


if __name__ == "__main__":
    sys.exit(main())
