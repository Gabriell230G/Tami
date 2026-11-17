"""unico_site.py · o site inteiro num arquivo só (tami-unico.html), para abrir direto no computador.
Como funciona: o arquivo é uma "casca" com um quadro (iframe) que mostra uma página por vez.
Fontes, imagens, CSS e scripts entram uma vez só; cada página é montada na hora em que você clica num link.
O botão voltar do navegador funciona (o endereço ganha #/nome-da-pagina).
Uso: python3 gen/unico_site.py  (depois de python3 gen/site2.py)
"""
import base64
import json
import os
import re

R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(R, "dist")
MIME = {".woff2": "font/woff2", ".webp": "image/webp", ".avif": "image/avif", ".png": "image/png", ".svg": "image/svg+xml", ".jpg": "image/jpeg"}
PAGINAS = ["index.html", "como-funciona/index.html", "seguranca/index.html", "sobre/index.html", "perguntas/index.html",
           "beta/index.html", "imprensa/index.html", "marca/index.html", "privacidade/index.html"]


def ler(rel):
    return open(os.path.join(D, rel), encoding="utf-8").read()


def data_uri(caminho):
    ext = os.path.splitext(caminho)[1]
    return f"data:{MIME[ext]};base64," + base64.b64encode(open(caminho, "rb").read()).decode()


# CSS: tudo junto, com as fontes dentro (uma vez só)
css = ler("assets/tokens-web.css") + "\n" + ler("assets/site.css")
css_marca = ler("assets/marca.css")  # só na página do manual, como no site publicado
css = re.sub(r'url\("\.\./fonts/([^"]+)"\)', lambda m: f'url("{data_uri(os.path.join(D, "fonts", m.group(1)))}")', css)

IMGS = {}


def img(nome):
    nome = nome.replace("-360.", "-720.")
    if nome not in IMGS:
        IMGS[nome] = data_uri(os.path.join(D, "img", nome))
    return f"__IMG:{nome}__"


def preparar(rel):
    h = ler(rel)
    rz = "../" if "/" in rel else "./"
    h = re.sub(r'<link rel="stylesheet"[^>]*>', "", h)
    h = h.replace("</head>", "<style>__CSS__</style></head>", 1)
    h = re.sub(r'<link rel="(preload|icon|apple-touch-icon|manifest|canonical)"[^>]*>\n?', "", h)
    h = re.sub(r'<meta (property|name)="(og:|twitter:)[^>]*>\n?', "", h)
    h = re.sub(r"<source [^>]*>", "", h)
    h = re.sub(r'\s(srcset|sizes)="[^"]*"', "", h)
    h = re.sub(r'src="(?:\./|\.\./)img/([^"]+)"', lambda m: f'src="{img(m.group(1))}"', h)
    h = re.sub(r'data-corpo="(?:\./|\.\./)img/([^"]+)"', lambda m: f'data-corpo="{img(m.group(1))}"', h)
    h = re.sub(r'href="(?:\./|\.\./)img/([^"]+\.webp)"', lambda m: f'href="{img(m.group(1))}"', h)  # baixar as telas
    h = re.sub(r'<script src="[^"]*tami\.js" defer></script>\n?', "", h)
    h = re.sub(r'<script src="[^"]*cenas2\.js" defer></script>\n?', "", h)
    h = h.replace("</body>", "__JS__</body>", 1)
    assert f'{rz}assets/' not in h and f'"{rz}img/' not in h, f"sobrou caminho local em {rel}"
    return h


paginas = {rel: preparar(rel) for rel in PAGINAS}
tami = ler("assets/tami.js").replace('s.src = raiz + "assets/cena.js";', 's.textContent = parent.TAMI.cena;')
js = {"tami": tami, "cenas2": ler("assets/cenas2.js"), "cena": ler("assets/cena.js")}

casca = """<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Tami</title>
<style>html,body{margin:0;height:100%;background:#fff}iframe{display:block;border:0;width:100%;height:100%}
#aviso{position:fixed;left:50%;bottom:24px;translate:-50% 0;z-index:9;max-width:90vw;padding:14px 20px;border-radius:16px;background:#1B1F4A;color:#fff;font:700 16px system-ui,sans-serif;box-shadow:0 10px 30px rgba(0,0,0,.3)}</style>
</head><body><iframe id="q" title="Site da Tami"></iframe><p id="aviso" hidden role="status"></p>
<script>
window.TAMI = __DADOS__;
(function(){
  var q = document.getElementById("q"), aviso = document.getElementById("aviso"), atual = null, primeira = true, t;
  function avisa(m){ aviso.textContent = m; aviso.hidden = false; clearTimeout(t); t = setTimeout(function(){ aviso.hidden = true; }, 3200); }
  function monta(rel){
    var h = TAMI.paginas[rel];
    h = h.replace("__CSS__", function(){ return TAMI.css + (rel.indexOf("marca/") === 0 ? "\\n" + TAMI.cssMarca : ""); });
    h = h.replace(/__IMG:([^_]+?)__/g, function(_, n){ return TAMI.imgs[n] || ""; });
    var js = "<script>" + TAMI.tami.replace(/<\\/script/gi, "<\\\\/script") + "<\\/script><script>" + TAMI.cenas2.replace(/<\\/script/gi, "<\\\\/script") + "<\\/script>";
    h = h.replace("__JS__", function(){ return js; });
    if (rel === "index.html" && !primeira) {
      // a abertura só passa na primeira visita
      h = h.replace(/<div class="abre"[\\s\\S]*?<\\/script>/, "<script>document.documentElement.classList.add('pronto')<\\/script>");
    }
    return h;
  }
  function resolve(href, base){
    try { var u = new URL(href, "https://tami.local/" + base); } catch(e) { return null; }
    if (u.host !== "tami.local") return null;
    var p = u.pathname.replace(/^\\//, ""); if (p === "" || p.endsWith("/")) p += "index.html";
    return { rel: p, hash: u.hash };
  }
  function abre(rel, hash){
    if (!TAMI.paginas[rel]) { avisa("Esse arquivo só abre no site publicado."); return; }
    var mesma = rel === atual;
    if (!mesma) {
      atual = rel; q.srcdoc = monta(rel); primeira = false;
      q.onload = function(){ liga(); rola(hash); document.title = q.contentDocument.title; };
    } else rola(hash);
  }
  function rola(hash){
    var w = q.contentWindow, el = hash && w.document.getElementById(decodeURIComponent(hash.slice(1)));
    if (el) el.scrollIntoView(); else if (!hash) w.scrollTo(0, 0);
  }
  function liga(){
    q.contentDocument.addEventListener("click", function(e){
      var a = e.target.closest && e.target.closest("a[href]"); if (!a) return;
      var href = a.getAttribute("href");
      if (/^(tel:|mailto:|https?:)/.test(href) || a.target === "_blank") { if (/^https?:/.test(href)) { e.preventDefault(); window.open(href, "_blank", "noopener"); } return; }
      if (href.charAt(0) === "#") return;
      var r = resolve(href, atual); if (!r) return;
      e.preventDefault();
      if (r.rel === atual) { rola(r.hash); return; }
      location.hash = "/" + r.rel.replace(/index\\.html$/, "") + (r.hash ? "|" + r.hash.slice(1) : "");
    }, true);
  }
  function peloEndereco(){
    var h = decodeURIComponent(location.hash.replace(/^#\\/?/, "")), partes = h.split("|");
    var rel = (partes[0] || "") + (partes[0] && !/\\.html$/.test(partes[0]) ? "index.html" : (partes[0] ? "" : "index.html"));
    abre(rel, partes[1] ? "#" + partes[1] : "");
  }
  addEventListener("hashchange", peloEndereco);
  peloEndereco();
})();
</script></body></html>"""

dados = {"css": css, "cssMarca": css_marca, "imgs": IMGS, "paginas": paginas, **js}
saida_js = json.dumps(dados, ensure_ascii=False).replace("</", "<\\/")
h = casca.replace("__DADOS__", saida_js)
saida = os.path.join(R, "tami-unico.html")
open(saida, "w", encoding="utf-8").write(h)
print(f"{saida} · {len(h) / 1024:.0f} KB · {len(paginas)} páginas")
