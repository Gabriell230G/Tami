"""readme.py · gera o README do GitHub da Tami e as artes em SVG (.github/readme/).

Todo texto dos SVGs vira desenho (caminho) com as fontes do site, Fredoka e Nunito.
Assim a arte fica igual em qualquer computador, no tema claro e no escuro do GitHub.
Uso: python3 gen/readme.py [pasta-de-saida]   (padrão: a raiz do repositório)
"""
import base64
import os
import sys

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

sys.path.insert(0, os.path.dirname(__file__))
import site2 as S  # noqa: E402
import manual as M  # noqa: E402

R = S.R
C = S.COR
t = S.t
SAIDA = sys.argv[1] if len(sys.argv) > 1 else (R if os.path.isdir(os.path.join(R, ".github")) else os.path.join(os.path.dirname(R), "tami-repo"))
ARTE = os.path.join(SAIDA, ".github", "readme")
LINKEDIN = S.LINKEDIN
SITE = "https://gabriell230g.github.io/tami/"
APP = os.environ.get("TAMI_APP_URL", "https://tami-app.onrender.com/")

FONTES = {k: TTFont(os.path.join(R, "dist", "fonts", f)) for k, f in
          (("f7", "fredoka-latin-700-normal.woff2"), ("f6", "fredoka-latin-600-normal.woff2"),
           ("n7", "nunito-latin-700-normal.woff2"), ("n8", "nunito-latin-800-normal.woff2"))}


# ---------- texto em caminho ----------
def medir(txt, f="f7", tam=20, track=0.0):
    ft = FONTES[f]; cm = ft.getBestCmap(); hm = ft["hmtx"]; k = tam / ft["head"].unitsPerEm
    return sum(hm[cm.get(ord(ch), cm[32])][0] * k + track * tam for ch in txt) - (track * tam if txt else 0)


_USADOS = {}  # glifos usados no SVG atual ➜ desenhados uma vez em <defs>


def _glifo(f, g):
    gid = f"{f}-{g}".replace(".", "_")
    if gid not in _USADOS:
        ft = FONTES[f]; gs = ft.getGlyphSet()
        pen = SVGPathPen(gs, lambda v: str(round(v)))
        gs[g].draw(pen)
        _USADOS[gid] = f'<path id="{gid}" d="{pen.getCommands()}"/>'
    return gid


def tx(txt, x, y, tam=20, f="f7", cor="#fff", ancora="start", track=0.0, extra=""):
    """Texto como glifos reaproveitados (<use>). y = linha de base. ancora: start, middle ou end."""
    ft = FONTES[f]; cm = ft.getBestCmap(); hm = ft["hmtx"]; k = tam / ft["head"].unitsPerEm
    w = medir(txt, f, tam, track)
    x0 = x - (w / 2 if ancora == "middle" else w if ancora == "end" else 0)
    us, cx = [], 0.0
    for ch in txt:
        g = cm.get(ord(ch), cm[32])
        if ch != " ":
            us.append(f'<use href="#{_glifo(f, g)}" x="{(x0 + cx) / k:.0f}"/>')
        cx += hm[g][0] * k + track * tam
    return f'<g {extra} fill="{cor}" transform="translate(0 {y:.1f}) scale({k:.5f} {-k:.5f})">{"".join(us)}</g>'


def quebra(txt, larg, f="n7", tam=18):
    linhas, atual = [], ""
    for p in txt.split():
        prova = (atual + " " + p).strip()
        if medir(prova, f, tam) <= larg or not atual:
            atual = prova
        else:
            linhas.append(atual); atual = p
    return linhas + ([atual] if atual else [])


def bloco(txt, x, y, larg, tam=18, f="n7", cor="#fff", entre=1.4, ancora="start"):
    ls = quebra(txt, larg, f, tam)
    return "".join(tx(l, x, y + i * tam * entre, tam, f, cor, ancora) for i, l in enumerate(ls)), len(ls)


def svg(w, h, corpo, titulo, estilo=""):
    defs = "<defs>" + "".join(_USADOS.values()) + "</defs>"; _USADOS.clear()
    return (f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 {w} {h}" width="{w}" height="{h}" role="img">'
            f'<title>{titulo}</title>{defs}<style>{estilo}@media (prefers-reduced-motion: reduce) {{ * {{ animation: none !important; }} }}</style>'
            f'{corpo}</svg>')


def salva(nome, conteudo):
    os.makedirs(ARTE, exist_ok=True)
    with open(os.path.join(ARTE, nome), "w", encoding="utf-8") as fh:
        fh.write(conteudo)
    print(f"  {nome:22s} {len(conteudo.encode()) / 1024:6.1f} KB")


def icone_app(nome, w, cor, acento, wcor=None):
    """Ícone do app, sem os atributos repetidos que o HTML aceita e o SVG não."""
    v = S.ic3(nome, w, m=cor, a=acento, wcor=wcor)
    return v.replace(' aria-hidden="true"', "").replace(' focusable="false"', "")


def rosto(tipo="oi", cor="#fff"):
    """O rosto da Tami (320 x 160), só as formas, sem o <svg> de fora."""
    s = S.rosto(cor, 320, tipo)
    return s[s.index(">") + 1:s.rindex("</svg>")]


SETA = '<path d="M-10 0 H9 M2 -7 L10 0 L2 7" fill="none" stroke="{c}" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"/>'


# ---------- 1. capa ----------
def capa():
    w, h = 1200, 560
    bolhas = "".join(f'<circle class="b b{i}" cx="{x}" cy="{y}" r="{r}" fill="{c}" opacity="{o}"/>' for i, (x, y, r, c, o) in enumerate([
        (1080, 90, 46, C["accent"], .9), (690, 470, 18, C["accent"], .85), (1140, 470, 70, "#fff", .08), (640, 80, 120, "#fff", .05),
        (760, 150, 10, "#fff", .5), (1110, 300, 12, "#fff", .4), (40, 520, 90, "#fff", .05)]))
    olhos = rosto("oi")
    corpo = (
        '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5560D6"/><stop offset=".55" stop-color="#3F4ACB"/><stop offset="1" stop-color="#2B3290"/></linearGradient>'
        '<radialGradient id="luz" cx=".5" cy=".5" r=".5"><stop offset="0" stop-color="#8F98F0" stop-opacity=".7"/><stop offset="1" stop-color="#8F98F0" stop-opacity="0"/></radialGradient>'
        f'<clipPath id="cc"><rect width="{w}" height="{h}" rx="36"/></clipPath></defs>'
        f'<g clip-path="url(#cc)"><rect width="{w}" height="{h}" fill="url(#g)"/>{bolhas}'
        '<circle cx="905" cy="275" r="230" fill="url(#luz)"/>'
        # rosto grande, piscando
        f'<g class="rosto" transform="translate(745 205)"><g class="pisca">{olhos}</g></g>'
        # balão
        '<g class="balao" transform="translate(760 395)"><rect width="330" height="74" rx="26" fill="#fff"/><path d="M60 0 L78 -18 L92 0 Z" fill="#fff"/>'
        + tx("Oi! Me conta o que está", 24, 32, 19, "n8", C["text"]) + tx("acontecendo, do seu jeito.", 24, 57, 19, "n8", C["text"]) + '</g>'
        # marca
        f'<g transform="translate(72 56)">{M.monograma(C["text"], "#fff", C["accent"], 64)}</g>'
        + tx("tami", 152, 104, 46, "f7", "#fff", track=-.02)
        + f'<rect x="72" y="166" width="150" height="34" rx="17" fill="{C["accent"]}"/>' + tx("EM TESTE", 147, 189, 15, "n8", C["text"], "middle", .1)
        + tx("O jeito mais calmo", 72, 278, 66, "f7", "#fff", track=-.015)
        + tx("de decidir para", 72, 352, 66, "f7", "#fff", track=-.015)
        + tx("onde ir", 72, 426, 66, "f7", C["accent"], track=-.015)
        + tx("Um app de orientação em saúde que conversa com você", 72, 482, 21, "n7", C["dark-muted"])
        + tx("e ajuda a escolher: 192, UPA, posto de saúde ou casa.", 72, 510, 21, "n7", C["dark-muted"])
        + '</g>')
    estilo = (".pisca{transform-box:fill-box;transform-origin:center;animation:pisca 5s infinite}"
              "@keyframes pisca{0%,90%,100%{transform:scaleY(1)}93%{transform:scaleY(.08)}96%{transform:scaleY(1)}}"
              ".rosto{animation:flutua 6s ease-in-out infinite}@keyframes flutua{0%,100%{translate:0 0}50%{translate:0 -10px}}"
              ".balao{animation:entra 6s ease-in-out infinite}@keyframes entra{0%,100%{translate:0 0}50%{translate:0 6px}}"
              ".b{animation:sobe 9s ease-in-out infinite}.b1{animation-delay:-2s}.b2{animation-delay:-4s}.b4{animation-delay:-6s}.b5{animation-delay:-1s}"
              "@keyframes sobe{0%,100%{translate:0 0}50%{translate:0 -18px}}")
    salva("capa.svg", svg(w, h, corpo, "Tami: o jeito mais calmo de decidir para onde ir", estilo))


# ---------- 2. selos ----------
def selos():
    itens = [("Em teste", C["accent"], C["text"]), ("Sem cookies", "#fff", C["text"]), ("Sem rastreadores", "#fff", C["text"]),
             ("JavaScript puro", "#fff", C["text"]), ("WebGL feito à mão", "#fff", C["text"]), ("Feito no Brasil", "#fff", C["text"])]
    tam, pad, gap = 17, 22, 12
    larg = [medir(n, "n8", tam) + pad * 2 + 22 for n, _, _ in itens]
    total = sum(larg) + gap * (len(itens) - 1)
    w, h = 1200, 60; x = (w - total) / 2; out = ""
    for (n, bg, cor), lw in zip(itens, larg):
        out += (f'<rect x="{x:.1f}" y="8" width="{lw:.1f}" height="44" rx="22" fill="{bg}" stroke="{C["surface-strong"] if bg == "#fff" else bg}" stroke-width="2"/>'
                f'<circle cx="{x + pad + 4:.1f}" cy="30" r="5" fill="{C["primary"] if bg == "#fff" else C["text"]}"/>'
                + tx(n, x + pad + 18, 36, tam, "n8", cor))
        x += lw + gap
    salva("selos.svg", svg(w, h, out, "Em teste, sem cookies, sem rastreadores, JavaScript puro, WebGL feito à mão, feito no Brasil"))


# ---------- 3. emergência ----------
def emergencia():
    w, h = 1200, 330
    nums = [("192", "SAMU · Emergência de saúde", C["danger"], "fone"), ("188", "CVV · Para conversar, 24 horas, de graça", "#2A2F63", "escuta"),
            ("0800 722 6001", "CIATox · Engoliu ou tomou algo", "#2A2F63", "alerta"), ("190", "Polícia · Alguém te machucando ou ameaçando", "#2A2F63", "avisar")]
    out = (f'<rect width="{w}" height="{h}" rx="32" fill="{C["text"]}"/>'
           f'<rect x="0" y="0" width="{w}" height="10" rx="5" fill="{C["danger"]}" opacity="0"/>'
           + tx("Em emergência,", 64, 128, 50, "f7", "#fff") + tx("não espere", 64, 186, 50, "f7", "#fff")
           + tx("Ligue direto. Não precisa", 64, 236, 20, "n7", C["dark-muted"]) + tx("abrir a Tami antes.", 64, 264, 20, "n7", C["dark-muted"]))
    for i, (n, rot, bg, ic) in enumerate(nums):
        cx, cy = 440 + (i % 2) * 370, 46 + (i // 2) * 128
        icone = icone_app(ic, 30, "#FFFFFF", C["accent"], wcor=C["text"]) if ic != "fone" else S.ICONE_FONE.replace('fill="currentColor"', 'fill="#fff"').replace(' aria-hidden="true" focusable="false"', "").replace("<svg", '<svg width="30" height="30"')
        icone = icone.replace("<svg", f'<svg x="{cx + 32}" y="{cy + 41}"', 1)
        sombra = C["danger-deep"] if i == 0 else "#0E1130"
        out += (f'<g class="c c{i}"><rect x="{cx}" y="{cy + 6}" width="350" height="110" rx="24" fill="{sombra}"/>'
                f'<rect x="{cx}" y="{cy}" width="350" height="110" rx="24" fill="{bg}" stroke="{bg if i == 0 else "#3A4080"}" stroke-width="2"/>'
                f'<rect x="{cx + 22}" y="{cy + 31}" width="50" height="50" rx="15" fill="#fff" fill-opacity=".14"/>{icone}'
                + tx(n, cx + 92, cy + 50, 32 if len(n) < 6 else 27, "f7", "#fff")
                + "".join(tx(l, cx + 92, cy + 80 + j * 20, 17, "n7", "#fff" if i == 0 else C["dark-muted"]) for j, l in enumerate(quebra(rot, 236, "n7", 17)))
                + '</g>')
    estilo = ".c0{animation:pulsa 2.4s ease-in-out infinite}@keyframes pulsa{0%,100%{translate:0 0}50%{translate:0 -3px}}"
    salva("emergencia.svg", svg(w, h, out, "Em emergência, ligue 192 (SAMU), 188 (CVV), 0800 722 6001 (CIATox) ou 190 (Polícia)", estilo))


# ---------- 4. caminhos ----------
def caminhos():
    w, h = 1200, 400
    cams = [("samu", C["danger"], "#fff", "firme"), ("upa", C["upa"], C["text"], "olha"), ("ubs", C["ubs"], C["text"], "oi"), ("casa", C["casa"], C["text"], "feliz")]
    out = ""
    for i, (k, bg, cor, expr) in enumerate(cams):
        x = i * 300 + 4
        face = rosto(expr, cor)
        out += (f'<g class="k k{i}"><rect x="{x}" y="10" width="288" height="380" rx="30" fill="{bg}"/>'
                f'<g transform="translate({x + 28} 40) scale(.32)" opacity=".95">{face}</g>'
                + tx(f'{i + 1} · {t("inicio", "c4", k + "_rotulo").upper()}', x + 28, 130, 15, "n8", cor, track=.08))
        y = 172
        for l in quebra(t("inicio", "c4", k + "_titulo"), 236, "f7", 31):
            out += tx(l, x + 28, y, 31, "f7", cor, track=-.01); y += 36
        corpo, n = bloco(t("inicio", "c4", k + "_texto"), x + 28, max(y + 14, 318), 236, 18, "n7", cor, 1.4)
        out += corpo + '</g>'
    estilo = "".join(f".k{i}{{animation:k 7s ease-in-out {i * .35}s infinite}}" for i in range(4)) + "@keyframes k{0%,100%{translate:0 0}12%{translate:0 -10px}24%{translate:0 0}}"
    salva("caminhos.svg", svg(w, h, out, "Os quatro caminhos da Tami: SAMU 192, UPA hoje, UBS nos próximos dias e cuidar em casa", estilo))


# ---------- 5. fluxo ----------
def fluxo():
    w, h = 1200, 300
    passos = [("Você conta", "Fala ou escreve, do seu jeito.", "mic"), ("A Tami pergunta", "Primeiro, os sinais de emergência.", "ouvir"),
              ("Vocês decidem", "192, UPA, posto de saúde ou casa.", "check")]
    out = f'<rect width="{w}" height="{h}" rx="32" fill="{C["mist"]}"/>'
    out += f'<path class="linha" d="M250 112 H950" stroke="{C["border"]}" stroke-width="4" stroke-dasharray="2 14" stroke-linecap="round" fill="none"/>'
    for i, (tit, sub, ic) in enumerate(passos):
        cx = 200 + i * 400
        bg = C["primary"] if i < 2 else C["accent"]
        icone = icone_app(ic, 46, "#FFFFFF" if i < 2 else C["text"], C["accent"] if i < 2 else C["primary"], wcor=C["primary"] if i < 2 else C["accent"]).replace("<svg", f'<svg x="{cx - 23}" y="{89}"', 1)
        out += (f'<g class="p p{i}"><circle cx="{cx}" cy="112" r="62" fill="{bg}"/><circle cx="{cx + 46}" cy="66" r="19" fill="{C["text"]}"/>'
                + tx(str(i + 1), cx + 46, 73, 20, "f7", "#fff", "middle") + icone
                + tx(tit, cx, 222, 28, "f7", C["text"], "middle") + tx(sub, cx, 258, 20, "n7", C["muted"], "middle") + '</g>')
    for i in range(2):
        out += f'<g transform="translate({400 + i * 400} 112)"><circle r="20" fill="#fff"/>{SETA.format(c=C["primary"])}</g>'
    estilo = (".linha{animation:corre 1.2s linear infinite}@keyframes corre{to{stroke-dashoffset:-32}}"
              + "".join(f".p{i}{{animation:p 6s ease-in-out {i * .5}s infinite}}" for i in range(3)) + "@keyframes p{0%,100%{translate:0 0}10%{translate:0 -8px}20%{translate:0 0}}")
    salva("fluxo.svg", svg(w, h, out, "Como funciona: você conta, a Tami pergunta e vocês decidem para onde ir", estilo))


# ---------- 6. escala ----------
def escala():
    w, h = 1200, 250
    niveis = t("inicio", "c5", "niveis").split("|")
    out = f'<rect width="{w}" height="{h}" rx="32" fill="#fff" stroke="{C["surface-alt"]}" stroke-width="3"/>'
    out += tx("De 0 a 10, quanto incomoda agora?", 600, 58, 28, "f7", C["text"], "middle")
    for n in range(11):
        x = 60 + n * 100
        out += (f'<g class="e e{n}"><g transform="translate({x + 8} 84) scale(.84)">{S.cara_n(n)}</g></g>'
                + tx(str(n), x + 50, 196, 22, "f7", C["text"], "middle"))
    for a, b, rot, cor in ((0, 3, "fraco", C["accent-deep"]), (4, 6, "médio", C["warning-text"]), (7, 10, "forte", C["danger-text"])):
        x1, x2 = 66 + a * 100, 154 + b * 100
        out += f'<rect x="{x1}" y="212" width="{x2 - x1}" height="6" rx="3" fill="{cor}" opacity=".3"/>' + tx(rot.upper(), (x1 + x2) / 2, 242, 15, "n8", cor, "middle", .1)
    estilo = "".join(f".e{n}{{transform-box:fill-box;transform-origin:center bottom;animation:e 5.5s ease-in-out {n * .12:.2f}s infinite}}" for n in range(11)) \
        + "@keyframes e{0%,100%{translate:0 0}8%{translate:0 -10px}16%{translate:0 0}}"
    salva("escala.svg", svg(w, h, out, "Escala de dor de 0 a 10, com um rostinho para cada número: " + ", ".join(niveis), estilo))


# ---------- 7. telas ----------
def telas():
    w, h = 1200, 640
    tl = [("voz", "Falar em vez de escrever"), ("alarme", "Sinais de emergência"), ("onde-doi", "Onde dói?"), ("pessoas", "Pessoas que você cuida")]
    out = (f'<defs><linearGradient id="fg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2B3290"/><stop offset="1" stop-color="{C["text"]}"/></linearGradient></defs>'
           f'<rect width="{w}" height="{h}" rx="32" fill="url(#fg)"/>'
           f'<circle cx="160" cy="80" r="140" fill="#fff" opacity=".04"/><circle cx="1080" cy="560" r="180" fill="{C["accent"]}" opacity=".07"/>')
    cw, ch = 226, 489
    for i, (n, leg) in enumerate(tl):
        x = 60 + i * 280; y = 46
        dado = base64.b64encode(open(os.path.join(R, "dist", "img", f"{n}-360.webp"), "rb").read()).decode()
        out += (f'<g class="t t{i}"><clipPath id="c{i}"><rect x="{x + 8}" y="{y + 8}" width="{cw - 16}" height="{ch - 16}" rx="26"/></clipPath>'
                f'<rect x="{x}" y="{y + 10}" width="{cw}" height="{ch}" rx="34" fill="#0E1130" opacity=".6"/>'
                f'<rect x="{x}" y="{y}" width="{cw}" height="{ch}" rx="34" fill="#0E1130" stroke="#4A519A" stroke-width="2"/>'
                f'<image href="data:image/webp;base64,{dado}" x="{x + 8}" y="{y + 8}" width="{cw - 16}" height="{ch - 16}" preserveAspectRatio="xMidYMin slice" clip-path="url(#c{i})"/>'
                + tx(leg, x + cw / 2, y + ch + 52, 20, "f7", "#fff", "middle") + '</g>')
    estilo = "".join(f".t{i}{{animation:t 8s ease-in-out {-i * 2}s infinite}}" for i in range(4)) + "@keyframes t{0%,100%{translate:0 0}50%{translate:0 -12px}}"
    salva("telas.svg", svg(w, h, out, "Quatro telas do app: falar em vez de escrever, sinais de emergência, onde dói e pessoas que você cuida", estilo))


# ---------- 8. faz e não faz ----------
def faz():
    w, h = 1200, 440
    out = ""
    for j, (tit, chaves, bg, borda, cor_ic, sim) in enumerate([
            (t("inicio", "c7", "faz_titulo"), [f"faz{i}" for i in range(1, 5)], C["lime-faint"], C["accent-mid"], C["accent-deep"], True),
            (t("inicio", "c7", "nao_titulo"), [f"nao{i}" for i in range(1, 5)], C["danger-soft"], "#F5C2C4", C["danger-text"], False)]):
        x = j * 606
        out += f'<rect x="{x + 2}" y="2" width="590" height="356" rx="30" fill="{bg}" stroke="{borda}" stroke-width="3"/>' + tx(tit, x + 40, 70, 32, "f7", C["text"])
        y = 120
        for i, k in enumerate(chaves):
            marca = ('<path d="M-7 0 L-2 5 L8 -6" fill="none" stroke="{c}" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"/>' if sim
                     else '<path d="M-5.5 -5.5 L5.5 5.5 M5.5 -5.5 L-5.5 5.5" stroke="{c}" stroke-width="3.4" stroke-linecap="round"/>').format(c=cor_ic)
            ls = quebra(t("inicio", "c7", k), 470, "n7", 20)
            out += (f'<g transform="translate({x + 58} {y})"><circle r="16" fill="none" stroke="{cor_ic}" stroke-width="3"/>{marca}</g>'
                    + "".join(tx(l, x + 90, y + 7 + j * 24, 20, "n7", C["text"]) for j, l in enumerate(ls)))
            y += 52 + (len(ls) - 1) * 24
    out += (f'<rect x="2" y="376" width="1196" height="62" rx="24" fill="{C["text"]}"/>'
            + tx(t("inicio", "c7", "anvisa"), 600, 414, 17, "n7", "#fff", "middle"))
    salva("faz.svg", svg(w, h, out, "O que a Tami faz e o que ela não faz"))


# ---------- 9. o site ----------
ICONES_SITE = {
    "rosto": '<rect x="9" y="12" width="7" height="16" rx="3.5"/><rect x="32" y="12" width="7" height="16" rx="3.5"/><path d="M18 31 A6 6 0 0 0 30 31" fill="none" stroke-width="5" stroke-linecap="round" stroke="currentColor"/>',
    "abre": '<rect x="9" y="12" width="7" height="16" rx="3.5"/><path d="M30 21 A5 5 0 0 1 40 21" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"/><path d="M18 31 A6 6 0 0 0 30 31" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"/>',
    "modos": '<rect x="6" y="10" width="10" height="28" rx="5"/><rect x="20" y="16" width="10" height="22" rx="5" opacity=".7"/><rect x="34" y="24" width="10" height="14" rx="5" opacity=".45"/>',
    "corpo": '<circle cx="24" cy="9" r="6"/><path d="M14 20 C14 17 17 16 24 16 C31 16 34 17 34 20 L32 31 H28 L27 44 H21 L20 31 H16 Z"/>',
    "escala": '<circle cx="24" cy="24" r="18"/><circle cx="18" cy="20" r="2.6" fill="#fff"/><circle cx="30" cy="20" r="2.6" fill="#fff"/><path d="M16 28 Q24 35 32 28" fill="none" stroke="#fff" stroke-width="3.4" stroke-linecap="round"/>',
    "manual": '<rect x="8" y="6" width="28" height="36" rx="5"/><rect x="14" y="13" width="16" height="4" rx="2" fill="#fff"/><rect x="14" y="21" width="12" height="4" rx="2" fill="#fff"/><rect x="34" y="14" width="8" height="28" rx="4" opacity=".6"/>',
    "transicao": '<rect x="4" y="10" width="24" height="28" rx="6" opacity=".45"/><rect x="18" y="10" width="26" height="28" rx="6"/>',
    "unico": '<path d="M12 4 H28 L38 14 V44 H12 Z"/><path d="M28 4 V14 H38" fill="#fff" opacity=".5"/>',
    "cadeado": '<rect x="9" y="21" width="30" height="22" rx="7"/><path d="M15 21 V15 A9 9 0 0 1 33 15 V21" fill="none" stroke="currentColor" stroke-width="5"/>',
}


def site():
    itens = [("rosto", "Rosto em 3D", "Um shader WebGL escrito do zero."), ("abre", "Abertura com piscadinha", "Letra por letra e uma transição líquida."),
             ("modos", "Três modos de movimento", "Show, leve e parado, com pausa."), ("corpo", "Onde dói?", "Toque no corpo e marque o nível da dor."),
             ("escala", "Escala de 0 a 10", "Um rostinho para cada número."), ("manual", "Manual de marca", "Pranchas, PDF e logos para baixar."),
             ("transicao", "Transição entre páginas", "O menu e a faixa do 192 ficam parados."), ("unico", "Arquivo único", "O site inteiro num só arquivo HTML."),
             ("cadeado", "Privacidade de verdade", "Sem cookies, sem rastreadores.")]
    w, h = 1200, 600
    out = ""
    for i, (ic, tit, sub) in enumerate(itens):
        x, y = (i % 3) * 404, (i // 3) * 200
        cor = [C["primary"], C["text"], C["accent-deep"]][i % 3] if i != 4 else C["primary"]
        bg_ic = [C["primary"], C["text"], C["accent"]][(i + i // 3) % 3]
        cor_ic = "#fff" if bg_ic != C["accent"] else C["text"]
        out += (f'<g class="s s{i}"><rect x="{x + 2}" y="{y + 2}" width="388" height="184" rx="28" fill="#fff" stroke="{C["surface-alt"]}" stroke-width="3"/>'
                f'<rect x="{x + 30}" y="{y + 30}" width="64" height="64" rx="20" fill="{bg_ic}"/>'
                f'<svg x="{x + 38}" y="{y + 38}" width="48" height="48" viewBox="0 0 48 48" fill="{cor_ic}" color="{cor_ic}">{ICONES_SITE[ic]}</svg>'
                + tx(tit, x + 30, y + 132, 23, "f7", C["text"]) + tx(sub, x + 30, y + 164, 18, "n7", C["muted"]) + '</g>')
    salva("site.svg", svg(w, h, out, "O que tem no site: rosto 3D, abertura, três modos de movimento, onde dói, escala de 0 a 10, manual de marca, transição entre páginas, arquivo único e privacidade"))


# ---------- 10. marca ----------
def marca():
    w, h = 1200, 400
    cores = [("Azul Tami", C["primary"], "#fff"), ("Limão", C["accent"], C["text"]), ("Marinho", C["text"], "#fff"), ("Névoa", C["mist"], C["text"])]
    cams = [("SAMU", C["danger"], "#fff"), ("UPA", C["upa"], C["text"]), ("UBS", C["ubs"], C["text"]), ("Casa", C["casa"], C["text"])]
    out = f'<rect width="{w}" height="{h}" rx="32" fill="#fff" stroke="{C["surface-alt"]}" stroke-width="3"/>'
    for i, (n, hx, cor) in enumerate(cores):
        x = 40 + i * 172
        out += (f'<rect x="{x}" y="40" width="160" height="190" rx="24" fill="{hx}" stroke="{C["surface-alt"] if hx == C["mist"] else hx}" stroke-width="2"/>'
                + tx(n, x + 20, 190, 22, "f7", cor) + tx(hx.upper(), x + 20, 214, 14, "n8", cor, track=.06))
    for i, (n, hx, cor) in enumerate(cams):
        x = 40 + i * 172
        out += (f'<rect x="{x}" y="250" width="160" height="110" rx="22" fill="{hx}"/>'
                + tx(n, x + 20, 300, 22, "f7", cor) + tx(hx.upper(), x + 20, 330, 14, "n8", cor, track=.06))
    x0 = 760
    out += (f'<rect x="{x0}" y="40" width="400" height="320" rx="26" fill="{C["mist"]}"/>'
            + tx("Aa", x0 + 32, 150, 96, "f7", C["primary"]) + tx("Fredoka", x0 + 190, 112, 26, "f7", C["text"])
            + tx("Títulos e números", x0 + 190, 140, 16, "n7", C["muted"])
            + tx("Aa", x0 + 32, 300, 96, "n8", C["text"]) + tx("Nunito", x0 + 190, 262, 26, "n8", C["text"])
            + tx("Textos e botões", x0 + 190, 290, 16, "n7", C["muted"]))
    salva("marca.svg", svg(w, h, out, "Cores da Tami: azul, limão, marinho e névoa, e as cores dos quatro caminhos. Fontes Fredoka e Nunito"))


# ---------- 11. história ----------
def historia():
    w, h = 1200, 300
    etapas = [("O sonho", "Um filme de infância e um robô que cuida das pessoas.", C["accent"]),
              ("O curso", "Sistemas de Informação na FIAP, para melhorar a vida das pessoas.", C["ubs"]),
              ("O nome", "Uma homenagem à Larissa Tami Hokama, médica e amiga.", C["casa"]),
              ("Hoje", "Protótipo funcionando, em teste, por convite.", C["accent"])]
    out = f'<rect width="{w}" height="{h}" rx="32" fill="{C["primary"]}"/><path d="M150 96 H1050" stroke="#fff" stroke-opacity=".35" stroke-width="4" stroke-dasharray="1 12" stroke-linecap="round"/>'
    for i, (tit, sub, cor) in enumerate(etapas):
        cx = 150 + i * 300
        out += (f'<g class="h h{i}"><circle cx="{cx}" cy="96" r="34" fill="{cor}"/>' + tx(f"0{i + 1}", cx, 105, 24, "f7", C["text"], "middle")
                + tx(tit, cx, 178, 28, "f7", "#fff", "middle")
                + "".join(tx(l, cx, 214 + j * 25, 18, "n7", C["dark-muted"], "middle") for j, l in enumerate(quebra(sub, 250, "n7", 18))) + '</g>')
    estilo = "".join(f".h{i}{{animation:hh 8s ease-in-out {i * .4}s infinite}}" for i in range(4)) + "@keyframes hh{0%,100%{translate:0 0}8%{translate:0 -8px}16%{translate:0 0}}"
    salva("historia.svg", svg(w, h, out, "A história da Tami: o sonho, o curso, o nome e hoje", estilo))


# ---------- 12. rodapé ----------
def rodape():
    w, h = 1200, 300
    def onda(y, a, fase):
        pts = " ".join(f"Q{fase + i * 150 + 75} {y - a if i % 2 == 0 else y + a} {fase + (i + 1) * 150} {y}" for i in range(-1, 11))
        return f"M{fase - 150} {y} {pts} V{h + 10} H{fase - 150} Z"
    out = (f'<defs><clipPath id="cr"><rect width="{w}" height="{h}" rx="32"/></clipPath></defs><g clip-path="url(#cr)">'
           f'<rect width="{w}" height="{h}" fill="{C["text"]}"/>'
           f'<path class="o o1" d="{onda(230, 18, 0)}" fill="{C["primary"]}" opacity=".55"/>'
           f'<path class="o o2" d="{onda(250, 14, -75)}" fill="{C["primary"]}"/>'
           f'<g transform="translate(520 52) scale(.5)">{rosto("feliz")}</g>'
           + tx("Cuidar começa com uma conversa", 600, 186, 36, "f7", "#fff", "middle")
           + tx("Falou em saúde? Tamo aqui.", 600, 270, 20, "n8", C["accent"], "middle") + '</g>')
    estilo = ".o{animation:onda 7s ease-in-out infinite alternate}.o2{animation-duration:5s}@keyframes onda{to{translate:-150px 0}}"
    salva("rodape.svg", svg(w, h, out, "Cuidar começa com uma conversa. Falou em saúde? Tamo aqui.", estilo))


# ---------- 13. botões ----------
def botoes():
    def botao(nome, txt, bg, cor, seta_cor, borda=None):
        tam = 20; lw = medir(txt, "n8", tam) + 96; h = 56
        corpo = (f'<rect x="2" y="2" width="{lw - 4:.0f}" height="{h - 4}" rx="{(h - 4) / 2}" fill="{bg}" stroke="{borda or bg}" stroke-width="3"/>'
                 + tx(txt, 30, 36, tam, "n8", cor)
                 + f'<g transform="translate({lw - 40:.0f} 28)">{SETA.format(c=seta_cor)}</g>')
        salva(f"botao-{nome}.svg", svg(int(lw), h, corpo, txt))
    botao("app", "Testar o app", C["accent"], C["text"], C["text"])
    botao("site", "Ver o site", C["primary"], "#fff", C["accent"])
    botao("marca", "Manual de marca", C["primary"], "#fff", C["accent"])
    botao("seguranca", "Segurança", C["text"], "#fff", C["accent"], "#3A4080")
    botao("privacidade", "Privacidade", C["text"], "#fff", C["accent"], "#3A4080")
    # LinkedIn
    tam = 20; txt = "linkedin.com/in/gabrielvasconcellosgomes"; lw = medir(txt, "n8", tam) + 136; h = 64
    corpo = (f'<rect x="2" y="2" width="{lw - 4:.0f}" height="{h - 4}" rx="30" fill="{C["text"]}" stroke="#3A4080" stroke-width="3"/>'
             f'<rect x="14" y="14" width="36" height="36" rx="9" fill="{C["accent"]}"/>'
             + tx("in", 32, 41, 22, "f7", C["text"], "middle") + tx(txt, 64, 39, tam, "n8", "#fff")
             + f'<g transform="translate({lw - 36:.0f} 32)">{SETA.format(c=C["accent"])}</g>')
    salva("botao-linkedin.svg", svg(int(lw), h, corpo, "LinkedIn de Gabriel Gomes"))


# ---------- README ----------
def readme():
    img = lambda n, alt, w=None: f'<img src=".github/readme/{n}.svg" alt="{alt}"{f" width={chr(34)}{w}{chr(34)}" if w else ""}>'
    txt = f'''<div align="center">

{img("capa", "Tami: o jeito mais calmo de decidir para onde ir. Um app de orientação em saúde, em teste.", "100%")}

<br>

{img("selos", "Em teste, sem cookies, sem rastreadores, JavaScript puro, WebGL feito à mão, feito no Brasil", "100%")}

<br>

<a href="{APP}"><img src=".github/readme/botao-app.svg" alt="Testar o app" height="44"></a>&nbsp;
<a href="{SITE}"><img src=".github/readme/botao-site.svg" alt="Ver o site" height="44"></a>&nbsp;
<a href="{SITE}marca/"><img src=".github/readme/botao-marca.svg" alt="Manual de marca" height="44"></a>&nbsp;
<a href="{SITE}seguranca/"><img src=".github/readme/botao-seguranca.svg" alt="Segurança" height="44"></a>&nbsp;
<a href="{SITE}privacidade/"><img src=".github/readme/botao-privacidade.svg" alt="Privacidade" height="44"></a>

</div>

<br>

> [!IMPORTANT]
> A Tami **não é médica, não faz diagnóstico e não indica remédio**. Ela está em teste e pode errar.
> Ainda não passou por avaliação da ANVISA nem por validação clínica. **Em emergência, ligue 192.**

<div align="center">

{img("emergencia", "Em emergência, ligue 192 (SAMU), 188 (CVV), 0800 722 6001 (CIATox) ou 190 (Polícia)", "100%")}

</div>

## O que é a Tami

Na hora do susto, a dúvida quase sempre é a mesma: **para onde eu vou?** Chamar a ambulância, ir hoje a uma UPA, esperar o posto abrir ou cuidar em casa?

A Tami é um app que conversa com você em português simples, pergunta primeiro sobre os sinais de emergência e ajuda a escolher um destes quatro caminhos.

<div align="center">

{img("caminhos", "Os quatro caminhos: Ligue 192 agora, Vá hoje a uma UPA, Procure o posto de saúde nos próximos dias, Dá para cuidar em casa por enquanto", "100%")}

</div>

## Como funciona

<div align="center">

{img("fluxo", "Você conta, a Tami pergunta e vocês decidem para onde ir", "100%")}

<br><br>

{img("escala", "Escala de dor de 0 a 10, com um rostinho para cada número", "100%")}

</div>

<br>

- **Fale em vez de escrever.** A Tami mostra o que ouviu, e você confere antes de mandar.
- **Para quem você cuida.** Bebê, criança, adulto ou idoso.
- **Onde dói?** Toque no corpo e marque o nível da dor.
- **Um resumo para o atendimento.** O que você contou e o caminho sugerido. Sem diagnóstico.

## Por dentro do app

<div align="center">

{img("telas", "Quatro telas do app: falar em vez de escrever, sinais de emergência, onde dói e pessoas que você cuida", "100%")}

</div>

## Segurança primeiro

<div align="center">

{img("faz", "O que a Tami faz e o que ela não faz", "100%")}

</div>

As orientações seguem **regras escritas a partir de materiais públicos de saúde**, como NHS, MedlinePlus e Ministério da Saúde. A Tami não usa inteligência artificial que inventa respostas. Na dúvida, ela sugere o caminho mais urgente.

## Teste o app

O app está aberto para um grupo pequeno de testadores. **[Abrir a Tami]({APP})**

> [!NOTE]
> O servidor gratuito dorme quando ninguém usa. Na primeira visita do dia ele pode levar até 1 minuto para acordar.

## Como o app funciona por dentro

Este repositório também guarda o **app** (pasta `app/`), feito para a Global Solution da FIAP (CitizenTech, 2º semestre de 2026).

| Parte | Como é feito |
| --- | --- |
| Servidor | Java 17 puro, com o servidor HTTP que já vem no Java. Sem framework |
| Motor de regras | Decide a próxima pergunta e o destino. Regras escritas a partir do NHS, MedlinePlus e Ministério da Saúde |
| Árvore do corpo | 38 regiões ligadas umas às outras. Escreveu "braço": a busca cai de 1.169 sinais para cerca de 100 |
| Banco de dados | PostgreSQL (Supabase) ou Oracle, com JDBC, DAOs e transações. 11 tabelas |
| Telas | HTML, CSS e JavaScript puro, sem biblioteca |
| Estatística | Intervalo de confiança de Wilson, kappa de Cohen, qui-quadrado, correlação de Pearson e matriz de confusão |

O motor foi testado com **10.000 casos escritos de propósito com armadilhas** (erro de digitação, negação, texto vago, sintoma no passado). O painel do app mostra os resultados.

<details>
<summary><b>Rodar o app no seu computador</b></summary>
<br>

Precisa do Java 17 ou mais novo.

```bash
cd app
cp tami.properties.exemplo tami.properties   # sem mudar nada, usa um banco local (H2)
./scripts/rodar.sh                           # no Windows: scripts\rodar.bat
# abra http://localhost:8080
```

Para usar o Supabase ou o Oracle, troque a conexão no `tami.properties`. Esse arquivo fica fora do Git: a senha nunca vai para o repositório.

</details>

<details>
<summary><b>Publicar o app (Render)</b></summary>
<br>

O arquivo `render.yaml` já descreve tudo. No Render: **New ➜ Blueprint ➜ este repositório**, e preencha só a senha do banco (`TAMI_DB_SENHA`).

</details>

## O site

A pasta `site/` guarda o site de divulgação da Tami. Ele é feito à mão, sem framework e sem biblioteca de JavaScript.

<div align="center">

{img("site", "O que tem no site", "100%")}

</div>

<details>
<summary><b>Os três modos de movimento</b></summary>
<br>

| Modo | Quando entra | O que acontece |
| --- | --- | --- |
| `show` | Aparelho com folga de memória e processador | Rosto em 3D, bolhas e céu que muda por seção |
| `leve` | Aparelho mais simples, rede lenta ou economia de dados | Rosto em 2D e animações curtas |
| `parado` | Botão "Pausar movimento" ou sistema com menos movimento | Nada se mexe. Tudo continua legível |

Para testar um modo: `?modo=show`, `?modo=leve` ou `?modo=parado`. Para pular a abertura: `?intro=nao`.

</details>

<details>
<summary><b>Rodar no seu computador</b></summary>
<br>

```bash
# 1. gerar o site a partir dos textos
cd site
python3 gen/site2.py

# 2. gerar o PDF e os logos do manual de marca
python3 gen/manual_pdf.py

# 3. conferir regras de texto, links e acessibilidade
python3 gen/checar_site.py
python3 gen/checar_site.py --visual

# 4. abrir no navegador
python3 -m http.server 8790 -d dist
```

Todos os textos do site ficam em `textos.csv` (página ➜ seção ➜ elemento ➜ texto). Mudou o texto, gera de novo.

</details>

<details>
<summary><b>Como as pastas estão organizadas</b></summary>
<br>

```
tami/
├── app/                o app (Java, banco, telas, testes)
│   ├── src/            motor de regras, DAOs, API
│   ├── web/            telas do app
│   └── sql/            scripts do Oracle e do PostgreSQL
├── render.yaml         publicação do app
└── site/
    ├── textos.csv      todos os textos do site
    ├── gen/
    │   ├── site2.py        gera as páginas
    │   ├── manual.py       manual de marca (pranchas)
    │   ├── manual_pdf.py   PDF e logos do manual
    │   ├── unico_site.py   o site inteiro num arquivo só
    │   ├── checar_site.py  regras de texto, links e tela
    │   ├── readme.py       este README e as artes
    │   └── assets/         CSS, JavaScript e o shader WebGL
    └── dist/               o site pronto para publicar
```

</details>

## A marca

<div align="center">

{img("marca", "Cores e fontes da Tami", "100%")}

</div>

O manual completo, com logos para baixar, está em **[{SITE.replace("https://", "")}marca/]({SITE}marca/)**.

## A história

<div align="center">

{img("historia", "A história da Tami: o sonho, o curso, o nome e hoje", "100%")}

</div>

Desde criança, o meu filme favorito tem um robô enfermeiro que escuta, pergunta e cuida de quem está por perto. Eu sempre quis que existisse algo assim de verdade. Escolhi Sistemas de Informação para melhorar a vida das pessoas, e a Tami é o meu jeito de começar.

O nome é uma homenagem à minha amiga **Larissa Tami Hokama**, médica, que está me ajudando no projeto. A ajuda dela não é uma validação clínica: a revisão das regras por profissionais de saúde ainda vai acontecer.

<sub>A Tami é um projeto independente, sem ligação com a Disney ou com o filme.</sub>

## Privacidade

O site **não usa cookies, rastreadores nem ferramentas de análise**. As fontes, as imagens e os códigos ficam no próprio site. A política completa, com o que o app guarda e por quê, está em **[Privacidade]({SITE}privacidade/)**.

## Licenças

| Item | Licença |
| --- | --- |
| Fredoka e Nunito (fontes) | SIL Open Font License 1.1 |
| Driver JDBC do PostgreSQL | BSD 2-Clause |
| Banco H2 (testes locais) | MPL 2.0 ou EPL 1.0 |
| Código, textos e marca da Tami | Projeto Tami. Todos os direitos reservados |

O site não usa nenhuma biblioteca de JavaScript de terceiros. O 3D é um shader WebGL escrito do zero.

## Quem faz

**Gabriel Gomes**, estudante de Sistemas de Informação da FIAP, em São Paulo.

<a href="{LINKEDIN}"><img src=".github/readme/botao-linkedin.svg" alt="LinkedIn de Gabriel Gomes" height="50"></a>

<br>

<div align="center">

{img("rodape", "Cuidar começa com uma conversa. Falou em saúde? Tamo aqui.", "100%")}

</div>
'''
    with open(os.path.join(SAIDA, "README.md"), "w", encoding="utf-8") as fh:
        fh.write(txt)
    print(f"  README.md              {len(txt.encode()) / 1024:6.1f} KB")


if __name__ == "__main__":
    for f in (capa, selos, botoes, emergencia, caminhos, fluxo, escala, telas, faz, site, marca, historia, rodape, readme):
        f()
    print("pronto em", SAIDA)
