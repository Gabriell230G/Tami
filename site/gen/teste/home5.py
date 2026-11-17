"""home5.py · primeira tela do site com a opção 5 (computador e celular) + rolagem até a cena 2."""
import asyncio
from playwright.async_api import async_playwright
from PIL import Image
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        for w, h in ((1440, 900), (390, 844)):
            pg = await b.new_page(viewport={"width": w, "height": h})
            pg.on("pageerror", lambda e: print("ERRO", e))
            await pg.goto("http://localhost:8790/?modo=show&vigia=nao")
            await pg.wait_for_timeout(9000)
            await pg.screenshot(path=f"/tmp/h5_{w}.png")
            await pg.evaluate("document.querySelector('.noite2').scrollIntoView()")
            await pg.wait_for_timeout(5000)
            await pg.screenshot(path=f"/tmp/h5n_{w}.png")
            await pg.close()
        await b.close()
    d = Image.open("/tmp/h5_1440.png").convert("RGB").resize((1152, 720)); m = Image.open("/tmp/h5_390.png").convert("RGB").resize((333, 720))
    F = Image.new("RGB", (1515, 740), "#EEF0FF"); F.paste(d, (5, 10)); F.paste(m, (1172, 10)); F.save("/tmp/h5.jpg", quality=84)
    d = Image.open("/tmp/h5n_1440.png").convert("RGB").resize((1152, 720)); m = Image.open("/tmp/h5n_390.png").convert("RGB").resize((333, 720))
    F = Image.new("RGB", (1515, 740), "#EEF0FF"); F.paste(d, (5, 10)); F.paste(m, (1172, 10)); F.save("/tmp/h5n.jpg", quality=84)
asyncio.run(main())
