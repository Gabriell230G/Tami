"""intro2.py · quadros da abertura (modo leve, sem WebGL, para o tempo ficar fiel)."""
import asyncio
from playwright.async_api import async_playwright
from PIL import Image
QUADROS = {"1": (300, 650, 1000, 1400, 1700), "2": (250, 550, 900, 1400)}
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        for k, ts in QUADROS.items():
            pg = await b.new_page(viewport={"width": 1440, "height": 900})
            await pg.goto(f"http://localhost:8790/opcoes2/{k}.html?modo=leve", wait_until="domcontentloaded")
            t0 = await pg.evaluate("performance.now()")
            for t in ts:
                agora = await pg.evaluate("performance.now()")
                await pg.wait_for_timeout(max(0, t - (agora - t0)))
                await pg.screenshot(path=f"/tmp/in_{k}_{t}.png")
            await pg.close()
        await b.close()
    for k, ts in QUADROS.items():
        fr = [Image.open(f"/tmp/in_{k}_{t}.png").convert("RGB").resize((576, 360)) for t in ts]
        G = Image.new("RGB", (len(fr) * 586 + 10, 380), "#EEF0FF")
        for i, f in enumerate(fr): G.paste(f, (10 + i * 586, 10))
        G.save(f"/tmp/o2_{k}_intro.jpg", quality=82)
asyncio.run(main())
