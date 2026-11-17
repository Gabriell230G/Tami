"""folhas.py · folhas de contato da home nas 9 situações do prompt."""
import asyncio
from playwright.async_api import async_playwright
from PIL import Image, ImageDraw
OUT = "../../reports/folhas"
SIT = [("android-fraco", 360, 640, 1, {"cpu": 6, "rede": True}, ""), ("celular", 390, 844, 1, {}, "?modo=show&vigia=nao"),
       ("tablet", 768, 1024, 1, {}, "?modo=show&vigia=nao"), ("notebook", 1280, 800, 1, {}, "?modo=show&vigia=nao"),
       ("monitor-1440", 1440, 900, 1, {}, "?modo=show&vigia=nao"), ("monitor-1920", 1920, 1080, 1, {}, "?modo=show&vigia=nao"),
       ("zoom-200", 195, 422, 2, {}, ""), ("menos-movimento", 390, 844, 1, {"rm": True}, ""), ("sem-js", 390, 844, 1, {"nojs": True}, ""),
       ("leve", 390, 844, 1, {}, "?modo=leve")]
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"])
        for nome, w, h, dsf, op, q in SIT:
            ctx = await b.new_context(viewport={"width": w, "height": h}, device_scale_factor=dsf, java_script_enabled=not op.get("nojs"),
                                      reduced_motion="reduce" if op.get("rm") else "no-preference")
            pg = await ctx.new_page()
            if op.get("cpu"):
                cdp = await ctx.new_cdp_session(pg)
                await cdp.send("Emulation.setCPUThrottlingRate", {"rate": op["cpu"]})
                await cdp.send("Network.emulateNetworkConditions", {"offline": False, "latency": 400, "downloadThroughput": 400 * 1024 / 8, "uploadThroughput": 400 * 1024 / 8})
            await pg.goto(f"http://localhost:8790/index.html{q}", wait_until="load", timeout=90000); await pg.wait_for_timeout(2500)
            modo = await pg.evaluate("document.documentElement.dataset.modo || 'sem js'")
            ys = await pg.evaluate("[...document.querySelectorAll('[data-cena]')].map(e=>e.getBoundingClientRect().top+scrollY)")
            ims = []
            for i in (0, 1, 2, 3, 4, 6, 7, 8):
                if i >= len(ys): break
                off = h * 0.9 if i == 2 else 0
                await pg.evaluate(f"scrollTo(0,{ys[i] + off})"); await pg.wait_for_timeout(700)
                await pg.screenshot(path="/tmp/f.png"); ims.append(Image.open("/tmp/f.png").convert("RGB").resize((w * dsf // (2 if w > 800 else 1) // dsf * dsf // dsf, h // (2 if w > 800 else 1))))
            await ctx.close()
            tw, th = ims[0].size
            F = Image.new("RGB", (tw * 4 + 50, th * 2 + 80), "#EEF0FF"); d = ImageDraw.Draw(F)
            d.text((10, 10), f"{nome} · {w}x{h} · modo: {modo}", fill="#1B1F4A")
            for k, im in enumerate(ims): F.paste(im, (10 + (k % 4) * (tw + 10), 40 + (k // 4) * (th + 10)))
            F.save(f"{OUT}/folha_{nome}.jpg", quality=72); print(nome, modo)
        await b.close()
asyncio.run(main())
