"""reavaliar.py · dá a nota de novo para conversas já gravadas, com a régua nova (simular.py) ou a antiga (avaliador_v1.py).

    python3 ferramentas/simulados/reavaliar.py conversas_prova.jsonl [v1|v2]
"""
import importlib
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gerar_casos import SAIDA  # noqa: E402

arq, versao = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "v2")
m = importlib.import_module("avaliador_v1" if versao == "v1" else "simular")
casos = {json.loads(l)["caso"]: json.loads(l) for l in open(os.path.join(SAIDA, "casos_simulados.jsonl"), encoding="utf-8")}
convs = [json.loads(l) for l in open(os.path.join(SAIDA, arq), encoding="utf-8")]
fbs = [m.avaliar(casos[c["caso"]], c) for c in convs]
res = m.resumir(fbs, convs)
print(json.dumps({k: (v["p"] if isinstance(v, dict) and "p" in v else v) for k, v in res.items() if k not in ("problemas", "por_familia")}, ensure_ascii=False))
