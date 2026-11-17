"""corpo_real.py · corpo realista e elegante para o "Onde dói?" (proposta v7).
- Proporção humana real (~7,5 cabeças): queixo, mamilos, umbigo e virilha em marcas de 1 cabeça.
- Partes separadas (tronco+pernas, braços com mão de 5 dedos, cabeça com orelha, cabelo) unidas pelo
  truque "traço grosso por baixo, preenchimento por cima": só sobra a linha de fora.
- Sem rosto (como os mapas de corpo dos apps de saúde sérios); cabelo em tom suave.
- A dor aparece como luz azul suave no formato da parte, com um ponto marcador.
Coordenadas: x a partir do centro, y de cima para baixo, corpo com ~600 de altura.
"""
import math
import sys
sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)))
from tokens import COR

CX = 140


def curva(pts, fechado=True):
    n = len(pts); P = [(p[0], p[1]) for p in pts]; C = [len(p) > 2 and p[2] for p in pts]
    tin, tout = [], []
    for i in range(n):
        a = P[i - 1] if (fechado or i > 0) else P[i]; c = P[(i + 1) % n] if (fechado or i < n - 1) else P[i]; p = P[i]
        if C[i]:
            tin.append(((p[0] - a[0]) * .5, (p[1] - a[1]) * .5)); tout.append(((c[0] - p[0]) * .5, (c[1] - p[1]) * .5))
        else:
            t = ((c[0] - a[0]) / 2, (c[1] - a[1]) / 2); tin.append(t); tout.append(t)
    f = lambda v: f"{v:.1f}"
    o = [f"M{f(P[0][0])} {f(P[0][1])}"]
    for i in range(n if fechado else n - 1):
        j = (i + 1) % n
        o.append(f"C{f(P[i][0] + tout[i][0] / 3)} {f(P[i][1] + tout[i][1] / 3)} {f(P[j][0] - tin[j][0] / 3)} {f(P[j][1] - tin[j][1] / 3)} {f(P[j][0])} {f(P[j][1])}")
    return "".join(o) + ("Z" if fechado else "")


def ab(pts):                       # do centro ➜ absoluto
    return [(CX + p[0], p[1]) + tuple(p[2:]) for p in pts]


_ab = None


def espelho(meia):                 # meia silhueta (lado direito) ➜ inteira
    return meia + [(2 * CX - p[0], p[1]) + tuple(p[2:]) for p in reversed(meia[:-1])]


_ab = ab


def lado(pts, s):                  # s = 1 direita da tela, -1 esquerda
    return [(CX + s * (p[0] - CX), p[1]) + tuple(p[2:]) for p in pts]


# ------------------------------------------------------------------ braço com mão
def braco(piv, ang, esc_x=1.0, esc_y=1.0):
    """Braço no próprio eixo (y para baixo, x para fora) girado no ombro. Mão de frente, polegar para fora."""
    fora = [(-4, -15), (6, -14), (13, -8), (16.5, 3), (17, 20), (16, 45), (14.8, 72), (13, 98), (12, 114), (13, 130),
            (13.6, 148), (11.8, 172), (9.4, 194), (8.6, 205)]
    # mão relaxada, dedos juntos (como na prancha de referência); a divisão dos dedos vira linha leve
    mao = [(12, 213), (16.5, 222), (19.5, 232), (20.5, 241), (19, 246.5), (15.5, 247), (13, 241), (11.2, 233, True),
           (12, 243), (12, 255), (10.8, 266), (8.6, 273), (4.5, 276.5), (0, 277.5), (-4.5, 276.5), (-9, 274), (-13.5, 269.5),
           (-16.6, 262), (-17.4, 252), (-16.8, 242), (-15.6, 230), (-12.8, 216)]
    dentro = [(-8.6, 205), (-9.6, 190), (-12, 165), (-13.6, 140), (-12.2, 118), (-13, 98), (-14.6, 72), (-15.2, 46),
              (-14.4, 24), (-12, 6), (-9, -8)]
    pts = fora + mao + dentro
    c, s = math.cos(ang), math.sin(ang)
    out = []
    for p in pts:
        x, y = p[0] * esc_x, p[1] * esc_y
        out.append((piv[0] + x * c + y * s, piv[1] - x * s + y * c) + tuple(p[2:]))
    meio = lambda y: (piv[0] + y * esc_y * s, piv[1] + y * esc_y * c)
    T_ = lambda x, y: (piv[0] + x * esc_x * c + y * esc_y * s, piv[1] - x * esc_x * s + y * esc_y * c)
    dedos = [[T_(4.2, 252), T_(3.8, 272)], [T_(-3.6, 253), T_(-3.8, 274)], [T_(-10.6, 252), T_(-10.8, 268)]]
    return out, meio, dedos


CORPOS = {
    # medidas tiradas das pranchas de referência (convertidas para cabeça = 82)
    "mulher": dict(
        cab=dict(m=25.5, ear=False),
        meia=[(14, 76, True), (15.5, 84), (16, 90), (22, 96), (34, 101), (48, 107), (60, 112), (66, 120), (68, 132), (64, 150),
              (55, 166), (45, 180, True), (44, 190), (41, 200), (36, 212), (33.5, 222), (34, 232), (38, 244), (44, 256), (50, 268),
              (54, 280), (56.5, 292), (56.5, 304), (55.5, 318), (53.5, 336), (51, 354), (48.5, 373), (46, 391), (43, 410),
              (41.5, 429), (43.5, 448), (46, 466), (46, 485), (44, 503), (41.5, 522), (39, 541), (38, 560), (38, 577),
              (42, 594), (48.5, 612), (54, 626), (54.5, 634), (50, 638.5), (27, 639), (17, 637), (16, 628), (16.5, 613), (17, 596),
              (18, 578), (18, 560), (17, 541), (14.5, 522), (11, 503), (9, 485), (9.5, 466), (12, 448), (13, 429), (11, 410),
              (8.6, 391), (7, 373), (5.4, 354), (4.4, 336), (3, 324), (0, 318)],
        piv=(55, 138), ang=4, esc=(0.86, 0.79),
        peito=(0, 196, 34, 18), barriga=(0, 238, 26, 20), pelve=(0, 286, 40, 16), virilha=318, ombro_y=104, joelho=429, alt=642),
    "homem": dict(
        cab=dict(m=27, ear=True),
        meia=[(16, 76, True), (18.6, 84), (19, 90), (24, 95), (36, 101), (52, 108), (66, 113), (74, 120), (77, 132), (73, 150),
              (62, 166), (50, 180, True), (47.5, 188), (44.7, 197), (43, 206), (41, 215), (41, 225), (42, 234), (44.7, 243),
              (46.6, 252), (48.5, 261), (50, 270), (51.7, 280), (54, 292), (54.5, 304), (54, 318), (54, 336), (53, 354), (51, 373),
              (49, 391), (46.6, 410), (45.7, 429), (48, 448), (51, 466), (51, 485), (49, 503), (46.6, 522), (43.8, 541), (42.9, 560),
              (43, 578), (47.5, 595), (54.5, 613), (60.5, 628), (61, 637), (56, 641.5), (30, 642), (19.5, 640), (18, 630),
              (18.6, 615), (18.8, 597), (19.6, 578), (19.6, 560), (18.6, 541), (15.8, 522), (12, 503), (9.8, 485), (10, 466),
              (13, 448), (14, 429), (12, 410), (9.3, 391), (7.5, 373), (5.6, 354), (4.7, 336), (3.4, 322), (0, 316)],
        piv=(66, 138), ang=3, esc=(1.0, 0.8),
        peito=(0, 196, 40, 20), barriga=(0, 240, 30, 20), pelve=(0, 288, 40, 16), virilha=316, ombro_y=102, joelho=429, alt=645),
    "crianca": dict(
        cab=dict(m=33, ear=True),
        meia=[(11, 74, True), (12.5, 80), (13, 86), (20, 92), (30, 97), (40, 101), (46, 108), (48, 118), (46, 130), (40, 142),
              (32, 155, True), (31.5, 162), (32.5, 176), (34, 189), (36.5, 202), (39, 213), (41.5, 224), (42, 233), (41.5, 242),
              (40.7, 253), (38.5, 268), (37, 283), (36, 299), (36.6, 315), (35.4, 331), (33, 346), (31, 361), (33, 372),
              (38.5, 382), (42.5, 390), (42, 396), (26, 397.5), (10.5, 397), (8.8, 389), (10.4, 376), (11.8, 363), (13, 347),
              (9.4, 331), (7.8, 315), (7, 299), (5, 283), (3.4, 268), (1.8, 252), (0, 245)],
        piv=(45, 112), ang=9, esc=(0.76, 0.55),
        peito=(0, 168, 28, 15), barriga=(0, 210, 30, 18), pelve=(0, 238, 32, 10), virilha=245, ombro_y=98, joelho=299, alt=400),
}


def cabeca(tipo):
    m = CORPOS[tipo]["cab"]["m"]; e = CORPOS[tipo]["cab"]["ear"]
    k = m / 28
    meia = [(0, 0), (19 * k, 3.5), (27 * k, 16), (m, 34)]
    meia += ([(m + 2.4, 36), (m + 4.4, 41), (m + 4.2, 50), (m + 1.6, 57)] if e else [(m - 0.2, 46)])
    meia += [(m - 2.5, 61), (m - 7, 70), (m - 14, 77.5), (8, 81), (0, 82)]
    return espelho(ab(meia))


def cabelo(tipo, vista):
    """Estilo manequim (como a prancha de referência): sem cabelo. A mulher ganha só um coque no alto,
    que diferencia sem desenhar rosto nem penteado. Devolve (forma de trás, forma da frente, rosto)."""
    if tipo == "mulher":
        coque = curva([(CX, -22), (CX + 11, -17), (CX + 14, -7), (CX + 10, 3), (CX, 6), (CX - 10, 3), (CX - 14, -7), (CX - 11, -17)])
        return coque, None, None
    return None, None, None


def corpo_svg(tipo="mulher", vista="frente", sel=(), uid="r", alto="560px", links=None):
    c = CORPOS[tipo]; alt = c["alt"]
    lin = COR["primary"]; pele = COR["bg"]; tom = COR["surface-alt"]; guia = COR["surface-strong"]
    tronco = curva(espelho(ab(c["meia"])))
    bd, meio_d, dedos_d = braco((CX + c["piv"][0], c["piv"][1]), math.radians(c["ang"]), *c["esc"])
    be = lado(bd, -1)
    bd_, be_ = curva(bd), curva(be)
    cab = curva(cabeca(tipo))
    c_tras, c_frente, _rosto = cabelo(tipo, vista)
    pecas = [p for p in (c_tras, tronco, bd_, be_, cab, c_frente) if p]
    U = "".join(f'<path d="{d}"></path>' for d in pecas)
    sw = 1.6
    s = [f'<defs><clipPath id="{uid}-u">{U}</clipPath>'
         f'<filter id="{uid}-s" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="3"></feGaussianBlur></filter>'
         f'<clipPath id="{uid}-cab"><path d="{cab}"></path></clipPath>'
         f'<clipPath id="{uid}-cp"><path d="{tronco}"></path><path d="{bd_}"></path><path d="{be_}"></path></clipPath>'
         f'<filter id="{uid}-g" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="7"></feGaussianBlur></filter>'
         f'<radialGradient id="{uid}-dor"><stop offset="0%" stop-color="{COR["primary"]}" stop-opacity="0.75"></stop>'
         f'<stop offset="60%" stop-color="{COR["primary-glow"]}" stop-opacity="0.35"></stop><stop offset="100%" stop-color="{COR["primary-glow"]}" stop-opacity="0"></stop></radialGradient>'
         f'</defs>',
         f'<ellipse cx="{CX}" cy="{alt - 2}" rx="70" ry="7" fill="{COR["surface-alt"]}"></ellipse>',
         ]
    # corpo e braços num contorno só (ombro e axila sem emenda); coque e cabeça por cima, cada um com o seu contorno.
    # O traço fica por baixo do preenchimento (só a metade de fora aparece), então a espessura é igual em todo lugar
    # e o queixo e o braço passam por cima com uma linha limpa, sem emenda.
    parte = lambda d: (f'<path d="{d}" fill="{pele}" stroke="{lin}" stroke-width="{2 * sw}" stroke-linejoin="round" '
                       f'stroke-linecap="round" paint-order="stroke"></path>')
    corpo_u = "".join(f'<path d="{d}"></path>' for d in (tronco, bd_, be_))
    s.append(f'<g fill="{lin}" stroke="{lin}" stroke-width="{2 * sw}" stroke-linejoin="round">{corpo_u}</g><g fill="{pele}">{corpo_u}</g>')
    for d in (c_tras, cab):
        if d: s.append(parte(d))
    # linhas de construção bem leves (como prancha de estudo de anatomia)
    oy = c["ombro_y"]; vir = c["virilha"]; L = []; meia = c["meia"]
    def x_em(y, fora=True):                     # contorno do tronco/perna numa altura
        cand = [p for p in meia if abs(p[1] - y) < 12]
        return max(p[0] for p in cand) if fora else min(p[0] for p in cand)
    kj = c["joelho"]; kfo, kdi = x_em(kj), min(p[0] for p in meia if abs(p[1] - kj) < 12 and p[0] < 25 and p[1] > vir)
    kc = (kfo + kdi) / 2; kr = (kfo - kdi) / 2 * 0.62
    em = meio_d(112); ex, ey = em; er = 9 * c["esc"][0]
    if vista == "frente":
        L += [f'M{CX} 2 L{CX} 80', f'M{CX - c["cab"]["m"] + 1} 40 Q{CX} 46 {CX + c["cab"]["m"] - 1} 40']
        L += [f'M{CX - 9} 86 L{CX - 3} {oy - 2}', f'M{CX + 9} 86 L{CX + 3} {oy - 2}',
              f'M{CX - 3} {oy - 2} Q{CX - 30} {oy - 4} {CX - 56} {oy + 6}', f'M{CX + 3} {oy - 2} Q{CX + 30} {oy - 4} {CX + 56} {oy + 6}',
              f'M{CX} {oy} L{CX} {vir - 4}']
        ax_ = [p for p in meia if len(p) > 2 and p[2] is True][1]; px0 = c["piv"][0] - 12 * c["esc"][0]
        for sx in (-1, 1):   # deltoide: onde o braço começa, em linha leve
            L.append(f'M{CX + sx * (px0 - 4):.1f} {oy + 12} Q{CX + sx * (ax_[0] + 2):.1f} {(oy + ax_[1]) / 2 + 6:.1f} {CX + sx * ax_[0]:.1f} {ax_[1] - 2}')
        py = c["peito"][1]; pr = c["peito"][2]
        if tipo == "mulher":
            L += [f'M{CX - pr - 6} {py - 8} Q{CX - pr * 0.5} {py + 18} {CX - 4} {py}', f'M{CX + pr + 6} {py - 8} Q{CX + pr * 0.5} {py + 18} {CX + 4} {py}']
        elif tipo == "homem":
            L += [f'M{CX - pr - 2} {py - 14} Q{CX - pr * 0.6} {py + 8} {CX} {py + 2} Q{CX + pr * 0.6} {py + 8} {CX + pr + 2} {py - 14}']
        bw = x_em(c["barriga"][1] + 26)
        if tipo != "crianca": L += [f'M{CX - bw + 2} {c["barriga"][1] + 26} Q{CX} {c["barriga"][1] + 32} {CX + bw - 2} {c["barriga"][1] + 26}',
              f'M{CX - bw + 6} {c["barriga"][1] + 34} L{CX - 6} {vir - 2} M{CX + bw - 6} {c["barriga"][1] + 34} L{CX + 6} {vir - 2}']
        s.append(f'<g fill="none" stroke="{guia}" stroke-width="1.4"><ellipse cx="{CX - kc:.1f}" cy="{kj}" rx="{kr:.1f}" ry="{kr * 1.25:.1f}"></ellipse>'
                 f'<ellipse cx="{CX + kc:.1f}" cy="{kj}" rx="{kr:.1f}" ry="{kr * 1.25:.1f}"></ellipse>'
                 f'<ellipse cx="{ex:.1f}" cy="{ey:.1f}" rx="{er:.1f}" ry="{er * 1.2:.1f}"></ellipse><ellipse cx="{2 * CX - ex:.1f}" cy="{ey:.1f}" rx="{er:.1f}" ry="{er * 1.2:.1f}"></ellipse></g>')
    else:
        L += [f'M{CX} 2 L{CX} 72', f'M{CX} {oy - 18} L{CX} {vir - 30}']
        for sx in (-1, 1):
            L.append(f'M{CX + sx * 12} {oy + 14} Q{CX + sx * 40} {oy + 10} {CX + sx * 38} {oy + 48}')
            L.append(f'M{CX} {vir - 30} Q{CX + sx * 10} {vir + 4} {CX + sx * x_em(vir - 12) * 0.92:.1f} {vir - 6}')
            L.append(f'M{CX + sx * (kc - kr)} {kj + 6} q{sx * kr} 4 {sx * 2 * kr} 0')
    for dd in dedos_d:
        for sx in (1, -1):
            (x1, y1), (x2, y2) = dd
            if sx < 0: x1, x2 = 2 * CX - x1, 2 * CX - x2
            L.append(f'M{x1:.1f} {y1:.1f} L{x2:.1f} {y2:.1f}')
    s.append(f'<g clip-path="url(#{uid}-u)" fill="none" stroke="{guia}" stroke-width="1.4" stroke-linecap="round">' + "".join(f'<path d="{d}"></path>' for d in L) + '</g>')
    # a dor: luz azul no formato da parte + ponto marcador
    zonas = {"peito": c["peito"], "barriga": c["barriga"], "pelve": c["pelve"]}
    for k in sel:
        if k in zonas:
            x0, y0, rx, ry = zonas[k]; px, py = CX + x0, y0
            luz = f'<ellipse cx="{px}" cy="{py}" rx="{rx * 1.15:.1f}" ry="{ry * 1.25:.1f}" fill="url(#{uid}-dor)"></ellipse>'
        elif k in ("braco_e", "braco_d"):
            d = be_ if k == "braco_e" else bd_; m = meio_d(110); px = CX - (m[0] - CX) if k == "braco_e" else m[0]; py = m[1]
            luz = f'<path d="{d}" fill="{COR["primary-glow"]}" opacity="0.38"></path>'
        elif k in ("joelho_e", "joelho_d"):
            sx = -1 if k == "joelho_e" else 1; px, py = CX + sx * (c["pelve"][2] * 0.5), c["joelho"]
            luz = f'<ellipse cx="{px:.1f}" cy="{py:.1f}" rx="22" ry="22" fill="url(#{uid}-dor)"></ellipse>'
        elif k in ("ombro_e", "ombro_d"):
            sx = -1 if k == "ombro_e" else 1; px, py = CX + sx * (c["peito"][2] + 26), c["ombro_y"] + 16
            luz = f'<ellipse cx="{px:.1f}" cy="{py:.1f}" rx="26" ry="22" fill="url(#{uid}-dor)"></ellipse>'
        elif k == "cabeca":
            px, py = CX, 44; luz = f'<path d="{cab}" fill="{COR["primary-glow"]}" opacity="0.38"></path>'
        elif k == "pescoco":
            px, py = CX, (84 + c["ombro_y"]) / 2; luz = f'<ellipse cx="{CX}" cy="{py}" rx="22" ry="16" fill="url(#{uid}-dor)"></ellipse>'
        elif k in ("perna_e", "perna_d"):
            sx = -1 if k == "perna_e" else 1; vir = c["virilha"]; py = (vir + alt) / 2 - 20; px = CX + sx * (c["pelve"][2] * 0.5)
            x0 = CX - 200 if sx < 0 else CX
            luz = (f'<clipPath id="{uid}-pl{k}"><rect x="{x0}" y="{vir}" width="200" height="{alt}"></rect></clipPath>'
                   f'<path d="{tronco}" fill="{COR["primary-glow"]}" opacity="0.38" clip-path="url(#{uid}-pl{k})"></path>')
        else:
            continue
        s.append(f'<g clip-path="url(#{uid}-u)">{luz}</g>')
        s.append(f'<circle cx="{px:.1f}" cy="{py:.1f}" r="13" fill="{COR["primary"]}" opacity="0.18"></circle>'
                 f'<circle cx="{px:.1f}" cy="{py:.1f}" r="7" fill="#FFFFFF" stroke="{COR["primary"]}" stroke-width="2.4"></circle>'
                 f'<circle cx="{px:.1f}" cy="{py:.1f}" r="3" fill="{COR["primary"]}"></circle>')
    # áreas de toque (invisíveis, bem maiores que 48 px)
    if links is not None:
        nomes = NOMES[vista]; tw = c["peito"][2] + 8; oy = c["ombro_y"]
        pb = (c["peito"][1] + c["barriga"][1]) / 2; bp = (c["barriga"][1] + c["pelve"][1]) / 2; vir = c["virilha"]
        mao_y = c["piv"][1] + 280 * c["esc"][1]
        T = {"braco_e": (0, oy + 40, CX - tw, mao_y - oy - 30), "braco_d": (CX + tw, oy + 40, 280 - CX - tw, mao_y - oy - 30),
             "ombro_e": (CX - tw - 48, oy - 12, 60, 52), "ombro_d": (CX + tw - 12, oy - 12, 60, 52),
             "joelho_e": (CX - c["pelve"][2] * 0.5 - 34, c["joelho"] - 34, 68, 68), "joelho_d": (CX + c["pelve"][2] * 0.5 - 34, c["joelho"] - 34, 68, 68),
             "cabeca": (CX - 48, -30, 96, 100), "pescoco": (CX - 40, 70, 80, oy - 66),
             "peito": (CX - tw, oy + 4, 2 * tw, pb - oy - 4), "barriga": (CX - tw, pb, 2 * tw, bp - pb), "pelve": (CX - tw, bp, 2 * tw, vir + 14 - bp),
             "perna_e": (CX - 80, vir + 14, 80, alt - vir), "perna_d": (CX, vir + 14, 80, alt - vir)}
        for k in ORDEM_ZONAS:
            x0, y0, ww, hh = T[k]; rot = nomes[k] + (" (marcado)" if k in sel else "")
            alvo = f'<rect x="{x0:.0f}" y="{y0:.0f}" width="{ww:.0f}" height="{hh:.0f}" fill="transparent"></rect>'
            href = links.get(k, links.get("*")) if isinstance(links, dict) else None
            if links == "interativo":
                s.append(f'<g class="zona" data-zona="{k}" role="button" tabindex="0" aria-pressed="false" aria-label="{nomes[k]}">{alvo}</g>')
            else:
                s.append(f'<a href="{href}" aria-label="{rot}">{alvo}</a>' if href else f'<g role="img" aria-label="{rot}">{alvo}</g>')
    rot_tipo = {"mulher": "mulher", "homem": "homem", "crianca": "criança"}[tipo]
    tam = f"height: {alto}; width: auto" if alto else "height: 100%; width: 100%"
    return (f'<svg viewBox="0 -30 280 {alt + 42}" role="group" style="display: block; {tam}" '
            f'aria-label="Corpo de {rot_tipo}, de {vista}. Toque onde dói.">' + "".join(s) + '</svg>')


NOMES = {
    "frente": {"cabeca": "Cabeça", "pescoco": "Pescoço", "peito": "Peito", "barriga": "Barriga", "pelve": "Pelve",
               "braco_e": "Braço direito", "braco_d": "Braço esquerdo", "perna_e": "Perna direita", "perna_d": "Perna esquerda",
               "ombro_e": "Ombro direito", "ombro_d": "Ombro esquerdo", "joelho_e": "Joelho direito", "joelho_d": "Joelho esquerdo"},
    "costas": {"cabeca": "Nuca", "pescoco": "Pescoço", "peito": "Costas, em cima", "barriga": "Costas, embaixo", "pelve": "Bumbum",
               "braco_e": "Braço esquerdo", "braco_d": "Braço direito", "perna_e": "Perna esquerda", "perna_d": "Perna direita",
               "ombro_e": "Ombro esquerdo", "ombro_d": "Ombro direito", "joelho_e": "Joelho esquerdo", "joelho_d": "Joelho direito"},
}
# a ordem importa: quem vem depois fica por cima (o ombro ganha do peito e do braço)
ORDEM_ZONAS = ("braco_e", "braco_d", "perna_e", "perna_d", "cabeca", "pescoco", "peito", "barriga", "pelve", "ombro_e", "ombro_d", "joelho_e", "joelho_d")
# "dor no/na ..." para o balão do site
DOR_EM = {"cabeca": "Dor na cabeça", "pescoco": "Dor no pescoço", "peito": "Dor no peito", "barriga": "Dor na barriga", "pelve": "Dor no pé da barriga",
          "braco_e": "Dor no braço direito", "braco_d": "Dor no braço esquerdo", "perna_e": "Dor na perna direita", "perna_d": "Dor na perna esquerda",
          "ombro_e": "Dor no ombro direito", "ombro_d": "Dor no ombro esquerdo", "joelho_e": "Dor no joelho direito", "joelho_d": "Dor no joelho esquerdo"}


def corpo_interativo(tipo="mulher", vista="frente", uid="ci", marcado="barriga", lado_tela=True):
    """Corpo com áreas de toque (botões) e uma marca de dor pronta para cada parte; o site mostra só a escolhida."""
    import re as _re
    base = corpo_svg(tipo, vista, (), uid, None, "interativo")
    marcas = []
    for k in ORDEM_ZONAS:
        v = corpo_svg(tipo, vista, (k,), uid, None, None)
        m = _re.search(r'(<g clip-path="url\(#' + uid + r'-u\)">.*?</g>)(<circle cx="([\d.]+)" cy="([\d.]+)".*?</circle><circle.*?</circle><circle.*?</circle>)', v)
        if not m:
            continue
        marcas.append(f'<g class="dor" data-dor="{k}" data-x="{m.group(3)}" data-y="{m.group(4)}" data-texto="{DOR_EM[k]}"'
                      f'{"" if k == marcado else ' style="display: none"'}>{m.group(1)}{m.group(2)}</g>')
    i = base.index('<g class="zona"')
    out = base[:i] + "".join(marcas) + base[i:]
    if lado_tela:  # no site, o lado é o de quem olha a tela (pedido do Gabriel): o ombro à esquerda da tela é o "esquerdo"
        troca = {"direito": "esquerdo", "esquerdo": "direito", "direita": "esquerda", "esquerda": "direita"}
        out = _re.sub(r'(data-texto|aria-label)="([^"]*)"',
                      lambda m: m.group(1) + '="' + _re.sub(r"direit[oa]|esquerd[oa]", lambda w: troca[w.group(0)], m.group(2)) + '"', out)
    return out


def corpo(vista="frente", tipo="mulher", sel=(), links=None, w=240, uid="c", alto=None):
    """API usada pelas telas (igual à das versões anteriores)."""
    if tipo == "adulto": tipo = "mulher"
    return corpo_svg(tipo, vista, sel, uid, alto, links or {})


if __name__ == "__main__":
    figs = [("mulher", "frente", {"barriga"}), ("homem", "frente", {"peito", "braco_e"}), ("crianca", "frente", {"barriga"}),
            ("mulher", "costas", ()), ("homem", "costas", ()), ("crianca", "costas", ())]
    html = "".join(f'<div style="background:#FFFFFF;border-radius:24px;padding:16px 4px;box-shadow:inset 0 0 0 2px {COR["divider"]}">{corpo_svg(t, v, s_, f"u{i}")}</div>'
                   for i, (t, v, s_) in enumerate(figs))
    open("/tmp/real.html", "w").write(f'<html><body style="margin:0;background:{COR["mist"]}"><div style="display:flex;gap:20px;padding:24px;width:max-content">{html}</div></body></html>')
    print("ok")


def painel(conteudo, w=350):
    return (f'<div style="width: 100%; border-radius: 28px; background: #FFFFFF; box-shadow: inset 0 0 0 2px {COR["divider"]}; display: flex; '
            f'flex-direction: column; align-items: center; padding: 16px 0 12px; box-sizing: border-box">{conteudo}</div>')


def lados(vista="frente", w=280):
    e, d = ("Direito", "Esquerdo") if vista == "frente" else ("Esquerdo", "Direito")
    est = f'font-weight: 700; font-size: 15px; color: {COR["muted"]}'
    return (f'<div style="display: flex; justify-content: space-between; width: {w}px; max-width: 90%" aria-hidden="true">'
            f'<span style="{est}">◂ {e}</span><span style="{est}">{d} ▸</span></div>')
