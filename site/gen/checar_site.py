"""checar_site.py · checador do site da Tami v2. Reprova o que a v1 reprovava e as regras novas do site imersivo.
Uso: python3 checar_site.py [--visual]   (o visual precisa do servidor em http://localhost:8790)
"""
import csv
import glob
import gzip
import html
import os
import re
import sys

R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = f"{R}/dist"
APP = os.path.join(R, "app")
sys.path.insert(0, os.path.join(R, "gen", "marca"))
from tokens import HEX_PERMITIDOS  # noqa: E402

falhas, avisos = [], []


def falha(regra, msg):
    falhas.append(f"[{regra}] {msg}")


PROIBIDAS = ["diagnostica", "médico virtual", "consulta", "laudo", "cura ", "garantido", "100%", "precisão", "triagem", "classifica",
             "avalia seu risco", "detecta", "monitora", "encaminha", "indica para onde", "indica o caminho", "caminho certo", "na hora certa",
             "sempre do lado seguro", "caminho mais seguro", "ninguém confunde", "infelizmente", "paciente", "não se preocupe",
             "respondemos", "não coleta nada", "nunca fala de remédio", "só o que você contou", "aprovada", "certificada", "—", "–"]
# exceções: a frase que diz que ela NÃO foi aprovada é a própria verdade; "Evite: diagnostica" no kit de imprensa
EXCECOES = {"aprovada": ["foi aprovada por algum órgão"], "diagnostica": ["evite"], "consulta": []}

TEXTOS = list(csv.DictReader(open(f"{R}/textos.csv", encoding="utf-8"), delimiter=";"))
T = {(r["pagina"], r["secao"], r["elemento"]): r["texto"] for r in TEXTOS}

# ---------- 1. textos: palavras proibidas e tamanho das frases ----------
CONGELADOS = {("inicio", "c3", "m2"), ("inicio", "c3", "m4")}   # perguntas do app, palavra por palavra
LONGOS_OK = {("imprensa", "paragrafo", "texto")}                # parágrafo de imprensa (frases contadas uma a uma)
for r in TEXTOS:
    k = (r["pagina"], r["secao"], r["elemento"]); tx = r["texto"]; low = tx.lower()
    for w in PROIBIDAS:
        if w in low:
            if any(e in low for e in EXCECOES.get(w, [])) or k[1] == "falar":
                continue
            falha("I-TXT-02", f"{k}: palavra proibida '{w}'")
    if k in CONGELADOS or k[1] == "meta":
        continue
    limite = 20 if k[2] in ("sub", "descricao") or k[:2] == ("imprensa", "frase") else 15
    for fr in re.split(r"(?<=[.?!])\s+", tx):
        n = len([p for p in re.split(r"\s+", fr.strip()) if p and p not in ("·", "•", "➜")])
        if n > limite and k not in LONGOS_OK:
            falha("I-TXT-01", f"{k}: frase com {n} palavras: {fr[:60]}")
        elif n > limite:
            avisos.append(f"{k}: frase longa ({n}) no parágrafo de imprensa")

# ---------- 2. conversa de exemplo igual ao app ----------
APPT = {r["codigo"]: (r["texto_tami"] or r["texto_atual"]) for r in csv.DictReader(open(f"{APP}/textos_tami.csv", encoding="utf-8"), delimiter=";")}
demo = open(f"{R}/reports/demo_motor.tsv", encoding="utf-8").read()
for k, cod in (("m2", "FIL_05"), ("m4", "GEN_02")):   # texto exato que o motor mostra hoje
    if f"Q\t{cod}\t{T[('inicio', 'c3', k)]}\t" not in demo:
        falha("7.0-1", f"pergunta {k} diferente da que o motor mostra em {cod}")
for k, cod in (("m3", "FIL_05"), ("m5", "GEN_02")):
    if f"A\t{cod}\t{T[('inicio', 'c3', k)]}" not in demo:
        falha("7.0-1", f"resposta {k} não bate com a opção do app em {cod}")
if "R\tCUIDADO_EM_CASA\tMSG-CASA" not in demo:
    falha("7.0-1", "o motor não confirma Cuidar em casa para a conversa")
if f'{T[("inicio", "c3", "r_titulo")].replace("disso ", "disso ")} {T[("inicio", "c3", "r_frase")]}' != APPT.get("MSG-CASA"):
    falha("7.0-1", "frase do resultado diferente de MSG-CASA")

# ---------- 3. HTML ----------
paginas = sorted(p for p in glob.glob(f"{D}/**/*.html", recursive=True) if not p.endswith("/marca/pdf.html"))  # pdf.html só vira PDF
publicas = [p for p in paginas if "/movimento/" not in p]
if os.path.isdir(f"{D}/componentes"):
    falha("7.0-15", "página /componentes/ não pode ser publicada")
for p in paginas:
    s = open(p, encoding="utf-8").read(); rel = p.replace(D, "")
    corpo = s[s.find("<body"):]
    if not re.search(r'<a class="pular"[^>]*>.*?</a>\s*<aside class="faixa" aria-label="Emergência"><a href="tel:192">', corpo, re.S):
        falha("I-ACE-03", f"{rel}: faixa do 192 não é o primeiro bloco depois do 'pular'")
    if "data-mov" not in s:
        falha("I-ACE-01", f"{rel}: sem botão Pausar movimento")
    if "cena.js" in s.split("</head>")[0]:
        falha("I-DES-03", f"{rel}: cena.js no HTML inicial")
    for m in re.finditer(r"<img\b[^>]*>", s):
        if 'alt="' not in m.group(0):
            falha("A11Y", f"{rel}: imagem sem alt")
    for m in re.finditer(r'<section class="cena[^"]*"[^>]*>(.*?)</section>', s, re.S):
        if not re.search(r"<h[12]\b", m.group(1)):
            falha("I-CON-04", f"{rel}: cena sem título real em HTML")
    txt = re.sub(r"<[^>]+>", " ", re.sub(r"<(script|style)\b.*?</\1>", " ", corpo, flags=re.S))
    txt = html.unescape(txt).lower()
    for w in ("triagem", "médico virtual", "indica para onde", "caminho certo", "respondemos", "—", "–"):
        if w in txt and not (w in ("médico virtual", "triagem") and ("/imprensa/" in p or "/marca/" in p)):
            falha("I-TXT-02", f"{rel}: '{w}' no texto da página")
    if "ambulancia" in s or 'id="cruz' in s:
        falha("7.0-11", f"{rel}: ícone com cruz")
    if re.search(r"gsap|scrolltrigger|lenis|locomotive|swiper", s, re.I):
        falha("R2-LIB", f"{rel}: biblioteca de animação ou carrossel")
    if re.search(r"img/(casa|upa|resumo|conversa-nova)-", s):
        falha("R2-TELA", f"{rel}: tela do app proibida (casa, upa, resumo ou conversa-nova)")
    if "data-orbita" in s and not re.search(r'data-orbita.*?aria-live="polite"', s, re.S):
        falha("R2-ORB", f"{rel}: órbita sem aria-live")
    # links internos existem
    base = os.path.dirname(p)
    for href in re.findall(r'href="([^"#:]+?)"', s):
        if href.startswith(("http", "/tami/")) or href.endswith((".css", ".png", ".webmanifest")) and not os.path.exists(os.path.normpath(os.path.join(base, href))):
            if not href.startswith(("http", "/tami/")):
                falha("LINK", f"{rel}: {href} não existe")
            continue
        alvo = os.path.normpath(os.path.join(base, href))
        if not os.path.exists(alvo):
            falha("LINK", f"{rel}: {href} não existe")
    for src in re.findall(r'(?:src|srcset)="([^"]+)"', s):
        for parte in src.split(","):
            u = parte.strip().split(" ")[0]
            if u and not u.startswith(("http", "data:", "/tami/")) and not os.path.exists(os.path.normpath(os.path.join(base, u))):
                falha("LINK", f"{rel}: arquivo {u} não existe")
    if "/404.html" in p and 'href="/tami/assets/site.css"' not in s:
        falha("7.0-24", "404 sem caminho absoluto")
    if "/movimento/" in p and 'name="robots" content="noindex"' not in s:
        falha("7.1-4", "prancha de movimento sem noindex")

# ---------- 4. camada show: bibliotecas, licenças, cores, regras do rosto ----------
js = {os.path.basename(f): open(f, encoding="utf-8").read() for f in glob.glob(f"{D}/assets/*.js")}
if set(js) - {"tami.js", "cena.js", "cenas2.js"}:
    falha("0.5", f"script não previsto: {set(js) - {'tami.js', 'cena.js', 'cenas2.js'}}")
for nome, s in js.items():
    if re.search(r"lenis|ScrollSmoother|normalizeScroll|locomotive", s, re.I):
        falha("I-INT-01", f"{nome}: biblioteca de rolagem")
cena = js.get("cena.js", "")
for h in set(re.findall(r'"(#[0-9A-Fa-f]{6})"', cena)):
    if h.upper() not in HEX_PERMITIDOS:
        falha("0.3", f"cena.js usa cor fora dos tokens: {h}")
if "piscadas >= 2" not in cena:
    falha("I-MOV-02", "limite de 2 piscadas sumiu")
rot = re.search(r"rot\(vec2\(M\.x\*([\d.]+),M\.y\*([\d.]+)\)\)", cena)
if not rot or max(float(rot.group(1)), float(rot.group(2))) > 0.23:
    falha("I-MOV-04", "giro do rosto acima de 13 graus")
if 'setAttribute("aria-hidden", "true")' not in cena:
    falha("I-CON-04", "canvas sem aria-hidden")
if "lentos > 2000" not in cena:
    falha("I-DES-05", "vigia de 30 fps sumiu")
if not os.path.exists(f"{D}/LICENCAS.md"):
    falha("0.5", "LICENCAS.md não existe")

# ---------- 5. peso ----------
def gz(caminho):
    return len(gzip.compress(open(caminho, "rb").read(), 9))


ini = sum(gz(f"{D}/{x}") for x in ("index.html", "assets/tokens-web.css", "assets/site.css", "assets/tami.js", "assets/cenas2.js"))
ini += sum(os.path.getsize(f"{D}/fonts/{x}") for x in ("fredoka-latin-600-normal.woff2", "nunito-latin-600-normal.woff2"))
show = gz(f"{D}/assets/cena.js")
print(f"peso inicial (gzip, com 2 fontes): {ini / 1024:.1f} KB · camada show: {show / 1024:.1f} KB")
if ini > 300 * 1024:
    falha("I-DES-02", f"peso inicial {ini / 1024:.0f} KB")
if show > 250 * 1024:
    falha("I-DES-02", f"camada show {show / 1024:.0f} KB")

# ---------- 6. visual (faixa por cima de tudo, em cada cena) ----------
if "--visual" in sys.argv:
    import asyncio
    from playwright.async_api import async_playwright

    async def vis():
        async with async_playwright() as p:
            b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
            for modo in ("show", "leve", "parado"):
                for w, h in ((390, 844), (1440, 900)):
                    pg = await b.new_page(viewport={"width": w, "height": h})
                    for cam in ("", "como-funciona/", "seguranca/", "beta/"):
                        await pg.goto(f"http://localhost:8790/{cam}index.html?modo={modo}&vigia=nao", wait_until="load")
                        await pg.wait_for_timeout(1200)
                        ys = await pg.evaluate("[0,...[...document.querySelectorAll('[data-cena],section')].map(e=>e.getBoundingClientRect().top+scrollY)]")
                        # cenas novas: a cada 10% do trilho
                        ys += await pg.evaluate("[...document.querySelectorAll('.caminhos,.orbita')].flatMap(e=>{const t=e.getBoundingClientRect().top+scrollY,h=Math.max(0,e.offsetHeight-innerHeight);return [...Array(11).keys()].map(k=>t+h*k/10)})")
                        if cam == "":
                            await pg.evaluate("scrollTo(0,0)"); await pg.wait_for_timeout(900)
                            cabe = await pg.evaluate("[...document.querySelectorAll('.heroi5 h1,.heroi5 .h5-btn')].every(e=>{const r=e.getBoundingClientRect();return r.height>0&&r.bottom<=innerHeight&&r.right<=innerWidth})")
                            if not cabe:
                                falha("R3-HEROI", f"{modo} {w}px: título ou botões da primeira tela fora da tela")
                        for y in ys:
                            await pg.evaluate(f"scrollTo(0,{y})"); await pg.wait_for_timeout(250)
                            topo = await pg.evaluate("(()=>{const f=document.querySelector('.faixa a').getBoundingClientRect();const e=document.elementFromPoint(f.left+f.width/2,f.top+f.height/2);return !!e.closest('.faixa') && f.top>=0 && f.height>=44})()")
                            if not topo:
                                falha("I-ACE-03", f"{modo} {w}px /{cam} y={y:.0f}: faixa do 192 coberta ou fora da tela")
                        lado = await pg.evaluate("document.documentElement.scrollWidth>innerWidth+1")
                        if lado:
                            falha("0.6", f"{modo} {w}px /{cam}: rolagem lateral")
                        if modo == "parado":
                            anim = await pg.evaluate("document.getAnimations().filter(a=>a.playState==='running').length")
                            if anim:
                                falha("I-ACE-02", f"parado /{cam}: {anim} animações rodando")
                    await pg.close()
            await b.close()
    asyncio.run(vis())

print(f"{len(falhas)} reprovações, {len(avisos)} avisos")
for f in falhas:
    print(" ", f)
for a in avisos[:10]:
    print("  aviso:", a)
sys.exit(1 if falhas else 0)
