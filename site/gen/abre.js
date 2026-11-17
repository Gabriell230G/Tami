(function () {
  var d = document.documentElement, a = document.getElementById("abre");
  if (!a) return;
  var v; try { v = sessionStorage.getItem("tami-abre"); } catch (e) {}
  if (d.dataset.modo === "parado" || v || /[?&]intro=nao/.test(location.search) || location.hash || scrollY > 0) { d.classList.add("pronto"); a.remove(); return; }
  try { sessionStorage.setItem("tami-abre", "1"); } catch (e) {}
  document.body.appendChild(a); a.hidden = false; d.classList.add("na-abre");
  var feito = 0, tm = [], raf = 0;
  function depois(ms, f) { tm.push(setTimeout(f, ms)); }
  function fim() { if (feito) return; feito = 1; tm.forEach(clearTimeout); cancelAnimationFrame(raf); d.classList.remove("na-abre"); d.classList.add("pronto"); a.remove(); }
  a.querySelector(".abre-pular").addEventListener("click", fim);
  addEventListener("keydown", function (e) { if (e.key === "Escape") fim(); });
  document.addEventListener("click", function (e) { if (e.target.closest && e.target.closest("[data-mov]")) fim(); }, true);

  // letras: aparecem uma de cada vez, numa cor só
  var ls = a.querySelectorAll("[data-l]");
  [].forEach.call(ls, function (l, i) { depois(550 + i * 38, function () { l.classList.add("on"); }); });
  depois(60, function () { a.classList.add("acorda"); });

  // líquido: duas camadas (limão e azul) sobem, cobrem a tela e seguem subindo até revelar a página
  var svg = a.querySelector(".abre-liquido"), ns = "http://www.w3.org/2000/svg", W, H, camadas = [];
  function camada(g, cor, atraso) {
    var n = W < 700 ? 7 : 12, bs = [], rect = document.createElementNS(ns, "rect");
    rect.setAttribute("fill", cor); rect.setAttribute("x", -40); rect.setAttribute("width", W + 80); g.appendChild(rect);
    for (var i = 0; i < 2 * n; i++) {
      var c = document.createElementNS(ns, "circle"), r = Math.max(34, W * (W < 700 ? .11 : .06)) * (.75 + Math.random() * .6);
      c.setAttribute("fill", cor); c.setAttribute("r", r); g.appendChild(c);
      bs.push({ el: c, r: r, x: ((i % n) + .5) / n * W + (Math.random() - .5) * W / n * .6, cima: i < n, sobra: Math.random(), atraso: Math.random() * 80 });
    }
    return { rect: rect, bs: bs, atraso: atraso };
  }
  function ease(t) { t = Math.max(0, Math.min(1, t)); return t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
  function liquido() {
    W = innerWidth; H = innerHeight - a.getBoundingClientRect().top;
    svg.setAttribute("viewBox", "0 0 " + W + " " + H);
    svg.querySelectorAll("filter").forEach(function (f) { f.setAttribute("width", W); f.setAttribute("height", H); });
    camadas = [camada(svg.querySelector(".liq-lima"), "#B6EC0C", 0), camada(svg.querySelector(".liq-azul"), "#3F4ACB", 70)];
    var t0 = performance.now(), DUR = 520, M = 120 * 2, trocou = 0;
    function passo(agora) {
      var t = agora - t0, ok = true;
      camadas.forEach(function (c, k) {
        // na subida a azul vem depois da limão; na saída a azul sai primeiro
        var tc = t - c.atraso, ts = t - (DUR + 160) - (k === 1 ? 0 : 70);
        var pc = ease(tc / DUR), ps = ease(ts / DUR);
        var topo = H + M - (H + 2 * M) * pc, base = H + M - (H + 2 * M) * ps;
        c.rect.setAttribute("y", Math.min(topo + M * .35, base)); c.rect.setAttribute("height", Math.max(0, base - topo - M * .35));
        c.bs.forEach(function (b) {
          var p1 = ease((tc - b.atraso) / DUR), p2 = ease((ts - b.atraso * .25) / DUR);
          var y = b.cima ? H + M - (H + 2 * M) * p1 - b.sobra * b.r * .9 + M * .35 : H + M - (H + 2 * M) * p2 + b.sobra * b.r * .45;
          b.el.setAttribute("cx", b.x); b.el.setAttribute("cy", y);
        });
        if (ps < 1) ok = false;
      });
      if (!trocou && t > DUR + 150) { trocou = 1; a.classList.add("coberto"); d.classList.remove("na-abre"); d.classList.add("pronto"); }
      if (ok && t > 2 * DUR) { fim(); return; }
      raf = requestAnimationFrame(passo);
    }
    raf = requestAnimationFrame(passo);
  }
  depois(3300, liquido);
  // trava de segurança
  depois(5600, fim);
})();
