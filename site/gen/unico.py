"""unico.py · gera um único arquivo tami-unico.html (só a página inicial) para abrir direto no computador.
Tudo vai embutido: CSS, fontes, scripts, imagens e o corpo. Os links para as outras páginas não funcionam fora do site.
Uso: python3 gen/unico.py  (depois de python3 gen/site2.py)
"""
import base64
import sys
import os
import re

R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(R, "dist")
MIME = {".woff2": "font/woff2", ".webp": "image/webp", ".avif": "image/avif", ".png": "image/png", ".svg": "image/svg+xml", ".jpg": "image/jpeg"}


def data_uri(caminho):
    ext = os.path.splitext(caminho)[1]
    return f"data:{MIME[ext]};base64," + base64.b64encode(open(caminho, "rb").read()).decode()


def ler(rel):
    return open(os.path.join(D, rel), encoding="utf-8").read()


PAG = sys.argv[1] if len(sys.argv) > 1 else "index.html"
RZ = "../" if "/" in PAG else "./"
h = ler(PAG)

# CSS embutido, com as fontes dentro
css = ler("assets/tokens-web.css") + "\n" + ler("assets/site.css")
css = re.sub(r'url\("\.\./fonts/([^"]+)"\)', lambda m: f'url("{data_uri(os.path.join(D, "fonts", m.group(1)))}")', css)
h = re.sub(re.escape(f'<link rel="stylesheet" href="{RZ}assets/tokens-web.css"><link rel="stylesheet" href="{RZ}assets/site.css">'), lambda m: f"<style>{css}</style>", h)
h = re.sub(r'<link rel="preload"[^>]*>\n?', "", h)
h = re.sub(r'<link rel="(icon|apple-touch-icon|manifest|canonical)"[^>]*>', "", h)

# imagens: uma versão só (720 px), embutida
h = re.sub(r'<source [^>]*>', "", h)
h = re.sub(r'\s(srcset|sizes)="[^"]*"', "", h)
h = re.sub(re.escape(f'src="{RZ}img/') + r'([^"]+)"', lambda m: f'src="{data_uri(os.path.join(D, "img", m.group(1).replace("-360.", "-720.")))}"', h)
h = re.sub(r'data-corpo="\./img/corpo-frente\.svg"', lambda m: f'data-corpo="{data_uri(os.path.join(D, "img", "corpo-frente.svg"))}"', h)

# scripts embutidos; a camada show (cena.js) fica guardada e só roda quando o modo show pede
tami = ler("assets/tami.js").replace('s.src = raiz + "assets/cena.js";', 's.textContent = document.getElementById("cena-src").textContent;')
cenas2 = ler("assets/cenas2.js")
cena = ler("assets/cena.js")
h = re.sub(re.escape(f'<script src="{RZ}assets/tami.js" defer></script>') + r'\n?(<script src="[^"]*cenas2\.js" defer></script>)?', "", h)
h = h.replace("</body>", f'<script type="text/plain" id="cena-src">{cena}</script>\n<script>{tami}</script>\n<script>{cenas2}</script>\n</body>')

assert f"{RZ}assets/" not in h and f"{RZ}img/" not in h, "sobrou caminho local"
saida = os.path.join(R, "tami-unico.html" if PAG == "index.html" else "tami-" + PAG.replace("/", "-"))
open(saida, "w", encoding="utf-8").write(h)
print(f"{saida} · {len(h) / 1024:.0f} KB")
