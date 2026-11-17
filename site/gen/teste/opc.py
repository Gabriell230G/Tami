"""opc.py · capturas das 3 opções de herói (computador e celular) numa folha só."""
import asyncio
from playwright.async_api import async_playwright
from PIL import Image, ImageDraw


async def main():
    ims = {}
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        for k in "abcde":
            for w, h in ((1440, 900), (390, 844)):
                pg = await b.new_page(viewport={"width": w, "height": h})
                erros = []
                pg.on("pageerror", lambda e: erros.append(str(e)))
                await pg.goto(f"http://localhost:8790/opcoes/{k}.html?modo=show&vigia=nao", wait_until="load")
                await pg.mouse.move(w * .75, h * .3)
                await pg.wait_for_timeout(3500)
                await pg.screenshot(path=f"/tmp/opc_{k}_{w}.png")
                if erros:
                    print(k, w, erros[:3])
                ims[(k, w)] = Image.open(f"/tmp/opc_{k}_{w}.png").convert("RGB")
                await pg.close()
        await b.close()
    for k in "abcde":
        d = ims[(k, 1440)].resize((1152, 720)); m = ims[(k, 390)].resize((333, 720))
        F = Image.new("RGB", (1152 + 333 + 30, 720 + 20), "#EEF0FF")
        F.paste(d, (5, 10)); F.paste(m, (1152 + 20, 10))
        F.save(f"/tmp/opcao_{k}.jpg", quality=80)
    print("ok")

asyncio.run(main())
