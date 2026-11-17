"""gerar_casos.py · cria os casos simulados da Tami (relato + ficha escondida).

Cada caso nasce de um sinal ou de uma condição da base de conhecimento (que já tem fonte oficial e o destino),
e ganha uma pessoa: idade, quem escreve, como escreve (com erro, abreviação, gíria), há quanto tempo, a dor,
como aconteceu (se foi machucado) e o que uma boa conversa deveria perguntar.

    python3 ferramentas/simulados/gerar_casos.py [quantos] [semente]

Saída: dados/simulados/casos_simulados.jsonl (um caso por linha) e casos_simulados.csv (resumo para planilha).
O destino esperado vem da base e está marcado como "precisa de revisão médica".
"""
import csv
import json
import os
import random
import re
import sys
import unicodedata

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BASE = os.path.join(RAIZ, "src", "main", "resources", "conhecimento")
SAIDA = os.path.join(RAIZ, "dados", "simulados")
ORDEM = ["CUIDADO_EM_CASA", "UBS_EM_BREVE", "UPA_HOJE", "SAMU_192"]


def norm(s):
    s = unicodedata.normalize("NFD", (s or "").lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9 ]+", " ", s).strip()


def ler(nome):
    with open(os.path.join(BASE, nome), encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter=";"))


# ---------------------------------------------------------------- base: sinais e condições
def carregar_base():
    itens = []
    for s in ler("sinais_alerta.csv"):
        itens.append(dict(id=s["id"], area=s["area"], desc=s["descricao_popular"], termo=s["termo_medico"], destino=s["destino"],
                          vale=s["vale_para"].split("|") if s["vale_para"] else ["todos"],
                          sobe=[x for x in s["sobe_um_nivel_se"].split("|") if x],
                          lexico=[x for x in s["palavras_lexico"].split("|") if x.strip()],
                          fonte=s["fonte_nome"], url=s["fonte_url"], sistema=""))
    for c in ler("condicoes.csv"):
        sint = [x for x in c["sintomas_tipicos"].split("|") if x]
        itens.append(dict(id=c["id"], area="CO", desc=c["nome_popular"].split(",")[0] + ": " + ", ".join(sint[:3]), termo=c["termo_medico"],
                          destino=c["destino_padrao"], vale=["todos"], sobe=[], grupos=[x for x in c["grupos_risco"].split("|") if x],
                          lexico=[x for x in c["palavras_lexico"].split("|") if x.strip()] + sint[:2],
                          fonte=c["fonte_nome"], url=c["fonte_url"], sistema=c["sistema"], regioes=c["regioes_corpo"]))
    return [i for i in itens if i["lexico"]]


# ---------------------------------------------------------------- famílias
FAMILIAS = {
    1: "Machucado no esporte", 2: "Queda", 3: "Pancada, corte e queimadura", 4: "Cabeça e tontura",
    5: "Febre e dengue", 6: "Barriga", 7: "Peito e falta de ar", 8: "Pele", 9: "Urina e região íntima",
    10: "Gravidez e pós-parto", 11: "Bebês e crianças", 12: "Idosos e anticoagulante", 13: "Saúde mental e crise",
    14: "Intoxicação e animais", 15: "Cirurgia, crônicos e 'não é nada'",
}
RX = lambda p: re.compile(p)
F_REGRAS = [
    (13, RX(r"suicid|tirar a propria vida|se machucar|acabar com tudo|ansiedade|panico|crise de choro|depress|vozes|surto|agitad|agressiv|se cortar|automutil")),
    (14, RX(r"intoxic|veneno|engoliu|bebeu|produto de limpeza|remedios? (a mais|demais)|picad|picou|mordid|cobra|escorpi|aranha|lagarta|taturana|abelha|vespa|morcego|cachorro|gato|monoxido|gas |ciatox|agrotox|chumbinho|bateria|ima")),
    (10, RX(r"gravid|gestant|gestacao|parto|amament|bebe (esta )?mex|pre natal|puerper|placenta|semanas de gravidez|resguardo")),
    (15, RX(r"cirurgi|operad|operou|operacao|corte da cirurgia|colonoscop|endoscop|biopsia|pontos|curativo|protese|gesso|dialise|quimio|sonda|bolsa de colostomia|ostomia")),
    (1, RX(r"treino|esporte|corrida|correndo|academia|musculo|tendao|jogo|futebol|atleta|arremess|estiramento|caimbra|fisgada|ligamento|menisco|entorse|torc")),
    (2, RX(r"caiu|queda|tombo|escorreg|caido|cair ")),
    (3, RX(r"pancada|corte|ferida|queimad|bateu|batida|choque|acidente|agredid|mordida|prego|fratur|osso|luxa|desloc")),
    (7, RX(r"peito|falta de ar|respir|tosse|chiado|coracao|palpita|pulmao|asma|bombinha")),
    (4, RX(r"cabeca|tontura|vertigem|desmai|enxaqueca|visao|vista|fala enrolada|boca torta|convuls|confus")),
    (5, RX(r"febre|dengue|calafrio|chikungunya|zika")),
    (6, RX(r"barriga|vomit|diarre|fezes|coco|estomago|azia|enjoo|intestino|abdome|figado|hemorroid")),
    (9, RX(r"urina|xixi|vagin|corrimento|penis|testicul|menstrua|genita|prostata|bexiga|rim")),
    (8, RX(r"pele|mancha|coceira|bolha|caroco|unha|cabelo|espinha|verruga|alergia|urticaria|ferida|olho|ouvido|dente|boca|garganta|nariz")),
]


def familia(it):
    t = norm(it["desc"] + " " + it["termo"] + " " + " ".join(it["lexico"][:4]))
    if set(it["vale"]) <= {"bebe", "crianca"} or re.match(r"(bebe|crianca)", norm(it["desc"])):
        return 11
    if it["vale"] == ["idoso"] or re.search(r"\bidos[oa]|afinar o sangue|anticoag", t):
        return 12
    if it["vale"] == ["gestante"]:
        return 10
    for f, rx in F_REGRAS:
        if rx.search(t):
            return f
    if it.get("sistema") == "osteomuscular":
        return 1
    return 15


# ---------------------------------------------------------------- pessoa
FAIXAS = {"bebe": (1, 23), "crianca": (24, 155), "adolescente": (156, 215), "adulto": (216, 719), "idoso": (720, 1068), "gestante": (216, 516)}
QUALIF = [  # palavra na descrição ➜ (fator para o motor, remédio ou doença na ficha, como a pessoa conta)
    (r"afinar o sangue|anticoag|varfarina", "anticoagulado", "remédio para afinar o sangue", ["tomo remedio pra afinar o sangue", "uso anticoagulante"]),
    (r"diabet|insulina|glicose", "diabetico", "diabetes", ["sou diabetico", "tenho diabetes", "tomo insulina"]),
    (r"cancer|quimio|tumor|radioterapia", "quimioterapia", "câncer em tratamento", ["to fazendo quimio", "faco tratamento de cancer"]),
    (r"transplant", "imunossuprimido", "transplante", ["sou transplantado"]),
    (r"\bhiv\b|aids", "imunossuprimido", "HIV", ["tenho hiv"]),
    (r"dialise|\\brenal|\\brins?\\b", "renal", "doença nos rins", ["faco dialise", "tenho problema nos rins"]),
    (r"colesterol|estatina", "colesterol", "remédio do colesterol", ["tomo remedio pro colesterol"]),
    (r"osteoporose", "osteoporose", "osteoporose", ["tenho osteoporose"]),
    (r"coracao|cardiac|insuficiencia cardiaca", "cardiopata", "problema no coração", ["tenho problema de coracao"]),
]


def idade_para(it, fam, r):
    vale = [v for v in it["vale"] if v in FAIXAS] or None
    d = norm(it["desc"])
    citados = [g for g, rx in (("bebe", r"\bbebes?\b"), ("crianca", r"\bcriancas?\b|\bmenin[oa]\b"), ("adolescente", r"\badolescentes?\b"),
                                ("idoso", r"\bidos[oa]s?\b")) if re.search(rx, d) and not re.search(r"bebe .{0,30}(mex|chut|semanas|barriga)", d)]
    if citados:   # o sinal fala de quem tem (ex.: "cotovelo de babá: criança pequena chora...")
        vale = citados
    if re.search(r"\bparto\b|gravid|gestant|amament|puerper|resguardo|cesar", d) and not re.match(r"(bebe|crianca|recem)", d):
        vale = ["gestante"] if re.search(r"gravid|gestant|semanas", d) else ["adulto"]   # quem escreve é a mãe, não o bebê
    if vale is None:
        if fam == 11:
            vale = ["bebe", "crianca"]
        elif fam == 12:
            vale = ["idoso"]
        elif fam == 10:
            vale = ["gestante"]
        else:
            vale = r.choices(["adulto", "adolescente", "idoso", "crianca"], [70, 10, 15, 5])
    g = r.choice(vale)
    lo, hi = FAIXAS[g]
    return g, r.randint(lo, hi)


def sexo_para(it, gestante, r):
    t = norm(it["desc"] + " " + it["termo"])
    if gestante or re.search(r"mama|vagin|menstru|utero|ovario|gravid|parto|corrimento|mulher|seio|calcinha", t):
        return "F"
    if re.search(r"penis|testicul|prostata|escroto|homem", t):
        return "M"
    return r.choice("FM")


# ---------------------------------------------------------------- o que aconteceu
MECANISMO = RX(r"pancada|queda|caiu|cair|caido|tombo|torc|batida|bateu|bater|bati|acidente|chute|machuc|estalo|fratur|desloc|golpe|soco|puxao|tranco|fisgada|atropel|agredid|mergulh|luxa|entorse|osso aparecendo|saiu do lugar|quebr")
CONCEITOS = {   # conceito do caso ➜ padrão na descrição do sinal (o simulador usa para responder com sinceridade)
    "osso": r"osso (aparecendo|pra fora|saindo)|fratura exposta",
    "frio": r"(mao|pe|dedos?|pes|maos)\b.{0,40}(gelad|frio|fria|palid|roxo|roxa|azulad)|(gelad|palid|azulad).{0,30}(mao|pe|dedo)",
    "coluna": r"(pescoco|costas|coluna).{0,60}(formig|fraqueza|dorm|perd)|mergulh|trauma raquimedular|pancada na coluna",
    "cabeca_grave": r"cabeca.{0,80}(vomit|confus|sonolen|convuls|fala enrolada|nao acorda|pupila)|(vomit|convuls|sonolen).{0,60}cabeca",
    "cabeca": r"cabeca.{0,60}(zonzo|tont|enjo|apagou|desmaiou por um instante|dor de cabeca)",
    "torto": r"torto|torta|deformad|fora do lugar|encurtad|virad[oa] para fora|saiu do lugar|desloc|formato estranho",
    "estalo": r"estalo|estalou|estral|crack|quebrando",
    "apoiar": r"(nao|sem) (consegu\w+ )?(apoiar|pisar|mexer|andar|ficar em pe|levantar|usar|esticar|dobrar)|sem conseguir (apoiar|mexer|usar)|nao aguenta",
    "dormente": r"dorm[eê]n|formig",
    "inchaco": r"inchaco (grande|que aumenta|aumentando)|muito inchad|roxo grande|incha\w* (rapido|cada vez)",
    "osso_dor": r"fratur|quebrad|dor forte num ponto do osso|dor em cima do osso",
}
CONC_FIL = {   # pergunta do filtro ➜ padrão que faz a pessoa responder "sim"
    "FIL_01": r"falta de ar forte|nao consegue falar|labios? (ou dedos )?roxo|nao respira|parou de respirar|sufoc",
    "FAR_01": r"falta de ar forte|nao consegue falar|labios? (ou dedos )?roxo|nao respira",
    "FIL_02": r"dor no peito|aperto no peito|peso no peito|peito (doendo|apertado)|dor ou aperto forte no peito",
    "FAR_03": r"peito",
    "DPE_01": r"dura mais|mais de (alguns|15|20) minutos|nao passa",
    "DPE_02": r"suor frio|palid|desmai|fraqueza",
    "DPE_03": r"braco|costas|pescoco|queixo|mandibula|estomago",
    "FIL_03": r"desmai|desacord|nao acorda|confus|convuls|apagou|nao responde",
    "FIL_04": r"boca torta|fala enrolada|fraqueza (de|em|num) (um )?lado|formigamento de um lado|metade do corpo|rosto caido",
    "FIL_05": r"sangramento forte|sangr\w* (muito|que nao para)|nao para (de sangrar|mesmo apertando)|acidente|queda de (mais de|altura)|caiu de mais de|atropel|choque eletrico|queimadura (grande|maior)",
    "FIL_06": r"inch\w*.{0,30}(rosto|labio|lingua|garganta)|garganta fechando|boca inchada",
    "FAR_02": r"inch\w*.{0,30}(rosto|labio|lingua|garganta)|garganta fechando",
    "FIL_07": r"engoliu|bebeu (produto|agua sanitaria|veneno|soda|gasolina)|veneno|produto de limpeza|remedios? (a mais|demais)|intoxic|soda|chumbinho|monoxido|agrotox|bateria",
    "FIL_08": r"suicid|tirar a propria vida|se machucar de proposito|acabar com tudo|me matar|se cortar|automutil",
    "CRI_01": r"suicid|tirar a propria vida|acabar com tudo|plano|ja tentou",
    "INT_01": r"sonolen|desmai|convuls|falta de ar|confus|nao acorda",
    "GEN_01": r"confus|sonolen|fala enrolada|nao fala coisa com coisa",
    "GEN_03": r"de repente|piorando rapido|cada vez (mais|pior)|subitamente|do nada forte|muito forte que comecou",
    "GEN_04": r"febre|calafrio",
    "RES_07": r"falta de ar|cansaco para respirar",
    "FIL_C01": r"nao consegue mamar|nao mama|nao (consegue )?beber|vomita tudo|molinh|caidinh|diferente do normal",
    "FIL_C03": r"umbigo|pele amarela|amarelad|pus .{0,10}ouvido",
    "FIL_G02": r"dor de cabeca forte|vista embac|pontinhos|incha\w*.{0,20}(rosto|maos)|pressao (alta|de 14)",
    "FIL_G03": r"bebe .{0,20}(mex\w* menos|nao mex|quase nao|parou de mex|mexendo menos)|contraco|barriga dura|dor forte na barriga",
    "FIL_I01": r"falta de ar|cansaco (muito )?forte",
    "DCV_02": r"mais de uma convulsao|convulsao (que dura|longa|mais de)|nao acorda",
    "DCV_03": r"dor no peito|falta de ar|coracao (disparado|acelerado)|palpita",
    "DCV_04B": r"convuls|olho virado|tremedeira",
    "FEB_A03": r"calor|sol forte|exercicio",
    "FEB_A03B": r"pele quente e seca|confus|desmai",
    "FEB_A04": r"pintinha|manchas? (vermelh|roxa|escura)|petequi",
    "FEB_A07": r"febre baixou|febre passou|melhorou a febre",
    "FEB_A08": r"dengue|mosquito",
    "FEB_A11": r"mora sozinh|sem ninguem",
}
MULTI_FEB = {
    "FEB_A02": {"confusa": r"confus", "falta_ar": r"falta de ar", "choque": r"pele fria|palid|desmai|pressao baixa|tonto ao levantar",
                "sangue_vomito": r"vomit\w* sangue|sangue no vomito", "sangramento_forte": r"sangramento forte|sangra muito",
                "pescoco_duro": r"nuca dura|pescoco duro|rigid"},
    "FEB_A05": {"dor_barriga": r"dor .{0,20}barriga", "vomito_toda_hora": r"vomit", "tontura_levantar": r"tontura|desmai|tonto",
                "sangue_nariz_gengiva": r"sangr\w* .{0,20}(nariz|gengiva)", "sonolento": r"sonolen|molinho|mole",
                "dor_cabeca_forte": r"dor de cabeca (forte|muito)"},
    "FEB_A09": {"corpo": r"dor no corpo|corpo doendo", "juntas": r"juntas|articula", "cabeca": r"dor de cabeca",
                "olhos": r"atras dos olhos|olhos", "manchas": r"mancha", "enjoo": r"enjo|vomit", "resfriado": r"tosse|coriza|garganta|nariz",
                "urinario": r"xixi|urina"},
}

REGIOES = [   # palavra ➜ nome da região no corpo (o app aceita qualquer um dos nomes da árvore)
    (r"tornozelo", "Tornozelo {l}"), (r"calcanhar|sola do pe|\bpe\b|\bpes\b|dedo do pe|dedinho", "Pé {l}"),
    (r"joelho|rotula|menisco", "Joelho {l}"), (r"panturrilha|batata da perna", "Batata da perna {l}"),
    (r"canela", "Canela {l}"), (r"coxa|quadril|virilha", "Coxa {l}"), (r"perna", "Canela {l}"),
    (r"punho|pulso|\bmao\b|\bmaos\b|dedo", "Mão {l}"), (r"antebraco", "Antebraço {l}"), (r"cotovelo", "Cotovelo {l}"),
    (r"ombro|clavicula", "Ombro {l}"), (r"braco", "Braço {l}"), (r"lombar", "Lombar"), (r"costas|coluna", "Costas"),
    (r"pescoco|nuca", "Nuca"), (r"cabeca", "Cabeça"), (r"rosto|olho|boca|dente|nariz|orelha|ouvido|queixo", "Rosto"),
    (r"garganta", "Garganta"), (r"peito|costela|mama|seio", "Peito"), (r"umbigo", "Em volta do umbigo"),
    (r"barriga|estomago|abdome", "Barriga"),
]


def regiao_para(it, r):
    t = norm(it["desc"] + " " + " ".join(it["lexico"][:3]))
    for rx, nome in REGIOES:
        if re.search(rx, t):
            return nome.format(l=r.choice(["direito", "esquerdo"])).replace("Mão direito", "Mão direita").replace("Mão esquerdo", "Mão esquerda")
    return None


def tempo_para(it, r):
    t = norm(it["desc"])
    if re.search(r"mais de (duas|2|tres|3|quatro|4|seis) semanas|ha semanas|meses|faz tempo", t):
        return r.choice(["1_4_semanas", "mais_1_mes"])
    if re.search(r"dias seguintes|dias depois|faz dias|ha dias|nos dias|semana", t):
        return "1_7_dias"
    if it["destino"] in ("SAMU_192", "UPA_HOJE") or re.search(r"de repente|agora|acabou de", t):
        return r.choices(["hoje", "1_7_dias"], [80, 20])[0]
    return r.choices(["hoje", "1_7_dias", "1_4_semanas"], [45, 45, 10])[0]


FRASE_TEMPO = {"hoje": ["agora", "agora pouco", "hoje de manha", "faz umas 2 horas", "desde cedo", "hj"],
               "1_7_dias": ["desde ontem", "faz 3 dias", "desde segunda", "faz uns 2 dias", "essa semana"],
               "1_4_semanas": ["faz 2 semanas", "ja tem umas 3 semanas", "desde o mes passado"],
               "mais_1_mes": ["faz uns 2 meses", "ja tem meses", "desde o comeco do ano"]}
MECANISMOS = {
    "esporte": ["no treino de judo", "jogando bola", "na academia", "no futsal", "correndo no parque", "no jiu jitsu", "jogando volei", "andando de skate"],
    "torcao": ["torci o pe descendo a escada", "pisei errado no meio fio", "virei o pe na calcada", "torci saindo do onibus"],
    "queda": ["cai em casa", "escorreguei no banheiro", "tropecei na rua", "levei um tombo de bicicleta", "cai da escada"],
    "pancada": ["bati na quina da mesa", "levei uma pancada", "bati com forca na porta", "tomei um chute"],
    "grave": ["cai do telhado", "bati de moto", "fui atropelado", "cai de uns 3 metros"],
}


def mecanismo_para(it, fam, r):
    t = norm(it["desc"] + " " + " ".join(it["lexico"][:4]))
    if fam in (4, 5, 6, 7, 8, 9, 10, 13, 14, 15):   # aqui a queixa não é um machucado (mesmo que a palavra apareça)
        return None
    if fam in (11, 12) and not re.search(r"caiu|queda|tombo|pancada|bateu|batida|torc|fratur|osso", norm(it["desc"])):
        return None
    if not MECANISMO.search(t) and fam not in (1, 2, 3):
        return None
    if not MECANISMO.search(t) and fam == 1 and not re.search(r"musculo|tendao|joelho|tornozelo|ombro|punho", t):
        return None
    if re.search(r"altura|mais de 1 metro|atropel|moto|carro|transito|capot", t):
        return "grave"
    if fam == 1 or re.search(r"treino|jogo|esporte|corrida|atleta", t):
        return "esporte"
    if re.search(r"torc|entorse|pisei|virou o pe", t):
        return "torcao"
    if re.search(r"caiu|queda|tombo|escorreg|cair", t):
        return "queda"
    return "pancada"


# ---------------------------------------------------------------- como a pessoa escreve
TROCAS = [(r"\bque\b", "q"), (r"\bvoce\b", "vc"), (r"\btambem\b", "tbm"), (r"\bhoje\b", "hj"), (r"\besta\b", "ta"), (r"\bestou\b", "to"),
          (r"\bpara\b", "pra"), (r"\bmuito\b", "mt"), (r"\bporque\b", "pq"), (r"\bnao\b", "n"), (r"\bcom\b", "c"), (r"\bde novo\b", "dnv")]


def bagunca(txt, r, nivel):
    """nivel 0: escreve certinho; 1: sem acento e com abreviação; 2: com erro de digitação também."""
    if nivel == 0:
        return txt
    t = norm(txt) if r.random() < 0.8 else txt.lower()
    for rx, rep in TROCAS:
        if r.random() < 0.5:
            t = re.sub(rx, rep, t)
    if nivel >= 2:
        palavras = t.split()
        for _ in range(r.randint(1, 2)):
            i = r.randrange(len(palavras))
            w = palavras[i]
            if len(w) > 4:
                j = r.randrange(1, len(w) - 2)
                w = w[:j] + w[j + 1] + w[j] + w[j + 2:]
                palavras[i] = w
        t = " ".join(palavras)
    return t


QUEM = {
    "bebe": ["minha bebe de {m} meses", "meu bebe de {m} meses", "meu filho de {m} meses"],
    "crianca": ["meu filho de {a} anos", "minha filha de {a} anos", "meu menino de {a} anos", "minha pequena de {a} anos"],
    "idoso": ["minha mae de {a} anos", "meu pai de {a} anos", "minha avo de {a} anos", "meu vo de {a} anos", "tenho {a} anos e"],
}
MINIMIZA = ["deve ser bobeira mas", "acho q nao e nada mas", "nao sei se e serio,", "provavelmente nao e nada,"]
EXAGERA = ["socorro", "to desesperada", "nunca senti nada assim", "to com muito medo"]


def montar_relato(it, ficha, r):
    partes = []
    g = ficha["grupo"]
    if g in QUEM and r.random() < 0.85:
        quem = r.choice(QUEM[g]).format(m=max(1, ficha["idade_meses"]), a=ficha["idade_meses"] // 12)
        partes.append(quem if quem.endswith(" e") else quem + r.choice([" ta com", ",", " tá:", " reclama de"]))
    elif g == "gestante":
        partes.append(r.choice(["to gravida de {s} semanas", "gravida de {s} semanas,", "to de {s} semanas"]).format(s=r.randint(8, 39)))
    if r.random() < 0.15:
        partes.insert(0, r.choice(MINIMIZA))
    elif r.random() < 0.06:
        partes.insert(0, r.choice(EXAGERA))
    if ficha["mecanismo"] and ficha["conta_mecanismo"]:
        m = r.choice(MECANISMOS[ficha["mecanismo"]])
        if g in QUEM:   # quem escreve é a mãe, o pai ou o neto: "caí" vira "caiu"
            for a, b in (("cai ", "caiu "), ("torci", "torceu"), ("bati ", "bateu "), ("levei", "levou"), ("pisei", "pisou"), ("virei", "virou"),
                         ("escorreguei", "escorregou"), ("tropecei", "tropecou"), ("tomei", "tomou"), ("fui ", "foi ")):
                m = m.replace(a, b)
        partes.append(m)
    frase = r.choice(it["lexico"]).strip()
    if r.random() < 0.3 and len(it["lexico"]) > 1:
        frase += r.choice([" e ", ", ", " tbm "]) + r.choice([x for x in it["lexico"] if x != frase] or [frase]).strip()
    partes.append(frase)
    if r.random() < 0.7:
        partes.append(r.choice(FRASE_TEMPO[ficha["tempo"]]))
    for q in ficha["conta_grupo"]:
        partes.append(q)
    if ficha.get("secundario_txt"):
        partes.append(r.choice(["e tbm", "alem disso", "ah e"]) + " " + ficha["secundario_txt"])
    txt = " ".join(partes)
    return bagunca(txt, r, r.choices([0, 1, 2], [35, 40, 25])[0])


# ---------------------------------------------------------------- boa conversa e o que não faz sentido
def boa_conversa(fam, ficha):
    if ficha["lesao"]:
        return ["mecanismo", "sinais_graves_lesao", "apoiar_mexer", "dor_0_10", "tempo"]
    base = {
        4: ["de_repente_ou_pior", "sinais_avc", "tempo", "dor_0_10"],
        5: ["dias_de_febre", "sinais_alerta_febre", "grupo_de_risco"],
        6: ["tempo", "dor_0_10", "beber_e_xixi", "febre"],
        7: ["falta_de_ar_grave", "peito", "tempo"],
        8: ["tempo", "febre", "espalhando"],
        9: ["febre", "tempo"],
        10: ["sangramento_gestacao", "sinais_gestacao", "bebe_mexendo"],
        11: ["mamar_beber", "febre", "tempo"],
        12: ["grupo_de_risco", "tempo", "dor_0_10"],
        13: ["risco_agora"],
        14: ["o_que_engoliu_ou_bicho", "sinais_graves_intox"],
        15: ["tempo", "febre"],
    }
    return base.get(fam, ["tempo", "dor_0_10"])


def sem_sentido(ficha):
    s = []
    if ficha["sexo"] == "M" or not ficha["gestante"]:
        s.append("gravidez")
    if ficha["grupo"] not in ("idoso",):
        s.append("idoso")
    if ficha["grupo"] not in ("bebe", "crianca"):
        s.append("crianca")
    for q in ("anticoagulado", "diabetico", "quimioterapia", "imunossuprimido", "renal", "colesterol", "osteoporose", "cardiopata"):
        if q not in ficha["fatores"]:
            s.append(q)
    if not ficha["lesao"]:
        s.append("trauma")
    else:
        s += ["febre", "veneno", "alergia_inchaco", "suicidio_sem_sinal", "avc", "peito"] if ficha["familia"] in (1, 2, 3) else []
    if not ficha["cirurgia"]:
        s.append("cirurgia")
    return s


def gerar(n=12000, semente=2026):
    r = random.Random(semente)
    base = carregar_base()
    por_fam = {}
    for it in base:
        por_fam.setdefault(familia(it), []).append(it)
    leves = [it for it in base if it["destino"] == "CUIDADO_EM_CASA" and it["area"] != "CO"]
    alvo = n // len(FAMILIAS)
    casos = []
    for fam in sorted(FAMILIAS):
        seeds = por_fam.get(fam, [])
        if not seeds:
            continue
        for k in range(alvo + (n - alvo * len(FAMILIAS) if fam == 15 else 0)):
            it = r.choice(seeds)
            grupo, idade = idade_para(it, fam, r)
            gestante = grupo == "gestante"
            sexo = sexo_para(it, gestante, r)
            t = norm(it["desc"] + " " + it["termo"] + " " + " ".join(it["lexico"][:3]))
            fatores, doencas, conta = [], [], []
            for rx, fator, doenca, frases in QUALIF:
                if re.search(rx, t):
                    fatores.append(fator)
                    doencas.append(doenca)
                    if r.random() < 0.8:
                        conta.append(r.choice(frases))
            if not fatores and r.random() < 0.08:   # doença de fundo que não tem nada a ver com a queixa
                fatores.append("hipertenso"); doencas.append("pressão alta")
            mec = mecanismo_para(it, fam, r)
            lesao = mec is not None
            tempo = tempo_para(it, r)
            dmin, dmax = {"SAMU_192": (7, 10), "UPA_HOJE": (5, 9), "UBS_EM_BREVE": (3, 6), "CUIDADO_EM_CASA": (1, 4)}[it["destino"]]
            if re.search(r"\bleve\b|suportavel|pouca dor", t):
                dmin, dmax = 1, 4
            conceitos = sorted(c for c, rx in CONCEITOS.items() if re.search(rx, t))
            if "osso_dor" in conceitos and it["destino"] not in ("UPA_HOJE", "SAMU_192"):
                conceitos.remove("osso_dor")
            presentes = [it["id"]]
            ficha = dict(
                caso="", familia=fam, nome_familia=FAMILIAS[fam], semente_sinal=it["id"], descricao_sinal=it["desc"],
                grupo=grupo, idade_meses=idade, sexo=sexo, gestante=gestante, diabetes="diabetico" in fatores,
                fatores=fatores + (["homem"] if sexo == "M" else ["mulher"]), doencas=doencas,
                cirurgia=bool(re.search(r"cirurgi|oper\w*|protese|colonoscop|endoscop|biopsia|amigdal|blefaro|lipo|abdomino|bariatric|histeroscop", t)),
                pos_parto=bool(re.search(r"\bparto\b|amament|puerper|resguardo|cesar", t)),
                lesao=lesao, mecanismo=mec, conta_mecanismo=lesao and r.random() < 0.65, tempo=tempo,
                dor=r.randint(dmin, dmax), conceitos=conceitos, texto_sinal=t,
                evolucao=r.choices(["melhorando", "igual", "piorando"], [55, 35, 10] if it["destino"] in ("CUIDADO_EM_CASA", "UBS_EM_BREVE") else [20, 40, 40])[0],
                regiao=regiao_para(it, r) if r.random() < 0.6 else None,
                sinais_presentes=presentes, conta_grupo=conta,
                destino_esperado=it["destino"], fonte=it["fonte"], fonte_url=it["url"], revisar_medico=True,
            )
            sobe = set(it.get("sobe", [])) & (set(fatores) | {grupo})
            if sobe and it["destino"] != "SAMU_192":
                ficha["destino_esperado"] = ORDEM[ORDEM.index(it["destino"]) + 1]
                ficha["subiu_por"] = sorted(sobe)
            boas = [x for x in leves if "todos" in x["vale"] or grupo in x["vale"] or (grupo == "gestante" and "adulto" in x["vale"])]
            boas = [x for x in boas if not re.match(r"(bebe|crianca|idos)", norm(x["desc"])) or grupo in ("bebe", "crianca", "idoso")]
            if r.random() < 0.10 and boas:   # mistura duas queixas: a segunda é leve e combina com a idade
                sec = r.choice(boas)
                ficha["secundario"] = sec["id"]
                ficha["secundario_txt"] = r.choice(sec["lexico"])
                ficha["sinais_presentes"].append(sec["id"])
            ficha["boa_conversa"] = boa_conversa(fam, ficha)
            ficha["sem_sentido"] = sem_sentido(ficha)
            ficha["relato"] = montar_relato(it, ficha, r)
            casos.append(ficha)
    r.shuffle(casos)
    for i, c in enumerate(casos):
        c["caso"] = f"SIM-{c['familia']:02d}-{i:05d}"
        c["conjunto"] = "prova" if i < 2000 else "treino"
    return casos


if __name__ == "__main__":
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 12000
    semente = int(sys.argv[2]) if len(sys.argv) > 2 else 2026
    casos = gerar(n, semente)
    os.makedirs(SAIDA, exist_ok=True)
    with open(os.path.join(SAIDA, "casos_simulados.jsonl"), "w", encoding="utf-8") as f:
        for c in casos:
            f.write(json.dumps(c, ensure_ascii=False) + "\n")
    with open(os.path.join(SAIDA, "casos_simulados.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["caso", "conjunto", "familia", "grupo", "idade_meses", "sexo", "relato", "destino_esperado", "sinal", "fonte", "fonte_url", "revisar_medico"])
        for c in casos:
            w.writerow([c["caso"], c["conjunto"], c["nome_familia"], c["grupo"], c["idade_meses"], c["sexo"], c["relato"],
                        c["destino_esperado"], c["semente_sinal"], c["fonte"], c["fonte_url"], "SIM"])
    from collections import Counter
    print(len(casos), "casos")
    print(Counter(c["nome_familia"] for c in casos))
    print(Counter(c["destino_esperado"] for c in casos))
    print("lesao", sum(c["lesao"] for c in casos))
