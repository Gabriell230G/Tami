"""relatorio.py · junta os resultados das conversas simuladas num relatório e nos dados da aba do painel.

    python3 ferramentas/simulados/relatorio.py

Lê dados/simulados/resumo_{treino,prova}{_base,}.json e os feedbacks, e grava:
  dados/simulados/relatorio_testes.md   ➜ o relatório
  dados/simulados/painel.json           ➜ o que a aba "Conversas simuladas" do painel mostra
"""
import json
import os
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gerar_casos import SAIDA  # noqa: E402

NOME_PROBLEMA = {
    "seguranca:sobretriagem": "Mais cuidadosa que o necessário (destino mais urgente que o esperado)",
    "seguranca:subtriagem_leve": "Mandou para um lugar mais leve (UBS ➜ casa)",
    "seguranca:subtriagem_critica": "Caso urgente mandado para UBS ou casa",
    "muitas_perguntas": "Perguntas demais para o caso",
    "irrelevante:outra_parte_do_corpo": "Lista com item de outra parte do corpo",
    "irrelevante:trauma_sem_machucado": "Item de \"depois de queda ou pancada\" para quem não se machucou",
    "irrelevante:grupo:cirurgia": "Item de cirurgia para quem não operou",
    "irrelevante:grupo:gravidez": "Item de gravidez ou parto para quem não está grávida",
    "irrelevante:grupo:quimioterapia": "Item de câncer para quem não tem",
    "irrelevante:grupo:imunossuprimido": "Item de transplante ou HIV para quem não tem",
    "irrelevante:grupo:diabetico": "Item de diabetes para quem não tem",
    "irrelevante:grupo:crianca": "Item de criança ou bebê para adulto",
    "irrelevante:grupo:idoso": "Item de idoso para quem não é idoso",
    "irrelevante:grupo:anticoagulado": "Item de remédio para afinar o sangue para quem não toma",
    "irrelevante:grupo:osteoporose": "Item de osteoporose para quem não tem",
    "irrelevante:grupo:renal": "Item de diálise para quem não faz",
    "irrelevante:grupo:colesterol": "Item de remédio do colesterol para quem não toma",
    "irrelevante:pergunta_FIL_06": "Pergunta de alergia com inchaço sem motivo no relato",
    "irrelevante:pergunta_FIL_07": "Pergunta de veneno ou remédio demais sem motivo no relato",
    "irrelevante:pergunta_FIL_02": "Pergunta de dor no peito num machucado de braço ou perna",
    "irrelevante:pergunta_FIL_08": "Pergunta de se machucar de propósito num machucado comum",
    "irrelevante:pergunta_FIL_04": "Pergunta de AVC num machucado de braço ou perna",
    "irrelevante:pergunta_GEN_01": "Pergunta de confusão e sonolência num machucado de braço ou perna",
    "orientacao:upa_sem_enquanto_isso": "Resultado de UPA sem o que fazer até chegar lá",
    "cuidados:faltou": "Resultado de UBS ou casa sem nenhum cuidado",
    "rota:machucado_fora_da_trilha": "Machucado que não entrou na trilha de machucado",
    "faltou:beber_e_xixi": "Vômito ou diarreia sem perguntar se bebe líquido e faz xixi",
    "faltou:mecanismo": "Machucado sem perguntar como aconteceu",
    "faltou:apoiar_mexer": "Machucado sem perguntar se consegue apoiar ou mexer",
    "faltou:dor_0_10": "Sem perguntar a dor de 0 a 10",
    "faltou:tempo": "Sem perguntar há quanto tempo",
    "repetida": "Pergunta repetida",
}
CAUSA = {
    "irrelevante:grupo:cirurgia": "A base tem 215 sinais de pós-operatório e a busca por palavras parecidas trazia todos para quem tinha febre ou dor.",
    "irrelevante:pergunta_FIL_06": "O filtro geral fazia as 8 perguntas de emergência para todo mundo.",
    "irrelevante:pergunta_FIL_07": "O filtro geral fazia as 8 perguntas de emergência para todo mundo.",
    "irrelevante:outra_parte_do_corpo": "A lista não sabia onde era o problema quando a pessoa não tocava no corpo.",
    "irrelevante:trauma_sem_machucado": "A lista mostrava sinais de \"depois de queda\" para quem não caiu.",
    "orientacao:upa_sem_enquanto_isso": "Só alguns caminhos de UPA tinham o bloco do que fazer até chegar.",
    "cuidados:faltou": "Resultados que vinham direto de um sinal da base não passavam pela decisão que junta os cuidados.",
    "muitas_perguntas": "O caminho genérico tem 8 perguntas de emergência e 6 gerais, além da lista.",
    "faltou:beber_e_xixi": "Não existia pergunta sobre beber líquido e fazer xixi no caminho genérico.",
}


def ler(nome):
    p = os.path.join(SAIDA, nome)
    return json.load(open(p, encoding="utf-8")) if os.path.exists(p) else None


def pct(p):
    return f"{max(0.0, 100 * p):.1f}%".replace(".", ",")


def exemplos_de(problema, feedbacks, n=3):
    """Exemplos em que o detalhe mostrado é o do próprio problema."""
    import re
    from simular import GRUPO_ITEM, NOME_CONCEITO
    from gerar_casos import norm
    out = []
    for f in feedbacks:
        if problema not in f.get("problemas", []):
            continue
        det = None
        if problema.startswith("irrelevante:pergunta_"):
            no = problema.split("_", 1)[1]
            det = next((i for i in f["irrelevantes"] if i.startswith(no + ":")), None)
        elif problema.startswith("irrelevante:grupo:"):
            rx = dict(GRUPO_ITEM)[problema.split(":")[2]]
            det = next((i for i in f["irrelevantes"] if re.search(rx, norm(i.split(": ", 1)[-1]))), None)
        elif problema.startswith("irrelevante:"):
            det = next((i for i in f["irrelevantes"] if i.startswith("CONF")), None)
        elif problema.startswith("faltou:"):
            det = "faltou perguntar " + NOME_CONCEITO.get(problema.split(":")[1], "")
        elif problema.startswith("seguranca:"):
            det = f"esperado {f['destino_esperado']}, a Tami deu {f.get('destino_tami')}"
        elif problema == "muitas_perguntas":
            det = f"{f['perguntas_feitas']} perguntas"
        if det or not problema.startswith("irrelevante"):
            out.append(dict(relato=f["relato"], detalhe=[det] if det else [], comentario=f["comentario"]))
        if len(out) >= n:
            break
    return out


def linha(nome, a, b, chave, fmt=pct, menor_melhor=False):
    va, vb = a[chave], b[chave]
    va = va["p"] if isinstance(va, dict) else va
    vb = vb["p"] if isinstance(vb, dict) else vb
    return f"| {nome} | {fmt(va)} | {fmt(vb)} |"


def tabela_geral(a, b):
    n = lambda v: f"{v:.2f}".replace(".", ",")
    ic = lambda d: f" ({pct(d['ic95'][0])} a {pct(d['ic95'][1])})"
    rows = ["| Medida | Antes | Depois |", "| --- | --- | --- |"]
    for nome, k in (("Destino exato", "destino_exato"), ("Urgentes que foram para UPA ou SAMU", "sensibilidade_urgente"),
                    ("Urgente mandado para UBS ou casa", "subtriagem_critica"), ("Conversas com pergunta ou item sem relação", "com_irrelevante"),
                    ("Conversas em que faltou uma pergunta importante", "com_falta"), ("UBS ou casa sem nenhum cuidado no fim", "sem_cuidado")):
        rows.append(f"| {nome} | {pct(a[k]['p'])}{ic(a[k])} | {pct(b[k]['p'])}{ic(b[k])} |")
    rows.append(f"| Perguntas por conversa (média) | {n(a['perguntas_media'])} | {n(b['perguntas_media'])} |")
    rows.append(f"| Nota de uma pessoa real (0 a 10) | {n(a['nota_media'])} | {n(b['nota_media'])} |")
    return "\n".join(rows)


def main():
    tb, tf, pb, pf = ler("resumo_treino_base.json"), ler("resumo_treino.json"), ler("resumo_prova_base.json"), ler("resumo_prova.json")
    fb_base = [json.loads(l) for l in open(os.path.join(SAIDA, "feedback_treino_base.jsonl"), encoding="utf-8")]
    out = []
    out.append("# Testes simulados da Tami: 12.000 conversas com feedback\n")
    out.append("Cada caso é uma pessoa simulada com uma ficha escondida (idade, o que aconteceu, a dor, os sinais que tem). "
               "Ela conversa com o motor de verdade e responde com sinceridade, às vezes \"Não sei\" e às vezes se confunde. "
               "Cada conversa recebe um feedback: perguntas sem relação, perguntas que faltaram, cuidados no fim, uma nota de 0 a 10 e um comentário.\n")
    out.append("> O destino esperado de cada caso vem de um sinal da base de conhecimento, escrito a partir de fontes públicas "
               "(NHS, MedlinePlus, Ministério da Saúde), e **ainda precisa de revisão médica**. Os números medem se a conversa faz sentido "
               "e se o motor segue as regras, não se a orientação é clinicamente correta.\n")
    out.append("## 1. Prova final (2.000 casos que não foram olhados durante as correções)\n")
    out.append(tabela_geral(pb, pf) + "\n")
    out.append("Intervalos de confiança de 95% pelo método de Wilson.\n")
    out.append("## 2. Casos de treino (10.000)\n")
    out.append(tabela_geral(tb, tf) + "\n")
    out.append("## 3. Os 20 problemas mais comuns antes das correções\n")
    agora = {p["problema"]: p["casos"] for p in tf["problemas"]}
    todos_agora = Counter(p for l in open(os.path.join(SAIDA, "feedback_treino.jsonl"), encoding="utf-8") for p in json.loads(l).get("problemas", []))
    for i, p in enumerate(tb["problemas"][:20], 1):
        nome = NOME_PROBLEMA.get(p["problema"], p["problema"])
        depois = todos_agora.get(p["problema"], 0)
        fams = ", ".join(f"{f} ({c})" for f, c in p["familias"])
        out.append(f"### {i}. {nome}\n")
        out.append(f"**{p['casos']} conversas antes ➜ {depois} depois.** Famílias mais atingidas: {fams}.\n")
        if p["problema"] in CAUSA:
            out.append(f"Causa: {CAUSA[p['problema']]}\n")
        for e in exemplos_de(p["problema"], fb_base):
            det = f" ➜ {e['detalhe'][0]}" if e["detalhe"] else ""
            out.append(f"- \"{e['relato'][:110]}\"{det}")
        out.append("")
    out.append("## 4. Por família (treino, depois das correções)\n")
    out.append("| Família | Casos | Nota antes | Nota depois | Perguntas depois | Destino exato depois | Problema mais comum depois |")
    out.append("| --- | --- | --- | --- | --- | --- | --- |")
    for fam, v in tf["por_familia"].items():
        a = tb["por_familia"].get(fam, {})
        prob = NOME_PROBLEMA.get(v["problemas"][0][0], v["problemas"][0][0]) if v["problemas"] else "-"
        out.append(f"| {fam} | {v['casos']} | {str(a.get('nota', '-')).replace('.', ',')} | {str(v['nota']).replace('.', ',')} | "
                   f"{str(v['perguntas']).replace('.', ',')} | {pct(v['destino_exato'])} | {prob} |")
    out.append("")
    out.append("## 5. O que ainda falta (de propósito ou para a próxima rodada)\n")
    out.append("- **Itens graves continuam na lista mesmo quando podem não ter relação.** Exemplo: lábio inchando de repente aparece para quem tem dor de estômago. "
               "A regra é: item leve sem relação sai; item grave só sai se for de outra parte do corpo e o relato não descrever nada dele. Isso segura a segurança, mas mantém parte dos itens \"estranhos\".")
    out.append("- **O caminho genérico ainda é longo** (8 perguntas de emergência e 6 gerais). O próximo passo é juntar as perguntas de emergência numa tela só, com uma lista para marcar.")
    out.append("- **\"Não sei\" em pergunta de emergência vale como \"sim\"** e manda para o SAMU. É uma regra de segurança do motor (M02): fica, mas aumenta a sobretriagem.")
    out.append("- **Trilhas próprias** (como a do machucado) ainda faltam para barriga, febre em criança e falta de ar. Elas cortariam o caminho genérico para esses casos.")
    out.append("- O avaliador é automático: ele usa palavras para decidir se um item é de outra parte do corpo ou de outro grupo, e às vezes erra. Os números servem para comparar antes e depois.\n")
    out.append("## Arquivos\n")
    out.append("- `casos_simulados.csv` e `casos_simulados.jsonl`: os 12.000 casos, com relato, ficha e fonte")
    out.append("- `feedback_treino.jsonl` e `feedback_prova.jsonl`: uma linha por conversa (os `_base` são de antes das correções)")
    out.append("- `conversas_*.jsonl`: cada conversa inteira, pergunta por pergunta")
    out.append("- `CHANGELOG_SIMULADOS.md`: cada correção, com os números antes e depois")
    with open(os.path.join(SAIDA, "relatorio_testes.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")

    # aba do painel
    fb_prova = [json.loads(l) for l in open(os.path.join(SAIDA, "feedback_prova.jsonl"), encoding="utf-8")]
    probs = Counter(p for f in fb_prova for p in f.get("problemas", []) if not p.startswith("seguranca:sobretriagem"))
    exemplos = [dict(relato=f["relato"][:120], comentario=f["comentario"], nota=f["nota_pessoa_real"]) for f in fb_prova if f["nota_pessoa_real"] <= 6][:5]
    bons = [dict(relato=f["relato"][:120], comentario=f["comentario"], nota=f["nota_pessoa_real"]) for f in fb_prova if f["nota_pessoa_real"] == 10][:3]
    painel = dict(
        casos=12000, prova=2000,
        antes={k: pb[k] for k in ("destino_exato", "sensibilidade_urgente", "subtriagem_critica", "com_irrelevante", "com_falta", "sem_cuidado", "nota_media", "perguntas_media")},
        depois={k: pf[k] for k in ("destino_exato", "sensibilidade_urgente", "subtriagem_critica", "com_irrelevante", "com_falta", "sem_cuidado", "nota_media", "perguntas_media")},
        problemas=[dict(problema=NOME_PROBLEMA.get(p, p), casos=c) for p, c in probs.most_common(5)],
        por_familia=[dict(familia=k, nota_antes=tb["por_familia"].get(k, {}).get("nota"), nota=v["nota"], perguntas=v["perguntas"]) for k, v in tf["por_familia"].items()],
        exemplos=exemplos, bons=bons,
    )
    with open(os.path.join(SAIDA, "painel.json"), "w", encoding="utf-8") as f:
        json.dump(painel, f, ensure_ascii=False, indent=1)
    print("relatorio_testes.md e painel.json gravados")


if __name__ == "__main__":
    main()
