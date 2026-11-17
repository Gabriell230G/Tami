"""Ícones v3 da Tami.
Regras (R-ICO): grade 48 (= 24 dp x2), área viva 40 (4 a 44), um peso só (traço 4 = 2 dp),
cantos redondos, forma cheia com UM detalhe limão no máximo. Duas variantes:
- cheio: forma na cor principal + detalhe limão (aba ativa, ícone de conteúdo);
- contorno: traço 4 na cor principal, sem limão (aba inativa).
Placeholders: {m} cor principal, {a} limão, {w} branco (furos).
"""
import re
from tokens import COR

# ícones novos ou redesenhados para a v3 (o resto vem de icons2, normalizado)
NOVOS = {
 "inicio": '<rect x="4" y="6" width="40" height="36" rx="13" fill="{m}"/><rect x="12" y="15" width="6" height="13" rx="3" fill="{w}"/><rect x="30" y="15" width="6" height="13" rx="3" fill="{w}"/><path d="M20 27 A4 4 0 0 0 28 27" fill="none" stroke="{w}" stroke-width="4" stroke-linecap="round"/><circle cx="40" cy="8" r="4" fill="{a}"/>',
 "eu": '<circle cx="24" cy="15" r="10" fill="{m}"/><path d="M19.5 15.5 A4.5 4.5 0 0 0 28.5 15.5" fill="none" stroke="{w}" stroke-width="3" stroke-linecap="round"/><path d="M8 43 C8 32 15 28 24 28 C33 28 40 32 40 43 Z" fill="{m}"/><circle cx="37" cy="8" r="4" fill="{a}"/>',
 "historico": '<path d="M24 6 A18 18 0 1 1 7.5 30" fill="none" stroke="{m}" stroke-width="5" stroke-linecap="round"/><path d="M4 16 L8 24 L15 18" fill="none" stroke="{m}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/><path d="M24 15 V25 L31 29" fill="none" stroke="{a}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>',
 "habitos": '<path d="M24 5 C24 5 38 20 38 30 A14 14 0 0 1 10 30 C10 20 24 5 24 5 Z" fill="{m}"/><path d="M18 30 A6 6 0 0 0 23 36" fill="none" stroke="{a}" stroke-width="4.5" stroke-linecap="round"/>',
 "perfil": '<circle cx="24" cy="16" r="9" fill="{m}"/><path d="M8 42 C8 31 15 27 24 27 C33 27 40 31 40 42 Z" fill="{m}"/><circle cx="36" cy="9" r="4" fill="{a}"/>',
 "pessoas": '<circle cx="17" cy="16" r="7.5" fill="{m}"/><path d="M4 41 C4 32 9 28 17 28 C25 28 30 32 30 41 Z" fill="{m}"/><circle cx="33" cy="18" r="6" fill="{a}"/><path d="M27 30 C29 29 31 29 33 29 C40 29 44 33 44 41 H33" fill="{a}"/>',
 "ouvir": '<path d="M6 19 H13 L23 10 V38 L13 29 H6 A2 2 0 0 1 4 27 V21 A2 2 0 0 1 6 19 Z" fill="{m}"/><path d="M30 17 A9 9 0 0 1 30 31" fill="none" stroke="{a}" stroke-width="4.5" stroke-linecap="round"/><path d="M35 11 A17 17 0 0 1 35 37" fill="none" stroke="{m}" stroke-width="4.5" stroke-linecap="round"/>',
 "copiar": '<rect x="16" y="14" width="26" height="30" rx="7" fill="{m}"/><path d="M10 34 H9 A3 3 0 0 1 6 31 V9 A5 5 0 0 1 11 4 H28 A3 3 0 0 1 31 7 V8" fill="none" stroke="{m}" stroke-width="4.5" stroke-linecap="round"/><rect x="22" y="23" width="14" height="4.5" rx="2.25" fill="{a}"/>',
 "nao_era": '<path d="M24 6 A18 18 0 1 0 24.1 6 Z" fill="{m}"/><path d="M17 17 L31 31 M31 17 L17 31" stroke="{w}" stroke-width="4.5" stroke-linecap="round"/>',
 "refazer": '<path d="M38 24 A14 14 0 1 1 33.9 14.1" fill="none" stroke="{m}" stroke-width="5" stroke-linecap="round"/><path d="M36 4 V15 H25" fill="none" stroke="{a}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>',
 "avisar": '<path d="M24 5 C35 5 43 12 43 22 C43 32 35 39 24 39 C21.5 39 19 38.6 16.8 37.8 L7 42 L9.5 33.5 C6.6 30.4 5 26.4 5 22 C5 12 13 5 24 5 Z" fill="none" stroke="{m}" stroke-width="4" stroke-linejoin="round"/><path d="M16 22 L22 28 L32 17" fill="none" stroke="{a}" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>',
 "atendente": '<path d="M9 26 V22 A15 15 0 0 1 39 22 V26" fill="none" stroke="{m}" stroke-width="4.5" stroke-linecap="round"/><rect x="5" y="24" width="10" height="14" rx="5" fill="{m}"/><rect x="33" y="24" width="10" height="14" rx="5" fill="{m}"/><path d="M38 38 C38 42 33 44 27 44" fill="none" stroke="{a}" stroke-width="4.5" stroke-linecap="round"/>',
 "fechar": '<path d="M12 12 L36 36 M36 12 L12 36" stroke="{m}" stroke-width="5" stroke-linecap="round"/>',
 "voltar": '<path d="M29 10 L15 24 L29 38" fill="none" stroke="{m}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>',
 "check": '<path d="M24 6 A18 18 0 1 0 24.1 6 Z" fill="{m}"/><path d="M15 24 L21 30 L33 18" fill="none" stroke="{w}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="38" cy="9" r="4" fill="{a}"/>',
 "alerta": '<path d="M24 5 C26 5 27.5 6 28.5 8 L43 35 C45 39 42.6 43 38 43 H10 C5.4 43 3 39 5 35 L19.5 8 C20.5 6 22 5 24 5 Z" fill="{m}"/><rect x="21.5" y="16" width="5" height="14" rx="2.5" fill="{w}"/><circle cx="24" cy="36" r="3" fill="{w}"/>',
 "mic": '<rect x="16" y="4" width="16" height="26" rx="8" fill="{m}"/><path d="M9 22 A15 15 0 0 0 39 22" fill="none" stroke="{m}" stroke-width="4.5" stroke-linecap="round"/><path d="M24 37 V44" stroke="{a}" stroke-width="4.5" stroke-linecap="round"/>',
 "teclado": '<rect x="4" y="11" width="40" height="27" rx="7" fill="{m}"/><g fill="{w}"><rect x="10" y="17" width="5" height="5" rx="1.5"/><rect x="18" y="17" width="5" height="5" rx="1.5"/><rect x="26" y="17" width="5" height="5" rx="1.5"/><rect x="34" y="17" width="4" height="5" rx="1.5"/></g><rect x="14" y="28" width="20" height="4.5" rx="2.25" fill="{a}"/>',
 "lupa": '<circle cx="21" cy="21" r="13" fill="none" stroke="{m}" stroke-width="5"/><path d="M31 31 L41 41" stroke="{a}" stroke-width="6" stroke-linecap="round"/>',
 "relogio": '<circle cx="24" cy="24" r="19" fill="{m}"/><path d="M24 13 V25 L32 30" fill="none" stroke="{w}" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="25" r="3" fill="{a}"/>',
 "calendario": '<rect x="5" y="9" width="38" height="34" rx="9" fill="{m}"/><rect x="14" y="4" width="5" height="11" rx="2.5" fill="{m}"/><rect x="29" y="4" width="5" height="11" rx="2.5" fill="{m}"/><rect x="11" y="19" width="26" height="4" rx="2" fill="{w}" opacity="0.5"/><rect x="27" y="28" width="9" height="9" rx="3" fill="{a}"/>',
 "ambulancia": '<path d="M4 16 A4 4 0 0 1 8 12 H28 A4 4 0 0 1 32 16 V18 H37 L44 26 V34 A3 3 0 0 1 41 37 H7 A3 3 0 0 1 4 34 Z" fill="{m}"/><circle cx="13" cy="38" r="5" fill="{m}" stroke="{w}" stroke-width="2.5"/><circle cx="35" cy="38" r="5" fill="{m}" stroke="{w}" stroke-width="2.5"/><rect x="14" y="17" width="4" height="12" rx="2" fill="{a}"/><rect x="10" y="21" width="12" height="4" rx="2" fill="{a}"/>',
 "posto": '<path d="M5 20 L24 8 L43 20 V40 A3 3 0 0 1 40 43 H8 A3 3 0 0 1 5 40 Z" fill="{m}"/><rect x="18" y="27" width="12" height="16" rx="3" fill="{w}" opacity="0.35"/><circle cx="24" cy="19" r="4" fill="{a}"/>',
 "folha": '<path d="M40 7 C40 7 10 6 8 30 C7 39 13 42 18 41 C36 38 41 20 40 7 Z" fill="{m}"/><path d="M8 42 C14 32 21 25 30 18" fill="none" stroke="{a}" stroke-width="4" stroke-linecap="round"/>',
}


def _norm(corpo):
    corpo = re.sub(r'stroke-width="[0-9.]+"', lambda m: 'stroke-width="4.5"' if float(m.group(0)[14:-1]) >= 3 else m.group(0), corpo)
    return corpo


def _carregar():
    from icons2 import IC2
    d = {k: _norm(v) for k, v in IC2.items()}
    d.update(NOVOS)
    # apelidos que o app usa
    d.setdefault("cal", NOVOS["calendario"]); d["cal"] = NOVOS["calendario"]
    d["depois"] = NOVOS["relogio"]
    d["nada"] = NOVOS["check"]
    d["fone"] = d.get("fone", NOVOS["atendente"])
    return d


IC3 = _carregar()


def _fechar_tags(s):
    return re.sub(r'<(\w+)([^<>]*?)/>', r'<\1\2></\1>', s)


def ic3(nome, w=24, m=None, a=None, wcor=None, variante="cheio", rotulo=None):
    """Retorna o SVG do ícone. variante: 'cheio' ou 'contorno'."""
    m = m or COR["primary"]; a = a or COR["accent"]; wcor = wcor or COR["bg"]
    corpo = IC3[nome]
    if variante == "contorno":
        # tudo vira traço na cor principal; furos e limão somem
        corpo = re.sub(r'fill="\{a\}"', 'fill="none" stroke="{m}" stroke-width="4"', corpo)
        corpo = re.sub(r'fill="\{m\}"', 'fill="none" stroke="{m}" stroke-width="4" stroke-linejoin="round"', corpo)
        corpo = corpo.replace('stroke="{a}"', 'stroke="{m}"')
        corpo = re.sub(r'<[^<>]*fill="\{w\}"[^<>]*/>', '', corpo)
        corpo = re.sub(r'<path[^<>]*stroke="\{w\}"[^<>]*/>', '', corpo)
        corpo = re.sub(r'<g fill="\{w\}">.*?</g>', '', corpo)
    corpo = corpo.replace("{m}", m).replace("{a}", a).replace("{w}", wcor)
    aria = f' role="img" aria-label="{rotulo}"' if rotulo else ' aria-hidden="true"'
    return f'<svg width="{w}" height="{w}" viewBox="0 0 48 48"{aria}>{_fechar_tags(corpo)}</svg>'


if __name__ == "__main__":
    nomes = sorted(IC3)
    linhas = []
    for n in nomes:
        linhas.append(f'<div style="display:inline-flex;flex-direction:column;align-items:center;width:120px;margin:6px;font:600 12px Nunito">'
                      f'<div style="display:flex;gap:8px;align-items:end">{ic3(n, 32)}{ic3(n, 24)}{ic3(n, 24, m="#000", a="#000", variante="cheio")}{ic3(n, 24, m=COR["muted"], variante="contorno")}</div>{n}</div>')
    open("/tmp/icones3.html", "w").write('<html><body style="background:#fff">' + "".join(linhas) + "</body></html>")
    print(len(nomes))
