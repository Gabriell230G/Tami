"""simular.py · roda os casos simulados no motor da Tami e dá feedback de cada conversa.

    python3 ferramentas/simulados/simular.py [treino|prova|todos] [limite]

Precisa do app compilado (scripts/compilar.sh): conversa com o motor pela classe br.com.fiap.tami.teste.Ponte,
sem banco e sem internet. A pessoa simulada responde com sinceridade a partir da ficha escondida do caso,
com 8% de "Não sei" quando a ficha não diz nada e 3% de erro de leitura (escolhe a opção vizinha).

Saída em dados/simulados/: conversas_<conjunto>.jsonl, feedback_<conjunto>.jsonl e resumo_<conjunto>.json.
"""
import json
import math
import os
import random
import re
import subprocess
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gerar_casos import CONC_FIL, CONCEITOS, MULTI_FEB, ORDEM, RAIZ, SAIDA, carregar_base, norm  # noqa: E402

BASE = {it["id"]: it for it in carregar_base()}
NOME = {"SAMU_192": "SAMU", "UPA_HOJE": "UPA", "UBS_EM_BREVE": "UBS", "CUIDADO_EM_CASA": "casa"}


# ---------------------------------------------------------------- a ponte com o motor
class Motor:
    def __init__(self):
        sep = ";" if os.name == "nt" else ":"
        raiz = os.environ.get("TAMI_RAIZ", RAIZ)   # para comparar com outra versão do app
        cp = sep.join([os.path.join(raiz, "build", "classes"), os.path.join(raiz, "lib", "*")])
        self.p = subprocess.Popen(["java", "-cp", cp, "br.com.fiap.tami.teste.Ponte"], cwd=RAIZ, stdin=subprocess.PIPE,
                                  stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, encoding="utf-8", bufsize=1)

    def pedir(self, d):
        self.p.stdin.write(json.dumps(d, ensure_ascii=False) + "\n")
        self.p.stdin.flush()
        return json.loads(self.p.stdout.readline())

    def fechar(self):
        self.p.stdin.close()
        self.p.wait()


# ---------------------------------------------------------------- a pessoa simulada
TRAUMA_ITEM = re.compile(r"\b(depois (de|da|do|que)|apos|durante) (uma |um |a |o )?(pancada|queda|torc\w*|batida|bater|lesao|acidente|machuc\w*|caiu|cair|tombo|chute|tranco|mergulh\w*)|\bcaiu\b|\bbateu\b|pancada")


def parecido(a, b):
    pa = {w for w in norm(a).split() if len(w) > 3}
    pb = {w for w in norm(b).split() if len(w) > 3}
    return len(pa & pb) / max(1, len(pa | pb))


def responder(ficha, q, r):
    """Como uma pessoa sincera responderia, olhando só a própria ficha."""
    nid, ops = q["id"], q["opcoes"]
    cods = [o["codigo"] for o in ops]
    t = ficha["texto_sinal"] + " " + " ".join(norm(BASE[s]["desc"]) for s in ficha["sinais_presentes"] if s in BASE)
    tem = lambda rx: re.search(rx, t) is not None
    negativa = next((c for c in ["nenhum", "nenhuma", "N", "normal"] if c in cods), None)
    informado = True

    if nid.startswith("CONF_SINAIS") or nid == "CONF_DESMARCOU":
        marcar = [o["codigo"] for o in ops if o["codigo"] in ficha["sinais_presentes"]
                  or (o["codigo"] in BASE and parecido(o["rotulo"], ficha["descricao_sinal"]) >= 0.5)]
        resp = marcar or ["nenhum"]
        informado = bool(marcar)
    elif nid == "FIL_EM":   # tela de emergência: cada item responde como a pergunta antiga
        resp = []
        for c, fid in (("ar", "FIL_01"), ("peito", "FIL_02"), ("avc", "FIL_04"), ("sangue", "FIL_05")):
            if c in cods and tem(CONC_FIL[fid]):
                resp.append(c)
        if tem(CONC_FIL["FIL_03"]):
            c = "apagou_agora" if tem(r"agora|nao acorda|desacord|convulsionando|nao responde") else "apagou_passou"
            if c in cods:
                resp.append(c)
        informado = bool(resp)
        resp = resp or ["nenhum"]
    elif nid == "ORI_01":
        resp = ["machucou" if ficha["lesao"] else "sozinho"]
    elif nid == "LES_01":
        resp = [ficha["mecanismo"] or "pancada"]
    elif nid in ("URI_01", "PEL_01", "BAR_01"):   # trilhas: o item aponta para um sinal da base
        descs = [norm(BASE[s]["desc"]) for s in ficha["sinais_presentes"] if s in BASE] + [norm(ficha["descricao_sinal"])]
        resp = []
        for o in ops:
            alvo = [e.split(":", 1)[1] for e in o.get("efeitos", []) if e.startswith("SINAL:")]
            if alvo and (alvo[0] in ficha["sinais_presentes"] or any(parecido(o["rotulo"], d) >= 0.4 for d in descs)):
                resp.append(o["codigo"])
        informado = bool(resp)
        resp = resp or ["nenhum"]
    elif nid in ("LES_02", "LES_03"):
        resp = [c for c in cods if c in ficha["conceitos"]] or ["nenhum"]
        informado = resp != ["nenhum"]
    elif nid == "GEN_02":
        resp = [str(max(0, min(10, ficha["dor"] + r.choice([-1, 0, 0, 1]))))]
    elif nid == "BEB_01":
        resp = ["N" if tem(r"vomita tudo|nao consegue beber|nao para nada|quase nao (faz|fez) xixi|sem fazer xixi|desidrat|boca seca") else "S"]
    elif nid == "GEN_06":
        resp = [ficha["tempo"]]
    elif nid == "LES_04":
        resp = [ficha["evolucao"]]
    elif nid == "GEN_05":
        resp = ["S" if [f for f in ficha["fatores"] if f not in ("homem", "mulher", "hipertenso")] or ficha["doencas"] else "N"]
    elif nid == "FEB_A06":
        resp = [{"hoje": "1_2", "1_7_dias": r.choice(["3_4", "5_7"]), "1_4_semanas": "mais_7", "mais_1_mes": "mais_7"}[ficha["tempo"]]]
    elif nid == "FEB_A10":
        mapa = {"diabetico": "diabetes", "renal": "rins", "anticoagulado": "afinar_sangue", "hipertenso": "pressao"}
        resp = [mapa[f] for f in ficha["fatores"] if f in mapa and mapa[f] in cods] or ["nenhuma"]
    elif nid == "FIL_C02":
        resp = ["37_5_mais" if tem(r"febre") else "normal"]
    elif nid == "PEL_03":
        resp = ["nao_somem" if tem(r"pintinha|manchas? (vermelh|roxa)|petequi") and ficha["destino_esperado"] in ("UPA_HOJE", "SAMU_192") else "somem"]
    elif nid == "FIL_G01":
        resp = ["forte" if tem(r"sangramento (forte|muito)|muito sangue") else "leve_ou_liquido" if tem(r"sangr|liquido|bolsa") else "N"]
    elif nid == "FIL_03":
        resp = ["agora" if tem(CONC_FIL["FIL_03"]) else "N"]
    elif nid == "INT_01":
        resp = ["de_proposito" if tem(r"proposito|tentou|tentativa") else "S" if tem(CONC_FIL["INT_01"]) else "N"]
    elif nid in MULTI_FEB:
        resp = [c for c, rx in MULTI_FEB[nid].items() if c in cods and tem(rx)] or [negativa or cods[-1]]
        informado = resp != [negativa]
    elif nid in CONC_FIL:
        pos = next((o["codigo"] for o in ops if o["gravidade"] > 0 and o["codigo"] != "nao_sei"), cods[0])
        if nid in ("FEB_A03", "FEB_A07", "FEB_A08"):
            pos = {"FEB_A03": "S", "FEB_A07": "baixou", "FEB_A08": "S"}[nid]
        sim = tem(CONC_FIL[nid])
        resp = [pos if sim else (negativa or "N")]
        informado = sim
    else:
        resp = [negativa or cods[0]]
        informado = False

    # erro humano: "Não sei" quando a ficha não diz nada, ou a opção vizinha por engano
    # (nas perguntas de emergência a pessoa sabe se tem ou não: lá o "Não sei" vale como "sim" e manda para o SAMU)
    if not informado and not q["alarme"] and "nao_sei" in cods and r.random() < 0.08:
        resp = ["nao_sei"]
    elif not q["multipla"] and r.random() < 0.03 and len(cods) > 1:
        i = cods.index(resp[0]) if resp[0] in cods else 0
        resp = [cods[min(len(cods) - 1, max(0, i + r.choice([-1, 1])))]]
    return [c for c in resp if c in cods] or [cods[0]]


def conversar(motor, ficha, r):
    d = motor.pedir(dict(op="iniciar", idadeMeses=ficha["idade_meses"], gestante=ficha["gestante"], diabetes=ficha["diabetes"],
                         fatores=ficha["fatores"], texto=ficha["relato"], regiao=ficha["regiao"]))
    turnos = []
    while "erro" not in d and not d["fim"] and len(turnos) < 40:
        q = d["pergunta"]
        resp = responder(ficha, q, r)
        turnos.append(dict(no=q["id"], texto=q["texto"], opcoes=[(o["codigo"], o["rotulo"]) for o in q["opcoes"]], resposta=resp))
        d = motor.pedir(dict(op="responder", sessao=d["sessao"], no=q["id"], codigos=resp))
    if "erro" in d:
        return dict(caso=ficha["caso"], erro=d["erro"], turnos=turnos)
    return dict(caso=ficha["caso"], queixa=d["queixa"], destino=d["destino"], blocos=[b["codigo"] for b in d["blocos"]],
                textos_blocos=[b["texto"] for b in d["blocos"]], historico=d["historico"], turnos=turnos, crise=d["crise"])


# ---------------------------------------------------------------- feedback de cada conversa
PALAVRAS_REGIAO = {
    "cabeca": r"cabeca|rosto|olho|boca|dente|ouvido|nariz|garganta|pescoco|nuca|testa|mandibula|queixo|gengiva|lingua|labio",
    "peito": r"peito|costela|coracao|pulmao|mama|seio",
    "barriga": r"barriga|estomago|umbigo|virilha|intestino|vagin|bexiga|abdome",
    "costas": r"costas|lombar|coluna|bumbum|quadril|nadega|coccix",
    "bracos": r"braco|ombro|cotovelo|antebraco|\bmao\b|maos|punho|dedo da mao|clavicula",
    "pernas": r"perna|coxa|joelho|canela|panturrilha|batata|tornozelo|\bpe\b|\bpes\b|calcanhar|dedo do pe",
}
GRUPO_ITEM = [
    ("gravidez", r"gravid|gestant|gestacao|parto|amament|puerper|pos parto|bebe mex"),
    ("idoso", r"^(idos[oa]|pessoa idosa)|\bem idos[oa]"),
    ("crianca", r"^(bebe|crianca)"),
    ("anticoagulado", r"afinar o sangue|anticoag"),
    ("quimioterapia", r"cancer|quimio"),
    ("colesterol", r"colesterol"),
    ("diabetico", r"diabet|insulina"),
    ("imunossuprimido", r"transplant|\bhiv\b|imunidade baixa"),
    ("renal", r"dialise"),
    ("osteoporose", r"osteoporose"),
    ("cirurgia", r"cirurgi|operad|protese|placa e parafusos|haste de metal|pos operatorio"),
]
NOME_GRUPO = {"gravidez": "gravidez", "idoso": "idoso", "crianca": "criança ou bebê", "anticoagulado": "remédio para afinar o sangue",
              "quimioterapia": "câncer", "colesterol": "colesterol", "diabetico": "diabetes", "imunossuprimido": "transplante ou HIV",
              "renal": "diálise", "osteoporose": "osteoporose", "cirurgia": "cirurgia"}
PERGUNTA_TEMA = {"FIL_01": "falta de ar", "FIL_02": "dor no peito", "FIL_04": "AVC", "FIL_06": "alergia com inchaço",
                 "FIL_07": "veneno ou remédio demais", "FIL_08": "pensar em se machucar", "GEN_04": "febre", "GEN_01": "confusão e sonolência",
                 "FIL_03": "desmaio ou convulsão"}
SATISFAZ = {
    "mecanismo": {"LES_01", "ORI_01"}, "sinais_graves_lesao": {"LES_02", "FIL_05"}, "apoiar_mexer": {"LES_03"},
    "dor_0_10": {"GEN_02"}, "tempo": {"GEN_06", "FEB_A06"}, "de_repente_ou_pior": {"GEN_03"}, "sinais_avc": {"FIL_04"},
    "dias_de_febre": {"FEB_A06", "GEN_06"}, "sinais_alerta_febre": {"FEB_A02", "FEB_A05"}, "grupo_de_risco": {"GEN_05", "FEB_A10"},
    "beber_e_xixi": {"FIL_C01", "BEB_01"}, "febre": {"GEN_04", "FEB_A02"}, "falta_de_ar_grave": {"FIL_01", "FAR_01"},
    "peito": {"FIL_02", "FAR_03", "DPE_01"}, "espalhando": {"GEN_03"}, "sangramento_gestacao": {"FIL_G01"},
    "sinais_gestacao": {"FIL_G02"}, "bebe_mexendo": {"FIL_G03"}, "mamar_beber": {"FIL_C01"}, "risco_agora": {"FIL_08", "CRI_01"},
    "o_que_engoliu_ou_bicho": set(), "sinais_graves_intox": {"INT_01", "FIL_03"},
}
NOME_CONCEITO = {
    "mecanismo": "como se machucou", "sinais_graves_lesao": "sinais graves do machucado", "apoiar_mexer": "se consegue apoiar ou mexer",
    "dor_0_10": "a dor de 0 a 10", "tempo": "há quanto tempo", "de_repente_ou_pior": "se começou de repente ou está piorando",
    "sinais_avc": "sinais de AVC", "dias_de_febre": "há quantos dias tem febre", "sinais_alerta_febre": "sinais de alerta da febre",
    "grupo_de_risco": "se tem alguma doença", "beber_e_xixi": "se está conseguindo beber líquido e fazer xixi", "febre": "se tem febre",
    "falta_de_ar_grave": "falta de ar forte", "peito": "dor no peito", "espalhando": "se está espalhando ou piorando",
    "sangramento_gestacao": "sangramento na gravidez", "sinais_gestacao": "sinais de pressão alta na gravidez",
    "bebe_mexendo": "se o bebê está mexendo", "mamar_beber": "se está mamando ou bebendo", "risco_agora": "o risco agora",
    "o_que_engoliu_ou_bicho": "o que engoliu ou que bicho foi", "sinais_graves_intox": "sinais graves de intoxicação",
}


def palavras(texto):
    vazias = {"para", "com", "sem", "que", "uma", "dor", "mais", "muito", "depois", "quando", "como", "esta", "ainda", "pelo", "pela", "isso", "dias", "forte"}
    return {w for w in norm(texto).split() if len(w) > 3 and w not in vazias}


def raizes(texto):
    return {w[:5] for w in palavras(texto)}


def grandes(texto):
    t = norm(texto)
    return {g for g, rx in PALAVRAS_REGIAO.items() if re.search(rx, t)}


def avaliar(ficha, conv):
    fb = dict(caso=ficha["caso"], familia=ficha["nome_familia"], relato=ficha["relato"], destino_esperado=ficha["destino_esperado"])
    if "erro" in conv:
        fb.update(erro=conv["erro"], nota_pessoa_real=0, seguranca="erro")
        return fb
    esp, got = ORDEM.index(ficha["destino_esperado"]), ORDEM.index(conv["destino"])
    seg = "ok" if esp == got else "sobretriagem" if got > esp else "subtriagem_critica" if esp >= 2 and got <= 1 else "subtriagem_leve"
    feitas = [t for t in conv["turnos"]]
    nos = [t["no"] for t in feitas]
    respondidos = {h["no"] for h in conv["historico"]}
    lesao_case = ficha["lesao"]
    # auditoria (Passo 1): a região vem do que a pessoa disse ou tocou; a descrição do sinal só entra se não houver nada
    regiao_caso = grandes((ficha["regiao"] or "") + " " + ficha["relato"]) or grandes(ficha["descricao_sinal"])
    rel = norm(ficha["relato"])
    pos_parto = ficha.get("pos_parto") or re.search(r"\bparto\b|ganhei|ganhou|tive (o |a |um |meu )?(bebe|nenem|filh)|teve (o |a |um )?(bebe|nenem|filh)|amament|resguardo|puerper|cesar", rel)
    operou = ficha["cirurgia"] or re.search(r"\b(oper\w*|cirur\w*|cirug\w*|pos op\w*|amigdal\w*|protese|blefaro\w*|lipo\w*|abdomino\w*|bariatric\w*|histeroscop\w*|colonoscop\w*|endoscop\w*|biopsia|cateter\w*|implante|pino|placa|parafuso)", rel)
    graves_plausiveis = []

    irrelevantes, problemas = [], []
    for t in feitas:
        if t["no"].startswith("CONF_SINAIS"):
            for cod, rot in t["opcoes"]:
                if cod in ficha["sinais_presentes"] or cod not in BASE or cod in ("nenhum",):
                    continue
                d = norm(rot)
                motivo = None
                for grupo, rx in GRUPO_ITEM:
                    if grupo == "gravidez" and pos_parto:
                        continue
                    if grupo == "cirurgia" and operou:
                        continue
                    if grupo in ficha["sem_sentido"] and re.search(rx, d):
                        motivo = "grupo:" + grupo
                        break
                if not motivo:   # auditoria: o item repete o que a pessoa escreveu (mesma raiz de palavra), então tem relação
                    ri, rr = raizes(rot), raizes(ficha["relato"])
                    if len(ri & rr) / max(1, min(len(ri), len(rr))) >= 0.34:
                        continue
                if not motivo and not lesao_case and TRAUMA_ITEM.search(d):
                    motivo = "trauma_sem_machucado"
                if not motivo and regiao_caso:
                    gi = grandes(rot)
                    if gi and not (gi & regiao_caso):
                        motivo = "outra_parte_do_corpo"
                grave_item = BASE.get(cod, {}).get("destino") in ("UPA_HOJE", "SAMU_192")
                if motivo and grave_item and not motivo.startswith("grupo") and grandes(rot) & regiao_caso and raizes(rot) & raizes(ficha["relato"]):
                    graves_plausiveis.append(f"{t['no']}: {rot}")   # auditoria: grave e da mesma parte do corpo, um profissional perguntaria
                    continue
                if motivo:
                    irrelevantes.append(f"{t['no']}: {rot}")
                    problemas.append("irrelevante:" + motivo)
        else:
            tema = PERGUNTA_TEMA.get(t["no"])
            ruim = False
            if lesao_case and ficha["familia"] in (1, 2, 3):
                cabeca = "cabeca" in regiao_caso or re.search(r"cabeca", ficha["texto_sinal"])
                ruim = t["no"] in ("FIL_02", "FIL_06", "FIL_07", "FIL_08", "GEN_04") or (t["no"] in ("FIL_04", "GEN_01") and not cabeca)
            elif t["no"] == "FIL_06" and ficha["familia"] not in (7, 8, 14) and not re.search(r"alerg|picad|incha|rosto|labio|garganta", ficha["texto_sinal"] + " " + rel):
                ruim = True
            elif t["no"] == "FIL_07" and ficha["familia"] not in (11, 13, 14, 6) and not re.search(r"remedio|engol|bebeu|produto|gas|fumaca", ficha["texto_sinal"]) \
                    and not re.search(r"vomit|sonolen|confus|desmai|tont|tremor|convuls", rel):   # auditoria: com esses sintomas, perguntar de veneno faz sentido
                ruim = True
            if ruim:
                irrelevantes.append(f"{t['no']}: {tema or t['texto'][:60]}")
                problemas.append(f"irrelevante:pergunta_{t['no']}")

    faltaram = []
    if ORDEM.index(conv["destino"]) <= 1:   # quando já mandou para UPA ou SAMU, menos perguntas é o certo
        for c in ficha["boa_conversa"]:
            alvo = SATISFAZ.get(c, set())
            if not alvo:
                continue
            if c == "grupo_de_risco" and (ficha["grupo"] in ("idoso", "gestante") or ficha["diabetes"]):
                continue
            if c == "febre" and conv["queixa"] == "FEB":
                continue
            if not (alvo & respondidos):
                faltaram.append(NOME_CONCEITO[c])
                problemas.append("faltou:" + c)

    textos = " ".join(conv["textos_blocos"])
    cuidado = "ok"
    if ORDEM.index(conv["destino"]) <= 1 and not any(b.startswith(("BLK-CASA", "BLK-SP", "BLK-HIDRATAR", "BLK-VOLTA")) for b in conv["blocos"]):
        cuidado = "faltou"
        problemas.append("cuidados:faltou")
    elif re.search(r"\b(tome|tomar|dose|comprimido|dipirona|paracetamol|ibuprofeno)\b", norm(textos)):
        cuidado = "inadequado"
        problemas.append("cuidados:cita_remedio")
    if conv["destino"] == "UPA_HOJE" and not any(b.startswith(("BLK-EI", "MATERNIDADE", "DISQUE_INTOX", "CVV188")) for b in conv["blocos"]):
        problemas.append("orientacao:upa_sem_enquanto_isso")

    repetidas = []
    vistos = []
    for t in feitas:
        for v in vistos:
            if parecido(t["texto"], v) >= 0.75 and len(t["texto"]) > 20:
                repetidas.append(t["texto"][:80])
        vistos.append(t["texto"])
    if repetidas:
        problemas.append("repetida")

    ideal = 6 if lesao_case else 3 if ficha["familia"] in (13, 14) else 8
    if len(feitas) > ideal + 4:
        problemas.append("muitas_perguntas")
    if seg in ("subtriagem_critica", "subtriagem_leve", "sobretriagem"):
        problemas.append("seguranca:" + seg)
    if lesao_case and conv["queixa"] != "LES":
        problemas.append("rota:machucado_fora_da_trilha")

    nota = 10 - len(irrelevantes) - len(faltaram) - (2 if cuidado == "faltou" else 0) - (1 if len(feitas) > ideal + 4 else 0)
    nota = 0 if seg == "subtriagem_critica" else max(0, nota)

    frases = []
    if seg == "subtriagem_critica":
        frases.append(f"Era caso de {NOME[ficha['destino_esperado']]} e ela mandou pra {NOME[conv['destino']]}. Isso é perigoso.")
    if lesao_case and "mecanismo" in [p.split(":")[1] for p in problemas if p.startswith("faltou:")]:
        frases.append("Me machuquei e ela nem perguntou como foi.")
    temas = []
    for i in irrelevantes:
        txt = norm(i)
        for grupo, rx in GRUPO_ITEM:
            if re.search(rx, txt.split(": ", 1)[-1]) and NOME_GRUPO[grupo] not in temas:
                temas.append(NOME_GRUPO[grupo])
    for i in irrelevantes:
        no = i.split(":")[0]
        if no in PERGUNTA_TEMA and PERGUNTA_TEMA[no] not in temas:
            temas.append(PERGUNTA_TEMA[no])
    if temas:
        frases.append("Perguntou de " + ", ".join(temas[:3]) + " sem ter nada a ver comigo.")
    elif irrelevantes:
        frases.append(f"Mostrou {len(irrelevantes)} coisa(s) de outra parte do corpo.")
    if faltaram:
        frases.append("Faltou perguntar " + " e ".join(faltaram[:2]) + ".")
    if len(feitas) > ideal + 4:
        frases.append(f"Foram {len(feitas)} perguntas, cansa.")
    if cuidado == "faltou":
        frases.append("No fim não deu nenhuma dica do que fazer em casa.")
    if not frases:
        frases.append("Conversa boa, direto ao ponto.")
    fb.update(destino_tami=conv["destino"], seguranca=seg, perguntas_feitas=len(feitas), perguntas_ideal=ideal,
              irrelevantes=irrelevantes, faltaram=faltaram, repetidas=repetidas, estranhas_para_o_perfil=[i for i, p in zip(irrelevantes, problemas) if p.startswith("irrelevante:grupo")],
              graves_plausiveis=graves_plausiveis, cuidados_no_fim=cuidado, linguagem="ok", nota_pessoa_real=nota, comentario=" ".join(frases),
              problemas=sorted(set(problemas)), queixa=conv["queixa"], perguntas=nos)
    return fb


# ---------------------------------------------------------------- números
def wilson(x, n, z=1.96):
    if n == 0:
        return [0, 0]
    p = x / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    m = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return [round(c - m, 4), round(c + m, 4)]


def prop(x, n):
    return dict(x=x, n=n, p=round(x / n, 4) if n else 0, ic95=wilson(x, n))


def resumir(fbs, conversas):
    ok = [f for f in fbs if "erro" not in f]
    n = len(ok)
    urg = [f for f in ok if f["destino_esperado"] in ("UPA_HOJE", "SAMU_192")]
    perg = sorted(f["perguntas_feitas"] for f in ok)
    prob = Counter(p for f in ok for p in f["problemas"])
    por_fam = defaultdict(list)
    for f in ok:
        por_fam[f["familia"]].append(f)
    exemplos = defaultdict(list)
    for f in ok:
        for p in f["problemas"]:
            if len(exemplos[p]) < 3:
                exemplos[p].append(dict(caso=f["caso"], relato=f["relato"], comentario=f["comentario"],
                                        detalhe=(f["irrelevantes"] + f["faltaram"])[:2]))
    return dict(
        conversas=n, erros=len(fbs) - n,
        destino_exato=prop(sum(f["destino_esperado"] == f["destino_tami"] for f in ok), n),
        sensibilidade_urgente=prop(sum(f["destino_tami"] in ("UPA_HOJE", "SAMU_192") for f in urg), len(urg)),
        subtriagem_critica=prop(sum(f["seguranca"] == "subtriagem_critica" for f in ok), len(urg)),
        perguntas_media=round(sum(perg) / n, 2), perguntas_mediana=perg[n // 2],
        com_irrelevante=prop(sum(bool(f["irrelevantes"]) for f in ok), n),
        com_falta=prop(sum(bool(f["faltaram"]) for f in ok), n),
        sem_cuidado=prop(sum(f["cuidados_no_fim"] == "faltou" for f in ok), sum(f["destino_tami"] in ("UBS_EM_BREVE", "CUIDADO_EM_CASA") for f in ok)),
        nota_media=round(sum(f["nota_pessoa_real"] for f in ok) / n, 2),
        problemas=[dict(problema=p, casos=c, familias=Counter(f["familia"] for f in ok if p in f["problemas"]).most_common(3),
                        exemplos=exemplos[p]) for p, c in prob.most_common(25)],
        por_familia={k: dict(casos=len(v), nota=round(sum(f["nota_pessoa_real"] for f in v) / len(v), 2),
                             perguntas=round(sum(f["perguntas_feitas"] for f in v) / len(v), 1),
                             destino_exato=round(sum(f["destino_esperado"] == f["destino_tami"] for f in v) / len(v), 3),
                             problemas=Counter(p for f in v for p in f["problemas"]).most_common(3)) for k, v in sorted(por_fam.items())},
    )


def main():
    conjunto = sys.argv[1] if len(sys.argv) > 1 else "treino"
    limite = int(sys.argv[2]) if len(sys.argv) > 2 else None
    casos = [json.loads(l) for l in open(os.path.join(SAIDA, "casos_simulados.jsonl"), encoding="utf-8")]
    if conjunto != "todos":
        casos = [c for c in casos if c["conjunto"] == conjunto]
    if limite:
        casos = casos[:limite]
    so = os.environ.get("SIM_IDS")   # para olhar poucos casos: SIM_IDS=SIM-01-00001,SIM-02-00002 (grava em *_debug)
    if so:
        casos = [c for c in casos if c["caso"] in set(so.split(","))]
        conjunto += "_debug"
    jobs = max(1, int(os.environ.get("SIM_JOBS", "2")))
    convs, fbs = [None] * len(casos), [None] * len(casos)

    def lote(k):
        motor = Motor()
        for i in range(k, len(casos), jobs):
            c = casos[i]
            r = random.Random(c["caso"])   # o mesmo caso erra do mesmo jeito em qualquer versão: dá para comparar antes e depois
            convs[i] = conversar(motor, c, r)
            fbs[i] = avaliar(c, convs[i])
        motor.fechar()

    from concurrent.futures import ThreadPoolExecutor
    with ThreadPoolExecutor(jobs) as ex:
        list(ex.map(lote, range(jobs)))
    with open(os.path.join(SAIDA, f"conversas_{conjunto}.jsonl"), "w", encoding="utf-8") as f:
        for c in convs:
            f.write(json.dumps(c, ensure_ascii=False) + "\n")
    with open(os.path.join(SAIDA, f"feedback_{conjunto}.jsonl"), "w", encoding="utf-8") as f:
        for b in fbs:
            f.write(json.dumps(b, ensure_ascii=False) + "\n")
    res = resumir(fbs, convs)
    with open(os.path.join(SAIDA, f"resumo_{conjunto}.json"), "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=1)
    print(json.dumps({k: v for k, v in res.items() if k not in ("problemas", "por_familia")}, ensure_ascii=False))
    for p in res["problemas"][:20]:
        print(f"  {p['casos']:5d}  {p['problema']}")


if __name__ == "__main__":
    main()
