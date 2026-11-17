"""abre.py · quadros da abertura do site (modo leve, tempo fiel) e estado final."""
import asyncio
from playwright.async_api import async_playwright
from PIL import Image
TS = (1600, 3600, 4300)
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        for w, h in ((1440, 900), (390, 844)):
            pg = await b.new_page(viewport={"width": w, "height": h})
            pg.on("pageerror", lambda e: print("ERRO", e))
            await pg.goto("http://localhost:8790/?modo=leve", wait_until="domcontentloaded")
            t0 = await pg.evaluate("performance.now()")
            for t in TS:
                ag = await pg.evaluate("performance.now()")
                await pg.wait_for_timeout(max(0, t - (ag - t0)))
                await pg.screenshot(path=f"/tmp/ab_{w}_{t}.png")
            print(w, await pg.evaluate("[!!document.getElementById('abre'), document.documentElement.className]"))
            await pg.close()
        await b.close()
    for w in (1440, 390):
        sz = (480, 300) if w == 1440 else (180, 390)
        fr = [Image.open(f"/tmp/ab_{w}_{t}.png").convert("RGB").resize(sz) for t in TS]
        G = Image.new("RGB", (len(fr) * (sz[0] + 8) + 8, sz[1] + 16), "#EEF0FF")
        for i, f in enumerate(fr): G.paste(f, (8 + i * (sz[0] + 8), 8))
        G.save(f"/tmp/abre_{w}.jpg", quality=84)
asyncio.run(main())
