/* tami.js · comportamento do site da Tami (sem bibliotecas). Tudo funciona sem ele; ele só melhora. */
(function () {
  "use strict";
  var d = document.documentElement, raiz = d.dataset.raiz || "./";
  function guarda(v) { try { localStorage.setItem("tami-mov", v); } catch (e) { /* navegador sem armazenamento: vale só nesta página */ } }

  /* ---------- troca de modo (show, leve, parado) ---------- */
  window.TamiModo = function (m, motivo) {
    ["show", "leve", "parado", "ceu-ok"].forEach(function (c) { d.classList.remove(c); });
    d.classList.add(m); d.dataset.modo = m; if (motivo) d.dataset.motivo = motivo;
    if (motivo && m === "leve") avisa(d.dataset.avisoLeve);
    if (m !== "show" && window.TamiCena) window.TamiCena.parar();
    atualizaBotao();
    if (m === "show") carregaCena();
  };

  /* ---------- aviso curto quando o site troca de modo sozinho ---------- */
  function avisa(txt) {
    var a = document.getElementById("aviso-modo"); if (!a || !txt) return;
    a.textContent = txt; a.hidden = false; clearTimeout(avisa.t); avisa.t = setTimeout(function () { a.hidden = true; }, 5000);
  }

  /* ---------- botão "Pausar movimento" (vale para o site inteiro) ---------- */
  var bm = document.querySelector("[data-mov]");
  function atualizaBotao() {
    if (!bm) return;
    var parado = d.dataset.modo === "parado";
    bm.setAttribute("aria-pressed", parado ? "true" : "false");
    bm.querySelector("span").textContent = parado ? bm.dataset.ligar : bm.dataset.pausar;
  }
  if (bm) {
    bm.hidden = false;
    bm.addEventListener("click", function () {
      if (d.dataset.modo === "parado") { var volta = d.dataset.antes || d.dataset.auto || "leve"; guarda(volta); window.TamiModo(volta); }
      else { d.dataset.antes = d.dataset.modo; guarda("parado"); window.TamiModo("parado"); }
    });
    atualizaBotao();
  }

  /* ---------- camada show: só depois do conteúdo, quando o navegador está livre ---------- */
  var cenaPedida = false;
  function carregaCena() {
    if (cenaPedida || d.dataset.modo !== "show") return; cenaPedida = true;
    var s = document.createElement("script"); s.src = raiz + "assets/cena.js"; s.async = true; document.body.appendChild(s);
  }
  function quandoLivre(f) { if ("requestIdleCallback" in window) requestIdleCallback(f, { timeout: 1500 }); else setTimeout(f, 300); }
  if (document.readyState === "complete") quandoLivre(carregaCena); else addEventListener("load", function () { quandoLivre(carregaCena); });

  /* ---------- menu do celular ---------- */
  var menu = document.querySelector(".menu-cel");
  if (menu) document.addEventListener("keydown", function (e) { if (e.key === "Escape" && menu.open) { menu.open = false; menu.querySelector("summary").focus(); } });

  /* ---------- título cinético: letras sobem uma vez; o leitor de tela lê a frase inteira ---------- */
  [].forEach.call(document.querySelectorAll("[data-cinetico]"), function (h) {
    if (d.dataset.modo === "parado") return;
    var txt = h.textContent.trim(), i = 0;
    var vis = document.createElement("span"); vis.setAttribute("aria-hidden", "true"); vis.className = "letras";
    txt.split(/(?<=[.?!])\s+/).forEach(function (frase) {
      var linha = document.createElement("span"); linha.className = "linha-t";
      var pals = frase.split(" ");
      pals.forEach(function (pal, k) {
        var w = document.createElement("span"); w.className = "pal";
        pal.split("").forEach(function (ch) { var l = document.createElement("span"); l.textContent = ch; l.style.setProperty("--i", i++); w.appendChild(l); });
        linha.appendChild(w); if (k < pals.length - 1) linha.appendChild(document.createTextNode(" "));
      });
      vis.appendChild(linha);
    });
    var sr = document.createElement("span"); sr.className = "sr"; sr.textContent = txt;
    h.textContent = ""; h.appendChild(sr); h.appendChild(vis); h.classList.add("cinetico");
  });

  /* ---------- conversa de exemplo: acompanha a rolagem (texto sempre no HTML) ---------- */
  var conv = document.querySelector("[data-conversa]");
  if (conv) {
    var msgs = [].slice.call(conv.querySelectorAll("[data-passo]")), cena = conv.closest("[data-cena]");
    var exprs = (cena && cena.dataset.rostos || "").split(",");
    conv.classList.add("viva");
    var passoAnt = -1;
    var rolar = function () {
      if (d.dataset.modo === "parado") { msgs.forEach(function (m) { m.classList.add("on"); }); return; }
      var passo;
      if (getComputedStyle(conv.closest(".fixo") || conv).position !== "sticky") {
        // celular: a conversa fica no fluxo; cada mensagem aparece quando entra na tela
        passo = 1; msgs.forEach(function (m, k) { if (k && m.getBoundingClientRect().top < innerHeight * .9) passo++; });
      } else {
        var r = cena.getBoundingClientRect(), total = r.height - innerHeight;
        var p = total > 0 ? Math.min(1, Math.max(0, -r.top / total)) : 1;
        passo = 1 + Math.floor(p * (msgs.length - 1 + .6));
      }
      if (passo === passoAnt) return; passoAnt = passo;
      msgs.forEach(function (m, k) { m.classList.toggle("on", k < passo); });
      if (exprs.length) cena.dataset.rosto = exprs[Math.min(exprs.length - 1, Math.max(0, passo - 1))];
    };
    addEventListener("scroll", rolar, { passive: true }); rolar();
  }

  /* ---------- quatro caminhos: tocar mostra a frase ---------- */
  [].forEach.call(document.querySelectorAll("[data-caminho]"), function (b) {
    var alvo = document.getElementById(b.getAttribute("aria-controls"));
    if (!alvo) return;
    b.setAttribute("aria-expanded", "false"); alvo.hidden = true;
    b.addEventListener("click", function () { var on = b.getAttribute("aria-expanded") !== "true"; b.setAttribute("aria-expanded", on ? "true" : "false"); alvo.hidden = !on; });
  });

  /* ---------- copiar texto (imprensa): [data-copiar="#id"] ou [data-copiar-texto="..."] ---------- */
  [].forEach.call(document.querySelectorAll("[data-copiar], [data-copiar-texto]"), function (bt) {
    bt.addEventListener("click", function () {
      var alvo = bt.dataset.copiar && document.querySelector(bt.dataset.copiar), txt = alvo ? alvo.textContent : bt.dataset.copiarTexto;
      var feito = function () { var antes = bt.textContent; bt.textContent = bt.dataset.ok; setTimeout(function () { bt.textContent = antes; }, 1800); };
      if (navigator.clipboard) navigator.clipboard.writeText(txt).then(feito, function () {}); else feito();
    });
  });

  /* ---------- Como funciona: a linha do tempo se enche conforme a leitura ---------- */
  var linha = document.querySelector("[data-linha]");
  if (linha) {
    var enche = function () {
      if (d.dataset.modo === "parado") { linha.style.setProperty("--cf", 1); return; }
      var r = linha.getBoundingClientRect(), v = (innerHeight * .55 - r.top) / r.height;
      linha.style.setProperty("--cf", Math.max(0, Math.min(1, v)).toFixed(3));
    };
    addEventListener("scroll", enche, { passive: true }); addEventListener("resize", enche); enche();
  }

  /* ---------- Perguntas: a busca filtra na hora (sem acento e sem maiúscula) ---------- */
  var pq = document.getElementById("pq-q");
  if (pq) {
    var limpa = function (x) { return x.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, ""); };
    var dts = [].slice.call(document.querySelectorAll(".pq details")), nada = document.querySelector(".pq-nada");
    document.querySelector(".pq-busca").hidden = false;
    pq.addEventListener("input", function () {
      var q = limpa(pq.value.trim()), n = 0;
      dts.forEach(function (dt) { var ok = !q || limpa(dt.textContent).indexOf(q) >= 0; dt.hidden = !ok; if (ok) n++; if (q && ok) dt.open = true; if (!q) dt.open = false; });
      nada.hidden = n > 0;
    });
  }

  /* ---------- revelar ao rolar: [data-revela] ganha .visto quando entra na tela ---------- */
  var rev = document.querySelectorAll("[data-revela]");
  if (rev.length) {
    if ("IntersectionObserver" in window) {
      var ior = new IntersectionObserver(function (es) { es.forEach(function (e) { if (e.isIntersecting) { e.target.classList.add("visto"); ior.unobserve(e.target); } }); }, { rootMargin: "0px 0px -12% 0px" });
      [].forEach.call(rev, function (el) { ior.observe(el); });
    } else [].forEach.call(rev, function (el) { el.classList.add("visto"); });
  }

  /* ---------- "tami" do rodapé: o líquido balança para o lado do mouse ---------- */
  var ml = document.querySelector(".ml-inclina"), rod = document.querySelector(".rodape");
  if (ml && rod) rod.addEventListener("pointermove", function (e) {
    if (d.dataset.modo === "parado") return;
    var r = rod.getBoundingClientRect(); ml.style.setProperty("--incl", (((e.clientX - r.left) / r.width - .5) * -8).toFixed(2) + "deg");
  }, { passive: true });
  if (rod) rod.addEventListener("pointerleave", function () { ml && ml.style.setProperty("--incl", "0deg"); });

  /* ---------- corpo: o desenho completo só chega perto da cena ---------- */
  var corpo = document.querySelector("[data-corpo]");
  if (corpo && "IntersectionObserver" in window && "fetch" in window) {
    var io = new IntersectionObserver(function (es) {
      if (!es[0].isIntersecting) return; io.disconnect();
      fetch(corpo.dataset.corpo).then(function (r) { return r.ok ? r.text() : null; }).then(function (svg) {
        if (!svg || svg.indexOf("<svg") < 0) return;
        var box = document.createElement("div"); box.className = "corpo-vivo"; box.setAttribute("role", "group"); box.setAttribute("aria-label", "Corpo de mulher, de frente. Toque onde dói.");
        box.innerHTML = svg.slice(svg.indexOf("<svg"));
        [].forEach.call(box.querySelectorAll("path"), function (p) { try { var L = p.getTotalLength(); p.style.setProperty("--len", Math.ceil(L)); } catch (e) {} });
        corpo.replaceChildren(box); corpo.classList.add("desenha");
        // tocar numa parte: marca a dor ali e mostra o balão ("Dor no ombro direito")
        var sv = box.querySelector("svg"), dores = [].slice.call(box.querySelectorAll("[data-dor]")), zonas = [].slice.call(box.querySelectorAll("[data-zona]"));
        var balao = document.createElement("p"); balao.className = "corpo-balao"; balao.setAttribute("aria-live", "polite"); box.appendChild(balao);
        var marca = function (k, anima) {
          var g = box.querySelector('[data-dor="' + k + '"]'); if (!g) return;
          dores.forEach(function (x) { x.style.display = x === g ? "" : "none"; });
          zonas.forEach(function (z) { z.setAttribute("aria-pressed", z.dataset.zona === k ? "true" : "false"); });
          var vb = sv.viewBox.baseVal, w = sv.getBoundingClientRect(), b = box.getBoundingClientRect();
          balao.style.left = (w.left - b.left + (g.dataset.x - vb.x) / vb.width * w.width) + "px";
          balao.style.top = (w.top - b.top + (g.dataset.y - vb.y) / vb.height * w.height) + "px";
          balao.textContent = g.dataset.texto;
          if (anima) { balao.classList.remove("pula"); void balao.offsetWidth; balao.classList.add("pula"); }
          box.dataset.marcado = k;
        };
        // cartão "Quanto dói aqui?": nível de 0 a 10 e como é a dor (só no site, nada é guardado)
        var ct = document.getElementById("dor-cartao"), estado = {}, atual = null;
        var niveis = ct ? ct.dataset.niveis.split("|") : [], faixa = function (n) { return n <= 3 ? "b1" : n <= 6 ? "b2" : "b3"; };
        var pinta = function () {
          if (!ct || !atual) return;
          var e = estado[atual], n = e.n;
          [].forEach.call(ct.querySelectorAll(".dn-n"), function (b) {
            var j = +b.dataset.n; b.className = "dn-n" + (n !== null && j <= n ? " " + faixa(j) : "");
            b.setAttribute("aria-pressed", j === n ? "true" : "false");
          });
          [].forEach.call(ct.querySelectorAll(".dn-t"), function (b) { b.setAttribute("aria-pressed", e.tipos.indexOf(b.textContent) >= 0 ? "true" : "false"); });
          ct.querySelector(".dn-valor").textContent = n === null ? "" : n + " · " + niveis[n];
          [].forEach.call(ct.querySelectorAll(".esc-cara"), function (c) { c.classList.toggle("on", n !== null && +c.dataset.n === n); });
          [].forEach.call(ct.querySelectorAll(".esc-rot"), function (c) { c.classList.toggle("on", n !== null && c.classList.contains(faixa(n))); });
          ct.querySelector(".dn-nome i").style.background = n === null ? "" : { b1: "var(--cor-accent)", b2: "#FFC24D", b3: "var(--cor-danger-light)" }[faixa(n)];
        };
        var resumo = function (k) {
          var z = box.querySelector('[data-zona="' + k + '"]'), e = estado[k], partes = [z.getAttribute("aria-label")];
          if (e && e.n !== null) partes.push(e.n);
          if (e && e.tipos.length) partes.push(e.tipos.join(", ").toLowerCase());
          return partes.join(" · ");
        };
        var abreCartao = function (k) {
          if (!ct) return;
          atual = k; estado[k] = estado[k] || { n: null, tipos: [] };
          ct.querySelector(".dn-nome span").textContent = box.querySelector('[data-zona="' + k + '"]').getAttribute("aria-label");
          ct.hidden = false; ct.classList.remove("pula"); void ct.offsetWidth; ct.classList.add("pula"); pinta();
          var r = ct.getBoundingClientRect(); if (r.bottom > innerHeight || r.top < 0) ct.scrollIntoView({ behavior: d.dataset.modo === "parado" ? "auto" : "smooth", block: "nearest" });
        };
        if (ct) {
          [].forEach.call(ct.querySelectorAll(".dn-n"), function (b) { b.addEventListener("click", function () { estado[atual].n = +b.dataset.n; pinta(); }); });
          [].forEach.call(ct.querySelectorAll(".dn-t"), function (b) {
            b.addEventListener("click", function () { var l = estado[atual].tipos, i = l.indexOf(b.textContent); if (i >= 0) l.splice(i, 1); else l.push(b.textContent); pinta(); });
          });
          ct.querySelector(".dn-pronto").addEventListener("click", function () {
            ct.hidden = true; balao.textContent = resumo(atual); balao.classList.remove("pula"); void balao.offsetWidth; balao.classList.add("pula");
            var z = box.querySelector('[data-zona="' + atual + '"]'); z && z.focus({ preventScroll: true });
          });
          ct.querySelector(".dn-tira").addEventListener("click", function () {
            var z = box.querySelector('[data-zona="' + atual + '"]');
            delete estado[atual]; ct.hidden = true; balao.textContent = ""; balao.hidden = true;
            dores.forEach(function (x) { x.style.display = "none"; }); zonas.forEach(function (y) { y.setAttribute("aria-pressed", "false"); });
            box.dataset.marcado = ""; z && z.focus({ preventScroll: true });
          });
        }
        var toca = function (k) { balao.hidden = false; marca(k, true); abreCartao(k); };
        zonas.forEach(function (z) {
          z.addEventListener("click", function () { toca(z.dataset.zona); });
          z.addEventListener("keydown", function (e) { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); toca(z.dataset.zona); } });
        });
        marca("barriga", false);
        addEventListener("resize", function () { if (box.dataset.marcado) { var txt = balao.textContent; marca(box.dataset.marcado, false); balao.textContent = txt; } }, { passive: true });
        [].forEach.call(document.querySelectorAll(".onde [data-so-js]"), function (x) { x.hidden = false; });
      }).catch(function () {});
    }, { rootMargin: "400px" });
    io.observe(corpo);
  }


  /* índice da política: marca a seção que está sendo lida */
  var indice = document.querySelector("[data-indice]");
  if (indice && "IntersectionObserver" in window) {
    var links = [].slice.call(indice.querySelectorAll("a[href^='#']")), visiveis = {};
    var marcaAtual = function () {
      var atual = null;
      links.forEach(function (a) { var id = a.getAttribute("href").slice(1); if (visiveis[id] && !atual) atual = id; });
      if (!atual) return;
      links.forEach(function (a) {
        var on = a.getAttribute("href") === "#" + atual;
        if (on) a.setAttribute("aria-current", "true"); else a.removeAttribute("aria-current");
        if (on && indice.scrollHeight > indice.clientHeight) {
          var r = a.offsetTop - indice.clientHeight / 2;
          indice.scrollTop = Math.max(0, r);
        }
      });
    };
    var spy = new IntersectionObserver(function (es) {
      es.forEach(function (e) { visiveis[e.target.id] = e.isIntersecting; });
      marcaAtual();
    }, { rootMargin: "-25% 0px -60% 0px" });
    links.forEach(function (a) { var el = document.getElementById(a.getAttribute("href").slice(1)); if (el) spy.observe(el); });
  }

})();
