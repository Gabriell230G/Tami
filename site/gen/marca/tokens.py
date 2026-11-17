"""Tokens v3 da Tami. Fonte única de medidas, cores, tipos e movimento.
Toda tela do app só pode usar valores daqui. checar.py reprova o resto.
Origem de cada valor: Livro de padrões (estudo/LIVRO.md), regras R-*.
"""
import json

# ---------- Cor (uma função por cor) ----------
COR = {
    # marca
    "primary": "#3F4ACB",        # azul Tami
    "primary-press": "#3540B8",  # azul pressionado / gradiente
    "primary-deep": "#2B3290",   # sombra sólida do botão primário
    "primary-light": "#5560D6",  # rosto em marca d'água sobre azul
    "primary-glow": "#6E78E3",   # luz do volume do avatar
    "accent": "#B6EC0C",         # limão: presença, seleção, conquista
    "accent-deep": "#4F6B00",    # limão escuro: texto/ícone sobre limão claro
    "accent-mid": "#8DBA00",     # limão médio: só preenchimento, nunca texto
    "text": "#1B1F4A",           # marinho
    "muted": "#454A78",          # texto secundário (8,4:1)
    "border": "#7C84DD",         # borda que informa (3,4:1 no branco)
    "divider": "#E3E6FB",        # linha decorativa, nunca única pista
    "bg": "#FFFFFF",
    "surface": "#F4F5FF",        # AZ50: cartão de status
    "surface-alt": "#E3E6FB",    # AZ100: trilho, chip parado
    "surface-strong": "#C9CDF5", # AZ200: só preenchimento
    "mist": "#EEF0FF",           # névoa da marca (fundo de tela suave)
    "lime-soft": "#ECF9C0",      # LM100: fundo de conquista/seleção
    "lime-faint": "#F7FCE4",     # LM50
    "danger": "#D63B40",         # emergência (branco 4,6:1)
    "danger-deep": "#9E2A2E",    # sombra do botão de perigo
    "danger-soft": "#FFF1F1",    # fundo de aviso de perigo
    "danger-text": "#B42318",    # texto vermelho sobre branco ou danger-soft (6,0:1)
    "warning-text": "#8A5200",   # texto de atenção no branco
    "warning-soft": "#FFF4E0",   # fundo de atenção
    "danger-light": "#FFB3B5",   # detalhe claro em ícone vermelho
    # destinos (fundo / texto definidos em DESTINO)
    "upa": "#F79009", "ubs": "#7CC4F2", "casa": "#B7EBC8",
    # modo escuro
    "dark-bg": "#0E1130", "dark-surface": "#1B1E3D", "dark-surface-2": "#2E3366", "dark-muted": "#C9CDF5",
}
DESTINO = {
    "SAMU": {"fundo": COR["danger"], "texto": "#FFFFFF", "rosto": "firme"},
    "UPA": {"fundo": COR["upa"], "texto": COR["text"], "rosto": "olha"},
    "UBS": {"fundo": COR["ubs"], "texto": COR["text"], "rosto": "oi"},
    "CASA": {"fundo": COR["casa"], "texto": COR["text"], "rosto": "acolhe"},
}
HEX_PERMITIDOS = {v.upper() for v in COR.values()} | {"#FFFFFF", "#000000"}

# ---------- Espaço, raio ----------
ESPACO = [0, 2, 4, 8, 12, 16, 20, 24, 32, 48, 56]   # 2 só para colar rótulo e subtítulo; 20 é a margem lateral; 56 só na área segura do topo (R-NAV)
RAIO = {"detalhe": 8, "chip": 999, "campo": 16, "opcao": 16, "botao": 999, "cartao": 24, "folha": 28, "pilula": 999}
RAIOS_PERMITIDOS = sorted(set(RAIO.values()) | {0})  # Livro 6.1: 8, 16, 24, 28 e pílula

# ---------- Tipo ----------
TIPO = {
    "display": (34, 38, 700, "Fredoka"),
    "titulo":  (26, 30, 600, "Fredoka"),
    "cartao":  (19, 24, 700, "Nunito"),
    "texto":   (17, 24, 600, "Nunito"),
    "apoio":   (15, 20, 600, "Nunito"),
    "botao":   (17, 20, 800, "Nunito"),
    "numero":  (28, 30, 700, "Fredoka"),
    "hero":    (44, 46, 700, "Fredoka"),
}
TAMANHOS_PERMITIDOS = sorted({v[0] for v in TIPO.values()})
PESOS_PERMITIDOS = [600, 700, 800]

# ---------- Toque ----------
TOQUE_MIN = 48
BOTAO_ALT = 56

# ---------- Sombra (só no que se toca) ----------
SOMBRA = {
    "botao": "0 3px 0 {deep}",
    "opcao": "0 3px 0 {deep}",
    "flutua": "0 8px 24px rgba(27,31,74,0.14)",
}

# ---------- Movimento ----------
MOV = {"toque": 100, "sem_rede": 300, "pulo": 400, "estado": 200, "entrada": 300, "pisca": 150, "pensa": 600, "resultado": 250, "texto_atraso": 150}
CURVA = {"entra": "cubic-bezier(0.05, 0.7, 0.1, 1)", "sai": "cubic-bezier(0.3, 0, 1, 1)", "padrao": "cubic-bezier(0.2, 0, 0, 1)"}


def exportar(caminho="/home/claude/tami-design/estudo/tokens.json"):
    json.dump({"cor": COR, "destino": DESTINO, "espaco": ESPACO, "raio": RAIO, "tipo": {k: dict(zip(["tamanho", "altura", "peso", "familia"], v)) for k, v in TIPO.items()},
               "toque_min": TOQUE_MIN, "botao_alt": BOTAO_ALT, "sombra": SOMBRA, "movimento_ms": MOV, "curva": CURVA},
              open(caminho, "w"), ensure_ascii=False, indent=2)


if __name__ == "__main__":
    exportar(); print("tokens.json exportado")
