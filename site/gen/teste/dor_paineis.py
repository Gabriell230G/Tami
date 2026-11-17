"""dor_paineis.py · testa o cartão de dor (joelho, 6, latejando, pronto) e os painéis dos caminhos."""
import asyncio
from playwright.async_api import async_playwright
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        for w, h in ((1440, 900), (390, 844)):
            pg = await b.new_page(viewport={"width": w, "height": h})
            pg.on("pageerror", lambda e: print("ERRO", e))
            await pg.goto("http://localhost:8790/?modo=leve&intro=nao")
            await pg.evaluate("document.querySelector('.onde').scrollIntoView({block:'start'})"); await pg.wait_for_timeout(2400)
            await pg.locator('[data-zona="joelho_d"]').click(); await pg.wait_for_timeout(700)
            await pg.locator('.dn-n[data-n="6"]').click(); await pg.locator('.dn-t', has_text="Latejando").click(); await pg.wait_for_timeout(400)
            await pg.screenshot(path=f"/tmp/dp_cartao_{w}.png")
            print(w, await pg.evaluate("[document.querySelector('.dn-nome').textContent, document.querySelector('.dn-valor').textContent]"))
            await pg.locator('.dn-pronto').click(); await pg.wait_for_timeout(500)
            print(w, await pg.evaluate("document.querySelector('.corpo-balao').textContent"))
            await pg.evaluate("document.querySelector('.figura').scrollIntoView({block:'center'})"); await pg.wait_for_timeout(500)
            await pg.screenshot(path=f"/tmp/dp_pronto_{w}.png")
            for j, f in enumerate((.05, .3, .55, .85)):
                await pg.evaluate(f"(()=>{{const t=document.querySelector('.paineis');const top=t.getBoundingClientRect().top+scrollY;scrollTo(0, top+(t.offsetHeight-innerHeight)*{f})}})()"); await pg.wait_for_timeout(1100)
                await pg.screenshot(path=f"/tmp/dp_pn{j}_{w}.png")
            print(w, await pg.evaluate("[document.querySelector('.paineis').dataset.k, document.querySelector('.paineis').className]"))
            await pg.close()
        await b.close()
asyncio.run(main())
