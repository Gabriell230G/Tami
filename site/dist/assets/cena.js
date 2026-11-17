/* cena.js · camada "show" do site da Tami (WebGL puro, sem bibliotecas).
   Só roda no modo show. O conteúdo real está todo no HTML; esta camada é enfeite com sentido.
   Céu (gradiente da cena) + bolhas que vão do caos à ordem + rosto da Tami em volume, desenhado
   por campo de distância a partir da geometria congelada da marca (viewBox 320 x 160). */
(function () {
  "use strict";
  var doc = document.documentElement;
  if (!doc.classList.contains("show")) return;

  var cv = document.createElement("canvas");
  cv.className = "ceu";
  cv.setAttribute("aria-hidden", "true");
  cv.setAttribute("role", "presentation");
  var gl = cv.getContext("webgl", { antialias: false, alpha: false, powerPreference: "low-power", preserveDrawingBuffer: false });
  if (!gl) { window.TamiModo && window.TamiModo("leve", "sem-webgl"); return; }
  document.body.prepend(cv);
  cv.addEventListener("webglcontextlost", function (e) { e.preventDefault(); parar(); window.TamiModo && window.TamiModo("leve", "contexto"); });

  var VS = "attribute vec2 p;void main(){gl_Position=vec4(p,0.,1.);}";
  var FS = [
    "precision mediump float;",
    "uniform vec2 R;uniform float T;uniform vec3 cTop,cBot,cGlow;uniform vec2 F;uniform float FS;",
    "uniform float EA,EB,EM,ORD,BLOB,FACE;uniform vec2 M;uniform vec2 BP[6];uniform vec3 BC[6];uniform float BR[6];",
    "float seg(vec2 p,vec2 a,vec2 b,float r){vec2 pa=p-a,ba=b-a;float h=clamp(dot(pa,ba)/dot(ba,ba),0.,1.);return length(pa-ba*h)-r;}",
    // arco de meia volta: s=1 aberto para baixo na tela (sorriso), s=-1 para cima (olho fechado)
    "float arc(vec2 p,vec2 c,float ra,float r,float s){vec2 q=p-c;q.y*=s;if(q.y>=0.)return abs(length(q)-ra)-r;return min(length(q-vec2(ra,0.)),length(q+vec2(ra,0.)))-r;}",
    "float hdisc(vec2 p,vec2 c,float ra){vec2 q=p-c;float d=max(length(q)-ra,-q.y);return d-5.;}",
    "float eyes(vec2 p,float e){vec2 g=vec2(M.x*6.,M.y*4.);",
    " if(e<.5)return min(seg(p,vec2(32,44)+g,vec2(32,108)+g,24.),seg(p,vec2(288,44)+g,vec2(288,108)+g,24.));",
    " if(e<2.5)return min(arc(p,vec2(38,84),26.,17.,-1.),arc(p,vec2(282,84),26.,17.,-1.));",
    " if(e<3.5)return min(seg(p,vec2(32,38)+g,vec2(32,114)+g,24.),seg(p,vec2(288,38)+g,vec2(288,114)+g,24.));",
    " if(e<4.5)return min(seg(p,vec2(32,30)+g,vec2(32,82)+g,24.),seg(p,vec2(288,30)+g,vec2(288,82)+g,24.));",
    " if(e<5.5)return min(arc(p,vec2(40,92),26.,15.,-1.),arc(p,vec2(280,92),26.,15.,-1.));",
    " if(e<6.5)return min(seg(p,vec2(32,60)+g,vec2(32,108)+g,24.),seg(p,vec2(288,60)+g,vec2(288,108)+g,24.));",
    " return min(seg(p,vec2(44,44),vec2(44,108),24.),seg(p,vec2(296,44),vec2(296,108),24.));}",
    "float mouth(vec2 p,float e){",
    " if(e<1.5)return arc(p,vec2(160,82),37.,18.,1.);",
    " if(e<2.5)return hdisc(p,vec2(160,94),32.);",
    " if(e<3.5)return seg(p,vec2(130,104),vec2(190,104),16.);",
    " if(e<4.5)return seg(p,vec2(134,116),vec2(186,116),15.);",
    " if(e<5.5)return arc(p,vec2(160,90),30.,16.,1.);",
    " if(e<6.5)return arc(p,vec2(160,88),26.,15.,1.);",
    " return arc(p,vec2(180,82),37.,18.,1.);}",
    "float face2(vec2 p,float e){return min(eyes(p,e),mouth(p,e));}",
    "float face3(vec3 p){float d2=mix(face2(p.xy,EA),face2(p.xy,EB),EM);vec2 w=vec2(d2+9.,abs(p.z)-6.);return min(max(w.x,w.y),0.)+length(max(w,0.))-9.;}",
    "vec3 nrm(vec3 p){vec2 e=vec2(.6,0.);return normalize(vec3(face3(p+e.xyy)-face3(p-e.xyy),face3(p+e.yxy)-face3(p-e.yxy),face3(p+e.yyx)-face3(p-e.yyx)));}",
    "mat3 rot(vec2 a){float cx=cos(a.y),sx=sin(a.y),cy=cos(a.x),sy=sin(a.x);return mat3(cy,0.,-sy,0.,1.,0.,sy,0.,cy)*mat3(1.,0.,0.,0.,cx,sx,0.,-sx,cx);}",
    "float hash(vec2 p){return fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453);}",
    "void main(){vec2 fc=gl_FragCoord.xy;vec2 uv=fc/R;",
    " float g=smoothstep(0.,1.,uv.y);vec3 col=mix(cBot,cTop,g);",
    " col+=cGlow*.35*exp(-3.*length((uv-vec2(.72,.62))*vec2(R.x/R.y,1.)));",
    // bolhas: campo de metabolas com volume falso
    " if(BLOB>.01){float f=0.;vec3 bc=vec3(0.);vec2 grad=vec2(0.);for(int i=0;i<6;i++){vec2 d=fc-BP[i];float r2=BR[i]*BR[i];float k=r2/(dot(d,d)+1.);f+=k;bc+=BC[i]*k;grad+=-2.*d*k*k/max(r2,1e-4);}",
    "  bc/=max(f,1e-3);float m=smoothstep(.85,1.05,f);vec2 g2=-grad*R.y*.02;g2/=1.+length(g2)*.7;vec3 n=normalize(vec3(g2,1.));float l=clamp(dot(n,normalize(vec3(-.4,.6,.7))),0.,1.);",
    "  vec3 bcol=bc*(.62+.48*l)+pow(l,24.)*.55+cGlow*.12*(1.-n.z);col=mix(col,bcol,m*BLOB);col+=bc*.10*BLOB*smoothstep(.25,.85,f)*(1.-m);}",
    // rosto em volume, só dentro da caixa dele
    " vec2 q=(fc-F)/FS*320.;q.y=-q.y;q+=vec2(160.,80.);",
    " if(FACE>.01&&q.x>-60.&&q.x<380.&&q.y>-70.&&q.y<230.){mat3 rm=rot(vec2(M.x*.22,M.y*.14));",
    "  vec3 ro=rm*vec3(q.x-160.,q.y-80.,-200.)+vec3(160.,80.,0.);vec3 rd=rm*vec3(0.,0.,1.);float t=150.,hit=0.;",
    "  for(int i=0;i<44;i++){vec3 pp=ro+rd*t;float d=face3(pp);if(d<.4){hit=1.;break;}t+=d;if(t>260.)break;}",
    "  if(hit>.5){vec3 pp=ro+rd*t;vec3 n=nrm(pp);vec3 L=normalize(vec3(-.5,-.7,-.8));float dif=clamp(dot(n,L),0.,1.);",
    "   float fr=pow(1.-clamp(-dot(n,rd),0.,1.),3.);float claro=step(.6,dot(cBot,vec3(.3,.59,.11)));vec3 base=mix(vec3(1.),vec3(.247,.29,.796),claro);",
    "   vec3 sh=mix(mix(cBot,vec3(.247,.29,.796),.55),vec3(.106,.122,.29),claro);vec3 c=mix(sh,base,.32+.68*dif);c+=vec3(.714,.925,.047)*fr*(.55-.25*claro);c+=pow(clamp(dot(reflect(rd,n),L),0.,1.),24.)*.45;",
    "   col=mix(col,c,FACE);}",
    "  else{float d=mix(face2(q,EA),face2(q,EB),EM);float px=320./FS;float claro=step(.6,dot(cBot,vec3(.3,.59,.11)));",
    "   vec3 borda=mix(mix(vec3(.85,.88,1.),vec3(.247,.29,.796),claro),cGlow,.25);",
    "   col=mix(col,cBot*.55,FACE*.30*exp(-max(d,0.)*.05)*step(0.,d));col=mix(col,borda,FACE*(1.-smoothstep(0.,px*1.6,d)));}}",
    " col+=(hash(fc+T)-.5)*.035;gl_FragColor=vec4(col,1.);}"
  ].join("\n");

  function sh(t, s) { var o = gl.createShader(t); gl.shaderSource(o, s); gl.compileShader(o); if (!gl.getShaderParameter(o, gl.COMPILE_STATUS)) { throw new Error(gl.getShaderInfoLog(o)); } return o; }
  var pg;
  try {
    pg = gl.createProgram(); gl.attachShader(pg, sh(gl.VERTEX_SHADER, VS)); gl.attachShader(pg, sh(gl.FRAGMENT_SHADER, FS)); gl.linkProgram(pg);
    if (!gl.getProgramParameter(pg, gl.LINK_STATUS)) throw new Error("link");
  } catch (e) { cv.remove(); window.TamiModo && window.TamiModo("leve", "shader"); return; }
  gl.useProgram(pg);
  var b = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, b);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  var loc = gl.getAttribLocation(pg, "p"); gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
  var U = {}; ["R", "T", "cTop", "cBot", "cGlow", "F", "FS", "EA", "EB", "EM", "ORD", "BLOB", "FACE", "M", "BP", "BC", "BR"].forEach(function (k) { U[k] = gl.getUniformLocation(pg, k); });

  /* ---------- paletas (só tokens da marca) ---------- */
  function hx(h) { return [parseInt(h.substr(1, 2), 16) / 255, parseInt(h.substr(3, 2), 16) / 255, parseInt(h.substr(5, 2), 16) / 255]; }
  var PAL = {
    noite: ["#2B3290", "#1B1F4A", "#6E78E3"], susto: ["#1B1F4A", "#0E1130", "#3F4ACB"], azul: ["#3F4ACB", "#2B3290", "#B6EC0C"],
    fundo: ["#1B1F4A", "#2B3290", "#5560D6"], nevoa: ["#FFFFFF", "#EEF0FF", "#C9CDF5"], calmo: ["#FFFFFF", "#F4F5FF", "#E3E6FB"],
    aurora: ["#F7FCE4", "#EEF0FF", "#B6EC0C"], limao: ["#B6EC0C", "#B6EC0C", "#F7FCE4"], marinho: ["#1B1F4A", "#1B1F4A", "#2B3290"]
  };
  var EXP = { oi: 0, pisca: 1, feliz: 2, firme: 3, pensa: 4, acolhe: 5, ouve: 6, olha: 7 };
  var DEST = ["#D63B40", "#F79009", "#7CC4F2", "#B7EBC8"];
  var NUVEM = ["#3F4ACB", "#5560D6", "#2B3290", "#3540B8", "#5560D6", "#2B3290"];

  var cenas = [].slice.call(document.querySelectorAll("[data-cena]"));
  var portais = [].slice.call(document.querySelectorAll("[data-portal]"));
  var st = { top: hx(PAL.noite[0]), bot: hx(PAL.noite[1]), glow: hx(PAL.noite[2]), fx: innerWidth * .7, fy: innerHeight * .5, fs: 300, face: 1, blob: 1,
             ea: 0, eb: 0, em: 0, mx: 0, my: 0, tmx: 0, tmy: 0, bp: [], bc: [], br: [], ativo: -1 };
  for (var i = 0; i < 6; i++) { st.bp.push([innerWidth * Math.random(), innerHeight * Math.random()]); st.bc.push(hx(NUVEM[i])); st.br.push(60); }

  var escala = 1, W = 0, H = 0;
  function tam() {
    var dpr = Math.min(window.devicePixelRatio || 1, 1.5);
    escala = (innerWidth < 700 ? .66 : .85) * dpr;
    W = Math.round(innerWidth * escala); H = Math.round(innerHeight * escala);
    cv.width = W; cv.height = H; gl.viewport(0, 0, W, H);
  }
  tam(); addEventListener("resize", tam, { passive: true });

  addEventListener("pointermove", function (e) {
    if (e.pointerType !== "mouse") return;
    st.tmx = Math.max(-1, Math.min(1, (e.clientX - st.fx) / (innerWidth * .5)));
    st.tmy = Math.max(-1, Math.min(1, (e.clientY - st.fy) / (innerHeight * .5)));
  }, { passive: true });
  portais.forEach(function (p) {
    var bt = p.closest("button,a") || p;
    ["pointerenter", "focus"].forEach(function (ev) { bt.addEventListener(ev, function () { st.ativo = p; }); });
    ["pointerleave", "blur"].forEach(function (ev) { bt.addEventListener(ev, function () { if (st.ativo === p) st.ativo = -1; }); });
  });
  // cada cena usa só os portais dela (a primeira tela e a cena dos caminhos têm os seus)
  cenas.forEach(function (c) { c.__portais = [].slice.call(c.querySelectorAll("[data-portal]")); });

  /* ---------- piscar: no máximo 2 vezes nos primeiros 5 s; depois, só quando a cena muda ---------- */
  var piscadas = 0, piscaAte = 0, t0 = performance.now(), exprAtual = null;
  function pisca(agora) { if (agora - t0 < 5000 && piscadas >= 2) return; piscadas++; piscaAte = agora + 170; }
  setTimeout(function () { pisca(performance.now()); }, 1400);
  setTimeout(function () { pisca(performance.now()); }, 3600);

  function lerp(a, b, k) { return a + (b - a) * k; }
  function mix3(a, b, k) { return [lerp(a[0], b[0], k), lerp(a[1], b[1], k), lerp(a[2], b[2], k)]; }

  /* ---------- lê a cena atual a partir da rolagem (a rolagem é a do navegador) ---------- */
  function ler() {
    var meio = innerHeight * .5, atual = cenas[0], prox = null, k = 0;
    for (var j = 0; j < cenas.length; j++) {
      var r = cenas[j].getBoundingClientRect();
      if (r.top <= meio && r.bottom > meio) { atual = cenas[j]; prox = cenas[j + 1] || null; k = r.bottom - meio < innerHeight * .35 ? 1 - (r.bottom - meio) / (innerHeight * .35) : 0; break; }
    }
    return { a: atual, b: prox, k: k };
  }

  var semVigia = /[?&]vigia=nao/.test(location.search), ultimaAssin = "";
  var ultimo = performance.now(), lentos = 0, rodando = true, quadros = 0, somaDt = 0;
  function quadro(agora) {
    if (!rodando) return;
    var dt = Math.min(64, agora - ultimo); ultimo = agora;
    // vigia de desempenho: abaixo de 30 fps por 2 s ➜ modo leve
    if (!semVigia && agora - t0 > 2500) { if (dt > 34) lentos += dt; else lentos = Math.max(0, lentos - dt * .5); if (lentos > 2000) { parar(); window.TamiModo && window.TamiModo("leve", "fps"); return; } }
    quadros++; somaDt += dt; window.__tamiFps = 1000 / (somaDt / quadros);

    var c = ler(), A = c.a.dataset, B = c.b ? c.b.dataset : A, k = c.k * c.k * (3 - 2 * c.k);
    var pa = PAL[A.ceu] || PAL.noite, pb = PAL[B.ceu] || pa, sm = 1 - Math.pow(.001, dt / 1000);
    st.top = mix3(st.top, mix3(hx(pa[0]), hx(pb[0]), k), sm * 3);
    st.bot = mix3(st.bot, mix3(hx(pa[1]), hx(pb[1]), k), sm * 3);
    st.glow = mix3(st.glow, mix3(hx(pa[2]), hx(pb[2]), k), sm * 3);

    // rosto: segue o alvo da cena (ancorado nela) e muda de expressão
    var alvo = c.a.querySelector("[data-rosto-alvo]"), ex = EXP[A.rosto || "oi"];
    if (alvo) {
      var ra = alvo.getBoundingClientRect();
      var vis = ra.bottom > 60 && ra.top < innerHeight && !document.documentElement.classList.contains("voando");
      st.fx = lerp(st.fx, ra.left + ra.width / 2, Math.min(1, sm * 9)); st.fy = lerp(st.fy, ra.top + ra.height / 2, Math.min(1, sm * 9)); st.fs = lerp(st.fs, ra.width, Math.min(1, sm * 6));
      st.face = lerp(st.face, vis ? 1 : 0, sm * 4);
    } else st.face = lerp(st.face, 0, sm * 4);
    if (exprAtual !== ex) { st.ea = (exprAtual === null ? ex : st.eb); st.eb = ex; st.em = exprAtual === null ? 1 : 0; exprAtual = ex; if (agora - t0 > 5000) pisca(agora); }
    st.em = Math.min(1, st.em + dt / 320);
    var ea = st.ea, eb = st.eb, em = st.em;
    if (agora < piscaAte && (eb === 0 || eb === 3 || eb === 4 || eb === 6 || eb === 7)) { ea = eb; eb = 1; em = Math.sin(Math.PI * (1 - (piscaAte - agora) / 170)); }
    st.mx = lerp(st.mx, st.tmx, sm * 2); st.my = lerp(st.my, st.tmy, sm * 2);

    // bolhas: caos (cena 2) ➜ ordem (portais da cena 4) ➜ somem nas cenas calmas
    var ordem = A.bolhas === "portais", calmo = A.bolhas === "nao" || !A.bolhas, tt = agora / 1000;
    st.blob = lerp(st.blob, calmo ? 0 : 1, sm * 2);
    var pc = c.a.__portais || [], rp = pc.map(function (p) { return p.getBoundingClientRect(); });
    for (var n = 0; n < 6; n++) {
      var tx, ty, tr, tc;
      if (ordem && rp[n % 4] && n < 4) { var q = rp[n]; tx = q.left + q.width / 2; ty = q.top + q.height / 2; tr = q.width * (st.ativo === pc[n] ? .62 : .5); tc = hx(DEST[n]); }
      else {
        var fase = n * 1.7, amp = A.bolhas === "caos" ? 1 : .5;
        tx = innerWidth * (.5 + .42 * amp * Math.sin(tt * (.13 + n * .03) + fase)); ty = innerHeight * (.5 + .38 * amp * Math.cos(tt * (.11 + n * .025) + fase * 1.3));
        tr = Math.min(innerWidth, innerHeight) * (A.bolhas === "caos" ? .09 + .025 * n : .07); tc = hx(NUVEM[n]);
        if (ordem) tr = 0;
      }
      var kb = ordem ? Math.min(1, sm * 14) : sm * 1.5; st.bp[n][0] = lerp(st.bp[n][0], tx, kb); st.bp[n][1] = lerp(st.bp[n][1], ty, kb);
      st.br[n] = lerp(st.br[n], tr, sm * 1.5); st.bc[n] = mix3(st.bc[n], tc, sm * 2);
    }

    var assin = [st.top[0], st.bot[0], st.glow[0], st.fx, st.fy, st.fs, st.face, st.blob, ea, eb, em, st.mx, st.my, W].map(function (v) { return Math.round(v * 400); }).join(",");
    if (st.blob < .01 && assin === ultimaAssin) { requestAnimationFrame(quadro); return; }
    ultimaAssin = assin;
    gl.uniform2f(U.R, W, H); gl.uniform1f(U.T, tt % 100);
    gl.uniform3fv(U.cTop, st.top); gl.uniform3fv(U.cBot, st.bot); gl.uniform3fv(U.cGlow, st.glow);
    gl.uniform2f(U.F, st.fx * escala, (innerHeight - st.fy) * escala); gl.uniform1f(U.FS, st.fs * escala);
    gl.uniform1f(U.EA, ea); gl.uniform1f(U.EB, eb); gl.uniform1f(U.EM, em);
    gl.uniform1f(U.BLOB, st.blob); gl.uniform1f(U.FACE, st.face); gl.uniform2f(U.M, st.mx, st.my);
    var bp = [], bc = [], br = [];
    for (n = 0; n < 6; n++) { bp.push(st.bp[n][0] * escala, (innerHeight - st.bp[n][1]) * escala); bc.push.apply(bc, st.bc[n]); br.push(st.br[n] * escala); }
    gl.uniform2fv(U.BP, bp); gl.uniform3fv(U.BC, bc); gl.uniform1fv(U.BR, br);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
    requestAnimationFrame(quadro);
  }
  function parar() { rodando = false; cv.remove(); }
  window.TamiCena = { parar: parar, fps: function () { return window.__tamiFps || 0; } };
  doc.classList.add("ceu-ok");
  requestAnimationFrame(quadro);
})();
