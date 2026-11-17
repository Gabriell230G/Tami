"""Componentes da Tami v3. Tudo lê de tokens.py (cores, medidas, tipos) e estilo3.py (CSS global).
Mantém os nomes e assinaturas da v2 para que todos os geradores herdem o visual novo.
"""
import json
from tokens import COR, RAIO, TIPO, MOV
from estilo3 import CSS
from lib import palavra  # noqa: F401

P = '/home/claude/tami-design/project/'
# nomes antigos ➜ tokens (os geradores antigos importam estes nomes)
AZUL = COR["primary"]; LIMAO = COR["accent"]; MARINHO = COR["text"]; NEVOA = COR["mist"]; BRANCO = COR["bg"]
VERMELHO_BOTAO = COR["danger"]; VERMELHO = COR["danger"]; CINZA = COR["muted"]; LINHA = COR["divider"]
SOMBRA_AZUL = COR["primary-deep"]; TRILHO = COR["surface-alt"]; DESL_TXT = COR["muted"]
AZ50 = COR["surface"]; AZ100 = COR["surface-alt"]; AZ200 = COR["surface-strong"]; AZ300 = COR["border"]; AZ400 = COR["primary-light"]
AZ600 = COR["primary-press"]; AZ700 = COR["primary-deep"]
LM50 = COR["lime-faint"]; LM100 = COR["lime-soft"]; LM600 = COR["accent-mid"]; LM800 = COR["accent-deep"]
FF_TIT = "TamiFredoka, Fredoka, TamiNunito, Nunito, sans-serif"
FF_TXT = "TamiNunito, Nunito, system-ui, sans-serif"
FRED = f"font-family: {FF_TIT}"
FONTES = ('<link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@600;700&amp;family=Nunito:wght@600;700;800&amp;display=swap" rel="stylesheet">'
          '<style>' + ''.join(f'@font-face{{font-family:TamiFredoka;font-weight:{w};font-display:swap;src:url(./fonts/fredoka-latin-{w}-normal.woff2) format("woff2")}}' for w in (600, 700))
          + ''.join(f'@font-face{{font-family:TamiNunito;font-weight:{w};font-display:swap;src:url(./fonts/nunito-latin-{w}-normal.woff2) format("woff2")}}' for w in (600, 700, 800))
          + '</style>')
from icons3 import ic3, IC3  # noqa: E402
VOLUME = (f"background: radial-gradient(120% 120% at 28% 18%, {COR['primary-glow']} 0%, {COR['primary']} 55%, {COR['primary-press']} 100%); "
          "box-shadow: inset 0 -5px 0 rgba(27,31,74,0.18), inset 0 3px 0 rgba(255,255,255,0.20), 0 8px 24px rgba(63,74,203,0.28)")


def tipo(nome, cor=None, extra=""):
    """Estilo de texto de um papel da escala. A altura de linha é proporcional (cresce com a fonte)."""
    t, a, p, fam = TIPO[nome]
    ff = FF_TIT if fam == "Fredoka" else FF_TXT
    return f"font-family: {ff}; font-size: {t}px; line-height: {max(a / t, 1.1):.2f}; font-weight: {p}" + (f"; color: {cor}" if cor else "") + (f"; {extra}" if extra else "")


# ---------------- rosto ----------------
def _boca_onda(cor):
    alturas = [40, 72, 96, 72, 40]
    return "".join(f'<rect class="barra" x="{108 + i * 22}" y="{84 - h / 2}" width="16" height="{h}" rx="8" fill="{cor}"></rect>' for i, h in enumerate(alturas))


def rosto(cor, w, tipo="oi", anima=None):
    """anima: None | 'pisca' | 'pensa' | 'ouve' | 'pulo'. A forma das expressões não muda (marca congelada)."""
    h = round(w / 2)
    olho = lambda x, y, hh: f'<rect class="olho" x="{x}" y="{y}" width="48" height="{hh}" rx="24" fill="{cor}"></rect>'
    arco = lambda d, sw: f'<path d="{d}" fill="none" stroke="{cor}" stroke-width="{sw}" stroke-linecap="round"></path>'
    if tipo == "pisca":
        d = arco("M12 84 A26 26 0 0 1 64 84", 34) + arco("M256 84 A26 26 0 0 1 308 84", 34) + arco("M123 82 A37 37 0 0 0 197 82", 36)
    elif tipo == "feliz":
        d = (arco("M12 84 A26 26 0 0 1 64 84", 34) + arco("M256 84 A26 26 0 0 1 308 84", 34)
             + f'<path d="M128 94 L192 94 A32 32 0 0 1 128 94 Z" fill="{cor}" stroke="{cor}" stroke-width="10" stroke-linejoin="round"></path>')
    elif tipo == "firme":
        d = olho(8, 14, 124) + olho(264, 14, 124) + arco("M130 104 L190 104", 32)
    elif tipo == "pensa":
        d = olho(8, 6, 100) + olho(264, 6, 100) + arco("M134 116 L186 116", 30)
    elif tipo == "acolhe":
        d = arco("M14 92 A26 26 0 0 1 66 92", 30) + arco("M254 92 A26 26 0 0 1 306 92", 30) + arco("M130 90 A30 30 0 0 0 190 90", 32)
    elif tipo == "ouve":
        d = olho(8, 36, 96) + olho(264, 36, 96) + (_boca_onda(cor) if anima == "ouve" else arco("M134 88 A26 26 0 0 0 186 88", 30))
    elif tipo == "olha":
        d = f'<rect class="olho" x="20" y="20" width="48" height="112" rx="24" fill="{cor}"></rect><rect class="olho" x="272" y="20" width="48" height="112" rx="24" fill="{cor}"></rect>' + arco("M143 82 A37 37 0 0 0 217 82", 36)
    elif tipo == "dorme":  # sem internet: olhos fechados
        d = arco("M12 92 L60 92", 30) + arco("M260 92 L308 92", 30) + arco("M136 110 L184 110", 28)
    else:
        d = olho(8, 20, 112) + olho(264, 20, 112) + arco("M123 82 A37 37 0 0 0 197 82", 36)
    cls = f' class="t-anima-{anima}"' if anima else ""
    return f'<svg{cls} width="{w}" height="{h}" viewBox="0 0 320 160" aria-hidden="true" style="overflow: visible">{d}</svg>'


def avatar(w, tipo="oi", fundo=AZUL, anima="pisca"):
    estilo = VOLUME if fundo == AZUL else f"background: {fundo}"
    an = anima if tipo in ("oi", "olha", "firme", "pensa", "ouve") else None
    if tipo == "pensa": an = "pensa"
    if tipo == "ouve": an = "ouve"
    return (f'<div style="width: {w}px; height: {w}px; border-radius: {round(w * 0.32)}px; {estilo}; display: flex; align-items: center; justify-content: center; flex-shrink: 0">'
            f'<div style="display: flex; filter: drop-shadow(0 {max(1, round(w / 40))}px 0 rgba(27,31,74,0.18))">{rosto(BRANCO, round(w * 0.68), tipo, an)}</div></div>')


# ---------------- fala ----------------
def ouvir_btn(txt="Ouvir", cor=AZUL):
    return (f'<button type="button" aria-label="Ouvir em voz alta" style="height: 48px; padding: 0 12px; margin: -8px 0 -8px -12px; border-radius: 999px; display: inline-flex; align-items: center; gap: 8px; color: {cor}; {tipo("apoio")}; font-weight: 800">'
            f'{ic3("ouvir", 24, m=cor)}{txt}</button>')


def balao(txt, grande=False, ouvir=True):
    t = tipo("cartao", MARINHO, "font-size: 20px; line-height: 26px") if grande else tipo("cartao", MARINHO)
    return (f'<div style="position: relative; border: 2px solid {AZ100}; border-radius: 20px; background: {BRANCO}; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px">'
            f'<div style="{t}">{txt}</div>' + (ouvir_btn() if ouvir else '') +
            f'<div style="position: absolute; left: -8px; top: 24px; width: 12px; height: 12px; background: {BRANCO}; border-left: 2px solid {AZ100}; border-bottom: 2px solid {AZ100}; transform: rotate(45deg)"></div></div>')


def balao_cima(txt):
    return (f'<div style="position: relative; border: 2px solid {AZ100}; border-radius: 20px; background: {BRANCO}; padding: 16px 20px; {tipo("cartao", MARINHO, "font-size: 20px; line-height: 26px; text-align: center")}">{txt}'
            f'<div style="position: absolute; left: calc(50% - 6px); bottom: -8px; width: 12px; height: 12px; background: {BRANCO}; border-right: 2px solid {AZ100}; border-bottom: 2px solid {AZ100}; transform: rotate(45deg)"></div></div>')


# ---------------- 192 e topo ----------------
def sos():
    return (f'<a href="tel:192" aria-label="Emergência: ligar 192" style="height: 48px; padding: 0 16px; border-radius: 999px; background: {VERMELHO_BOTAO}; color: {BRANCO}; display: flex; align-items: center; gap: 8px; {tipo("apoio")}; font-weight: 800; text-decoration: none; flex-shrink: 0">'
            f'{ic3("fone", 20, m=BRANCO, a=COR["danger-light"], wcor=VERMELHO_BOTAO)}192</a>')


def voltar_btn(href, rotulo="Voltar"):
    return (f'<a href="{href}" aria-label="{rotulo}" style="width: 48px; height: 48px; border-radius: 999px; display: flex; align-items: center; justify-content: center; flex-shrink: 0">'
            f'{ic3("voltar", 24, m=MARINHO)}</a>')


def topo(voltar, pct):
    barra = (f'<div role="progressbar" aria-valuenow="{pct}" aria-valuemin="0" aria-valuemax="100" aria-label="Progresso" style="flex-grow: 1; height: 16px; border-radius: 999px; background: {TRILHO}; overflow: hidden">'
             f'<div style="width: {pct}%; height: 100%; border-radius: 999px; background: {AZUL}; position: relative; transition: width {MOV["estado"]}ms"><div style="position: absolute; left: 8px; right: 8px; top: 4px; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.35)"></div></div></div>') if pct is not None else '<div style="flex-grow: 1"></div>'
    return f'<div style="display: flex; align-items: center; gap: 12px">{voltar_btn(voltar)}{barra}{sos()}</div>'


# ---------------- botões ----------------
def frase(txt):
    from normalizar import frase as _f
    return _f(txt)


def botao(txt, href, estilo="primario", motivo=None):
    """estilo: primario | contorno | perigo | desligado. 'desligado' mostra o motivo embaixo (nunca some calado)."""
    if estilo == "primario":
        s = f"background: {AZUL}; color: {BRANCO}; box-shadow: 0 3px 0 {SOMBRA_AZUL}"
    elif estilo == "contorno":
        s = f"background: {BRANCO}; color: {AZUL}; border: 2px solid {AZ200}; box-shadow: 0 3px 0 {COR['border']}"
    elif estilo == "perigo":
        s = f"background: {VERMELHO_BOTAO}; color: {BRANCO}; box-shadow: 0 3px 0 {COR['danger-deep']}"
    elif estilo == "branco":
        s = f"background: {BRANCO}; color: {COR['danger-text']}; box-shadow: 0 3px 0 {COR['danger-deep']}"
    else:
        s = f"background: {TRILHO}; color: {CINZA}"
    txt = frase(txt)
    ini = f'<a href="{href}"' if (href and estilo != "desligado") else '<div role="button" aria-disabled="true"'
    fim = '</a>' if (href and estilo != "desligado") else '</div>'
    b = (f'{ini} style="min-height: 56px; border-radius: {RAIO["botao"]}px; {s}; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 0 20px; '
         f'{tipo("botao")}; text-decoration: none; box-sizing: border-box; text-align: center">{txt}{fim}')
    if estilo == "desligado" and motivo:
        b = f'<div style="display: flex; flex-direction: column; gap: 8px">{b}<div style="{tipo("apoio", CINZA)}; text-align: center">{motivo}</div></div>'
    return b


def icone(nome, cor=AZUL, fundo=None, w=48):
    fundo = fundo or (AZ50 if cor != VERMELHO_BOTAO else COR["danger-soft"])
    g = ic3(nome, round(w * 0.6), m=cor, a=(LIMAO if cor not in (VERMELHO_BOTAO, BRANCO) else COR["danger-light"]))
    return (f'<div style="width: {w}px; height: {w}px; border-radius: {RAIO["campo"]}px; background: {fundo}; display: flex; align-items: center; justify-content: center; flex-shrink: 0">{g}</div>')


def opcao(ic, txt, sub=None, sel=False, multi=False, perigo=False, href=None):
    borda = MARINHO if sel else (VERMELHO_BOTAO if perigo else COR["border"])
    fundo = LM100 if sel else BRANCO
    cor = MARINHO if sel else (COR["danger-text"] if perigo else MARINHO)
    caixa = ""
    marca = (f'<svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12 L10 17 L19 7" fill="none" stroke="{BRANCO}" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
    if sel and not multi:
        caixa = f'<div style="width: 24px; height: 24px; border-radius: 999px; background: {MARINHO}; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-left: auto">{marca.replace(BRANCO, LIMAO)}</div>'
    if multi:
        caixa = (f'<div style="width: 24px; height: 24px; border-radius: 6px; border: 2px solid {AZUL if sel else COR["border"]}; background: {AZUL if sel else BRANCO}; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-left: auto; box-sizing: border-box">'
                 + (marca if sel else '') + '</div>')
    subt = f'<div style="{tipo("apoio", CINZA)}">{sub}</div>' if sub else ""
    papel = 'role="checkbox"' if multi else 'role="radio"'
    # opção com link é uma ação (responde e avança): é link, não "radio"
    ini = (f'<a href="{href}" style="text-decoration: none; width: 100%;' if href
           else f'<button type="button" {papel} aria-checked="{"true" if sel else "false"}" style="width: 100%;')
    fim = '</a>' if href else '</button>'
    return (f'{ini} min-height: 56px; border-radius: {RAIO["opcao"]}px; border: 2px solid {borda}; background: {fundo}; box-shadow: 0 3px 0 {borda}; '
            f'display: flex; align-items: center; gap: 12px; padding: 8px 16px 8px 8px; box-sizing: border-box; text-align: left; font-family: {FF_TXT}; color: {cor}">'
            f'{icone(ic, AZUL if not perigo else VERMELHO_BOTAO, BRANCO if sel else None)}'
            f'<div style="display: flex; flex-direction: column; gap: 2px"><div style="{tipo("texto")}; font-weight: 800">{txt}</div>{subt}</div>{caixa}{fim}')


def pergunta(av_tipo, txt):
    return f'<div style="display: flex; align-items: flex-start; gap: 12px">{avatar(64, av_tipo)}<div style="flex-grow: 1; padding-top: 4px">{balao(txt)}</div></div>'


# ---------------- tela ----------------
def tela(arquivo, titulo, corpo, fundo=BRANCO, link_raiz=None, escuro=False):
    if link_raiz:
        corpo = f'<a href="{link_raiz}" aria-label="Continuar" style="display: flex; flex-direction: column; flex-grow: 1; text-decoration: none; color: inherit">{corpo}</a>'
    cor_txt = BRANCO if (escuro or fundo in (AZUL, MARINHO, VERMELHO_BOTAO, COR["dark-bg"], COR["primary-press"])) else MARINHO
    html = f'''<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titulo}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
{FONTES}
<style>{CSS}</style>
</helmet>
<div class="t-raiz" style="width: 390px; height: 844px; box-sizing: border-box; background: {fundo}; padding: 56px 20px 32px; display: flex; flex-direction: column; font-family: {FF_TXT}; font-weight: 600; color: {cor_txt}; position: relative; overflow-x: hidden; overflow-y: auto">
{corpo}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":390,"height":844}}}}'>
class Component extends DCLogic {{
renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''
    open(P + arquivo, 'w').write(html)


IC = {
 "eu": '<circle cx="24" cy="15" r="8"></circle><path d="M9 41 A15 15 0 0 1 39 41"></path>',
 "crianca": '<circle cx="24" cy="18" r="6"></circle><path d="M14 40 A10 10 0 0 1 34 40"></path><path d="M19 9 A5 5 0 0 1 24 8"></path>',
 "idoso": '<circle cx="20" cy="13" r="7"></circle><path d="M8 41 A12 12 0 0 1 32 41"></path><path d="M38 24 V43 M38 24 A4 4 0 0 0 34 20"></path>',
 "cuido": '<path d="M24 40 C15 33 8 27 8 19 A8 8 0 0 1 24 15 A8 8 0 0 1 40 19 C40 27 33 33 24 40 Z"></path>',
 "familia": '<circle cx="16" cy="15" r="6"></circle><circle cx="32" cy="15" r="6"></circle><path d="M5 40 A11 11 0 0 1 27 40 M21 40 A11 11 0 0 1 43 40"></path>',
 "voz": '<rect x="18" y="6" width="12" height="22" rx="6"></rect><path d="M10 22 A14 14 0 0 0 38 22 M24 36 V42"></path>',
 "texto": '<path d="M8 10 H40 A4 4 0 0 1 44 14 V30 A4 4 0 0 1 40 34 H20 L11 41 V34 H8 A4 4 0 0 1 4 30 V14 A4 4 0 0 1 8 10 Z"></path><path d="M14 22 H34"></path>',
 "os2": '<rect x="6" y="8" width="10" height="18" rx="5"></rect><path d="M2 20 A9 9 0 0 0 20 20"></path><path d="M26 12 H44 V30 H34 L28 36 V30 H26 Z"></path>',
 "onde": '<path d="M24 44 C24 44 38 30 38 19 A14 14 0 0 0 10 19 C10 30 24 44 24 44 Z"></path><path d="M19 19 A5 5 0 0 0 29 19"></path>',
 "agua": '<path d="M24 5 C24 5 37 20 37 29 A13 13 0 0 1 11 29 C11 20 24 5 24 5 Z"></path>',
 "sono": '<path d="M34 34 A16 16 0 1 1 22 8 A12 12 0 0 0 34 34 Z"></path>',
 "olhar": '<rect x="13" y="12" width="7" height="18" rx="3.5"></rect><rect x="28" y="12" width="7" height="18" rx="3.5"></rect><circle cx="24" cy="24" r="20"></circle>',
 "gravida": '<circle cx="22" cy="10" r="6"></circle><path d="M18 18 C14 24 14 30 18 34 L18 44 M28 22 A8 8 0 0 1 28 36 L24 44"></path>',
 "doenca": '<rect x="8" y="12" width="32" height="28" rx="8"></rect><path d="M17 7 V16 M31 7 V16 M16 27 H32"></path>',
 "remedio": '<rect x="10" y="18" width="28" height="14" rx="7" transform="rotate(-35 24 25)"></rect><path d="M19 32 L28 18"></path>',
 "nada": '<circle cx="24" cy="24" r="17"></circle><path d="M16 24 L22 30 L32 18"></path>',
 "depois": '<circle cx="24" cy="24" r="17"></circle><path d="M24 14 V24 L31 29"></path>',
 "sim": '<path d="M24 6 L42 38 H6 Z"></path><path d="M24 18 V28 M24 33 V34"></path>',
 "escuta": '<path d="M14 30 A14 14 0 1 1 34 30 C30 34 30 40 24 42"></path><path d="M19 22 A5 5 0 0 1 29 22"></path>',
 "fica": '<path d="M24 40 C15 33 8 27 8 19 A8 8 0 0 1 24 15 A8 8 0 0 1 40 19 C40 27 33 33 24 40 Z"></path>',
}
