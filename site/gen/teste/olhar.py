"""olhar.py · capturas rápidas de cada cena. Uso: python3 olhar.py <modo> <largura> <altura> [pagina]"""
import sys, asyncio
from playwright.async_api import async_playwright
modo, W, H = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]); pag = sys.argv[4] if len(sys.argv) > 4 else ""
OUT = "../../reports/shots"
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        pg = await b.new_page(viewport={"width": W, "height": H})
        erros = []
        pg.on("console", lambda m: erros.append(m.text) if m.type in ("error", "warning") else None)
        pg.on("pageerror", lambda e: erros.append(str(e)))
        await pg.goto(f"http://localhost:8790/{pag}index.html?modo={modo}&vigia=nao", wait_until="load")
        await pg.wait_for_timeout(2500)
        cenas = await pg.evaluate("[...document.querySelectorAll('[data-cena]')].map(e=>e.getBoundingClientRect().top+scrollY)")
        info = await pg.evaluate("({modo:document.documentElement.dataset.modo, motivo:document.documentElement.dataset.motivo, ceu:!!document.querySelector('canvas.ceu'), fps:window.__tamiFps||0, h:document.body.scrollHeight})")
        print(info, erros[:5])
        for i, y in enumerate(cenas):
            alvo = y + (H * 0.9 if 'conversa' in (await pg.evaluate(f"document.querySelectorAll('[data-cena]')[{i}].className")) else 0)
            await pg.evaluate(f"window.scrollTo(0,{alvo})"); await pg.wait_for_timeout(900)
            await pg.screenshot(path=f"{OUT}/{(pag or 'inicio').strip('/')}_{modo}_{W}_{i}.jpg", quality=55, type="jpeg")
        await b.close()
asyncio.run(main())
