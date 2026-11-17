"""corpo_escala.py · testa o toque no corpo (ombro) e a escala de 0 a 10 na conversa."""
import asyncio
from playwright.async_api import async_playwright
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        for w, h in ((1440, 900), (390, 844)):
            pg = await b.new_page(viewport={"width": w, "height": h})
            pg.on("pageerror", lambda e: print("ERRO", e))
            await pg.goto("http://localhost:8790/?modo=leve&intro=nao")
            await pg.evaluate("document.querySelector('.onde').scrollIntoView({block:'center'})"); await pg.wait_for_timeout(2600)
            await pg.screenshot(path=f"/tmp/ce_corpo0_{w}.png")
            z = pg.locator('[data-zona="ombro_e"]'); await z.click(); await pg.wait_for_timeout(600)
            print(w, await pg.evaluate("[document.querySelector('.corpo-balao').textContent, document.querySelector('[data-zona=ombro_e]').getAttribute('aria-pressed')]"))
            await pg.screenshot(path=f"/tmp/ce_corpo1_{w}.png")
            await pg.locator('[data-zona="cabeca"]').focus(); await pg.keyboard.press("Enter"); await pg.wait_for_timeout(500)
            print(w, await pg.evaluate("document.querySelector('.corpo-balao').textContent"))
            # escala: rola até a mensagem da escala e depois até a resposta
            await pg.evaluate("(()=>{const c=document.querySelector('.conversa-cena');const r=c.getBoundingClientRect();scrollTo(0,scrollY+r.top+(c.offsetHeight-innerHeight)*.62)})()"); await pg.wait_for_timeout(900)
            if w < 800: await pg.evaluate("document.querySelector('.escala').scrollIntoView({block:'center'})"); await pg.wait_for_timeout(900)
            await pg.screenshot(path=f"/tmp/ce_esc0_{w}.png")
            await pg.evaluate("(()=>{const c=document.querySelector('.conversa-cena');const r=c.getBoundingClientRect();scrollTo(0,scrollY+r.top+(c.offsetHeight-innerHeight)*.86)})()"); await pg.wait_for_timeout(900)
            if w < 800: await pg.evaluate("document.querySelector('.marca-fraco').scrollIntoView({block:'center'})"); await pg.wait_for_timeout(900)
            await pg.screenshot(path=f"/tmp/ce_esc1_{w}.png")
            await pg.close()
        await b.close()
asyncio.run(main())
