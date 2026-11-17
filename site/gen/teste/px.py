import asyncio
from playwright.async_api import async_playwright
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle","--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist"])
        pg = await b.new_page(viewport={"width":1440,"height":900})
        await pg.goto("http://localhost:8790/index.html?modo=show", wait_until="load"); await pg.wait_for_timeout(2500)
        await pg.evaluate("scrollTo(0,document.querySelectorAll('[data-cena]')[3].getBoundingClientRect().top+scrollY)")
        for w in (300, 1500, 3000):
            await pg.wait_for_timeout(w)
            await pg.screenshot(path="/tmp/px.png")
            from PIL import Image
            im=Image.open("/tmp/px.png").convert("RGB")
            print(w, [im.getpixel((100,y)) for y in (100,500,700,780,850,890)], await pg.evaluate("[innerHeight, document.querySelector('canvas.ceu').getBoundingClientRect().height, document.querySelector('.caminhos').getBoundingClientRect().bottom]"))
        await b.close()
asyncio.run(main())
