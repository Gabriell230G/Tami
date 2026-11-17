"""manual.py · Manual de marca da Tami (página /marca/ + versão para PDF + arquivos do logo).
Estrutura inspirada em manuais clássicos (pranchas 16:9 com índice lateral e seções numeradas), sem copiar nenhum.
Uso: chamado pelo site2.main(). O PDF sai de gen/manual_pdf.py.
"""
import html
import os
import re

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

import site2 as S

R = S.R
D = os.path.join(R, "dist")
C = S.COR
VERSAO = "1.0"
DATA = "Novembro de 2026"

# ---------------------------------------------------------------- logo em curvas (não depende de fonte instalada)
_F = TTFont(os.path.join(R, "dist", "fonts", "fredoka-latin-700-normal.woff2")) if os.path.exists(os.path.join(R, "dist", "fonts", "fredoka-latin-700-normal.woff2")) else None


def _fonte():
    global _F
    if _F is None:
        for base in (os.path.join(R, "dist", "fonts"), os.path.join(S.V1, "fonts")):
            p = os.path.join(base, "fredoka-latin-700-normal.woff2")
            if os.path.exists(p):
                _F = TTFont(p)
                break
    return _F


def _texto(txt, tam, x0=0.0, base=0.0, track=-0.02):
    """Caminho SVG do texto em Fredoka 700 (tam = corpo em px). Devolve (d, largura, (xmin, ymin, xmax, ymax))."""
    f = _fonte(); gs = f.getGlyphSet(); cm = f.getBestCmap(); hm = f["hmtx"]; k = tam / f["head"].unitsPerEm
    x = 0.0; ds = []; bx = [1e9, 1e9, -1e9, -1e9]
    for ch in txt:
        g = cm[ord(ch)]
        pen = SVGPathPen(gs); gs[g].draw(TransformPen(pen, (k, 0, 0, -k, x0 + x, base))); ds.append(pen.getCommands())
        bp = BoundsPen(gs); gs[g].draw(bp)
        if bp.bounds:
            a, b, c, d = bp.bounds
            bx = [min(bx[0], x0 + x + a * k), min(bx[1], base - d * k), max(bx[2], x0 + x + c * k), max(bx[3], base - b * k)]
        x += hm[g][0] * k + track * tam
    return " ".join(ds), x - track * tam, tuple(bx)


def monograma(fundo=C["primary"], letra="#FFFFFF", ponto=C["accent"], s=100, x=0, y=0):
    """Monograma: quadrado arredondado, "t" e a pílula limão (o "pingo" da Tami)."""
    k = s / 100
    d, w, bx = _texto("t", 66 * k)
    cx = x + s / 2 - (bx[0] + bx[2]) / 2 - 3 * k
    cy = y + s / 2 - (bx[1] + bx[3]) / 2 + 2 * k
    d, _, _ = _texto("t", 66 * k, cx, cy)
    return (f'<rect x="{x}" y="{y}" width="{s}" height="{s}" rx="{30 * k:.1f}" fill="{fundo}"/>'
            f'<path d="{d}" fill="{letra}"/><rect x="{x + 60 * k:.1f}" y="{y + 22 * k:.1f}" width="{17 * k:.1f}" height="{32 * k:.1f}" rx="{8.5 * k:.1f}" fill="{ponto}"/>')


def lockup(tipo="h", cor_m=C["primary"], cor_t="#FFFFFF", cor_p=C["accent"], cor_nome=C["text"], margem=0):
    """Logo completo em SVG. tipo: h (horizontal), v (vertical), m (só monograma). margem em unidades x."""
    s = 100; x = s / 4  # x = 1/4 do monograma: unidade de área de proteção
    if tipo == "m":
        w, h = s, s; corpo = monograma(cor_m, cor_t, cor_p, s)
    elif tipo == "h":
        d, wn, bx = _texto("tami", 104, 0, 0)
        ox = s + 26 - bx[0]; oy = s / 2 - (bx[1] + bx[3]) / 2
        d, wn, bx = _texto("tami", 104, ox, oy)
        w, h = bx[2], s; corpo = monograma(cor_m, cor_t, cor_p, s) + f'<path d="{d}" fill="{cor_nome}"/>'
    else:
        d, wn, bx = _texto("tami", 92, 0, 0)
        larg = bx[2] - bx[0]; w = max(s, larg)
        ox = w / 2 - (bx[0] + bx[2]) / 2; oy = s + 20 - bx[1]
        d, wn, bx = _texto("tami", 92, ox, oy)
        corpo = monograma(cor_m, cor_t, cor_p, s, (w - s) / 2, 0) + f'<path d="{d}" fill="{cor_nome}"/>'
        h = bx[3]
    m = margem * x
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{-m:.1f} {-m:.1f} {w + 2 * m:.1f} {h + 2 * m:.1f}" '
            f'width="{w + 2 * m:.0f}" height="{h + 2 * m:.0f}" role="img" aria-label="Logo da Tami">{corpo}</svg>')


VARIANTES = {  # nome do arquivo ➜ (monograma, letra, pingo, nome)
    "padrao": (C["primary"], "#FFFFFF", C["accent"], C["text"]),
    "sobre-azul": ("#FFFFFF", C["primary"], C["accent"], "#FFFFFF"),
    "sobre-marinho": (C["primary"], "#FFFFFF", C["accent"], "#FFFFFF"),
    "sobre-limao": (C["text"], "#FFFFFF", C["accent"], C["text"]),
    "marinho": (C["text"], "#FFFFFF", "#FFFFFF", C["text"]),
    "branco": ("#FFFFFF", C["text"], C["text"], "#FFFFFF"),
}


def arquivos_logo():
    """Grava os SVG oficiais em dist/marca/logos/ (o PNG e o ZIP saem do manual_pdf.py)."""
    pasta = os.path.join(D, "marca", "logos"); os.makedirs(pasta, exist_ok=True)
    nomes = []
    for var, cores in VARIANTES.items():
        for tipo, rot in (("h", "horizontal"), ("v", "vertical"), ("m", "monograma")):
            n = f"tami-{rot}-{var}.svg"
            open(os.path.join(pasta, n), "w").write(lockup(tipo, *cores, margem=1))
            nomes.append(n)
    return nomes


# ---------------------------------------------------------------- pranchas
SEC = [("1", "Essência"), ("2", "Logotipo"), ("3", "O rosto"), ("4", "Cores"), ("5", "Tipografia"),
       ("6", "Voz"), ("7", "Elementos"), ("8", "Movimento"), ("9", "Segurança"), ("10", "Aplicações")]


def _lum(h):
    h = h.lstrip("#"); r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= .03928 else ((c + .055) / 1.055) ** 2.4
    return .2126 * f(r) + .7152 * f(g) + .0722 * f(b)


def contraste(a, b):
    la, lb = sorted((_lum(a), _lum(b)), reverse=True)
    return (la + .05) / (lb + .05)


def svg(conteudo, vb="0 0 100 100", cls=""):
    return f'<svg viewBox="{vb}" class="{cls}" aria-hidden="true" focusable="false">{conteudo}</svg>'


_USADOS = {}


def logo_inline(tipo="h", var="padrao", cls="", margem=0, ref=False):
    """Logo direto no HTML. Com ref=True, só um <use> que aponta para o símbolo (o manual repete o logo dezenas de vezes)."""
    s = lockup(tipo, *VARIANTES[var], margem=margem)
    if ref:
        k = f"lg-{tipo}-{var}"
        vb = re.search(r'viewBox="([^"]+)"', s).group(1)
        _USADOS[k] = (vb, s.split(">", 1)[1].rsplit("</svg>", 1)[0])
        return f'<svg class="{cls}" viewBox="{vb}" aria-hidden="true" focusable="false"><use href="#{k}"/></svg>'
    return s.replace('<svg xmlns="http://www.w3.org/2000/svg" ', f'<svg class="{cls}" ', 1).replace('role="img" aria-label="Logo da Tami"', 'aria-hidden="true"')


def _sprite():
    return ('<svg width="0" height="0" style="position:absolute" aria-hidden="true" focusable="false"><defs>'
            + "".join(f'<symbol id="{k}" viewBox="{vb}">{c}</symbol>' for k, (vb, c) in _USADOS.items()) + '</defs></svg>')


def prancha(n, sec, num, titulo, tema, corpo, pag):
    ind = "".join(f'<li class="{"on" if k == sec else ""}"><b>{k}</b>{nome}</li>' for k, nome in SEC)
    return (f'<section class="pr pr-{tema}" id="m-{n}" aria-labelledby="mh-{n}"><div class="pr-in">'
            f'<aside class="pr-ind" aria-hidden="true"><p class="pr-marca">tami <span>manual de marca</span></p><ol>{ind}</ol></aside>'
            f'<div class="pr-corpo"><header class="pr-cab"><span class="pr-num">{num}</span><span>{dict(SEC).get(sec, "")}</span></header>'
            f'<h2 id="mh-{n}">{titulo}</h2>{corpo}'
            f'<footer class="pr-pe" aria-hidden="true"><span>Tami · Manual de marca {VERSAO}</span><span>{pag:02d}</span></footer></div></div></section>')


def pranchas(raiz):
    global logo_inline
    _orig = logo_inline
    logo_inline = lambda tipo="h", var="padrao", cls="", margem=0: _orig(tipo, var, cls, margem, ref=True)
    try:
        corpo = _pranchas(raiz)
        return _sprite() + corpo
    finally:
        logo_inline = _orig


def _pranchas(raiz):
    P = []
    add = lambda *a: P.append(prancha(*a, len(P) + 1))
    # capa
    P.append(f'<section class="pr pr-capa" id="m-capa" aria-labelledby="mh-capa"><div class="pr-in"><div class="capa-in">'
             f'{logo_inline("h", "sobre-marinho", "capa-logo")}<h2 id="mh-capa">Manual de marca</h2>'
             f'<p>Versão {VERSAO} · {DATA}</p></div><div class="capa-rosto">{S.face("oi", "#FFFFFF")}</div>'
             f'<p class="capa-pe">Falou em saúde? Tamo aqui.</p></div></section>')
    # sumário
    sum_ = "".join(f'<li><a href="#m-s{k}"><b>{k}.0</b>{nome}</a></li>' for k, nome in SEC)
    add("sumario", "", "00", "Como usar este manual", "claro",
        '<div class="g2"><div class="pr-txt"><p class="grande">Este manual mostra como usar a marca da Tami.</p>'
        '<p>Use sempre os arquivos oficiais. Na dúvida, escolha a opção mais simples e mais clara.</p>'
        f'<p class="pr-baixar"><a class="btn btn-p" href="{raiz}marca/tami-manual-de-marca.pdf" download>Baixar o manual (PDF)</a>'
        f'<a class="btn btn-s" href="{raiz}marca/index.html#baixar">Baixar os logos</a></p></div>'
        f'<ol class="sum">{sum_}</ol></div>')
    # 1 essência
    add("s1", "1", "1.0", "Falou em saúde? Tamo aqui.", "azul",
        '<div class="g3 ess">'
        '<div><p class="rot">Propósito</p><p class="grande">Ajudar quem está com medo e com pressa a decidir para onde ir.</p></div>'
        '<div><p class="rot">Promessa</p><p class="grande">A Tami orienta, não dá diagnóstico.</p></div>'
        '<div><p class="rot">Jeito</p><p class="grande">Calma, clara e cuidadosa. Uma pergunta por vez.</p></div></div>'
        f'<div class="ess-rosto">{S.face("acolhe", "#FFFFFF")}</div>')
    add("s1b", "1", "1.1", "Personalidade", "claro",
        '<div class="g3">'
        f'<div class="card"><div class="mini">{S.face("ouve", C["primary"])}</div><h3>Calma</h3><p>Fala devagar, sem alarme. Uma pergunta por vez.</p></div>'
        f'<div class="card"><div class="mini">{S.face("oi", C["primary"])}</div><h3>Clara</h3><p>Português simples. Frases curtas, sem palavra difícil.</p></div>'
        f'<div class="card"><div class="mini">{S.face("firme", C["primary"])}</div><h3>Cuidadosa</h3><p>Na dúvida, sugere o caminho mais seguro.</p></div></div>'
        '<div class="g2 enao"><div class="sim"><p class="rot">A Tami é</p><ul><li>acolhedora</li><li>direta</li><li>honesta sobre o que não sabe</li></ul></div>'
        '<div class="nao"><p class="rot">A Tami não é</p><ul><li>médica</li><li>um robô frio</li><li>piadista na hora da urgência</li></ul></div></div>')
    # 2 logotipo
    add("s2", "2", "2.0", "O logotipo", "claro",
        '<div class="logos3">'
        f'<figure><div class="lg-caixa">{logo_inline("h", cls="lg")}</div><figcaption><b>Horizontal</b> · versão principal. Use sempre que couber.</figcaption></figure>'
        f'<figure><div class="lg-caixa">{logo_inline("v", cls="lg lg-v")}</div><figcaption><b>Vertical</b> · para espaços altos e estreitos.</figcaption></figure>'
        f'<figure><div class="lg-caixa">{logo_inline("m", cls="lg lg-m")}</div><figcaption><b>Monograma</b> · ícone do app, avatar e favicon.</figcaption></figure></div>'
        '<p class="nota">O nome é sempre "Tami". No logo, "tami" vem em minúsculas. O pingo limão é a marca registrada do monograma.</p>')
    grade = "".join(f'<line x1="{i * 25}" y1="-60" x2="{i * 25}" y2="160" />' for i in range(-2, 22)) + "".join(f'<line x1="-60" y1="{j * 25}" x2="560" y2="{j * 25}" />' for j in range(-2, 7))
    logo_c = lockup("h", *VARIANTES["padrao"]).split(">", 1)[1].rsplit("</svg>", 1)[0]
    vb_w = float(re.search(r'viewBox="[^ ]+ [^ ]+ ([\d.]+)', lockup("h")).group(1))
    add("s2b", "2", "2.1", "Construção e área de proteção", "nevoa",
        '<div class="g2 cons"><div class="cons-arte">'
        f'<svg viewBox="-50 -50 {vb_w + 100:.0f} 200" aria-hidden="true"><g class="grd">{grade}</g>'
        f'<rect class="area" x="-25" y="-25" width="{vb_w + 50:.0f}" height="150"/>{logo_c}'
        '<g class="cota"><line x1="-25" y1="112" x2="0" y2="112"/><text x="-12.5" y="134">x</text>'
        '<line x1="112" y1="-25" x2="112" y2="0"/><text x="120" y="-8">x</text>'
        '<line x1="104" y1="0" x2="104" y2="100"/><text x="78" y="56">4x</text></g></svg></div>'
        '<div class="pr-txt"><p class="grande">x = um quarto da altura do monograma.</p>'
        '<p>Deixe em volta do logo um espaço livre de pelo menos x. Nada de texto, foto ou borda dentro dessa área.</p>'
        '<p>O nome fica a 1x do monograma e tem a mesma altura visual dele.</p>'
        '<p>O pingo limão fica no canto de cima, à direita do "t". Ele nunca muda de lugar.</p></div></div>')
    add("s2c", "2", "2.2", "Tamanho mínimo", "claro",
        '<div class="g2 tam">'
        f'<div class="tam-it"><div class="tam-arte"><span style="width:96px">{logo_inline("h", cls="lg")}</span><span style="width:120px">{logo_inline("h", cls="lg")}</span><span style="width:180px">{logo_inline("h", cls="lg")}</span></div>'
        '<h3>Horizontal</h3><p>Tela: no mínimo 96 px de largura. Impresso: 25 mm.</p></div>'
        f'<div class="tam-it"><div class="tam-arte"><span style="width:24px">{logo_inline("m", cls="lg")}</span><span style="width:40px">{logo_inline("m", cls="lg")}</span><span style="width:64px">{logo_inline("m", cls="lg")}</span></div>'
        '<h3>Monograma</h3><p>Tela: no mínimo 24 px. Impresso: 8 mm. Abaixo disso, o pingo some.</p></div></div>')
    tiles = [("padrao", "#FFFFFF", "Fundo branco ou névoa", C["text"]), ("sobre-azul", C["primary"], "Fundo azul", "#fff"),
             ("sobre-marinho", C["text"], "Fundo marinho", "#fff"), ("sobre-limao", C["accent"], "Fundo limão", C["text"]),
             ("marinho", "#FFFFFF", "Uma cor só: marinho", C["text"]), ("branco", C["text"], "Uma cor só: branco", "#fff")]
    add("s2d", "2", "2.3", "Versões de cor", "claro",
        '<div class="g3 tiles">' + "".join(f'<figure class="tile" style="background:{bg};color:{tc}">{logo_inline("h", v, "lg")}<figcaption>{rot}</figcaption></figure>' for v, bg, rot, tc in tiles) + '</div>'
        '<p class="nota">Foto atrás do logo só se a área do logo for lisa. Se não for, use a versão em uma cor só.</p>')
    erros = [("est", "Não estique nem achate.", 'style="transform:scaleX(1.5)"'), ("gir", "Não gire.", 'style="transform:rotate(-14deg)"'),
             ("cor", "Não troque as cores.", 'style="filter:hue-rotate(150deg)"'), ("som", "Não use sombra, brilho ou contorno.", 'style="filter:drop-shadow(6px 6px 0 #D63B40)"'),
             ("caps", "Não escreva TAMI em maiúsculas no logo.", ""), ("cruz", "Não junte cruz, jaleco ou estetoscópio.", "")]
    def erro(k, txt, st):
        if k == "caps":
            arte = '<span class="caps">TAMI</span>'
        elif k == "cruz":
            arte = (f'<span class="cruz-box">{logo_inline("m", cls="lg")}<svg viewBox="0 0 40 40" class="cruz" aria-hidden="true"><path d="M15 4h10v11h11v10H25v11H15V25H4V15h11z" fill="#D63B40"/></svg></span>')
        else:
            arte = f'<span class="err-arte" {st}>{logo_inline("h", cls="lg")}</span>'
        return f'<figure class="erro"><div class="err-caixa">{arte}<i class="x" aria-hidden="true">✕</i></div><figcaption>{txt}</figcaption></figure>'
    add("s2e", "2", "2.4", "O que não fazer", "claro", '<div class="g3 erros">' + "".join(erro(*e) for e in erros) + '</div>')
    # 3 o rosto
    exprs = [("oi", "Oi", "Chegada e cumprimento. Também no caminho UBS."), ("ouve", "Ouve", "Enquanto a pessoa fala ou escreve."),
             ("pensa", "Pensa", "Enquanto a Tami confere as respostas."), ("acolhe", "Acolhe", "Quando a pessoa conta algo difícil. Caminho casa."),
             ("olha", "Olha", "Chama a atenção para uma ação. Caminho UPA."), ("firme", "Firme", "Emergência. Sério e direto. Caminho SAMU."),
             ("feliz", "Feliz", "Deu certo, conversa concluída."), ("pisca", "Pisca", "Momento leve. Nunca na urgência.")]
    add("s3", "3", "3.0", "O rosto da Tami", "azul",
        '<div class="g4 exprs">' + "".join(f'<figure><div class="ex">{S.face(k, "#FFFFFF")}</div><figcaption><b>{n}</b>{q}</figcaption></figure>' for k, n, q in exprs) + '</div>'
        '<p class="nota">O desenho do rosto nunca muda. Só a expressão muda, sempre com um motivo.</p>')
    add("s3b", "3", "3.1", "A escala de 0 a 10", "claro",
        '<div class="escala-m">' + "".join(f'<figure><svg viewBox="-6 -6 112 112" aria-hidden="true">{S.cara_n(n)}</svg><figcaption>{n}</figcaption></figure>' for n in range(11)) + '</div>'
        '<div class="g3 regras"><p><b>Sempre de 0 a 10.</b> Zero é nada. Dez é a pior dor que a pessoa já sentiu.</p>'
        '<p><b>Sempre com o número.</b> O rostinho ajuda, mas quem decide é o número.</p>'
        '<p><b>Três faixas.</b> Fraco de 0 a 3, médio de 4 a 6, forte de 7 a 10. As mesmas do app.</p></div>')
    # 4 cores
    sw = [("Azul Tami", C["primary"], "#fff", "Cor principal. Botões, logo, destaques."), ("Marinho", C["text"], "#fff", "Texto e fundos escuros."),
          ("Limão", C["accent"], C["text"], "Só destaque: botão principal, seleção, pingo."), ("Névoa", C["mist"], C["text"], "Fundo suave das telas."),
          ("Branco", "#FFFFFF", C["text"], "Respiro. A maior parte do espaço.")]
    rgb = lambda h: ", ".join(str(int(h.lstrip("#")[i:i + 2], 16)) for i in (0, 2, 4))
    add("s4", "4", "4.0", "Cores da marca", "claro",
        '<div class="sw5">' + "".join(f'<figure class="sw" style="background:{h};color:{tc}"><b>{n}</b><span>HEX {h.upper()}<br>RGB {rgb(h)}</span><p>{u}</p></figure>' for n, h, tc, u in sw) + '</div>'
        '<p class="rot" style="margin-top:2.2em">Proporção</p><div class="prop"><i style="flex:45;background:#fff"></i><i style="flex:20;background:#EEF0FF"></i>'
        f'<i style="flex:20;background:{C["primary"]}"></i><i style="flex:10;background:{C["text"]}"></i><i style="flex:5;background:{C["accent"]}"></i></div>'
        '<p class="nota">Muito branco e névoa. Azul como cor principal. Limão só como tempero.</p>')
    cam = [("SAMU 192", C["danger"], "#fff", "Emergência. Ligar agora."), ("UPA hoje", C["upa"], C["text"], "Atendimento ainda hoje."),
           ("UBS", C["ubs"], C["text"], "Posto de saúde nos próximos dias."), ("Cuidar em casa", C["casa"], C["text"], "Com os sinais para ficar de olho.")]
    add("s4b", "4", "4.1", "As cores dos caminhos", "marinho",
        '<div class="g4 cams">' + "".join(f'<figure style="background:{h};color:{tc}"><b>{n}</b><span>HEX {h.upper()}</span><p>{u}</p></figure>' for n, h, tc, u in cam) + '</div>'
        '<p class="grande" style="margin-top:1.4em">Estas quatro cores só falam dos caminhos. Nunca use como enfeite.</p>'
        '<p>O vermelho é só para emergência. Se ele aparece, é porque é sério.</p>')
    pares = [("#FFFFFF", C["primary"], "Branco no azul"), (C["text"], "#FFFFFF", "Marinho no branco"), (C["text"], C["accent"], "Marinho no limão"),
             ("#FFFFFF", C["danger"], "Branco no vermelho"), (C["text"], C["upa"], "Marinho no laranja"), (C["text"], C["ubs"], "Marinho no azul-claro"),
             (C["accent"], "#FFFFFF", "Limão no branco"), (C["primary"], C["text"], "Azul no marinho")]
    def par(fg, bg, rot):
        r = contraste(fg, bg); ok = r >= 4.5; med = 3 <= r < 4.5
        sel = "AA" if ok else ("só título grande" if med else "não use")
        leg = fg if ok else (C["text"] if _lum(bg) > .4 else "#FFFFFF")  # a legenda sempre legível, mesmo no exemplo ruim
        return (f'<figure class="par {"ok" if ok else "med" if med else "nao"}" style="background:{bg};color:{leg}">'
                f'<svg class="aa-s" viewBox="0 0 64 36" aria-hidden="true"><text x="0" y="31" fill="{fg}">Aa</text></svg>'
                f'<figcaption><span>{rot}</span><span>{r:.1f}:1 · {sel}</span></figcaption></figure>')
    add("s4c", "4", "4.2", "Contraste", "claro",
        '<div class="g4 pares">' + "".join(par(*p_) for p_ in pares) + '</div>'
        '<p class="nota">Texto comum precisa de pelo menos 4,5:1. Título grande, 3:1. Limão nunca vira texto sobre branco.</p>')
    # 5 tipografia
    add("s5", "5", "5.0", "Tipografia", "claro",
        '<div class="g2 tipos"><div class="tipo-f"><p class="rot">Títulos</p><p class="aa fred">Aa</p><p class="nome-f fred">Fredoka</p>'
        '<p class="abc fred">ABCDEFGHIJKLMNOPQRSTUVWXYZ<br>abcdefghijklmnopqrstuvwxyz<br>0123456789 áéíóú ç ?!</p><p>Pesos 600 e 700. Redonda e amiga, como o rosto.</p></div>'
        '<div class="tipo-n"><p class="rot">Texto</p><p class="aa nun">Aa</p><p class="nome-f nun">Nunito</p>'
        '<p class="abc nun">ABCDEFGHIJKLMNOPQRSTUVWXYZ<br>abcdefghijklmnopqrstuvwxyz<br>0123456789 áéíóú ç ?!</p><p>Pesos 600, 700 e 800. Fácil de ler em tela pequena.</p></div></div>'
        '<p class="nota">As duas fontes são abertas (licença OFL). Use só estas duas.</p>')
    hier = [("Título de tela", "Fredoka 700", "34/38", "fred", "34"), ("Título de bloco", "Fredoka 600", "26/30", "fred", "26"),
            ("Texto", "Nunito 600", "17/24", "nun", "17"), ("Apoio", "Nunito 600", "15/20", "nun", "15"), ("RÓTULO", "Nunito 800, caixa alta", "15/20", "nun rotx", "15")]
    add("s5b", "5", "5.1", "Hierarquia", "nevoa",
        '<ul class="hier">' + "".join(f'<li><span class="{c}" style="--tam:{tm}">{n}</span><span>{f}</span><span>{m} px</span></li>' for n, f, m, c, tm in hier) + '</ul>'
        '<p class="nota">No site, os títulos crescem com a tela. A proporção entre eles fica a mesma.</p>')
    # 6 voz
    regras = ["Fala no feminino e na primeira pessoa: \"eu sou a Tami\".", "Frases de até 15 palavras. Uma pergunta por vez.",
              "Diz o que vai fazer antes de fazer.", "Valida antes de perguntar: \"obrigada por me contar\".",
              "Diz o limite com calma: \"eu não indico remédio\".", "Na urgência, a voz encolhe: só a ação."]
    ex = [("Descreva o que está sentindo.", "Oi, eu sou a Tami. Me conta o que está acontecendo."),
          ("Resultado: UBS.", "Pronto. Já sei para onde você deve ir."),
          ("(fim)", "Você se sente bem cuidado com essa orientação?")]
    add("s6", "6", "6.0", "A voz da Tami", "nevoa",
        '<div class="g2 voz"><ol class="regras6">' + "".join(f"<li>{html.escape(r)}</li>" for r in regras) + '</ol>'
        '<div class="ex">' + "".join(f'<div class="ex-l"><p class="b-neutro">{html.escape(a)}</p><p class="b-tami">{html.escape(b)}</p></div>' for a, b in ex) + '</div></div>')
    trocas = [("diagnostica", "ajuda a decidir para onde ir"), ("médico virtual", "app de orientação em saúde"),
              ("substitui o médico", "ajuda a chegar ao serviço certo"), ("faz triagem", "orienta para onde ir"),
              ("monitora a sua saúde", "conversa quando você precisa"), ("aprovada pela ANVISA", "está em teste"),
              ("app do SUS", "projeto independente de um estudante"), ("IA que responde tudo", "segue regras de fontes públicas")]
    add("s6b", "6", "6.1", "Diga e evite", "claro",
        '<div class="trocas"><p class="rot r-nao">Evite</p><span></span><p class="rot r-sim">Diga</p>'
        + "".join(f'<p class="t-nao"><s>{n}</s></p><span class="t-seta" aria-hidden="true">➜</span><p class="t-sim">{y}</p>' for n, y in trocas) + '</div>'
        '<p class="nota">Sem travessão. Sem palavra difícil. Sem número inventado. Sem depoimento inventado.</p>')
    # 7 elementos
    orbs = "".join(f'<i class="mn-orb" style="--c:{h}"></i>' for h in (C["danger"], C["upa"], C["ubs"], C["casa"]))
    formas = (svg('<rect x="38" y="10" width="24" height="80" rx="12" fill="#B6EC0C"/>', cls="forma") +
              svg('<path d="M18 30 A32 32 0 0 0 82 30" fill="none" stroke="#fff" stroke-width="18" stroke-linecap="round"/>', cls="forma") +
              svg('<path d="M50 6 C54 34 66 46 94 50 C66 54 54 66 50 94 C46 66 34 54 6 50 C34 46 46 34 50 6Z" fill="none" stroke="#B6EC0C" stroke-width="6" stroke-linejoin="round"/>', cls="forma"))
    add("s7", "7", "7.0", "Elementos gráficos", "azul",
        f'<div class="g3 elem"><div><div class="orbs">{orbs}</div><h3>Bolinhas</h3><p>Uma para cada caminho, sempre na cor dele. Têm luz e sombra, como bolas de gude.</p></div>'
        f'<div><div class="formas">{formas}</div><h3>Formas</h3><p>Pílula, arco e estrela vêm do próprio rosto. Use poucas, soltas, como adesivos.</p></div>'
        '<div><div class="liq"><svg viewBox="0 0 170 80" aria-hidden="true"><defs><clipPath id="liq-c"><text x="85" y="66" text-anchor="middle">tami</text></clipPath></defs>'
        '<text x="85" y="66" text-anchor="middle" fill="#6E78E3">tami</text><g clip-path="url(#liq-c)"><path d="M0 44 Q21 36 42 44 T85 44 T127 44 T170 44 V80 H0Z" fill="#fff"/></g></svg></div><h3>Líquido</h3><p>Ondas calmas, nunca agitadas. Lembram água e cuidado.</p></div></div>')
    # 8 movimento
    add("s8", "8", "8.0", "Movimento", "marinho",
        '<div class="g2 movi"><ol class="regras6">'
        '<li>Calma: nada pisca rápido nem treme.</li><li>Com motivo: o movimento explica algo.</li>'
        '<li>Abertura curta, sempre com o botão Pular.</li><li>Sempre com "Pausar movimento". Quem pede menos movimento, ganha a versão parada.</li>'
        '<li>A faixa do 192 nunca se mexe e nunca fica coberta.</li></ol>'
        '<div class="curvas"><p class="rot">Curvas</p><div class="curva"><svg viewBox="0 0 120 80" aria-hidden="true"><path d="M5 75 C40 75 50 5 115 5" fill="none" stroke="#B6EC0C" stroke-width="5" stroke-linecap="round"/></svg>'
        '<p><b>Entrada</b>320 a 600 ms, suave no fim.</p></div>'
        '<div class="curva"><svg viewBox="0 0 120 80" aria-hidden="true"><path d="M5 75 C40 75 45 -15 80 8 C95 18 100 5 115 5" fill="none" stroke="#B6EC0C" stroke-width="5" stroke-linecap="round"/></svg>'
        '<p><b>Pulinho</b>só em seleção e conquista.</p></div></div></div>')
    # 9 segurança
    add("s9", "9", "9.0", "Segurança vem antes da marca", "claro",
        '<div class="faixa-m"><span>☎ Emergência? Ligue 192</span></div>'
        '<div class="g2 seg"><ul class="regras6 x">'
        '<li>A faixa do 192 fica sempre no topo, em vermelho, em toda tela.</li>'
        '<li>Nunca prometa diagnóstico. O aviso padrão é: "A Tami orienta, não dá diagnóstico."</li>'
        '<li>Nunca use cruz, jaleco, estetoscópio ou foto de hospital.</li>'
        '<li>Nunca invente número, selo ou depoimento.</li></ul>'
        '<div class="selos"><span class="selo-m">Em teste · ainda não está nas lojas</span><span class="aviso-m">A Tami orienta, não dá diagnóstico.</span>'
        '<p>Estes dois textos aparecem perto de todo botão de "Quero testar".</p></div></div>')
    # 10 aplicações
    apps = "".join(f'<i style="background:{c}"></i>' for c in ("#E3E6FB", "#C9CDF5", "#EEF0FF", "#E3E6FB", "#C9CDF5", "#EEF0FF", "#E3E6FB"))
    add("s10", "10", "10.0", "Aplicações", "nevoa",
        '<div class="g4 apl">'
        f'<figure><div class="cel"><div class="home">{apps}<span class="ic">{logo_inline("m", cls="lg")}<b>Tami</b></span></div></div><figcaption>Ícone do app</figcaption></figure>'
        f'<figure><div class="post"><p>Falou em saúde?<br><em>Tamo aqui.</em></p><div class="post-r">{S.face("feliz", "#FFFFFF")}</div>{logo_inline("h", "sobre-azul", "post-l")}</div><figcaption>Post quadrado</figcaption></figure>'
        f'<figure><div class="story"><p>De 0 a 10, quanto dói?</p><div class="st-caras">' + "".join(f'<svg viewBox="-6 -6 112 112" aria-hidden="true">{S.cara_n(n)}</svg>' for n in (0, 3, 5, 8, 10)) +
        f'</div>{logo_inline("h", "padrao", "story-l")}</div><figcaption>Story</figcaption></figure>'
        f'<figure><div class="adesivo"><div>{S.face("pisca", "#FFFFFF")}</div></div><figcaption>Adesivo</figcaption></figure></div>')
    add("s10b", "10", "10.1", "No app e no site", "azul",
        f'<div class="g2 app-site"><div class="cel-app"><img src="{raiz}img/voz-720.webp" alt="" width="360" height="779" loading="lazy"></div>'
        '<div class="pr-txt"><p class="grande">Mesmo rosto, mesmas cores, mesma voz.</p>'
        '<p>No app, a faixa do 192 fica no topo de toda tela. Os botões são grandes, com pelo menos 48 px.</p>'
        '<p>No site, o rosto acompanha a leitura e muda de expressão em cada parte.</p></div></div>')
    # fim
    P.append(f'<section class="pr pr-capa pr-fim" id="m-fim" aria-labelledby="mh-fim"><div class="pr-in"><div class="capa-in">'
             f'<h2 id="mh-fim">Dúvidas sobre a marca?</h2><p><a href="{S.LINKEDIN}">{S.LINKEDIN_TXT}</a></p>'
             f'<p class="fim-nota">Gabriel Gomes · Tami · Manual de marca {VERSAO} · {DATA}</p></div>'
             f'<div class="capa-rosto">{S.face("feliz", "#FFFFFF")}</div></div></section>')
    return "".join(P)


FUNDO = {"padrao": "#FFFFFF", "sobre-azul": C["primary"], "sobre-marinho": C["text"], "sobre-limao": C["accent"], "marinho": "#FFFFFF", "branco": C["text"]}
NOME_VAR = {"padrao": "Padrão", "sobre-azul": "Sobre azul", "sobre-marinho": "Sobre marinho", "sobre-limao": "Sobre limão", "marinho": "Uma cor: marinho", "branco": "Uma cor: branco"}


def baixar(r):
    """Área de downloads: cada logo em SVG e PNG (1024 px)."""
    itens = ""
    for var in VARIANTES:
        for tipo, rot in (("h", "horizontal"), ("v", "vertical"), ("m", "monograma")):
            n = f"tami-{rot}-{var}"
            itens += (f'<li><div class="bx-logo bx-{tipo}" style="background:{FUNDO[var]}">{logo_inline(tipo, var, "lg")}</div>'
                      f'<p><b>{rot.capitalize()}</b> · {NOME_VAR[var]}</p>'
                      f'<p class="bx-links"><a href="{r}marca/logos/{n}.svg" download>SVG</a><a href="{r}marca/logos/{n}.png" download>PNG</a></p></li>')
    return (f'<section class="baixar" id="baixar" aria-labelledby="h-baixar"><div class="cx"><h2 id="h-baixar">Baixar os logos</h2>'
            f'<p>SVG para tudo que puder. PNG com 1024 px de largura e fundo transparente.</p><ul class="bx">{itens}</ul></div></section>')


def pagina_marca():
    p = "marca"; r = S.raiz(p)
    topo = (f'<section class="cena topo-pag" data-cena data-ceu="nevoa" data-rosto="oi" data-bolhas="nao" aria-labelledby="h1">'
            f'<div class="cx"><div class="txt"><p class="rotulo">{S.t(p, "topo", "rotulo")}</p><h1 id="h1">{S.t(p, "topo", "titulo")}</h1>'
            f'<p class="sub">{S.t(p, "topo", "sub")}</p>'
            f'<p class="acoes"><a class="btn btn-p" href="{r}marca/tami-manual-de-marca.pdf" download>Baixar o manual (PDF)</a>'
            f'<a class="btn btn-s" href="#baixar">Baixar os logos</a></p></div>'
            f'{S.alvo("oi", C["primary"])}</div></section>')
    corpo = f'<section class="manual" aria-label="Pranchas do manual de marca"><div class="manual-cx">{pranchas(r)}</div></section>' + baixar(r)
    return S.pagina(p, "nevoa", topo + corpo)


def pagina_pdf():
    """Só as pranchas, 1600 x 900 cada, para virar PDF."""
    r = "../"
    return f"""<!DOCTYPE html><html lang="pt-BR" class="pdf"><head><meta charset="utf-8"><title>Tami · Manual de marca {VERSAO}</title>
<link rel="stylesheet" href="{r}assets/tokens-web.css"><link rel="stylesheet" href="{r}assets/site.css"><link rel="stylesheet" href="{r}assets/marca.css">
<style>@page {{ size: 1600px 900px; margin: 0; }} html, body {{ margin: 0; background: #fff; padding: 0 !important; }} .manual {{ padding: 0 !important; background: #fff !important; }}
.manual-cx {{ display: block !important; width: 1600px !important; max-width: none !important; padding: 0 !important; }}
.pr {{ width: 1600px; height: 900px; aspect-ratio: auto !important; border-radius: 0 !important; box-shadow: none !important; margin: 0 !important; break-after: page; page-break-after: always; }}
[data-revela] {{ opacity: 1 !important; translate: none !important; }}</style></head>
<body><div class="manual"><div class="manual-cx">{pranchas(r)}</div></div></body></html>"""
