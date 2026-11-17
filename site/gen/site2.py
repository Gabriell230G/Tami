"""site2.py · gera o site da Tami v2 (imersivo) a partir de textos.csv.
Saída: dist/ (ao lado da pasta gen). Uso: python3 gen/site2.py
Base: HTML e CSS com o conteúdo real (modos leve e parado). A camada show (assets/cena.js) só acrescenta.
"""
import csv
import html
import math
import os
import re
import shutil
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "marca"))
from tokens import COR  # noqa: E402
from comp import rosto  # noqa: E402
from icons3 import ic3  # noqa: E402
import corpo as CORPO  # noqa: E402

R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = f"{R}/dist"
V1 = os.path.join(R, "gen", "base")
BASE = "https://gabriell230g.github.io/tami/"   # provisório (domínio a confirmar com o Gabriel)
LINKEDIN = "https://www.linkedin.com/in/gabrielvasconcellosgomes"  # contato público (o e-mail saiu do site a pedido do Gabriel)
LINKEDIN_TXT = "linkedin.com/in/gabrielvasconcellosgomes"


def linkedin(cls="email"):
    return f'<a class="{cls}" href="{LINKEDIN}" target="_blank" rel="noopener">{LINKEDIN_TXT}<span class="sr"> (abre em outra aba)</span></a>'

T = {}
for r in csv.DictReader(open(f"{R}/textos.csv", encoding="utf-8"), delimiter=";"):
    T[(r["pagina"], r["secao"], r["elemento"])] = r["texto"]


def t(p, s, e):
    return html.escape(T[(p, s, e)], quote=False)


def ta(p, s, e):
    return html.escape(T[(p, s, e)], quote=True)


PAG = {"inicio": "", "como": "como-funciona/", "seguranca": "seguranca/", "sobre": "sobre/", "perguntas": "perguntas/",
       "beta": "beta/", "imprensa": "imprensa/", "privacidade": "privacidade/", "movimento": "movimento/", "marca": "marca/"}
OG = {"inicio": "inicio", "como": "como-funciona", "seguranca": "seguranca", "sobre": "sobre", "perguntas": "perguntas",
      "beta": "beta", "imprensa": "imprensa", "privacidade": "privacidade", "p404": "inicio", "movimento": "inicio", "marca": "imprensa"}


def raiz(de):
    if de == "p404":
        return "/tami/"   # 404 do GitHub Pages: caminho absoluto, abre com estilo em qualquer endereço
    return "../" * PAG.get(de, "").count("/") or "./"


def link(de, para):
    return raiz(de) + PAG[para] + "index.html"


# ---------------------------------------------------------------- peças
def ic(nome, w=24, cor=None, acento=None, wcor=None):
    return ic3(nome, w, m=cor or COR["primary"], a=acento or COR["accent"], wcor=wcor).replace("<svg", '<svg aria-hidden="true" focusable="false"', 1)


def face(tipo="oi", cor="#FFFFFF", pisca=False):
    s = rosto(cor, 320, tipo)
    return s.replace("<svg", '<svg class="rosto2d pisca2"' if pisca else '<svg class="rosto2d"', 1)


def alvo(tipo, cor="#FFFFFF", pisca=False, extra=""):
    return f'<div class="rosto-alvo {extra}" data-rosto-alvo aria-hidden="true">{face(tipo, cor, pisca)}</div>'


ICONE_FONE = ('<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M6.6 2.6c.6-.5 1.5-.4 2 .2l2 2.6c.4.6.4 1.4-.1 1.9l-1.3 1.4a12.6 12.6 0 0 0 6.1 6.1l1.4-1.3c.5-.5 1.3-.5 1.9-.1l2.6 2c.6.5.7 1.4.2 2l-1.4 1.8c-.8 1-2.1 1.4-3.3 1.1C9.8 18.6 5.4 14.2 3.7 7.4c-.3-1.2.1-2.5 1.1-3.3z"/></svg>')
ICONE_PAUSA = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="6" y="5" width="4" height="14" rx="2" fill="currentColor"/><rect x="14" y="5" width="4" height="14" rx="2" fill="currentColor"/></svg>'
ICONE_SETA = '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><path d="M5 12h13m-5-6 6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
ICONE_BAIXO = '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false"><path d="m6 9 6 6 6-6" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
ICONE_SIM = ('<svg viewBox="0 0 26 26" aria-hidden="true" focusable="false"><circle cx="13" cy="13" r="13" fill="#4F6B00"/>'
             '<path d="m7.5 13.5 3.6 3.6 7.4-8" fill="none" stroke="#fff" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/></svg>')
ICONE_NAO = ('<svg viewBox="0 0 26 26" aria-hidden="true" focusable="false"><circle cx="13" cy="13" r="13" fill="#454A78"/>'
             '<path d="m8.5 8.5 9 9m0-9-9 9" fill="none" stroke="#fff" stroke-width="2.8" stroke-linecap="round"/></svg>')


def faixa():
    return (f'<aside class="faixa" aria-label="Emergência"><a href="tel:192">{ICONE_FONE}'
            f'<span>{t("geral", "faixa", "texto")}</span></a></aside>')


MENU = [("como", "como"), ("seguranca", "seguranca"), ("sobre", "sobre"), ("perguntas", "perguntas"), ("imprensa", "imprensa")]


def topo(de, ceu):
    itens = "".join(f'<li><a href="{link(de, p)}"{" aria-current=\"page\"" if p == de else ""}>{t("geral", "menu", k)}</a></li>' for p, k in [("inicio", "inicio")] + MENU)
    marca = (f'<a class="marca" href="{link(de, "inicio")}"><span class="mono" aria-hidden="true"><b>t</b><i></i></span>'
             f'<span class="nome">tami<span class="sr">, página inicial</span></span></a>')
    mov = (f'<button type="button" class="mov" data-mov hidden aria-pressed="false" data-pausar="{ta("geral", "mov", "pausar")}" '
           f'data-ligar="{ta("geral", "mov", "ligar")}">{ICONE_PAUSA}<span>{t("geral", "mov", "pausar")}</span></button>')
    return (f'<header class="topo" data-ceu="{ceu}"><div class="cx">{marca}'
            f'<nav class="nav" aria-label="Principal"><ul>{itens}</ul></nav>'
            f'<div class="topo-acoes">{mov}<a class="btn btn-p btn-topo" href="{link(de, "beta")}">{t("geral", "menu", "botao")}</a>'
            f'<details class="menu-cel"><summary>{t("geral", "menu", "abrir")}<span class="sr"> do site</span></summary>'
            f'<div class="folha"><nav aria-label="Menu do celular"><ul>{itens}</ul></nav>'
            f'<a class="btn btn-p" href="{link(de, "beta")}">{t("geral", "menu", "botao")}</a></div></details></div></div></header>')


def emerg(classe=""):
    itens = [("tel:192", "samu", "e192", "fone"), ("tel:188", "cvv", "", "escuta"), ("tel:08007226001", "ciatox", "", "alerta"), ("tel:190", "policia", "", "avisar")]
    li = []
    for href, e, cl, icone in itens:
        if cl:
            sv = ICONE_FONE.replace('aria-hidden', 'width="28" height="28" aria-hidden')
        else:
            sv = ic(icone, 28, "#FFFFFF" if classe != "claro" else COR["primary"], COR["accent"] if classe != "claro" else COR["accent-mid"],
                    wcor=COR["text"] if classe != "claro" else "#FFFFFF")
        txt = t("geral", "rodape", e)
        if "grande" in classe:  # número grande em cima, o resto embaixo: os quatro cartões com o mesmo peso
            num, _, resto = txt.partition(" · ")
            corpo = f'<span class="en"><b>{num}</b><small>{resto}</small></span>'
        else:
            corpo = f'<span>{txt}</span>'
        li.append(f'<li><a class="{cl}" href="{href}">{sv}{corpo}</a></li>')
    return f'<ul class="emerg {classe}">{"".join(li)}</ul>'


def _onda(amp, per, base=0, larg=2000, fundo=420):
    pts = " ".join(f"L{x} {base + amp * math.sin(2 * math.pi * x / per):.1f}" for x in range(0, larg + 1, 20))
    return f"M0 {base} {pts[pts.index(' ') + 1:] if False else pts} L{larg} {fundo} L0 {fundo}Z".replace("M0 0 L0", "M0", 1)


# "tami" gigante no rodapé: letras azuis com um líquido branco dentro, em ondas que andam devagar
MARCA_LIQUIDA = ('<div class="marca-liquida" aria-hidden="true"><svg viewBox="0 0 1000 330" focusable="false">'
                 '<defs><clipPath id="ml-letras"><text x="500" y="292" text-anchor="middle">tami</text></clipPath></defs>'
                 '<text class="ml-letras" x="500" y="292" text-anchor="middle">tami</text>'
                 '<g clip-path="url(#ml-letras)"><g class="ml-nivel"><g class="ml-inclina">'
                 f'<path class="ml-onda ml-o2" d="{_onda(16, 330)}"/><path class="ml-onda ml-o1" d="{_onda(12, 250)}"/>'
                 '</g></g></g></svg></div>')


def rodape(de):
    links = "".join(f'<li><a href="{link(de, p)}">{t("geral", "menu", k)}</a></li>' for p, k in [("inicio", "inicio")] + MENU)
    links += f'<li><a href="{link(de, "beta")}">{t("geral", "menu", "botao")}</a></li>'
    links += f'<li><a href="{link(de, "marca")}">{t("geral", "rodape", "marca")}</a></li>'
    return (f'<footer class="rodape"><div class="cx"><div class="grade">'
            f'<div><h2>{t("geral", "rodape", "titulo")}</h2>{emerg()}</div>'
            f'<div><h2>{t("geral", "rodape", "paginas")}</h2><ul class="links">{links}'
            f'<li><a href="{link(de, "privacidade")}">{t("geral", "rodape", "privacidade")}</a></li></ul>'
            f'<p style="margin-top:16px">{t("geral", "rodape", "contato")}: {linkedin()}</p>'
            f'<p class="nota">{t("geral", "rodape", "nao_mande")}</p></div></div>'
            f'<div class="base"><p class="slogan">{t("geral", "rodape", "slogan")}</p>'
            f'<p><strong>{t("geral", "aviso", "texto")}</strong> {t("geral", "aviso", "emergencia")}</p>'
            f'<p>{t("geral", "rodape", "teste")} {t("geral", "rodape", "anvisa")}</p>'
            f'<p>{t("geral", "rodape", "autoria")}</p></div></div>'
            f'{MARCA_LIQUIDA}</footer>')


def convite_fim(de):
    return (f'<section class="convite-fim" aria-labelledby="cf-{de}"><div class="cx"><div><h2 id="cf-{de}">{t("geral", "convite", "titulo")}</h2>'
            f'<p>{t("geral", "convite", "texto")}</p></div><a class="btn btn-p" href="{link(de, "beta")}">{t("geral", "convite", "botao")}</a></div></section>')


MODO = ('(function(d){var m,q,s,n=navigator,c=n.connection||{};try{s=localStorage.getItem("tami-mov")}catch(e){}'
        'try{q=new URLSearchParams(location.search).get("modo")}catch(e){}var ok=/^(show|leve|parado)$/;'
        'var rm=matchMedia("(prefers-reduced-motion: reduce)").matches;'
        'var fraco=(n.deviceMemory&&n.deviceMemory<=4)||(n.hardwareConcurrency&&n.hardwareConcurrency<=4)||c.saveData||/2g|3g/.test(c.effectiveType||"");'
        'd.dataset.auto=fraco?"leve":"show";m=ok.test(q)?q:ok.test(s)?s:rm?"parado":fraco?"leve":"show";d.className+=" js "+m;d.dataset.modo=m;'
        # transição entre páginas: some quando o movimento está pausado
        'function pula(e){if(e.viewTransition&&d.dataset.modo==="parado")e.viewTransition.skipTransition()}'
        'addEventListener("pageswap",pula);addEventListener("pagereveal",pula)})(document.documentElement)')


ABRE_JS = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "abre.js"), encoding="utf-8").read()
ABRE_JS = re.sub(r"\n\s*//[^\n]*", "", ABRE_JS)
ABRE_JS = re.sub(r"\n\s*", "", ABRE_JS)

def pagina(de, ceu_topo, corpo_html, sem_convite=False, noindex=False):
    r = raiz(de)
    titulo = T[(de, "meta", "titulo")]
    desc = T[(de, "meta", "descricao")]
    canon = BASE + PAG.get(de, "")
    og = f"{BASE}img/og-{OG[de]}.jpg"
    robots = '<meta name="robots" content="noindex">' if noindex or de == "p404" else ""
    return f"""<!DOCTYPE html>
<html lang="pt-BR" data-raiz="{r}" data-aviso-leve="{ta("geral", "mov", "aviso_leve")}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(titulo)}</title>
<meta name="description" content="{html.escape(desc)}">
{robots}<link rel="canonical" href="{canon}">
<meta name="theme-color" content="#1B1F4A">
<meta property="og:type" content="website"><meta property="og:locale" content="pt_BR">
<meta property="og:title" content="{html.escape(titulo)}"><meta property="og:description" content="{html.escape(desc)}">
<meta property="og:image" content="{og}"><meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:image:alt" content="Rosto da Tami e o título da página.">
<meta name="twitter:card" content="summary_large_image">
<link rel="icon" href="{r}img/icone-32.png" sizes="32x32"><link rel="apple-touch-icon" href="{r}img/icone-180.png">
<link rel="manifest" href="{r}site.webmanifest">
<link rel="preload" href="{r}fonts/fredoka-latin-600-normal.woff2" as="font" type="font/woff2" crossorigin>
<link rel="preload" href="{r}fonts/nunito-latin-600-normal.woff2" as="font" type="font/woff2" crossorigin>
<script>{MODO}</script>
<link rel="stylesheet" href="{r}assets/tokens-web.css"><link rel="stylesheet" href="{r}assets/site.css">{f'<link rel="stylesheet" href="{r}assets/marca.css">' if de == "marca" else ""}
<script src="{r}assets/tami.js" defer></script>
<script src="{r}assets/cenas2.js" defer></script>
</head>
<body>
<a class="pular" href="#conteudo">{t("geral", "menu", "pular")}</a>
{faixa()}
<p id="aviso-modo" class="aviso-modo" role="status" hidden></p>
<div class="progresso" aria-hidden="true"></div>
{topo(de, ceu_topo)}
<main id="conteudo" tabindex="-1">
{corpo_html}
{'' if sem_convite else convite_fim(de)}
</main>
{rodape(de)}
</body>
</html>
"""


def figura(nome, alt, de, classe="celular"):
    r = raiz(de)
    larg = "(min-width: 900px) 300px, 78vw"
    return (f'<div class="{classe}"><picture>'
            f'<source type="image/avif" srcset="{r}img/{nome}-360.avif 360w, {r}img/{nome}-720.avif 720w" sizes="{larg}">'
            f'<source type="image/webp" srcset="{r}img/{nome}-360.webp 360w, {r}img/{nome}-720.webp 720w" sizes="{larg}">'
            f'<img src="{r}img/{nome}-720.webp" width="360" height="779" alt="{alt}" loading="lazy" decoding="async"></picture></div>')


def topo_pag(de, ceu, expr, cor_rosto="#FFFFFF"):
    return (f'<section class="cena topo-pag" data-cena data-ceu="{ceu}" data-rosto="{expr}" data-bolhas="nao" aria-labelledby="h1">'
            f'<div class="cx"><div class="txt"><p class="rotulo">{t(de, "topo", "rotulo")}</p><h1 id="h1" data-cinetico>{t(de, "topo", "titulo")}</h1>'
            f'<p class="sub">{t(de, "topo", "sub")}</p></div>{alvo(expr, cor_rosto)}</div></section>')


# ---------------------------------------------------------------- início
def mock_caminhos(p="inicio"):
    cams = "".join(f'<li class="m-{k}"><b>{t("inicio", "c4", k + "_rotulo")}</b><span>{t("inicio", "c4", k + "_titulo")}</span></li>' for k in ("samu", "upa", "ubs", "casa"))
    return f'<div class="mock" role="img" aria-label="{ta("inicio", "orb", "a5")}"><p class="mock-t">Os quatro caminhos</p><ul>{cams}</ul></div>'


def mock_resumo():
    return ('<div class="mock" role="img" aria-label="Exemplo de resumo para o atendimento."><p class="mock-t">Resumo</p><dl>'
            '<dt>Para quem</dt><dd>Você, 30 anos</dd><dt>O que foi contado</dt><dd>Torceu o tornozelo jogando bola. Está inchado.</dd>'
            '<dt>Sinais de emergência</dt><dd>Nenhum confirmado</dd><dt>Caminho sugerido</dt><dd>Cuidar em casa</dd></dl>'
            '<p class="mock-n">A Tami não faz diagnóstico. O resumo mostra o que foi contado e o caminho sugerido.</p></div>')


def orbita(de):
    """Galeria 3D das telas do app. Sem JS ou no modo parado: grade simples (a lista abaixo)."""
    r = raiz(de)
    telas = ["voz", "alarme", "onde-doi", "pessoas", None, None]
    itens = []
    for i, nome in enumerate(telas, 1):
        if nome:
            fig = (f'<picture><source type="image/avif" srcset="{r}img/{nome}-360.avif 360w, {r}img/{nome}-720.avif 720w" sizes="220px">'
                   f'<img src="{r}img/{nome}-360.webp" srcset="{r}img/{nome}-360.webp 360w, {r}img/{nome}-720.webp 720w" sizes="220px" '
                   f'width="360" height="779" alt="{ta("inicio", "orb", f"a{i}")}" loading="lazy" decoding="async" draggable="false"></picture>')
        else:
            fig = mock_caminhos() if i == 5 else mock_resumo()
        itens.append(f'<li class="orb-item" data-titulo="{ta("inicio", "orb", f"t{i}")}"><div class="orb-tela">{fig}</div><p>{t("inicio", "orb", f"t{i}")}</p></li>')
    return (f'<section class="cena orbita" data-cena data-ceu="fundo" data-rosto="olha" data-bolhas="nao" aria-labelledby="h-orb-{de}">'
            f'<div class="cx"><header><p class="rotulo">{t("inicio", "orb", "rotulo")}</p><h2 id="h-orb-{de}">{t("inicio", "orb", "titulo")}</h2>'
            f'<p>{t("inicio", "orb", "sub")}</p></header></div>'
            f'<div class="orb" data-orbita aria-roledescription="carrossel" aria-label="{ta("inicio", "orb", "rotulo")}">'
            f'<ul class="orb-lista">{"".join(itens)}</ul>'
            f'<div class="orb-palco" tabindex="0" aria-label="{ta("inicio", "orb", "aria")}" hidden><div class="orb-anel"></div></div>'
            f'<div class="orb-info" hidden><p class="orb-cont"><span class="orb-n">01</span><span aria-hidden="true">/</span><span>06</span></p>'
            f'<div class="orb-sel"><button type="button" class="orb-seta" data-dir="-1" aria-label="{ta("inicio", "orb", "anterior")}">{ICONE_SETA.replace("<path", "<path transform=\"rotate(180 12 12)\"", 1)}</button>'
            f'<p class="orb-titulo fred">{t("inicio", "orb", "t1")}</p>'
            f'<button type="button" class="orb-seta" data-dir="1" aria-label="{ta("inicio", "orb", "proxima")}">{ICONE_SETA}</button></div>'
            f'<div class="orb-pontos" data-ver="{ta("inicio", "orb", "ver")}"></div>'
            f'<p class="sr" aria-live="polite" data-anuncio></p></div></div></section>')


CARAS = {  # reações da escala, no traço da Tami (olhos de pílula)
    "b1": '<rect x="8" y="20" width="48" height="112" rx="24"/><rect x="264" y="20" width="48" height="112" rx="24"/><path d="M118 78 A42 42 0 0 0 202 78" class="tr"/>',
    "b2": '<rect x="8" y="20" width="48" height="112" rx="24"/><rect x="264" y="20" width="48" height="112" rx="24"/><path d="M122 104 L198 104" class="tr"/>',
    "b3": '<rect x="8" y="46" width="48" height="70" rx="24"/><rect x="264" y="46" width="48" height="70" rx="24"/><path d="M122 124 A40 40 0 0 1 198 124" class="tr"/>',
}


CORES_N = ["#B6EC0C", "#C8F03A", "#D8F25C", "#E8F27A", "#FFE36B", "#FFCC4D", "#FFB347", "#FF9F7E", "#FF8B8B", "#F7707C", "#EE5865"]


def cara_n(n):
    """Rostinho da Tami para cada número (0 a 10), estilo adesivo: cor do limão ao vermelho, contorno escuro,
    bochechas nos números baixos, sobrancelhas preocupadas a partir do 5 e boca aberta no 0 (rindo) e no 10 (de dor)."""
    m = "#1B1F4A"
    out = [f'<circle cx="50" cy="50" r="45" fill="{CORES_N[n]}" stroke="{m}" stroke-width="4.5"/>',
           '<ellipse cx="34" cy="26" rx="14" ry="8" fill="#fff" opacity=".45" transform="rotate(-30 34 26)"/>']
    if n <= 2:  # bochechas
        op = (.75, .55, .4)[n]
        out.append(f'<ellipse cx="22" cy="60" rx="7" ry="4.5" fill="#FF7FA0" opacity="{op}"/><ellipse cx="78" cy="60" rx="7" ry="4.5" fill="#FF7FA0" opacity="{op}"/>')
    if 5 <= n <= 8:  # sobrancelhas: a ponta de dentro sobe cada vez mais (no 9 e no 10 os olhos apertados já dizem tudo)
        d = 2 + (n - 5) * 2.2
        out.append(f'<path d="M23 {30 + d:.1f} L40 {30 - d * .4:.1f}" stroke="{m}" stroke-width="5" stroke-linecap="round" fill="none"/>'
                   f'<path d="M77 {30 + d:.1f} L60 {30 - d * .4:.1f}" stroke="{m}" stroke-width="5" stroke-linecap="round" fill="none"/>')
    if n == 0:
        out.append(f'<path d="M24 46 Q32 36 40 46" stroke="{m}" stroke-width="6" stroke-linecap="round" fill="none"/><path d="M60 46 Q68 36 76 46" stroke="{m}" stroke-width="6" stroke-linecap="round" fill="none"/>')
    elif n >= 9:
        out.append(f'<path d="M25 37 L39 45 L25 53" stroke="{m}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/>'
                   f'<path d="M75 37 L61 45 L75 53" stroke="{m}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/>')
    else:
        h = 18 - max(0, n - 2) * 1.4
        out.append(f'<rect x="27" y="{45 - h / 2:.1f}" width="10" height="{h:.1f}" rx="5" fill="{m}"/><rect x="63" y="{45 - h / 2:.1f}" width="10" height="{h:.1f}" rx="5" fill="{m}"/>'
                   f'<circle cx="34.5" cy="{45 - h / 2 + 4:.1f}" r="2" fill="#fff"/><circle cx="70.5" cy="{45 - h / 2 + 4:.1f}" r="2" fill="#fff"/>')
    if n == 0:
        out.append(f'<path d="M30 60 Q50 90 70 60 Z" fill="{m}" stroke="{m}" stroke-width="3" stroke-linejoin="round"/><ellipse cx="50" cy="74" rx="8" ry="5" fill="#FF7FA0"/>')
    elif n == 1:
        out.append(f'<path d="M33 62 Q50 82 67 62 Z" fill="{m}" stroke="{m}" stroke-width="3" stroke-linejoin="round"/>')
    elif n >= 9:
        w = 16 if n == 9 else 20
        out.append(f'<path d="M{50 - w} 78 Q50 {54 if n == 10 else 58} {50 + w} 78 Z" fill="{m}" stroke="{m}" stroke-width="3" stroke-linejoin="round"/>')
    else:
        c = 13 - (n - 2) * 26 / 6
        y = 68 - c / 2
        out.append(f'<path d="M35 {y:.1f} Q50 {y + c:.1f} 65 {y:.1f}" stroke="{m}" stroke-width="6" stroke-linecap="round" fill="none"/>')
    return "".join(out)


def caras_escala(p):
    faixa = lambda n: "b1" if n <= 3 else "b2" if n <= 6 else "b3"
    caras = "".join(f'<span class="esc-cara {faixa(n)}" data-n="{n}"><svg viewBox="0 0 100 100" aria-hidden="true">{cara_n(n)}</svg></span>' for n in range(11))
    rots = "".join(f'<i class="esc-rot {k}">{t(p, "c3", "esc_" + k)}</i>' for k in ("b1", "b2", "b3"))
    return caras + rots


def escala(p):
    """Escala de 0 a 10 com 3 reações (as faixas são as mesmas do app: 0 a 3, 4 a 6, 7 a 10)."""
    caras = caras_escala(p)
    nums = "".join(f'<li class="{"b1" if n <= 3 else "b2" if n <= 6 else "b3"}">{n}</li>' for n in range(11))
    return f'<span class="escala" role="img" aria-label="{ta(p, "c3", "esc_rotulo")}"><span class="esc-caras">{caras}</span><ol class="esc-num" aria-hidden="true">{nums}</ol></span>'


def cartao_dor(p):
    """Cartão que abre ao tocar no corpo: quanto dói (0 a 10) e como é a dor. Só demonstração, nada é guardado."""
    nums = "".join(f'<button type="button" class="dn-n" data-n="{n}" aria-pressed="false">{n}</button>' for n in range(11))
    tipos = "".join(f'<button type="button" class="dn-t" aria-pressed="false">{html.escape(x)}</button>' for x in T[(p, "c5", "tipos")].split("|"))
    return (f'<div class="dor-cartao" id="dor-cartao" role="group" aria-labelledby="dn-nome" hidden data-niveis="{ta(p, "c5", "niveis")}">'
            f'<p class="dn-nome" id="dn-nome"><i aria-hidden="true"></i><span></span></p>'
            f'<div class="dn-linha"><p class="dn-q" id="dn-q">{t(p, "c5", "quanto")}</p><p class="dn-valor" aria-live="polite"></p></div>'
            f'<span class="esc-caras" aria-hidden="true">{caras_escala(p)}</span><div class="dn-nums" role="group" aria-labelledby="dn-q">{nums}</div>'
            f'<p class="dn-q" id="dn-como">{t(p, "c5", "como")}</p><div class="dn-tipos" role="group" aria-labelledby="dn-como">{tipos}</div>'
            f'<div class="dn-acoes"><button type="button" class="dn-tira">{t(p, "c5", "desmarcar")}</button>'
            f'<button type="button" class="btn btn-p dn-pronto">{t(p, "c5", "pronto")}</button></div></div>')


def inicio():
    p = "inicio"
    # abertura (cerca de 4,5 s, pedido do Gabriel; uma vez por visita, com Pular; não aparece no modo parado)
    def letras(txt):
        return " ".join('<span class="pal">' + "".join(f'<span data-l>{c}</span>' for c in pal) + '</span>' for pal in txt.split(" "))
    abertura = (f'<div class="abre" id="abre" hidden><div class="abre-cena">'
                '<svg class="abre-rosto" viewBox="0 0 320 160" aria-hidden="true">'
                '<rect class="abre-olho" x="8" y="20" width="48" height="112" rx="24"/><rect class="abre-olho abre-pisca" x="264" y="20" width="48" height="112" rx="24"/>'
                '<path class="abre-boca" d="M123 82 A37 37 0 0 0 197 82"/></svg>'
                f'<p class="abre-txt" aria-hidden="true"><span class="abre-l1">{letras(T[(p, "intro", "oi1")])}</span>'
                f'<span class="abre-l2">{letras(T[(p, "intro", "oi2")])}</span></p></div>'
                '<svg class="abre-liquido" aria-hidden="true"><defs>'
                '<filter id="gosma" filterUnits="userSpaceOnUse" x="0" y="0"><feGaussianBlur in="SourceGraphic" stdDeviation="16" result="b"/>'
                '<feColorMatrix in="b" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 24 -10"/></filter></defs>'
                '<g class="liq-lima" filter="url(#gosma)"></g><g class="liq-azul" filter="url(#gosma)"></g></svg>'
                f'<button class="abre-pular" type="button">{t(p, "intro", "pular")}</button></div>'
                f'<script>{ABRE_JS}</script>')

    # cena 1 · primeira tela: só o rosto da Tami, grande, ao lado do título (as bolinhas saíram a pedido do Gabriel)
    c1 = (f'<section class="cena heroi5" data-cena data-ceu="calmo" data-rosto="feliz" data-bolhas="nao" aria-labelledby="h1">'
          f'<div class="cx h5-cx"><div class="h5-rosto" aria-hidden="true">{alvo("feliz", COR["primary"], pisca=True)}</div>'
          f'<div class="h5-txt"><p class="selo">{t(p, "c1", "selo")}</p>'
          f'<h1 id="h1">{t(p, "c1", "titulo")}</h1>'
          f'<p class="aviso-chegada">{t("geral", "aviso", "texto")}</p>'
          f'<a class="btn btn-p h5-btn" href="{link(p, "beta")}">{t(p, "c1", "botao")}</a>'
          f'<a class="btn btn-s h5-btn" href="{link(p, "como")}">{t(p, "c1", "link")}</a></div></div></section>')

    # cena 2 · a noite das dúvidas
    duv = "".join(f'<li>{t(p, "c2", k)}</li>' for k in ("d1", "d2", "d3", "d4", "d5"))
    c2 = (f'<section class="cena noite2" data-cena data-ceu="susto" data-rosto="pensa" data-bolhas="caos" aria-labelledby="h-c2">'
          f'<div class="cx"><p class="rotulo">{t(p, "c2", "rotulo")}</p>'
          f'<ul class="duvidas" aria-label="Dúvidas comuns na hora do susto">{duv}</ul>'
          f'<h2 id="h-c2">{t(p, "c2", "titulo")}</h2><p class="vidro">{t(p, "c2", "texto")}</p></div></section>')

    lista = T[(p, "c3", "m2")].split(" • ")
    chat = (f'<ol class="chat" data-conversa aria-label="{ta(p, "c3", "rotulo")}">'
            f'<li class="voce" data-passo><span class="sr">Você: </span>{t(p, "c3", "m1")}</li>'
            f'<li class="ela" data-passo><span class="sr">Tami: </span>{t(p, "c3", "m2_intro")}<br>{html.escape(lista[0])}'
            f'<ul class="lista">{"".join(f"<li>{html.escape(x)}</li>" for x in lista[1:])}</ul></li>'
            f'<li class="voce" data-passo><span class="sr">Você: </span>{t(p, "c3", "m3")}</li>'
            f'<li class="ela" data-passo><span class="sr">Tami: </span>{t(p, "c3", "m4")}{escala(p)}</li>'
            f'<li class="voce marca-fraco" data-passo><span class="sr">Você: </span>{t(p, "c3", "m5")}</li>'
            f'<li class="resultado" data-passo><b>{t(p, "c3", "r_rotulo")}</b><strong>{t(p, "c3", "r_titulo")}</strong>{t(p, "c3", "r_frase")}</li></ol>')
    c3 = (f'<section class="cena conversa-cena" data-cena data-ceu="azul" data-rosto="ouve" data-rostos="ouve,pensa,ouve,pensa,ouve,acolhe" data-bolhas="calmas" aria-labelledby="h-c3">'
          f'<div class="trilho"><div class="fixo"><div class="cx"><div class="lado"><p class="rotulo">{t(p, "c3", "rotulo")}</p>'
          f'<h2 id="h-c3">{t(p, "c3", "titulo")}</h2>{alvo("ouve")}<p class="nota">{t(p, "c3", "nota")}</p></div>'
          f'{chat}</div></div></div></section>')

    # cena 4 · os 4 caminhos em painéis: o da vez se abre, os outros viram faixas (opção 4 aprovada)
    EXP4 = {"samu": "firme", "upa": "olha", "ubs": "pensa", "casa": "feliz"}
    pns = "".join(
        f'<li class="pn p-{k}" data-i="{i}"><button type="button" class="pn-faixa" aria-controls="pn-{k}" aria-expanded="{"true" if i == 0 else "false"}">'
        f'<span>{t(p, "c4", k + "_rotulo")}</span></button>'
        f'<div class="pn-in" id="pn-{k}"><div class="pn-cara" aria-hidden="true">{face(EXP4[k], "#FFFFFF" if k == "samu" else COR["text"])}</div>'
        f'<p class="pn-rot">{i + 1} · {t(p, "c4", k + "_rotulo")}</p><h3>{t(p, "c4", k + "_titulo")}</h3><p class="pn-frase">{t(p, "c4", k + "_texto")}</p></div></li>'
        for i, k in enumerate(("samu", "upa", "ubs", "casa")))
    c4 = (f'<section class="cena caminhos paineis" data-cena data-ceu="fundo" data-rosto="olha" data-bolhas="nao" data-paineis data-k="0" aria-labelledby="h-c4">'
          f'<div class="pn-trilho"><div class="pn-palco"><div class="pn-topo"><p class="rotulo">{t(p, "c4", "rotulo")}</p>'
          f'<h2 id="h-c4">{t(p, "c4", "fantasma")}<span class="sr"> {t(p, "c4", "sr")}</span></h2></div>'
          f'<ol class="pn-lista">{pns}</ol></div></div></section>')

    c5 = (f'<section class="cena onde" data-cena data-ceu="nevoa" data-rosto="acolhe" data-bolhas="nao" aria-labelledby="h-c5">'
          f'<div class="cx"><div class="txt"><p class="rotulo">{t(p, "c5", "rotulo")}</p><h2 id="h-c5">{t(p, "c5", "titulo")}</h2>'
          f'<p>{t(p, "c5", "texto")}</p><p class="dica-corpo" data-so-js hidden>{t(p, "c5", "dica")}</p></div>{cartao_dor(p)}'
          f'<div class="figura" data-corpo="{raiz(p)}img/corpo-frente.svg"><img src="{raiz(p)}img/corpo-frente.svg" width="280" height="714" '
          f'alt="{ta(p, "c5", "alt")}" loading="lazy" decoding="async"></div></div></section>')

    onda = '<span class="onda" aria-hidden="true">' + "".join(f'<i style="--k:{i}"></i>' for i in range(7)) + '</span>'
    cart = [("mic", "voz", onda), ("ouvir", "ouvir", ""), ("pessoas", "cuido", ""), ("historico", "resumo", "")]
    pil = "".join(f'<li style="--k:{i}"><span class="ico">{ic(n, 30, "#FFFFFF", COR["accent"], wcor=COR["primary"])}</span>'
                  f'<h3>{t(p, "c6", k + "_titulo")}</h3><p>{t(p, "c6", k + "_texto")}</p>{x}</li>' for i, (n, k, x) in enumerate(cart))
    c6 = (f'<section class="cena pressa" data-cena data-ceu="azul" data-rosto="ouve" data-bolhas="calmas" aria-labelledby="h-c6">'
          f'<div class="cx"><div class="txt"><p class="rotulo">{t(p, "c6", "rotulo")}</p><h2 id="h-c6">{t(p, "c6", "titulo")}</h2></div>'
          f'<ul class="pilha">{pil}</ul></div></section>')

    faz = "".join(f'<li>{ICONE_SIM}<span>{t(p, "c7", f"faz{i}")}</span></li>' for i in range(1, 5))
    nao = "".join(f'<li>{ICONE_NAO}<span>{t(p, "c7", f"nao{i}")}</span></li>' for i in range(1, 5))
    c7 = (f'<section class="cena seg" data-cena data-ceu="calmo" data-rosto="firme" data-bolhas="nao" aria-labelledby="h-c7">'
          f'<div class="cx"><header><p class="rotulo">{t(p, "c7", "rotulo")}</p><h2 id="h-c7">{t(p, "c7", "titulo")}</h2>{alvo("firme", COR["primary"], extra="mini")}</header>'
          f'<div class="lados"><div class="lado-box lado-faz"><h3>{t(p, "c7", "faz_titulo")}</h3><ul>{faz}</ul></div>'
          f'<div class="lado-box lado-nao"><h3>{t(p, "c7", "nao_titulo")}</h3><ul>{nao}</ul></div></div>'
          f'<div class="dentro"><div><h3>{t(p, "c7", "dentro_titulo")}</h3><p>{t(p, "c7", "dentro")}</p></div>'
          f'<div><h3>{t("seguranca", "teste", "titulo")}</h3><p>{t(p, "c7", "anvisa")}</p>'
          f'<a class="link-seta" href="{link(p, "seguranca")}">{t(p, "c7", "link")} {ICONE_SETA}</a></div></div></div></section>')

    tl = "".join(f'<li class="{"feito" if i == 1 else ""}"><b>{t(p, "c8", f"t{i}_quando")}</b><span>{t(p, "c8", f"t{i}")}</span></li>' for i in range(1, 5))
    c8 = (f'<section class="cena quem" data-cena data-ceu="nevoa" data-rosto="oi" data-bolhas="nao" aria-labelledby="h-c8">'
          f'<div class="cx"><div class="txt"><p class="rotulo">{t(p, "c8", "rotulo")}</p><h2 id="h-c8">{t(p, "c8", "titulo")}</h2>'
          f'<p>{t(p, "c8", "texto")}</p><a class="link-seta" href="{link(p, "sobre")}">{t(p, "c8", "link")} {ICONE_SETA}</a></div>'
          f'<ol class="linha">{tl}</ol></div></section>')

    c9 = (f'<section class="cena convite" data-cena data-ceu="aurora" data-rosto="feliz" data-bolhas="nao" aria-labelledby="h-c9">'
          f'<div class="cx">{alvo("feliz", COR["primary"])}<h2 id="h-c9">{t(p, "c9", "titulo")}</h2><p>{t(p, "c9", "texto")}</p>'
          f'<a class="btn btn-p" href="{link(p, "beta")}">{t(p, "c9", "botao")}</a></div></section>')
    return pagina(p, "calmo", abertura + c1 + c2 + c3 + c4 + c5 + c6 + orbita(p) + c7 + c8 + c9, sem_convite=True)


# ---------------------------------------------------------------- internas
def como():
    """Como funciona: 5 passos numa linha do tempo que se enche ao rolar, por dentro da Tami e o que ela não faz."""
    p = "como"
    imgs = {"c1": "voz", "c2": "alarme", "c3": "onde-doi"}
    mocks = {"c4": f'<div class="celular">{mock_caminhos()}</div>', "c5": f'<div class="celular">{mock_resumo()}</div>'}
    exp = {"c1": "ouve", "c2": "pensa", "c3": "olha", "c4": "firme", "c5": "feliz"}
    passos = "".join(
        f'<li class="cf-passo" data-revela><span class="cf-num" aria-hidden="true">0{i + 1}</span>'
        f'<div class="cf-txt"><h2>{re.sub(r"^\d+\.\s*", "", T[(p, k, "titulo")])}</h2><p>{t(p, k, "texto1")}</p><p>{t(p, k, "texto2")}</p>'
        f'<div class="cf-cara" aria-hidden="true">{face(exp[k], COR["primary"])}</div></div>'
        f'<div class="cf-arte">{mocks[k] if k in mocks else figura(imgs[k], ta(p, k, "alt"), p)}</div></li>'
        for i, k in enumerate(("c1", "c2", "c3", "c4", "c5")))
    nao = "".join(f"<li>{x.strip()}.</li>" for x in T[(p, "limites", "texto")].rstrip(".").split(".") if x.strip())
    dentro = (f'<section class="cena cf-dentro" data-cena data-ceu="marinho" data-rosto="pensa" data-bolhas="nao" aria-label="{ta(p, "dentro", "titulo")}"><div class="cx">'
              f'<div class="cf-d1" data-revela><h2>{t(p, "dentro", "titulo")}</h2><ul class="cf-pontos">'
              + "".join(f'<li>{t(p, "dentro", f"texto{i}")}</li>' for i in (1, 2, 3)) + '</ul></div>'
              f'<div class="cf-d2" data-revela><h2>{t(p, "limites", "titulo")}</h2><ul class="cf-nao">{nao}</ul>'
              f'<p>{t("seguranca", "teste", "texto4")}</p><a class="link-seta" href="{link(p, "seguranca")}">{t(p, "limites", "link")} {ICONE_SETA}</a></div></div></section>')
    corpo_ = (f'<section class="cf" aria-label="Passo a passo"><div class="cx"><ol class="cf-lista" data-linha>{passos}</ol></div></section>' + dentro + orbita(p))
    return pagina(p, "azul", topo_pag(p, "azul", "pensa") + corpo_)

def seguranca():
    """Segurança: faixa vermelha com os números, as regras do app em cartões, o estado do teste e os dados."""
    p = "seguranca"
    regras = "".join(f'<li data-revela><span class="sg-n" aria-hidden="true">{i}</span><p>{t(p, "regras", f"r{i}")}</p></li>' for i in range(1, 6))
    dados = "".join(f'<li data-revela>{t(p, "dados", f"d{i}")}</li>' for i in range(1, 5))
    estado = [("sim", "Regras escritas a partir de materiais públicos de saúde"), ("sim", "Sem inteligência artificial que inventa respostas"),
              ("espera", "Revisão das regras por profissionais de saúde"), ("nao", "Avaliação da ANVISA"), ("nao", "Validação clínica")]
    rot = {"sim": "feito", "espera": "próximo passo", "nao": "ainda não"}
    est = "".join(f'<li class="sg-{k}"><i aria-hidden="true"></i><span>{x}</span><b>{rot[k]}</b></li>' for k, x in estado)
    corpo_ = (f'<section class="cena sg-agora" data-cena data-ceu="susto" data-rosto="firme" data-bolhas="nao" aria-labelledby="h-agora"><div class="cx">'
              f'<div><h2 id="h-agora">{t(p, "agora", "titulo")}</h2><p class="sub">{t(p, "agora", "texto")}</p></div>{emerg("grande")}</div></section>'
              f'<section class="miolo sg"><div class="cx">'
              f'<div class="sg-bloco"><h2>{t(p, "regras", "titulo")}</h2><ol class="sg-regras">{regras}</ol></div>'
              f'<div class="sg-teste" data-revela><div><p class="selo">{t("inicio", "c1", "selo")}</p><h2>{t(p, "teste", "titulo")}</h2>'
              + "".join(f'<p>{t(p, "teste", f"texto{i}")}</p>' for i in (1, 2)) +
              f'</div><ul class="sg-estado" aria-label="Em que pé está">{est}</ul></div>'
              f'<div class="sg-bloco"><h2>{t(p, "dados", "titulo")}</h2><ul class="sg-dados">{dados}</ul>'
              f'<p><a class="link-seta" href="{link(p, "privacidade")}">{t(p, "dados", "link")} {ICONE_SETA}</a></p></div></div></section>')
    return pagina(p, "calmo", topo_pag(p, "calmo", "firme", COR["primary"]) + corpo_)

def sobre():
    """Sobre: a história da Tami em 4 capítulos (o sonho, a escala, o curso, o nome)."""
    p = "sobre"
    pe = "".join(f"<li>{t(p, 'pe', f'p{i}')}</li>" for i in range(1, 5))
    guia = "".join(f'<div class="cartao" data-revela><h3>{t(p, "guia", f"g{i}_t")}</h3><p>{t(p, "guia", f"g{i}")}</p></div>' for i in range(1, 5))
    estrelas = "".join(f'<span class="sb-est" style="--x:{x}%;--y:{y}%;--k:{k}"></span>' for k, (x, y) in enumerate([(8, 18), (88, 12), (92, 70), (6, 78), (50, 4)]))
    tela = (f'<div class="sb-tela" aria-hidden="true">{estrelas}<div class="sb-tela-in">{face("acolhe", "#FFFFFF")}</div>'
            '<div class="sb-pes"><i></i><i></i></div></div>')
    caras = "".join(f'<span class="sb-cara" style="--k:{n}"><svg viewBox="-6 -6 112 112" aria-hidden="true">{cara_n(n)}</svg><b>{n}</b></span>' for n in range(11))
    cap = lambda k, ceu, rosto, txt, arte, extra="": (
        f'<section class="cena sb-cap sb-{k}" data-cena data-ceu="{ceu}" data-rosto="{rosto}" data-bolhas="nao" aria-labelledby="h-{k}">'
        f'<div class="cx"><div class="sb-txt" data-revela><p class="rotulo">{t(p, k, "rot")}</p><h2 id="h-{k}">{t(p, k, "titulo")}</h2>{txt}</div>'
        f'<div class="sb-arte" data-revela>{arte}</div>{extra}</div></section>')
    c1 = cap("c1", "azul", "acolhe", f'<p>{t(p, "c1", "t1")}</p><p>{t(p, "c1", "t2")}</p>', tela)
    c2 = cap("c2", "nevoa", "ouve", f'<p>{t(p, "c2", "t1")}</p>', f'<div class="sb-escala" aria-hidden="true">{caras}</div>')
    c3 = cap("c3", "calmo", "feliz", f'<p>{t(p, "c3", "t1")}</p><p class="sb-selo">{t(p, "c3", "selo")}</p>',
             f'<div class="sb-assina" aria-hidden="true"><span class="sb-ass-nome">{t(p, "quem", "assina")}</span><span>{t(p, "quem", "assina_sub")}</span></div>')
    c4 = cap("c4", "marinho", "feliz", f'<p>{t(p, "c4", "t1")}</p><p class="sb-nota">{t(p, "c4", "t2")}</p>',
             f'<div class="sb-nome" aria-hidden="true"><p class="sb-tami">Tami</p><div class="sb-cracha"><span class="sb-cor"></span>'
             f'<b>{t(p, "c4", "cartao")}</b><span>{t(p, "c4", "cartao_sub")}</span></div></div>')
    resto = (f'<section class="miolo"><div class="cx">'
             f'<div class="cartao cl" data-revela><h2>{t(p, "pe", "titulo")}</h2><ul class="lista">{pe}</ul></div>'
             f'<div><h2 style="margin-bottom:24px">{t(p, "guia", "titulo")}</h2><div class="grade2">{guia}</div></div>'
             f'<div class="cartao" data-revela><h2>{t(p, "contato", "titulo")}</h2><p>{t(p, "contato", "texto")}</p>'
             f'<p>{linkedin()}</p><p class="nota">{t("geral", "rodape", "nao_mande")}</p></div>'
             f'<p class="nota sb-disney">{t(p, "disney", "texto")}</p></div></section>')
    return pagina(p, "nevoa", topo_pag(p, "nevoa", "acolhe", COR["primary"]) + c1 + c2 + c3 + c4 + resto)


def perguntas():
    """Perguntas: busca que filtra na hora, acordeão grande e as perguntas de emergência destacadas."""
    p = "perguntas"
    resp = {"r2": f'<p>Ligue <a href="tel:192">192</a> sem esperar a Tami. No app, o botão do 192 fica em todas as telas.</p>',
            "r8": f'<p>{t(p, "q", "r8")}</p><p><a class="link-seta" href="{link(p, "beta")}">{t("geral", "menu", "botao")} {ICONE_SETA}</a></p>',
            "r11": (f'<p>Você não precisa passar por isso sem ajuda. Ligue <a href="tel:188">188</a>, do CVV, para conversar, 24 horas, de graça. '
                    f'Se estiver em perigo agora, ligue <a href="tel:192">192</a>.</p>')}
    urg = {2, 11}
    qs = "".join(f'<details class="{"urg" if i in urg else ""}"><summary>{t(p, "q", f"p{i}")}<i aria-hidden="true"></i></summary>'
                 f'<div class="r">{resp.get(f"r{i}", "<p>" + t(p, "q", f"r{i}") + "</p>")}</div></details>' for i in range(1, 13))
    busca = (f'<div class="pq-busca" data-so-js hidden><label for="pq-q">{t(p, "busca", "rotulo")}</label>'
             f'<input id="pq-q" type="search" autocomplete="off" placeholder="{ta(p, "busca", "exemplo")}">'
             f'<p class="pq-nada" hidden role="status">{t(p, "busca", "nada")}</p></div>')
    lado = (f'<aside class="lado-col"><div class="cartao pq-li"><h2>{t(p, "lado", "titulo")}</h2><p>{t(p, "lado", "texto")}</p>'
            f'<p>{linkedin()}</p><p class="nota">{t("geral", "rodape", "nao_mande")}</p></div>'
            f'<div class="cartao"><h2>{t("geral", "rodape", "titulo")}</h2>{emerg("claro")}</div></aside>')
    return pagina(p, "nevoa", topo_pag(p, "nevoa", "pensa", COR["primary"]) +
                  f'<section class="miolo"><div class="cx com-lado"><div class="faq pq">{busca}{qs}</div>{lado}</div></section>')

def beta():
    """Quero testar: o que esperar (funciona, ainda não, importante), os 3 passos e o pedido pelo LinkedIn."""
    p = "beta"
    pas = "".join(f'<li data-revela><span class="bt-n" aria-hidden="true">{i}</span><p>{t(p, "passos", f"p{i}")}</p></li>' for i in range(1, 4))
    form = (f'<div class="convite-li"><p>{t(p, "form", "como")}</p>'
            f'<p class="convite-msg"><span>"{t(p, "form", "mensagem")}"</span>'
            f'<button type="button" class="btn btn-s" data-copiar-texto="{ta(p, "form", "mensagem")}" data-ok="{ta(p, "form", "copiado")}">{t(p, "form", "copiar")}</button></p>'
            f'<p class="nota">{t(p, "form", "nao_mande")} <a href="{link(p, "privacidade")}">{t("geral", "rodape", "privacidade")}</a></p>'
            f'<a class="btn btn-p" href="{LINKEDIN}" target="_blank" rel="noopener">{t(p, "form", "botao")}<span class="sr"> (abre em outra aba)</span></a></div>')
    esperar = (f'<div class="bt-esp"><div class="bt-sim" data-revela><h3>{t(p, "esperar", "sim")}</h3><p>{t(p, "limites", "l1").split(": ", 1)[-1][:1].upper() + t(p, "limites", "l1").split(": ", 1)[-1][1:]}</p></div>'
               f'<div class="bt-ainda" data-revela><h3>{t(p, "esperar", "ainda")}</h3><p>{t(p, "limites", "l2").split(": ", 1)[-1][:1].upper() + t(p, "limites", "l2").split(": ", 1)[-1][1:]}</p></div>'
               f'<div class="bt-imp" data-revela><h3>{t(p, "esperar", "importante")}</h3><p>{t(p, "limites", "l3")}</p><p>{t(p, "limites", "l4")}</p></div></div>')
    corpo_ = (f'<section class="miolo bt"><div class="cx">'
              f'<div><h2 class="bt-h">{t(p, "limites", "titulo")}</h2>{esperar}</div>'
              f'<div><h2 class="bt-h">{t(p, "passos", "titulo")}</h2><ol class="bt-passos">{pas}</ol></div>'
              f'<div class="com-lado"><div><h2 class="bt-h">{t(p, "form", "titulo")}</h2>{form}</div>'
              f'<aside class="lado-col"><div class="cartao"><h2>{t(p, "lado", "titulo")}</h2>{emerg("claro")}</div></aside></div></div></section>')
    return pagina(p, "aurora", topo_pag(p, "aurora", "feliz", COR["primary"]) + corpo_, sem_convite=True)

def imprensa():
    """Imprensa: kit com frase, fatos, parágrafo para copiar, marca (com o manual), telas e contato."""
    import manual as M
    p = "imprensa"; r = raiz(p)
    fatos = "".join((lambda a, b: f'<li data-revela><b>{a}</b><span>{b}</span></li>')(*t(p, "fatos", f"f{i}").split(":", 1)) for i in range(1, 8))
    telas = "".join(f'<li data-revela><a href="{r}img/{n}-720.webp" download>{figura(n, a, p, "celular")}<span>{a}</span><i>Baixar imagem</i></a></li>' for n, a in
                    (("voz", "Falar em vez de escrever"), ("alarme", "Sinais de emergência"), ("onde-doi", "Onde dói"), ("pessoas", "Pessoas que você cuida")))
    pares = "".join(f'<li data-revela><p class="tr-nao"><span class="sr">{t(p, "falar", "nao_titulo")}: </span><s>{t(p, "falar", f"p{i}n")}</s></p>'
                    f'<span class="tr-seta" aria-hidden="true">{ICONE_SETA}</span>'
                    f'<p class="tr-sim"><span class="sr">{t(p, "falar", "sim_titulo")}: </span>{t(p, "falar", f"p{i}s")}</p>'
                    f'<p class="tr-pq"><b>{t(p, "falar", "pq")}</b> {t(p, "falar", f"p{i}q")}</p></li>' for i in range(1, 9))
    logos = "".join(f'<li><div class="im-logo" style="background:{bg}">{M.logo_inline("h", v, "lg")}</div>'
                    f'<a href="{r}marca/logos/tami-horizontal-{v}.svg" download>SVG</a><a href="{r}marca/logos/tami-horizontal-{v}.png" download>PNG</a></li>'
                    for v, bg in (("padrao", "#FFFFFF"), ("sobre-azul", COR["primary"]), ("sobre-marinho", COR["text"])))
    topo = (f'<section class="cena topo-pag" data-cena data-ceu="nevoa" data-rosto="oi" data-bolhas="nao" aria-labelledby="h1">'
            f'<div class="cx"><div class="txt"><p class="rotulo">{t(p, "topo", "rotulo")}</p><h1 id="h1">{t(p, "topo", "titulo")}</h1>'
            f'<p class="sub">{t(p, "topo", "sub")}</p><p class="acoes"><a class="btn btn-p" href="{r}marca/tami-manual-de-marca.pdf" download>{t(p, "kit", "pdf")}</a>'
            f'<a class="btn btn-s" href="{link(p, "marca")}#baixar">{t(p, "kit", "zip")}</a></p></div>{alvo("oi", COR["primary"])}</div></section>')
    frase = (f'<section class="cena im-frase" data-cena data-ceu="azul" data-rosto="feliz" data-bolhas="nao" aria-labelledby="h-frase">'
             f'<div class="cx"><h2 class="rotulo" id="h-frase">{t(p, "frase", "titulo")}</h2><p class="im-grande" data-revela>{t(p, "frase", "texto")}</p></div></section>')
    corpo_ = (f'<section class="miolo im"><div class="cx">'
              f'<div class="im-bloco"><h2>{t(p, "fatos", "titulo")}</h2><ul class="im-fatos">{fatos}</ul></div>'
              f'<div class="im-bloco im-par" data-revela><div class="im-par-cab"><h2>{t(p, "paragrafo", "titulo")}</h2>'
              f'<button type="button" class="btn btn-s im-copiar" data-copiar="#im-texto" data-ok="{ta(p, "kit", "copiado")}">{t(p, "kit", "copiar")}</button></div>'
              f'<p id="im-texto">{t(p, "paragrafo", "texto")}</p></div>'
              f'<div class="im-bloco im-marca" data-revela><div class="im-marca-txt"><h2>{t(p, "marca", "titulo")}</h2><p>{t(p, "marca", "texto")}</p>'
              f'<p>{t(p, "marca", "fontes")}</p><p><a class="link-seta" href="{link(p, "marca")}">{t(p, "kit", "ver_manual")} {ICONE_SETA}</a></p></div>'
              f'<ul class="im-logos">{logos}</ul></div>'
              f'<div class="im-bloco"><h2>{t(p, "telas", "titulo")}</h2><ul class="im-telas">{telas}</ul></div>'
              f'<div class="im-bloco im-troca"><div class="im-troca-cab"><h2>{t(p, "falar", "titulo")}</h2><p>{t(p, "falar", "sub")}</p></div>'
              f'<div class="tr-rotulos" aria-hidden="true"><span class="tr-r-nao">{t(p, "falar", "nao_titulo")}</span><span></span><span class="tr-r-sim">{t(p, "falar", "sim_titulo")}</span></div>'
              f'<ul class="tr-lista">{pares}</ul></div>'
              f'<div class="im-contato" data-revela><div><h2>{t(p, "contato", "titulo")}</h2><p class="im-email">{linkedin("im-li")}</p>'
              f'<p class="nota">{t("geral", "rodape", "nao_mande")}</p></div>'
              f'<a class="btn btn-p" href="{LINKEDIN}" target="_blank" rel="noopener">{t(p, "kit", "copiar_email")}<span class="sr"> (abre em outra aba)</span></a></div>'
              f'</div></section>')
    return pagina(p, "nevoa", topo + frase + corpo_)


PV_ICONES = {  # traço simples, 24x24, para os direitos
    "d1": '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.5 4.5"/>',
    "d2": '<path d="M12 4v11m-5-5 5 5 5-5M5 20h14"/>',
    "d3": '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
    "d4": '<path d="M4 7h16M9 7V4.5h6V7m-9 0 1 13h10l1-13"/>',
    "d5": '<rect x="3" y="7" width="18" height="10" rx="5"/><circle cx="8" cy="12" r="2.6"/>',
    "d6": '<circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="m8.3 10.8 7.4-3.6m-7.4 6 7.4 3.6"/>',
    "d7": '<circle cx="12" cy="12" r="8.5"/><path d="M9.6 9.6a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4"/><circle cx="12" cy="17" r=".6" fill="currentColor"/>',
    "d8": '<path d="M12 3 4.5 6v5.5c0 4.5 3.2 8 7.5 9.5 4.3-1.5 7.5-5 7.5-9.5V6z"/><path d="m8.8 12 2.3 2.3 4.2-4.6"/>',
}
PV_LEIS = [("l1", "https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm"),
           ("l2", "https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm"),
           ("l3", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/lei/L15211.htm"),
           ("l4", "https://www.in.gov.br/en/web/dou/-/resolucao-cd/anpd-n-15-de-24-de-abril-de-2024-556243024")]


def privacidade():
    """Privacidade: resumo em 4 cartões, índice que acompanha a leitura e o texto completo (site + app em teste)."""
    p = "privacidade"
    fora = '<span class="sr"> (abre em outra aba)</span>'
    res = "".join(f'<li data-revela><b>{t(p, "resumo", f"t{i}")}</b><span>{t(p, "resumo", f"s{i}")}</span></li>' for i in (1, 2, 3, 4))
    paras = lambda k, n: "".join(f"<p>{t(p, k, f'texto{i}')}</p>" for i in range(1, n + 1))
    # tabela do app: vira cartões no celular (cada célula leva o nome da coluna)
    cab = [t(p, "app", f"h{i}") for i in (1, 2, 3, 4)]
    linhas = "".join("<tr>" + "".join(f'<{"th scope=row" if j == 0 else "td"} data-c="{cab[j]}">{t(p, "app", f"r{i}{c}")}</{"th" if j == 0 else "td"}>'
                                      for j, c in enumerate("abcd")) + "</tr>" for i in range(1, 8))
    tabela = (f'<div class="pv-tab"><table><caption class="sr">{t(p, "app", "titulo")}</caption><thead><tr>'
              + "".join(f'<th scope="col">{h}</th>' for h in cab) + f'</tr></thead><tbody>{linhas}</tbody></table></div>')
    nunca = (f'<div class="pv-nunca"><h3>{t(p, "compartilha", "nunca")}</h3><ul>'
             + "".join(f'<li>{t(p, "compartilha", f"n{i}")}</li>' for i in range(1, 7)) + '</ul></div>')
    dir_ = "".join(f'<li><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">{PV_ICONES[f"d{i}"]}</svg><b>{t(p, "direitos", f"d{i}")}</b>'
                   + (f'<span>{t(p, "direitos", "d7x")}</span>' if i == 7 else "") + '</li>' for i in range(1, 9))
    leis = "".join(f'<li><a href="{u}" target="_blank" rel="noopener"><span>{t(p, "leis", k)}</span>{ICONE_SETA}{fora}</a></li>' for k, u in PV_LEIS)
    secs = [
        ("quem", paras("quem", 2) + f'<p>{t(p, "quem", "texto3")} {linkedin()}</p>'),
        ("site", paras("site", 4)),
        ("convite", paras("convite", 4)),
        ("app", f'<p>{t(p, "app", "texto")}</p>{tabela}'),
        ("saude", paras("saude", 4)),
        ("criancas", paras("criancas", 3)),
        ("onde", paras("onde", 4)),
        ("compartilha", paras("compartilha", 3) + nunca),
        ("seguranca", '<ul class="pv-lista">' + "".join(f"<li>{t(p, 'seguranca', f'texto{i}')}</li>" for i in range(1, 5)) + "</ul>"),
        ("risco", paras("risco", 2)),
        ("incidente", paras("incidente", 3)),
        ("prazo", paras("prazo", 3)),
        ("direitos", f'<p>{t(p, "direitos", "texto")}</p><ul class="pv-dir">{dir_}</ul>'),
        ("mudancas", paras("mudancas", 2)),
        ("leis", f'<ul class="pv-leis">{leis}</ul>'),
    ]
    ind = "".join(f'<li><a href="#pv-{k}"><b>{n + 1}</b><span>{t(p, k, "titulo")}</span></a></li>' for n, (k, _) in enumerate(secs))
    corpo = "".join(f'<section class="pv-sec pv-{k}" id="pv-{k}" aria-labelledby="pvh-{k}"><h2 id="pvh-{k}"><span aria-hidden="true">{n + 1}</span>{t(p, k, "titulo")}</h2>{html}</section>'
                    for n, (k, html) in enumerate(secs))
    corpo_ = (f'<section class="miolo pv"><div class="cx"><ul class="pv-resumo" aria-label="{ta(p, "resumo", "rotulo")}">{res}</ul>'
              f'<div class="pv-grade"><nav class="pv-ind" aria-label="{ta(p, "resumo", "indice")}" data-indice><p>{t(p, "resumo", "indice")}</p><ol>{ind}</ol></nav>'
              f'<div class="pv-texto">{corpo}</div></div></div></section>')
    return pagina(p, "calmo", topo_pag(p, "calmo", "oi", COR["primary"]) + corpo_)

def p404():
    p = "p404"
    pags = "".join(f'<li><a href="{raiz(p)}{PAG[k]}index.html">{t("geral", "menu", m)}</a></li>' for k, m in (("como", "como"), ("seguranca", "seguranca"), ("perguntas", "perguntas"), ("beta", "botao")))
    corpo_ = (f'<section class="cena topo-pag e404" data-cena data-ceu="calmo" data-rosto="olha" data-bolhas="nao" aria-labelledby="h1"><div class="cx"><div class="txt">'
              f'<p class="e404-n" aria-hidden="true">404</p><p class="rotulo">{t(p, "topo", "rotulo")}</p><h1 id="h1">{t(p, "topo", "titulo")}</h1><p class="sub">{t(p, "topo", "sub")}</p>'
              f'<div class="acoes"><a class="btn btn-p" href="{raiz(p)}index.html">{t(p, "topo", "link")}</a></div>'
              f'<ul class="e404-links">{pags}</ul>{emerg("claro")}</div>'
              f'{alvo("olha", COR["primary"])}</div></section>')
    PAG["p404"] = ""
    return pagina(p, "calmo", corpo_, sem_convite=True)

def movimento():
    """Prancha de movimento: cada efeito isolado, com duração e curva. Fora do menu, noindex e fora da publicação."""
    p = "movimento"
    T[(p, "meta", "titulo")] = "Prancha de movimento · Tami"
    T[(p, "meta", "descricao")] = "Página interna com cada efeito do site isolado."
    itens = [("Letras do título", "600 ms por letra, atraso de 28 ms, curva de entrada da cena", '<h2 data-cinetico>Sentiu algo?</h2>'),
             ("Rosto 2D (modo leve)", "pisca 2 vezes e para", f'<div class="rosto-alvo">{face("oi", COR["primary"], True)}</div>'),
             ("Onda de voz", "1,2 s, 4 vezes, vai e volta", '<span class="onda" style="background:#3F4ACB;padding:12px;border-radius:12px">' + "".join(f'<i style="--k:{i}"></i>' for i in range(7)) + '</span>'),
             ("Expressões", "troca em 320 ms no WebGL", "".join(f'<div class="rosto-alvo" style="width:120px">{face(e, COR["primary"])}</div><p>{e}</p>' for e in ("oi", "pisca", "feliz", "firme", "pensa", "acolhe", "ouve", "olha")))]
    corpo_ = (f'<section class="miolo"><div class="cx"><h1>Prancha de movimento</h1>' +
              "".join(f'<div class="cartao"><h2>{n}</h2><p class="nota">{d}</p>{x}</div>' for n, d, x in itens) + '</div></section>')
    return pagina(p, "calmo", corpo_, sem_convite=True, noindex=True)


# ---------------------------------------------------------------- arquivos
def copiar_base():
    if os.path.isdir(D):
        shutil.rmtree(D)
    os.makedirs(f"{D}/assets")
    shutil.copytree(f"{V1}/fonts", f"{D}/fonts")
    shutil.copytree(f"{V1}/img", f"{D}/img")
    for x in os.listdir(f"{R}/gen/img"):
        shutil.copy(f"{R}/gen/img/{x}", f"{D}/img/{x}")
    for x in ("tokens-web.css",):
        shutil.copy(f"{V1}/{x}", f"{D}/assets/{x}")
    for x in ("site.css", "marca.css", "tami.js", "cena.js", "cenas2.js"):
        shutil.copy(f"{R}/gen/assets/{x}", f"{D}/assets/{x}")
    shutil.copy(f"{V1}/site.webmanifest", f"{D}/site.webmanifest")
    # corpo do "Onde dói?" como arquivo à parte (sai do HTML inicial)
    s = CORPO.corpo_interativo("mulher", "frente", uid="cs", marcado="barriga")
    s = re.sub(r'\s(role|aria-label)="[^"]*"', "", s, count=2)
    s = s.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" ', 1).replace('style="display: block; height: 100%; width: 100%"', 'width="280" height="714"')
    s = re.sub(r"<a [^>]*>|</a>", "", s)
    open(f"{D}/img/corpo-frente.svg", "w").write(s)


def gravar(caminho, conteudo):
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    open(caminho, "w", encoding="utf-8").write(conteudo)


LICENCAS = """# Licenças do site da Tami v2

| Item | Onde | Licença |
| --- | --- | --- |
| Fredoka (fonte) | fonts/fredoka-*.woff2 | SIL Open Font License 1.1 |
| Nunito (fonte) | fonts/nunito-*.woff2 | SIL Open Font License 1.1 |
| tami.js e cena.js | assets/ | Código próprio do projeto Tami |

Nenhuma biblioteca de terceiros em JavaScript. O 3D é um shader WebGL escrito do zero.
GSAP ficou de fora porque a licença é da Webflow (não é MIT, Apache, BSD ou ISC). Three.js não foi necessário.
"""

LEIAME = """# Site da Tami v2 · como publicar

1. Crie um repositório chamado `tami` no GitHub e envie todo o conteúdo desta pasta para a raiz.
2. Em Settings ➜ Pages, escolha a branch principal e a pasta raiz.
3. O endereço fica https://SEU-USUARIO.github.io/tami/ . Se mudar o nome do repositório, troque `/tami/` no 404.html e o `BASE` em gen/site2.py.
4. A pasta `movimento/` é só para estudo (noindex). Pode apagar antes de publicar.

Modos: `?modo=show`, `?modo=leve` e `?modo=parado` forçam um modo para teste.
"""


def main():
    copiar_base()
    for nome, f in (("inicio", inicio), ("como", como), ("seguranca", seguranca), ("sobre", sobre), ("perguntas", perguntas),
                    ("beta", beta), ("imprensa", imprensa), ("privacidade", privacidade), ("movimento", movimento)):
        gravar(f"{D}/{PAG[nome]}index.html", f())
    gravar(f"{D}/404.html", p404())
    import manual  # manual de marca: página, versão para PDF e arquivos do logo
    gravar(f"{D}/marca/index.html", manual.pagina_marca())
    gravar(f"{D}/marca/pdf.html", manual.pagina_pdf())
    manual.arquivos_logo()
    urls = "".join(f"<url><loc>{BASE}{PAG[k]}</loc></url>" for k in ("inicio", "como", "seguranca", "sobre", "perguntas", "beta", "imprensa", "marca", "privacidade"))
    gravar(f"{D}/sitemap.xml", f'<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">{urls}</urlset>\n')
    gravar(f"{D}/robots.txt", f"User-agent: *\nAllow: /\nDisallow: /movimento/\nDisallow: /marca/pdf.html\nSitemap: {BASE}sitemap.xml\n")
    gravar(f"{D}/LICENCAS.md", LICENCAS)
    gravar(f"{D}/LEIA-ME.md", LEIAME)
    print("site v2 gerado em", D)


if __name__ == "__main__":
    main()
