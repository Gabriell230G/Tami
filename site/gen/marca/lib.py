"""Peças comuns das pranchas do case da Tami (gera .dc.html literais)."""
AZUL = "#3F4ACB"; LIMAO = "#B6EC0C"; MARINHO = "#1B1F4A"; NEVOA = "#EEF0FF"; BRANCO = "#FFFFFF"
VERMELHO = "#E5484D"; VERMELHO_BOTAO = "#D63B40"; CINZA = "#454A78"; LINHA = "#D5D9F5"
FONTES = '<link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&amp;family=Nunito:wght@400;600;700;800&amp;display=swap" rel="stylesheet">'
FRED = "font-family: Fredoka, Nunito, sans-serif"


def face(cor, w, label=None, extra=""):
    h = round(w / 2)
    a = f'role="img" aria-label="{label}"' if label else 'aria-hidden="true"'
    return (f'<svg width="{w}" height="{h}" viewBox="0 0 320 160" {a}{(" " + extra) if extra else ""}>'
            f'<rect x="8" y="20" width="48" height="112" rx="24" fill="{cor}"></rect>'
            f'<rect x="264" y="20" width="48" height="112" rx="24" fill="{cor}"></rect>'
            f'<path d="M123 82 A37 37 0 0 0 197 82" fill="none" stroke="{cor}" stroke-width="36" stroke-linecap="round"></path></svg>')


def olhos(cor, w):
    """Versão compacta: só os dois olhos."""
    h = round(w * 112 / 200)
    return (f'<svg width="{w}" height="{h}" viewBox="0 0 200 112" aria-hidden="true">'
            f'<rect x="0" y="0" width="48" height="112" rx="24" fill="{cor}"></rect>'
            f'<rect x="152" y="0" width="48" height="112" rx="24" fill="{cor}"></rect></svg>')


def palavra(cor, px, extra=""):
    return f'<div style="{FRED}; font-weight: 700; font-size: {px}px; line-height: 0.9; letter-spacing: -1px; color: {cor}{extra}">tami</div>'


def icone(w, fundo=AZUL, cor=BRANCO, raio=0.3):
    return (f'<div style="width: {w}px; height: {w}px; border-radius: {round(w*raio)}px; background: {fundo}; '
            f'display: flex; align-items: center; justify-content: center; flex-shrink: 0">{face(cor, round(w*0.68))}</div>')


def cabecalho(num, titulo, sub, escuro=False):
    cor = BRANCO if escuro else MARINHO
    cs = "rgba(255,255,255,0.8)" if escuro else CINZA
    return (f'<div style="display: flex; justify-content: space-between; align-items: flex-end; gap: 40px">'
            f'<div style="display: flex; flex-direction: column; gap: 14px">'
            f'<div style="display: flex; align-items: center; gap: 14px">'
            f'<div style="height: 36px; padding: 0 16px; border-radius: 18px; background: {LIMAO}; display: flex; align-items: center; {FRED}; font-weight: 600; font-size: 18px; color: {MARINHO}">{num}</div>'
            f'<div style="font-weight: 800; font-size: 15px; letter-spacing: 2px; color: {cor}">{titulo.upper()}</div></div>'
            f'<div style="font-size: 18px; line-height: 1.55; color: {cs}; max-width: 860px">{sub}</div></div>'
            f'{face(cor if not escuro else BRANCO, 96)}</div>')


def rotulo(txt, cor=AZUL):
    return f'<div style="font-weight: 800; font-size: 15px; letter-spacing: 2px; color: {cor}">{txt.upper()}</div>'


def pagina(titulo, w, h, corpo, fundo=NEVOA, props=None):
    import json
    p = {"$preview": {"width": w, "height": h}}
    if props:
        p.update(props)
    dp = json.dumps(p, ensure_ascii=False).replace("'", "&#39;")
    return f'''<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<title>{titulo}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
{FONTES}
<style>
body{{margin:0}}
a{{color:#3F4ACB}}a:hover{{color:#1B1F4A}}
</style>
</helmet>
<div style="width: {w}px; height: {h}px; box-sizing: border-box; background: {fundo}; padding: 64px 72px; display: flex; flex-direction: column; gap: 40px; font-family: Nunito, system-ui, sans-serif; color: {MARINHO}; position: relative; overflow: hidden">
{corpo}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{dp}'>
class Component extends DCLogic {{
renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''


def cartao(conteudo, fundo=BRANCO, extra=""):
    return f'<div style="border-radius: 28px; background: {fundo}; padding: 28px; display: flex; flex-direction: column; gap: 16px{extra}">{conteudo}</div>'


def legenda(txt, cor=CINZA):
    return f'<div style="font-size: 15px; line-height: 1.5; color: {cor}">{txt}</div>'
