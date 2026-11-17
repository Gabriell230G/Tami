/* cenas2.js · cenas da rodada 2: cortinas, caminhos girando e órbita das telas.
   Sem bibliotecas. A rolagem é a do navegador; as cenas só leem onde a seção está. */
(function () {
  "use strict";
  var d = document.documentElement;
  function parado() { return d.dataset.modo === "parado"; }
  function clamp(v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
  function suave(t) { t = clamp(t); return t * t * (3 - 2 * t); }
  function progresso(sec, palco) {
    var r = sec.getBoundingClientRect(), total = r.height - palco.offsetHeight;
    return total > 0 ? clamp(-r.top / total) : 1;
  }
  // se o conteúdo não cabe no palco (zoom grande, tela baixa), a cena vira lista normal
  function cabe(sec, palco, cls) {
    sec.classList.remove(cls);
    var ok = Array.prototype.every.call(palco.querySelectorAll("[data-cabe]"), function (el) { return el.scrollHeight <= palco.clientHeight + 2; });
    if (!ok) sec.classList.add(cls);
    return ok;
  }

  /* ---------- 1 · cortinas: os olhos da Tami se abrem e revelam a noite ---------- */
  var ab = document.querySelector("[data-abertura]");
  if (ab) {
    var abPalco = ab.querySelector(".ab-palco"), cort = ab.querySelectorAll(".cortina"), duv = ab.querySelectorAll(".duvidas li");
    var abAnt = -1;
    var abRolar = function () {
      if (parado() || ab.classList.contains("plana")) { cort.forEach(function (c) { c.inert = false; c.style.visibility = ""; }); return; }
      var p = progresso(ab, abPalco), t = suave(p / .55);
      if (Math.abs(p - abAnt) < .0005) return; abAnt = p;
      ab.style.setProperty("--abre", t.toFixed(4));
      ab.style.setProperty("--noite", suave((p - .12) / .45).toFixed(4));
      var caos = 1 - suave((p - .5) / .35);
      duv.forEach(function (li) { li.style.setProperty("--caos", caos.toFixed(3)); });
      cort.forEach(function (c) { c.style.pointerEvents = t > .4 ? "none" : ""; c.inert = t >= .98; c.style.visibility = t >= .98 ? "hidden" : ""; });
      // o céu e o rosto do show leem estes atributos
      ab.dataset.ceu = t > .5 ? "susto" : "nevoa";
      ab.dataset.rosto = t > .5 ? "ouve" : "oi";
      ab.dataset.bolhas = p > .5 ? "caos" : "nao";
    };
    var abMedir = function () { cabe(ab, abPalco, "plana"); abAnt = -1; abRolar(); };
    ab.querySelectorAll(".cortina .ct-txt, .ab-noite .cx").forEach(function (el) { el.setAttribute("data-cabe", ""); });
    addEventListener("scroll", abRolar, { passive: true }); addEventListener("resize", abMedir);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(abMedir); abMedir();
  }

  /* ---------- 3 · os 4 caminhos entram girando ---------- */
  var cm = document.querySelector("[data-giros]");
  if (cm) {
    var cmPalco = cm.querySelector(".cm-palco"), cams = cm.querySelectorAll("[data-giro]"), letras = cm.querySelectorAll(".fantasma.frente span");
    var marcas = cm.querySelectorAll(".cm-prog i"), lados = [1, -1, 1, -1], incl = [-3, 2.5, -2, 3];
    var cmRolar = function () {
      if (parado() || cm.classList.contains("plana")) { cams.forEach(function (c) { c.style.transform = ""; c.style.opacity = ""; }); return; }
      var p = progresso(cm, cmPalco), estreito = innerWidth < 720;
      cams.forEach(function (c, i) {
        var t = suave((p - i * .18) / .30), x = (1 - t) * lados[i] * 60, ry = (1 - t) * lados[i] * 90, z = (1 - t) * 400;
        c.style.transform = "translate3d(" + x.toFixed(2) + "vw,0," + z.toFixed(1) + "px) rotateY(" + ry.toFixed(2) + "deg) rotate(" + (incl[i] * t).toFixed(2) + "deg)";
        c.style.opacity = clamp((t - .12) * 2.2).toFixed(3);
        if (marcas[i]) marcas[i].classList.toggle("on", t > .95);
      });
      var tl = suave((p - .10) / .50);
      letras.forEach(function (l, i) { var k = clamp(tl * (letras.length + 4) - i); l.style.setProperty("--o", (1 - k).toFixed(3)); });
      cm.dataset.bolhas = p > .84 || estreito ? "portais" : "calmas";
    };
    var cmMedir = function () { cabe(cm, cmPalco, "plana"); cmRolar(); };
    cm.querySelectorAll(".cx").forEach(function (el) { el.setAttribute("data-cabe", ""); });
    addEventListener("scroll", cmRolar, { passive: true }); addEventListener("resize", cmMedir);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(cmMedir); cmMedir();
  }

  /* ---------- 3b · os 4 caminhos em painéis: a rolagem abre um de cada vez; tocar numa faixa leva até ela ---------- */
  var pn = document.querySelector("[data-paineis]");
  if (pn) {
    var pnPalco = pn.querySelector(".pn-palco"), pnBts = [].slice.call(pn.querySelectorAll(".pn-faixa")), pnK = -1;
    var pnAbre = function (k) {
      if (k === pnK) return; pnK = k; pn.dataset.k = k;
      pnBts.forEach(function (b, i) { b.setAttribute("aria-expanded", i === k ? "true" : "false"); });
    };
    var pnRola = function () {
      if (parado() || pn.classList.contains("plana")) return;
      pnAbre(Math.min(3, Math.floor(progresso(pn, pnPalco) * 4 * .999)));
    };
    pnBts.forEach(function (b, i) {
      b.addEventListener("click", function () {
        if (parado() || pn.classList.contains("plana")) { pnAbre(i); return; }
        var total = pn.offsetHeight - pnPalco.offsetHeight;
        scrollTo({ top: pn.getBoundingClientRect().top + scrollY + total * (i + .5) / 4, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
      });
    });
    pn.querySelectorAll(".pn-in").forEach(function (el) { el.setAttribute("data-cabe", ""); });
    var pnMede = function () { if (!parado()) cabe(pn, pnPalco, "plana"); pnRola(); };
    addEventListener("scroll", pnRola, { passive: true }); addEventListener("resize", pnMede); pnMede();
  }

  /* ---------- 2 · órbita das telas do app ---------- */
  document.querySelectorAll("[data-orbita]").forEach(function (orb) {
    var itens = Array.prototype.map.call(orb.querySelectorAll(".orb-item"), function (li) { return { titulo: li.dataset.titulo, tela: li.querySelector(".orb-tela").innerHTML }; });
    var n = itens.length, palco = orb.querySelector(".orb-palco"), anel = orb.querySelector(".orb-anel"), info = orb.querySelector(".orb-info");
    var numEl = orb.querySelector(".orb-n"), tituloEl = orb.querySelector(".orb-titulo"), pontos = orb.querySelector(".orb-pontos"), anuncio = orb.querySelector("[data-anuncio]");
    var pos = 0, alvo = null, espaco = 240, cartoes = [], ativo = -1, arrastando = false, ultX = 0, tAnt = 0;
    function wrap(v, m) { return ((v % m) + m) % m; }
    palco.hidden = false; info.hidden = false; orb.classList.add("vivo");
    for (var k = 0; k < n; k++) {
      var b = document.createElement("button"); b.type = "button"; b.innerHTML = "<i></i>";
      b.setAttribute("aria-label", pontos.dataset.ver + " " + (k + 1) + ": " + itens[k].titulo);
      b.addEventListener("click", (function (idx) { return function () { var delta = wrap(idx - pos + n / 2, n) - n / 2; ir(pos + delta); }; })(k));
      pontos.appendChild(b);
    }
    function medir() {
      var w = palco.clientWidth, cw = Math.min(220, Math.max(150, innerWidth * .14));
      espaco = cw + Math.min(48, w * .03);
      anel.style.setProperty("--cw", cw + "px"); anel.style.setProperty("--ch", (cw * 2.16) + "px");
      var qtd = (Math.ceil(w / (2 * espaco)) + 2) * 2 + 1;
      if (qtd !== cartoes.length) {
        anel.innerHTML = ""; cartoes = [];
        for (var i = 0; i < qtd; i++) {
          var c = document.createElement("article"); c.className = "orb-cartao"; c.setAttribute("aria-roledescription", "slide");
          c.innerHTML = '<div class="orb-tela"></div><div class="orb-refl" aria-hidden="true"></div>';
          anel.appendChild(c); cartoes.push(c);
        }
      }
      desenha();
    }
    function desenha() {
      var centro = Math.round(pos), raio = Math.floor(cartoes.length / 2), w = palco.clientWidth;
      cartoes.forEach(function (c, i) {
        var slot = centro + wrap(i - centro + raio, cartoes.length) - raio, idx = wrap(slot, n);
        if (c.dataset.slot !== String(slot)) {
          c.dataset.slot = slot; c.firstChild.innerHTML = itens[idx].tela; c.lastChild.innerHTML = itens[idx].tela;
          c.lastChild.querySelectorAll("img").forEach(function (im) { im.alt = ""; });
          c.setAttribute("aria-label", (idx + 1) + " de " + n + ": " + itens[idx].titulo);
        }
        var meio = slot === centro; c.setAttribute("aria-hidden", meio ? "false" : "true"); c.inert = !meio;
        var x = (slot - pos) * espaco;
        if (Math.abs(x) > w / 2 + espaco) { c.style.visibility = "hidden"; return; }
        c.style.visibility = "";
        var cv = x / Math.max(w / 2, 500);
        c.style.transform = "translateX(-50%) translate3d(" + x.toFixed(1) + "px," + (-cv * cv * 18).toFixed(1) + "px," + (cv * cv * 120).toFixed(1) + "px) rotateY(" + (-cv * 16).toFixed(2) + "deg)";
      });
      var a = wrap(Math.round(pos), n);
      if (a !== ativo) {
        ativo = a; numEl.textContent = (a < 9 ? "0" : "") + (a + 1); tituloEl.textContent = itens[a].titulo;
        Array.prototype.forEach.call(pontos.children, function (b, k) { b.setAttribute("aria-current", k === a ? "true" : "false"); });
      }
    }
    function ir(v) {
      alvo = v; var a = wrap(Math.round(v), n); anuncio.textContent = (a + 1) + " de " + n + ": " + itens[a].titulo;
      if (parado() || matchMedia("(prefers-reduced-motion: reduce)").matches) { pos = alvo; alvo = null; desenha(); }
    }
    orb.querySelectorAll(".orb-seta").forEach(function (b) { b.addEventListener("click", function () { ir(Math.round(alvo == null ? pos : alvo) + Number(b.dataset.dir)); }); });
    palco.addEventListener("keydown", function (e) {
      if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
      e.preventDefault(); ir(Math.round(alvo == null ? pos : alvo) + (e.key === "ArrowRight" ? 1 : -1));
    });
    palco.addEventListener("pointerdown", function (e) {
      if (e.pointerType === "mouse" && e.button !== 0) return;
      arrastando = true; alvo = null; ultX = e.clientX; palco.classList.add("arrasta"); palco.setPointerCapture(e.pointerId);
    });
    palco.addEventListener("pointermove", function (e) { if (!arrastando) return; pos -= (e.clientX - ultX) / espaco; ultX = e.clientX; desenha(); });
    ["pointerup", "pointercancel", "lostpointercapture"].forEach(function (ev) {
      palco.addEventListener(ev, function () { if (!arrastando) return; arrastando = false; palco.classList.remove("arrasta"); ir(Math.round(pos)); });
    });
    function laco(t) {
      var dt = Math.min(50, t - (tAnt || t)); tAnt = t;
      if (alvo !== null && !arrastando && !document.hidden) {
        pos += (alvo - pos) * (1 - Math.exp(-dt / 150));
        if (Math.abs(alvo - pos) < .0005) { pos = alvo; alvo = null; }
        desenha();
      }
      requestAnimationFrame(laco);
    }
    addEventListener("resize", medir); medir(); requestAnimationFrame(laco);
  });
})();
