#!/usr/bin/env python3
"""Publica como anotacoes os testes que falharam.

Le os relatorios JUnit XML gerados pelo Gradle e emite, para cada teste
falho, uma anotacao com classe, teste e causa.

Uso: python3 scripts/ci/test_report.py [diretorio-de-resultados]
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

DEFAULT_DIR = "build/test-results/testDebugUnitTest"
MAX_ANNOTATIONS = 40


def main() -> int:
    directory = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DIR
    files = glob.glob(os.path.join(directory, "**", "TEST-*.xml"), recursive=True)
    if not files:
        print(f"::warning::Nenhum relatorio de teste encontrado em {directory}")
        return 0

    failures = 0
    emitted = 0
    for path in sorted(files):
        root = ET.parse(path).getroot()
        for case in root.findall("testcase"):
            failure = case.find("failure")
            error = case.find("error")
            node = failure if failure is not None else error
            if node is None:
                continue
            failures += 1
            if emitted >= MAX_ANNOTATIONS:
                continue
            emitted += 1
            class_name = case.get("classname", "?").replace(".", "/") + ".java"
            test_name = case.get("name", "?")
            # Rearranja a mensagem para que a PARTE ACIONAVEL venha primeiro.
            # Uma violacao de arquitetura lista os nomes das classes antes de
            # dizer o que esta errado; truncar no comeco entrega so o titulo da
            # regra e nenhuma pista de qual classe violou.
            raw = (node.get("message") or "falhou").replace("\n", " ")
            message = _focus(raw)
            print(f"::error file=src/test/java/{class_name}::{test_name} -> {message}")

    if failures == 0:
        print("OK: nenhum teste falhou.")
        return 0
    print(f"::error::{failures} teste(s) falharam.")
    return 1


def _focus(message):
    """Encurta a mensagem preservando a parte que identifica a violacao.

    Para violacoes do ArchUnit, as classes envolvidas aparecem no fim da
    mensagem; para os demais casos, o inicio e o mais informativo.
    """
    match = re.search(r"was violated.*", message)
    if match:
        detail = match.group(0)
        # "was violated (N times):" carrega o essencial; descarta os prefixos.
        return detail[:900]
    return message[:600]


if __name__ == "__main__":
    sys.exit(main())
