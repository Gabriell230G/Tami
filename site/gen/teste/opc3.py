"""opc3.py · capturas das 5 opções dos caminhos em vários pontos da rolagem (computador e celular)."""
import asyncio, sys
from playwright.async_api import async_playwright
from PIL import Image
PONTOS = (.02, .3, .55, .8, .99)
KS = sys.argv[1:] or list("12345")
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        for k in KS:
            for w, h in ((1440, 900), (390, 844)):
                pg = await b.new_page(viewport={"width": w, "height": h})
                pg.on("pageerror", lambda e: print("ERRO", k, e))
                await pg.goto(f"http://localhost:8790/opcoes3/{k}.html?modo=leve"); await pg.wait_for_timeout(400)
                for j, f in enumerate(PONTOS):
                    await pg.evaluate(f"(()=>{{const t=document.querySelector('main > section');const top=t.getBoundingClientRect().top+scrollY;const tot=Math.max(t.offsetHeight-innerHeight, t.offsetHeight*.75);scrollTo(0, top+tot*{f})}})()")
                    await pg.wait_for_timeout(900)
                    await pg.screenshot(path=f"/tmp/o3_{k}_{w}_{j}.png")
                await pg.close()
        await b.close()
    for k in KS:
        fr = [Image.open(f"/tmp/o3_{k}_1440_{j}.png").convert("RGB").resize((576, 360)) for j in range(len(PONTOS))]
        fm = [Image.open(f"/tmp/o3_{k}_390_{j}.png").convert("RGB").resize((166, 360)) for j in range(len(PONTOS))]
        G = Image.new("RGB", (len(fr) * 586 + 10, 760), "#EEF0FF")
        for i, f in enumerate(fr): G.paste(f, (10 + i * 586, 10))
        for i, f in enumerate(fm): G.paste(f, (10 + i * 586 + 205, 390))
        G.save(f"/tmp/caminhos_{k}.jpg", quality=84)
asyncio.run(main())
