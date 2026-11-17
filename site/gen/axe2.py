import asyncio, json
from playwright.async_api import async_playwright
AXE=open('axe.min.js').read()
import sys
PAGS=["", "como-funciona/", "seguranca/", "sobre/", "perguntas/", "beta/", "imprensa/", "privacidade/"]
if len(sys.argv) > 1: PAGS=[x if x != "inicio" else "" for x in sys.argv[1:]]
async def main():
    async with async_playwright() as p:
        b=await p.chromium.launch(args=["--use-gl=angle","--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist"])
        res={}
        for modo in ("show","leve","parado"):
            for w in (360,1280):
                pg=await b.new_page(viewport={"width":w,"height":800})
                for u in PAGS:
                    await pg.goto(f"http://localhost:8790/{u}index.html?modo={modo}&vigia=nao"); await pg.wait_for_timeout(800 if modo=="show" else 200)
                    # a abertura cobre a página por uns 4,5 s: espera ela sair antes de auditar
                    await pg.wait_for_function("!document.getElementById('abre')", timeout=30000)
                    await pg.add_script_tag(content=AXE)
                    r=await pg.evaluate("axe.run(document,{runOnly:['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa','best-practice']})")
                    ruins=[(v['id'],v['impact'],len(v['nodes']),v['nodes'][0]['target'][0]) for v in r['violations']]
                    res[f"{modo} {w} {u or 'inicio'}"]=ruins
                    if ruins: print(modo, w, u or 'inicio', ruins)
                await pg.close()
        json.dump(res,open('../reports/axe.json','w'),indent=1)
        print("total com falha:", sum(1 for v in res.values() if v), "de", len(res))
        await b.close()
asyncio.run(main())
