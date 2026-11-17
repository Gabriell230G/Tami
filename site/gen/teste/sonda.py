import sys, asyncio, json
from playwright.async_api import async_playwright
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle","--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist"])
        pg = await b.new_page(viewport={"width": int(sys.argv[2]), "height": int(sys.argv[3])})
        await pg.goto(f"http://localhost:8790/index.html?modo={sys.argv[1]}", wait_until="load")
        await pg.wait_for_timeout(2500)
        print(await pg.evaluate(sys.stdin.read()))
        await b.close()
asyncio.run(main())
