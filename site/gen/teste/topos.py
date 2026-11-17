import asyncio, sys
from playwright.async_api import async_playwright
from PIL import Image
PAGS=["", "como-funciona/", "seguranca/", "sobre/", "perguntas/", "beta/", "imprensa/", "privacidade/"]
async def main():
    modo, W, H = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
    async with async_playwright() as p:
        b=await p.chromium.launch(args=["--use-gl=angle","--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist"])
        pg=await b.new_page(viewport={"width":W,"height":H})
        ims=[]
        for u in PAGS:
            await pg.goto(f"http://localhost:8790/{u}index.html?modo={modo}&vigia=nao", wait_until="load"); await pg.wait_for_timeout(2200)
            await pg.screenshot(path="/tmp/t.png"); ims.append(Image.open("/tmp/t.png").convert("RGB"))
        await b.close()
    s=0.5 if W>800 else 0.5
    w,h=int(W*s),int(H*s); cols=4
    F=Image.new("RGB",(w*cols,h*2),"white")
    for i,im in enumerate(ims): F.paste(im.resize((w,h)),((i%cols)*w,(i//cols)*h))
    F.save(f"../../reports/shots/topos_{modo}_{W}.jpg",quality=70)
asyncio.run(main())
