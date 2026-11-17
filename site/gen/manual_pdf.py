"""manual_pdf.py · gera o PDF do manual de marca, os PNG dos logos e o ZIP de logos.
Uso: python3 gen/manual_pdf.py (depois do site2.py)."""
import asyncio
import os
import zipfile
from playwright.async_api import async_playwright

R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
M = os.path.join(R, "dist", "marca")
LOGOS = os.path.join(M, "logos")

LEIA = """Logos oficiais da Tami
======================
SVG: use sempre que puder (não perde qualidade).
PNG: 1024 px de largura, fundo transparente.

padrao ........ fundo branco ou névoa
sobre-azul .... fundo azul #3F4ACB
sobre-marinho . fundo marinho #1B1F4A
sobre-limao ... fundo limão #B6EC0C
marinho ....... uma cor só, para fundo claro
branco ........ uma cor só, para fundo escuro

Regras rápidas
- Deixe em volta um espaço livre de 1/4 da altura do monograma.
- Mínimo: 96 px (horizontal) e 24 px (monograma).
- Não estique, não gire, não troque as cores, não use sombra.
- Nada de cruz, jaleco ou estetoscópio junto da marca.
Manual completo: marca/tami-manual-de-marca.pdf
"""


async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page(viewport={"width": 1600, "height": 900})
        await pg.goto("file://" + os.path.join(M, "pdf.html")); await pg.wait_for_timeout(1200)
        await pg.evaluate("document.fonts.ready")
        await pg.pdf(path=os.path.join(M, "tami-manual-de-marca.pdf"), width="1600px", height="900px", print_background=True,
                     margin={"top": "0", "right": "0", "bottom": "0", "left": "0"})
        for n in sorted(os.listdir(LOGOS)):
            if not n.endswith(".svg"):
                continue
            svg = open(os.path.join(LOGOS, n)).read()
            await pg.set_content(f'<html><body style="margin:0;background:transparent">{svg.replace("<svg ", "<svg style=\"display:block;width:1024px;height:auto\" ", 1)}</body></html>')
            await pg.locator("svg").screenshot(path=os.path.join(LOGOS, n[:-4] + ".png"), omit_background=True)
        await b.close()
    with zipfile.ZipFile(os.path.join(M, "tami-logos.zip"), "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("tami-logos/LEIA-ME.txt", LEIA)
        for n in sorted(os.listdir(LOGOS)):
            z.write(os.path.join(LOGOS, n), "tami-logos/" + n)
    print("pdf:", os.path.getsize(os.path.join(M, "tami-manual-de-marca.pdf")) // 1024, "KB · zip:", os.path.getsize(os.path.join(M, "tami-logos.zip")) // 1024, "KB")

asyncio.run(main())
