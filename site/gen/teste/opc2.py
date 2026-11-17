"""opc2.py · capturas da rodada 2 (computador grande e celular) + quadros da abertura."""
import asyncio, sys
from playwright.async_api import async_playwright
from PIL import Image

KS = sys.argv[1:] or list("12345")
INTRO = {}

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        for k in KS:
            for w, h in ((1440, 900), (390, 844)):
                pg = await b.new_page(viewport={"width": w, "height": h})
                erros = []
                pg.on("pageerror", lambda e: erros.append(str(e)))
                await pg.goto(f"http://localhost:8790/opcoes2/{k}.html?modo=show&vigia=nao", wait_until="commit")
                t0 = 0
                if w == 1440 and k in INTRO:
                    for t in INTRO[k]:
                        await pg.wait_for_timeout(t - t0); t0 = t
                        await pg.screenshot(path=f"/tmp/o2_{k}_i{t}.png")
                await pg.mouse.move(w * .75, h * .3)
                await pg.wait_for_timeout((11000 if k == "2" else 6500) - t0)
                await pg.screenshot(path=f"/tmp/o2_{k}_{w}.png")
                if erros: print(k, w, erros[:3])
                await pg.close()
        await b.close()
    for k in KS:
        d = Image.open(f"/tmp/o2_{k}_1440.png").convert("RGB").resize((1152, 720))
        m = Image.open(f"/tmp/o2_{k}_390.png").convert("RGB").resize((333, 720))
        F = Image.new("RGB", (1152 + 333 + 30, 740), "#EEF0FF"); F.paste(d, (5, 10)); F.paste(m, (1172, 10))
        F.save(f"/tmp/o2_{k}.jpg", quality=82)
        if k in INTRO:
            fr = [Image.open(f"/tmp/o2_{k}_i{t}.png").convert("RGB").resize((480, 300)) for t in INTRO[k]]
            G = Image.new("RGB", (len(fr) * 490 + 10, 320), "#EEF0FF")
            for i, f in enumerate(fr): G.paste(f, (10 + i * 490, 10))
            G.save(f"/tmp/o2_{k}_intro.jpg", quality=82)
    print("ok")
asyncio.run(main())
