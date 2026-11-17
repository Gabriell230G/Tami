"""r2.py · capturas das cenas novas em vários pontos da rolagem. Uso: python3 r2.py <modo> <largura> <altura> [saida]"""
import asyncio
import sys
from playwright.async_api import async_playwright
from PIL import Image

modo, W, H = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
SAI = sys.argv[4] if len(sys.argv) > 4 else f"/tmp/r2_{modo}_{W}.jpg"
PONTOS = [(".abertura", 0), (".abertura", .3), (".abertura", .6), (".abertura", 1), (".conversa-cena", .5),
          (".caminhos", .1), (".caminhos", .45), (".caminhos", 1), (".pressa", 0), (".orbita", 0), (".seg", 0), (".convite", 0)]


async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        pg = await b.new_page(viewport={"width": W, "height": H})
        erros = []
        pg.on("pageerror", lambda e: erros.append(str(e)))
        pg.on("console", lambda m: erros.append(m.text) if m.type == "error" else None)
        await pg.goto(f"http://localhost:8790/index.html?modo={modo}&vigia=nao", wait_until="load")
        await pg.wait_for_timeout(2000)
        ims = []
        for sel, k in PONTOS:
            await pg.evaluate(f"(()=>{{const e=document.querySelector('{sel}');if(!e)return;const t=e.getBoundingClientRect().top+scrollY;scrollTo(0,t+Math.max(0,e.offsetHeight-innerHeight)*{k})}})()")
            await pg.wait_for_timeout(1100)
            await pg.screenshot(path="/tmp/r2.png")
            ims.append(Image.open("/tmp/r2.png").convert("RGB"))
        info = await pg.evaluate("({modo:document.documentElement.dataset.modo, ab:document.querySelector('.abertura').className, cm:document.querySelector('.caminhos').className, lado:document.documentElement.scrollWidth>innerWidth+1})")
        print(info, erros[:4])
        await b.close()
    esc = .5 if W > 800 else .6
    w, h = int(W * esc), int(H * esc)
    cols = 4
    F = Image.new("RGB", (w * cols, h * ((len(ims) + cols - 1) // cols)), "white")
    for i, im in enumerate(ims):
        F.paste(im.resize((w, h)), ((i % cols) * w, (i // cols) * h))
    F.save(SAI, quality=68)

asyncio.run(main())
