/* Tami · app web. Sem framework: cada tela é uma função que monta o HTML e liga os botões.
   Todos os dados vêm da API em Java (/api/...), que lê e grava no banco. */
(function () {
  "use strict";

  const tela = document.getElementById("tela");
  const DESTINO = {
    SAMU_192: { titulo: "Ligue 192 agora", curto: "SAMU 192", rosto: "firme", cor: "#D63B40" },
    UPA_HOJE: { titulo: "Vá hoje a uma UPA", curto: "UPA hoje", rosto: "olha", cor: "#F79009" },
    UBS_EM_BREVE: { titulo: "Procure o posto de saúde nos próximos dias", curto: "UBS em breve", rosto: "oi", cor: "#7CC4F2" },
    CUIDADO_EM_CASA: { titulo: "Dá para cuidar em casa por enquanto", curto: "Cuidar em casa", rosto: "feliz", cor: "#B7EBC8" }
  };
  const ORDEM = ["SAMU_192", "UPA_HOJE", "UBS_EM_BREVE", "CUIDADO_EM_CASA"];
  const FAIXAS = ["Sem dor", "Fraco", "Fraco", "Fraco", "Médio", "Médio", "Médio", "Forte", "Forte", "Forte", "Muito forte"];

  // ---------------------------------------------------------------- estado
  const guardado = (k, v) => { try { if (v === undefined) return JSON.parse(localStorage.getItem("tami." + k)); if (v === null) localStorage.removeItem("tami." + k); else localStorage.setItem("tami." + k, JSON.stringify(v)); } catch (e) { return null; } };
  let usuario = guardado("usuario");
  let conversa = null;   // { id, pergunta, feitas: [], filtro, resultado, perfil }
  let preparo = null;    // o que foi escolhido em "Para quem"

  // ---------------------------------------------------------------- utilidades
  const esc = (s) => String(s == null ? "" : s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const num = (n) => Number(n).toLocaleString("pt-BR");
  const pct = (p, casas = 1) => p == null ? "-" : (100 * p).toLocaleString("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas }) + "%";
  const ic = (prop) => prop && prop.ic95 ? "IC 95%: " + pct(prop.ic95[0]) + " a " + pct(prop.ic95[1]) : "";
  const rosto = (expr, cor = "#FFFFFF", cls = "rosto") => `<svg class="${cls}" viewBox="0 0 320 160" style="color:${cor}" aria-hidden="true">${(window.ROSTOS || {})[expr] || ""}</svg>`;
  const cara = (n) => `<svg viewBox="0 0 100 100" aria-hidden="true">${window.CARAS[n]}</svg>`;
  const data = (s) => { if (!s) return ""; const d = new Date(s); return isNaN(d) ? s : d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }); };
  const ICONE = {
    lixo: '<svg viewBox="0 0 24 24"><path d="M4 7h16M9 7V4.5h6V7m-9 0 1 13h10l1-13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    lapis: '<svg viewBox="0 0 24 24"><path d="M4 20h4L19 9l-4-4L4 16z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
    fone: '<svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M6.6 2.6c.6-.5 1.5-.4 2 .2l2 2.6c.4.6.4 1.4-.1 1.9l-1.3 1.4a12.6 12.6 0 0 0 6.1 6.1l1.4-1.3c.5-.5 1.3-.5 1.9-.1l2.6 2c.6.5.7 1.4.2 2l-1.4 1.8c-.8 1-2.1 1.4-3.3 1.1C9.8 18.6 5.4 14.2 3.7 7.4c-.3-1.2.1-2.5 1.1-3.3z"/></svg>'
  };

  function avisar(msg) {
    const el = document.querySelector(".aviso-erro");
    el.textContent = msg;
    el.hidden = false;
    clearTimeout(avisar.t);
    avisar.t = setTimeout(() => (el.hidden = true), 4200);
  }

  // ---------------------------------------------------------------- API + painel "Por dentro"
  const log = document.querySelector(".log");
  const logLista = document.querySelector(".log-lista");

  /** O que cada chamada faz no banco (para o vídeo técnico e para quem quer entender). */
  function oQueFazNoBanco(metodo, caminho, corpo, resp) {
    const p = caminho.replace(/\?.*$/, "");
    if (p === "/api/entrar") return "SELECT * FROM T_TAMI_USUARIO WHERE DS_CONTATO = ?\n➜ se não existe: INSERT INTO T_TAMI_USUARIO (NM_USUARIO, DS_CONTATO, ...)";
    if (/consentimento$/.test(p)) return "UPDATE T_TAMI_USUARIO SET ST_CONSENT_SAUDE = '" + (corpo.saude ? "S" : "N") + "', DT_CONSENTIMENTO = ? WHERE ID_USUARIO = ?";
    if (p === "/api/filtro" || (p === "/api/conversas" && metodo === "POST")) {
      const f = resp && (resp.filtro || resp);
      let t = "";
      if (f && f.regiao) t += "Árvore: " + (f.caminho || []).join(" › ") + "\n";
      t += "SELECT DS_DESTINO, COUNT(DISTINCT CD_SINAL), COUNT(ID_EXPRESSAO)\n  FROM T_TAMI_SINAL LEFT JOIN T_TAMI_EXPRESSAO ...\n  WHERE CD_SINAL IN (SELECT CD_SINAL FROM T_TAMI_SINAL_REGIAO WHERE CD_REGIAO IN (?))\n  GROUP BY DS_DESTINO";
      if (f && f.regiao) t += "\n➜ " + num(f.sinaisTotal) + " sinais viram " + num(f.sinaisRegiao) + " · " + num(f.expressoesTotal) + " expressões viram " + num(f.expressoesRegiao);
      if (p === "/api/conversas") t += "\nINSERT INTO T_TAMI_CONVERSA (ID_USUARIO, NR_IDADE_MESES, ST_GUARDAR, DS_RELATO, CD_REGIAO, ...)" + (resp && resp.conversa ? " ➜ ID " + resp.conversa : "");
      return t;
    }
    if (/\/respostas$/.test(p)) {
      if (resp && resp.fim) return "Fim da conversa, numa transação só:\nUPDATE T_TAMI_CONVERSA SET DS_DESTINO = '" + resp.resultado.destino + "', DT_FIM = ?, NR_PERGUNTAS = ?\nINSERT INTO T_TAMI_RESPOSTA × " + resp.respondidas + " (se tiver permissão)\nINSERT INTO T_TAMI_REGRA_ACIONADA (as regras que explicam o destino)\nCOMMIT";
      return "Motor de regras (em memória): resposta " + (corpo.codigos || []).join(", ") + " ➜ próxima pergunta " + (resp && resp.pergunta ? resp.pergunta.id : "");
    }
    if (/\/dor$/.test(p)) return "INSERT INTO T_TAMI_DOR (ID_CONVERSA, CD_REGIAO, DS_LADO, NR_NIVEL) VALUES (?, ?, ?, " + corpo.nivel + ")";
    if (/^\/api\/dor\//.test(p)) return "UPDATE T_TAMI_DOR SET NR_NIVEL = " + corpo.nivel + " WHERE ID_DOR = ?";
    if (/avaliacao$/.test(p)) return resp && resp.atualizada ? "UPDATE T_TAMI_FEEDBACK SET DS_AVALIACAO = '" + corpo.avaliacao + "' WHERE ID_CONVERSA = ? (já existia)" : "UPDATE T_TAMI_FEEDBACK ... ➜ 0 linhas\nINSERT INTO T_TAMI_FEEDBACK (ID_CONVERSA, DS_AVALIACAO) VALUES (?, '" + corpo.avaliacao + "')";
    if (/historico$/.test(p)) return "SELECT * FROM T_TAMI_CONVERSA WHERE ID_USUARIO = ? AND DS_DESTINO IS NOT NULL\n  ORDER BY DT_INICIO DESC FETCH FIRST 50 ROWS ONLY";
    if (/\/pessoas/.test(p)) return { GET: "SELECT * FROM T_TAMI_PESSOA WHERE ID_USUARIO = ?", POST: "INSERT INTO T_TAMI_PESSOA (ID_USUARIO, NM_APELIDO, DS_RELACAO, NR_IDADE_MESES, ...)", PUT: "UPDATE T_TAMI_PESSOA SET NM_APELIDO = ?, NR_IDADE_MESES = ?, ... WHERE ID_PESSOA = ?", DELETE: "DELETE FROM T_TAMI_PESSOA WHERE ID_PESSOA = ? AND ID_USUARIO = ?" }[metodo];
    if (/\/conversas\/\d+$/.test(p) && metodo === "GET") return "SELECT * FROM T_TAMI_CONVERSA WHERE ID_CONVERSA = ?\nSELECT * FROM T_TAMI_RESPOSTA WHERE ID_CONVERSA = ? ORDER BY NR_ORDEM\nSELECT * FROM T_TAMI_DOR ... · SELECT * FROM T_TAMI_FEEDBACK ...";
    if (/\/conversas\/\d+$/.test(p) && metodo === "DELETE") return "DELETE FROM T_TAMI_CONVERSA WHERE ID_CONVERSA = ? (respostas, regras, dor e avaliação saem junto: ON DELETE CASCADE)";
    if (/^\/api\/usuarios\/\d+$/.test(p) && metodo === "DELETE") return "DELETE FROM T_TAMI_USUARIO WHERE ID_USUARIO = ?\n(pessoas, conversas, respostas e avaliações saem junto: ON DELETE CASCADE)";
    if (/\/dados$/.test(p)) return "SELECT em T_TAMI_USUARIO, T_TAMI_PESSOA, T_TAMI_CONVERSA, T_TAMI_RESPOSTA, T_TAMI_DOR e T_TAMI_FEEDBACK (exportar tudo)";
    if (p === "/api/estatisticas") return "SELECT DS_DESTINO, NR_PERGUNTAS, ... FROM T_TAMI_CONVERSA\nSELECT C.DS_DESTINO, F.DS_AVALIACAO, COUNT(*) FROM T_TAMI_FEEDBACK F JOIN T_TAMI_CONVERSA C ... GROUP BY ...\nSELECT ..., AVG(D.NR_NIVEL) FROM T_TAMI_DOR D JOIN T_TAMI_REGIAO R ... GROUP BY ...";
    if (p === "/api/demo/simular") return "INSERT em lote: T_TAMI_USUARIO, T_TAMI_CONVERSA, T_TAMI_REGRA_ACIONADA, T_TAMI_DOR, T_TAMI_FEEDBACK (pessoas simuladas)";
    if (p === "/api/regioes") return "SELECT CD_REGIAO, CD_REGIAO_PAI, NM_REGIAO FROM T_TAMI_REGIAO (montada como árvore no Java)";
    return null;
  }

  function registrar(metodo, caminho, status, ms, corpo, resp) {
    const li = document.createElement("li");
    const sql = oQueFazNoBanco(metodo, caminho, corpo || {}, resp);
    li.innerHTML = `<div class="l1"><span class="m m-${metodo}">${metodo}</span><span>${esc(caminho)}</span><span class="st ${status >= 400 ? "erro" : ""}">${status} · ${ms} ms</span></div>`
      + (sql ? `<div class="sql">${esc(sql)}</div>` : "") + (status >= 400 && resp && resp.erro ? `<div class="nota">${esc(resp.erro)}</div>` : "");
    logLista.appendChild(li);
    logLista.scrollTop = logLista.scrollHeight;
  }

  async function api(metodo, caminho, corpo) {
    const t0 = performance.now();
    let r, json = null;
    try {
      const cab = corpo ? { "Content-Type": "application/json" } : {};
      if (usuario && usuario.token) cab["X-Tami-Token"] = usuario.token;
      r = await fetch(caminho, { method: metodo, headers: cab, body: corpo ? JSON.stringify(corpo) : undefined });
      json = await r.json().catch(() => null);
    } catch (e) {
      registrar(metodo, caminho, 0, Math.round(performance.now() - t0), corpo);
      avisar("Sem conexão com o servidor da Tami. Ele está rodando?");
      throw e;
    }
    registrar(metodo, caminho, r.status, Math.round(performance.now() - t0), corpo, json);
    if (r.status === 401) { usuario = null; guardado("usuario", null); avisar((json && json.erro) || "Entre de novo."); ir("#/entrar"); throw new Error("401"); }
    if (!r.ok) { avisar((json && json.erro) || "Algo deu errado."); throw new Error(json && json.erro); }
    return json;
  }

  document.querySelector("[data-por-dentro]").addEventListener("click", (e) => {
    const on = e.currentTarget.getAttribute("aria-pressed") !== "true";
    e.currentTarget.setAttribute("aria-pressed", String(on));
    log.hidden = !on;
    guardado("porDentro", on);
  });
  document.querySelector("[data-limpar-log]").addEventListener("click", () => (logLista.innerHTML = ""));
  if (guardado("porDentro")) { log.hidden = false; document.querySelector("[data-por-dentro]").setAttribute("aria-pressed", "true"); }
  let local = false;
  api("GET", "/api/status").then((s) => { document.querySelector("[data-banco]").textContent = s.banco; local = !!s.local; }).catch(() => {});

  // ---------------------------------------------------------------- rotas
  const rotas = [
    [/^#?\/?$/, inicio], [/^#\/entrar$/, entrar], [/^#\/combinado$/, combinado], [/^#\/para-quem$/, paraQuem], [/^#\/contar$/, contar],
    [/^#\/conversa$/, telaConversa], [/^#\/resultado$/, telaResultado], [/^#\/resumo\/(\d+)$/, resumo], [/^#\/historico$/, historico],
    [/^#\/pessoas$/, pessoas], [/^#\/dados$/, meusDados], [/^#\/painel$/, painel]
  ];
  function ir(h) { if (location.hash === h) rotear(); else location.hash = h; }
  function rotear() {
    const h = location.hash || "#/";
    for (const [re, fn] of rotas) {
      const m = h.match(re);
      if (m) {
        document.querySelectorAll("[data-nav]").forEach((a) => a.removeAttribute("aria-current"));
        const nav = { "#/": "inicio", "#/historico": "historico", "#/pessoas": "pessoas", "#/dados": "dados", "#/painel": "painel" }[h] || (h === "" ? "inicio" : null);
        if (nav) document.querySelector(`[data-nav="${nav}"]`).setAttribute("aria-current", "page");
        window.scrollTo(0, 0);
        fn(...m.slice(1));
        tela.focus({ preventScroll: true });
        return;
      }
    }
    inicio();
  }
  addEventListener("hashchange", rotear);

  // ---------------------------------------------------------------- telas
  function inicio() {
    tela.innerHTML = `<div class="coluna">
      <section class="ola">${rosto("oi")}
        <h1>${usuario ? "Oi, " + esc(usuario.nome.split(" ")[0]) + "!" : "Oi, eu sou a Tami."}</h1>
        <p>Me conta o que está acontecendo, do seu jeito. Eu te ajudo a saber para onde ir.</p>
        <div class="acoes" style="justify-content:center">
          <button class="btn btn-l" data-comecar>Começar</button>
          ${usuario ? "" : '<button class="btn btn-s" data-anonimo>Usar sem conta</button>'}
        </div>
      </section>
      <div class="cartao" style="display:grid;gap:10px">
        <p><b>A Tami não é médica e não faz diagnóstico.</b> Ela orienta para onde ir. Quem cuida é o serviço de saúde.</p>
        <p class="nota">Em emergência, ligue 192 sem esperar a Tami. Está em teste e pode errar.</p>
      </div>
      ${usuario ? "" : '<p class="nota" style="text-align:center">Já usou antes? <a href="#/entrar">Entrar</a></p>'}
    </div>`;
    tela.querySelector("[data-comecar]").onclick = () => ir(usuario ? (guardado("termos") ? "#/para-quem" : "#/combinado") : "#/entrar");
    const an = tela.querySelector("[data-anonimo]");
    if (an) an.onclick = () => ir(guardado("termos") ? "#/para-quem" : "#/combinado");
  }

  function entrar() {
    tela.innerHTML = `<div class="coluna">
      <h1>Entrar na Tami</h1>
      <p class="sub">Sem senha difícil. Na próxima versão, um código chega no seu celular ou e-mail.</p>
      <form class="cartao" style="display:grid;gap:14px" data-form>
        <div class="campo"><label for="nome">Como posso te chamar?</label><input id="nome" name="nome" autocomplete="given-name" required maxlength="80"></div>
        <div class="campo"><label for="contato">Celular ou e-mail</label><input id="contato" name="contato" autocomplete="email" required maxlength="120"></div>
        <button class="btn btn-p cheio">Entrar</button>
      </form>
      <button class="btn btn-t" data-anonimo>Usar sem conta</button>
    </div>`;
    tela.querySelector("[data-form]").onsubmit = async (e) => {
      e.preventDefault();
      const f = new FormData(e.target);
      usuario = await api("POST", "/api/entrar", { nome: f.get("nome"), contato: f.get("contato") });
      guardado("usuario", usuario);
      ir(guardado("termos") ? "#/para-quem" : "#/combinado");
    };
    tela.querySelector("[data-anonimo]").onclick = () => { usuario = null; guardado("usuario", null); ir(guardado("termos") ? "#/para-quem" : "#/combinado"); };
  }

  function combinado() {
    tela.innerHTML = `<div class="coluna">
      <section class="ola" style="padding:26px 22px">${rosto("acolhe")}<h1>Antes de testar, o nosso combinado</h1></section>
      <ol class="cartao" style="margin:0;padding:22px 22px 22px 44px;display:grid;gap:10px">
        <li>A Tami não é médica e não faz diagnóstico.</li>
        <li>Ela orienta para onde ir. Quem cuida é o serviço de saúde.</li>
        <li>Em emergência, ligue 192 sem esperar a Tami.</li>
        <li>É um teste: a Tami pode errar. Conte pra gente quando errar.</li>
        <li>Você pode sair do teste e apagar seus dados quando quiser.</li>
      </ol>
      <label class="marcar"><input type="checkbox" data-termos><span><b>Li e aceito participar do teste.</b></span></label>
      ${usuario ? `<label class="marcar destaque"><input type="checkbox" data-saude ${usuario.consentimentoSaude ? "checked" : ""}><span><b>Guardar meus dados de saúde (opcional)</b><br><span class="nota">Guarda o que você contar e as respostas, para o histórico e o resumo. Sem isso, a Tami orienta do mesmo jeito. Você pode mudar quando quiser em Meus dados.</span></span></label>`
        : '<p class="nota">Sem conta, nada do que você contar fica guardado.</p>'}
      <button class="btn btn-p cheio" data-ok disabled>Aceito</button>
    </div>`;
    const t = tela.querySelector("[data-termos]"), ok = tela.querySelector("[data-ok]");
    t.onchange = () => (ok.disabled = !t.checked);
    ok.onclick = async () => {
      guardado("termos", true);
      const s = tela.querySelector("[data-saude]");
      if (usuario && s && s.checked !== usuario.consentimentoSaude) {
        usuario = await api("PUT", `/api/usuarios/${usuario.id}/consentimento`, { saude: s.checked });
        guardado("usuario", usuario);
      }
      ir("#/para-quem");
    };
  }

  async function paraQuem() {
    let lista = [];
    if (usuario) lista = await api("GET", `/api/usuarios/${usuario.id}/pessoas`).catch(() => []);
    tela.innerHTML = `<div class="coluna">
      <div class="balao"><span class="av">${rosto("oi", "#fff", "")}</span><div class="txt"><b>Essa ajuda é para quem?</b></div></div>
      <div class="opcoes">
        ${lista.map((p) => `<button class="op" data-pessoa="${p.id}"><span class="ini" style="width:36px;height:36px;border-radius:12px;background:var(--nevoa);display:grid;place-items:center;font:700 18px TamiFredoka;color:var(--azul)">${esc(p.apelido[0])}</span><span>${esc(p.apelido)}<br><small class="nota">${esc(p.relacaoTexto)} · ${esc(p.idade)}</small></span></button>`).join("")}
        <button class="op" data-tipo="eu">Para mim</button>
        <button class="op" data-tipo="outra">Para outra pessoa</button>
        <button class="op" data-tipo="crianca">Para um bebê ou criança</button>
      </div>
      <form class="cartao" data-idade hidden style="display:grid;gap:14px">
        <div class="linha"><div class="campo"><label for="idade">Quantos anos ou meses?</label><input id="idade" name="idade" type="number" min="0" max="125" required inputmode="numeric"></div>
          <div class="campo" style="flex:.7"><label for="unidade" class="sr">Unidade</label><select id="unidade" name="unidade"><option value="anos">anos</option><option value="meses">meses</option></select></div></div>
        <label class="marcar" data-gest><input type="checkbox" name="gestante"><span>Está grávida</span></label>
        <label class="marcar"><input type="checkbox" name="diabetes"><span>Tem diabetes</span></label>
        <button class="btn btn-p cheio">Continuar</button>
      </form>
      ${usuario ? '<p class="nota">Cuida sempre das mesmas pessoas? Cadastre em <a href="#/pessoas">Pessoas</a>.</p>' : ""}
    </div>`;
    const form = tela.querySelector("[data-idade]");
    tela.querySelectorAll("[data-pessoa]").forEach((b) => (b.onclick = () => {
      const p = lista.find((x) => String(x.id) === b.dataset.pessoa);
      preparo = { pessoa: p.id, quem: p.apelido, idadeMeses: p.idadeMeses };
      ir("#/contar");
    }));
    tela.querySelectorAll("[data-tipo]").forEach((b) => (b.onclick = () => {
      tela.querySelectorAll(".op").forEach((x) => x.setAttribute("aria-pressed", "false"));
      b.setAttribute("aria-pressed", "true");
      form.hidden = false;
      form.dataset.tipo = b.dataset.tipo;
      form.unidade.value = b.dataset.tipo === "crianca" ? "meses" : "anos";
      form.querySelector("[data-gest]").hidden = b.dataset.tipo === "crianca";
      form.idade.focus();
    }));
    form.onsubmit = (e) => {
      e.preventDefault();
      const idade = parseInt(form.idade.value, 10);
      const meses = form.unidade.value === "meses" ? idade : idade * 12;
      if (isNaN(meses) || meses < 0 || meses > 1500) return avisar("Confere a idade?");
      preparo = { quem: { eu: "você", outra: "outra pessoa", crianca: "a criança" }[form.dataset.tipo], idadeMeses: meses,
        gestante: form.gestante.checked, diabetes: form.diabetes.checked };
      ir("#/contar");
    };
  }

  async function contar() {
    if (!preparo) return ir("#/para-quem");
    const podeGuardar = usuario && usuario.consentimentoSaude;
    tela.innerHTML = `<div class="coluna">
      <div class="balao"><span class="av">${rosto("ouve", "#fff", "")}</span><div class="txt"><b>Me conta o que está acontecendo, do seu jeito.</b><span class="nota">Pode escrever errado, curto ou do jeito que vier.</span></div></div>
      <div class="campo"><label for="relato" class="sr">O que está acontecendo</label><textarea id="relato" maxlength="4000" placeholder="Ex.: caí de bicicleta e meu braço tá doendo e inchado"></textarea></div>
      <div class="filtro" data-filtro aria-live="polite"><p class="vazio-f">Conforme você escreve, eu procuro em que parte do corpo é e separo só o que faz sentido para ela.</p></div>
      <details class="cartao" data-corpo><summary style="cursor:pointer;font-weight:800">Não achei o lugar certo? Escolher no corpo</summary><div class="arvore" data-arvore style="margin-top:14px"><span class="pensando"><i></i><i></i><i></i></span></div></details>
      ${podeGuardar ? '<label class="marcar"><input type="checkbox" data-guardar checked><span>Guardar esta conversa no meu histórico</span></label>'
        : `<p class="nota">${usuario ? 'Esta conversa não vai ficar guardada. Para guardar, ligue a permissão em <a href="#/dados">Meus dados</a>.' : "Sem conta, esta conversa não fica guardada."}</p>`}
      <button class="btn btn-p cheio" data-enviar>Continuar</button>
    </div>`;
    const relato = tela.querySelector("#relato");
    let regiaoEscolhida = null, t;
    const atualizar = async () => {
      if (!relato.value.trim() && !regiaoEscolhida) return;
      const f = await api("POST", "/api/filtro", { texto: relato.value, regiao: regiaoEscolhida }).catch(() => null);
      if (f) desenharFiltro(tela.querySelector("[data-filtro]"), f);
    };
    relato.addEventListener("input", () => { clearTimeout(t); t = setTimeout(atualizar, 450); });
    relato.focus();
    tela.querySelector("[data-corpo]").addEventListener("toggle", async (e) => {
      if (!e.target.open || e.target.dataset.pronto) return;
      e.target.dataset.pronto = "1";
      const raiz = await api("GET", "/api/regioes");
      const caixa = tela.querySelector("[data-arvore]");
      caixa.innerHTML = `<div class="chips" data-n1>${raiz.filhos.map((g) => `<button type="button" class="chip" data-g="${g.codigo}" aria-pressed="false">${esc(g.nome)}</button>`).join("")}</div><div class="chips" data-n2></div>`;
      caixa.querySelectorAll("[data-g]").forEach((b) => (b.onclick = () => {
        caixa.querySelectorAll("[data-g]").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
        const g = raiz.filhos.find((x) => x.codigo === b.dataset.g);
        regiaoEscolhida = g.codigo;
        const n2 = caixa.querySelector("[data-n2]");
        n2.innerHTML = g.filhos.map((s) => `<button type="button" class="chip" data-s="${s.codigo}" aria-pressed="false">${esc(s.nome)}</button>`).join("");
        n2.querySelectorAll("[data-s]").forEach((s) => (s.onclick = () => {
          n2.querySelectorAll("[data-s]").forEach((x) => x.setAttribute("aria-pressed", String(x === s)));
          regiaoEscolhida = s.dataset.s;
          atualizar();
        }));
        atualizar();
      }));
    });
    tela.querySelector("[data-enviar]").onclick = async (e) => {
      if (!relato.value.trim() && !regiaoEscolhida) return avisar("Me conta um pouco do que está acontecendo.");
      e.target.disabled = true;
      const g = tela.querySelector("[data-guardar]");
      try {
        const r = await api("POST", "/api/conversas", {
          usuario: usuario ? usuario.id : null, pessoa: preparo.pessoa || null, idadeMeses: preparo.pessoa ? null : preparo.idadeMeses,
          gestante: !!preparo.gestante, diabetes: !!preparo.diabetes, relato: relato.value, regiao: regiaoEscolhida, guardar: !!(g && g.checked)
        });
        conversa = { id: r.conversa, feitas: [{ eu: true, texto: relato.value || "(escolhi no corpo)" }, { texto: r.abertura }], filtro: r.filtro, pergunta: r.pergunta, resultado: r.resultado, total: 0 };
        ir(r.fim ? "#/resultado" : "#/conversa");
      } catch (err) { e.target.disabled = false; }
    };
  }

  function desenharFiltro(el, f) {
    if (!f.regiao) {
      el.innerHTML = `<p class="vazio-f">Ainda não achei uma parte do corpo no que você escreveu. Tudo bem: eu procuro nos ${num(f.sinaisTotal)} sinais.</p>`;
      return;
    }
    const total = Object.values(f.porDestino || {}).reduce((a, b) => a + b, 0) || 1;
    el.innerHTML = `<div class="caminho">${f.caminho.map((c) => `<span>${esc(c)}</span>`).join("<i>›</i>")}</div>
      <div class="numeros"><span class="de">${num(f.sinaisTotal)}</span><span class="para">${num(f.sinaisRegiao)}</span><span>sinais para conferir aqui</span></div>
      <div class="barras">${ORDEM.map((d) => `<i style="width:${100 * (f.porDestino[d] || 0) / total}%;background:${DESTINO[d].cor}"></i>`).join("")}</div>
      <div class="legenda">${ORDEM.map((d) => `<b style="--c:${DESTINO[d].cor}">${DESTINO[d].curto}: ${f.porDestino[d] || 0}</b>`).join("")}</div>
      <p class="nota" style="color:#9AA0D8">E ${num(f.expressoesRegiao)} jeitos de falar, de ${num(f.expressoesTotal)}. Os sinais de emergência são perguntados sempre, em qualquer parte do corpo.</p>`;
  }

  function telaConversa() {
    if (!conversa || !conversa.pergunta) return ir("#/");
    const p = conversa.pergunta;
    const marcados = new Set(p.preMarcados || []);
    const exclusivas = new Set(["nenhum", "N", "nao_sei"]);
    const feitas = conversa.feitas.slice(-6);
    tela.innerHTML = `<div class="coluna">
      <div class="prog"><span class="rotulo">Pergunta ${conversa.feitas.filter((x) => x.eu).length}</span><div class="barra"><i style="width:${Math.min(95, 8 + conversa.feitas.length * 5)}%"></i></div></div>
      <div class="historico-conv">${feitas.map((f) => f.eu ? `<div class="balao eu velho"><div class="txt">${esc(f.texto)}</div></div>`
        : `<div class="balao velho"><span class="av">${rosto("oi", "#fff", "")}</span><div class="txt">${esc(f.texto)}</div></div>`).join("")}</div>
      <div class="balao"><span class="av">${rosto("pensa", "#fff", "")}</span><div class="txt">
        ${p.intro ? `<span class="nota">${esc(p.intro)}</span>` : ""}
        <b>${esc(p.texto)}</b>
        ${p.itens && p.itens.length ? `<ul>${p.itens.map((i) => `<li>${esc(i)}</li>`).join("")}</ul>` : ""}
        ${p.ajuda ? `<div class="ajuda">${esc(p.ajuda)}</div>` : ""}
        ${p.multipla ? '<span class="nota">Pode marcar mais de um.</span>' : ""}
      </div></div>
      ${p.id === "GEN_02" ? `<div class="escala escala-conv" role="radiogroup" aria-label="Nível de 0 a 10">${p.opcoes.map((o) => `<button type="button" role="radio" aria-checked="false" data-c="${esc(o.codigo)}" aria-label="${o.codigo}, ${FAIXAS[+o.codigo]}">${cara(+o.codigo)}<span>${esc(o.rotulo)}</span></button>`).join("")}</div>
      <div class="escala-legenda"><span>Sem dor</span><span>Média</span><span>A pior que já senti</span></div>` :
      `<div class="opcoes" role="group" aria-label="Respostas">${p.opcoes.map((o) => `<button class="op ${o.codigo === "nao_sei" ? "nao-sei" : ""}" data-c="${esc(o.codigo)}" aria-pressed="${marcados.has(o.codigo)}">${p.multipla ? '<span class="caixa"></span>' : ""}<span>${esc(o.rotulo)}</span></button>`).join("")}</div>`}
      ${p.multipla ? '<button class="btn btn-p cheio" data-continuar>Continuar</button>' : ""}
    </div>`;
    const botoes = [...tela.querySelectorAll("[data-c]")];
    const enviar = async (codigos) => {
      botoes.forEach((b) => (b.disabled = true));
      const rotulos = codigos.map((c) => p.opcoes.find((o) => o.codigo === c).rotulo);
      try {
        const r = await api("POST", `/api/conversas/${conversa.id}/respostas`, { pergunta: p.id, codigos });
        conversa.feitas.push({ texto: p.texto }, { eu: true, texto: p.id === "GEN_02" ? rotulos[0] + " de 10 · " + FAIXAS[+codigos[0]] : rotulos.join(", ") });
        if (p.id === "GEN_02") conversa.dor = +codigos[0];
        if (r.fim) { conversa.resultado = r.resultado; conversa.pergunta = null; ir("#/resultado"); }
        else { conversa.pergunta = r.pergunta; telaConversa(); }
      } catch (e) { botoes.forEach((b) => (b.disabled = false)); }
    };
    botoes.forEach((b) => (b.onclick = () => {
      const c = b.dataset.c;
      if (!p.multipla) return enviar([c]);
      const on = b.getAttribute("aria-pressed") !== "true";
      if (on && exclusivas.has(c)) botoes.forEach((x) => x.setAttribute("aria-pressed", "false"));
      if (on && !exclusivas.has(c)) botoes.filter((x) => exclusivas.has(x.dataset.c)).forEach((x) => x.setAttribute("aria-pressed", "false"));
      b.setAttribute("aria-pressed", String(on));
    }));
    const cont = tela.querySelector("[data-continuar]");
    if (cont) cont.onclick = () => {
      const cods = botoes.filter((b) => b.getAttribute("aria-pressed") === "true").map((b) => b.dataset.c);
      if (!cods.length) return avisar("Marque pelo menos uma opção. Se nada vale, marque “Nenhum destes”.");
      enviar(cods);
    };
  }

  function telaResultado() {
    if (!conversa || !conversa.resultado) return ir("#/");
    const r = conversa.resultado, d = DESTINO[r.destino];
    const semCvv = r.blocos.filter((b) => b.codigo !== "CVV188");
    const listas = semCvv.filter((b) => b.texto.includes("•")), simples = semCvv.filter((b) => !b.texto.includes("•"));
    const regiao = conversa.filtro && conversa.filtro.regiao;
    tela.innerHTML = `<div class="coluna">
      <section class="res ${r.destino}">
        ${rosto(d.rosto, r.destino === "SAMU_192" ? "#fff" : "#1B1F4A")}
        <span class="rotulo" style="color:inherit;opacity:.85">${d.curto}</span>
        <h1>${d.titulo}</h1>
        <p style="font-size:19px">${esc(r.mensagem)}</p>
        ${r.destino === "SAMU_192" ? `<a class="btn btn-v cheio" href="tel:192">${ICONE.fone} Ligar 192 agora</a>` : ""}
        ${simples.length ? `<ul class="blocos">${simples.map((b) => `<li>${esc(b.texto)}</li>`).join("")}</ul>` : ""}
        ${r.porNaoSei ? '<p class="nota" style="color:inherit">Você respondeu “Não sei” em uma pergunta de emergência. Na dúvida, eu fico com o caminho mais cuidadoso.</p>' : ""}
      </section>
      ${r.crise ? `<section class="crise"><b style="font:700 22px TamiFredoka">Você não precisa passar por isso sozinho.</b><p>${esc((r.blocos.find((b) => b.codigo === "CVV188") || {}).texto || "Ligue 188 (CVV): é de graça e funciona 24 horas.")}</p><a class="btn cheio" href="tel:188">${ICONE.fone} Ligar 188 (CVV)</a></section>` : ""}
      ${listas.map((b) => { const [tit, ...itens] = b.texto.split(/\s*•\s*/); return `<section class="cartao cuidados"><span class="cuid-ic">${b.codigo.startsWith("BLK-EI") ? "➜" : "✓"}</span><h2>${esc(tit)}</h2><ul>${itens.map((i) => `<li>${esc(i)}</li>`).join("")}</ul></section>`; }).join("")}
      ${r.sinaisConfirmados && r.sinaisConfirmados.length ? `<div class="cartao"><span class="rotulo">O que pesou</span><ul style="margin:8px 0 0;padding-left:20px">${r.sinaisConfirmados.map((s) => `<li>${esc(s)}</li>`).join("")}</ul></div>` : ""}
      <section class="cartao" style="display:grid;gap:12px" data-dor>
        <h2 style="font-size:24px">Onde dói, e quanto?</h2>
        <p class="nota">${regiao ? "Pelo que você contou: <b>" + esc(regiao.nome) + "</b>." : "Se tiver dor, marque o nível."} Isso vai para o resumo do atendimento.</p>
        <div class="chips" data-lado>${["DIREITO", "MEIO", "ESQUERDO"].map((l) => `<button type="button" class="chip" data-l="${l}" aria-pressed="false">${{ DIREITO: "Lado direito", MEIO: "No meio", ESQUERDO: "Lado esquerdo" }[l]}</button>`).join("")}</div>
        <div class="escala" role="radiogroup" aria-label="Nível da dor de 0 a 10">${[...Array(11).keys()].map((n) => `<button type="button" role="radio" aria-checked="false" aria-pressed="false" data-n="${n}" aria-label="${n}, ${FAIXAS[n]}">${cara(n)}<span>${n}</span></button>`).join("")}</div>
        <p class="escala-faixa" data-faixa></p>
        <button class="btn btn-s" data-salvar-dor disabled>Guardar a dor</button>
        <p class="nota" data-dor-ok></p>
      </section>
      <section class="cartao" style="display:grid;gap:12px">
        <h2 style="font-size:24px">${esc(r.fechamento || "Isso te ajudou?")}</h2>
        <div class="chips" data-aval>${[["SIM", "Sim"], ["MAIS_OU_MENOS", "Mais ou menos"], ["NAO", "Não"]].map(([v, t]) => `<button type="button" class="chip" data-a="${v}" aria-pressed="false">${t}</button>`).join("")}</div>
        <p data-aval-resp class="sub"></p>
        <div class="campo" data-coment hidden><label for="coment">Quer contar mais? (opcional)</label><textarea id="coment" maxlength="1000" style="min-height:90px"></textarea><button class="btn btn-s" data-enviar-coment>Enviar</button></div>
      </section>
      <div class="acoes"><a class="btn btn-p" href="#/resumo/${conversa.id}">Ver resumo para o atendimento</a><button class="btn btn-s" data-nova>Nova conversa</button></div>
    </div>`;
    let lado = null, nivel = null, idDor = null, aval = null;
    const salvar = tela.querySelector("[data-salvar-dor]");
    tela.querySelectorAll("[data-l]").forEach((b) => (b.onclick = () => { tela.querySelectorAll("[data-l]").forEach((x) => x.setAttribute("aria-pressed", String(x === b))); lado = b.dataset.l; }));
    tela.querySelectorAll("[data-n]").forEach((b) => (b.onclick = () => {
      tela.querySelectorAll("[data-n]").forEach((x) => { x.setAttribute("aria-pressed", String(x === b)); x.setAttribute("aria-checked", String(x === b)); });
      nivel = +b.dataset.n;
      tela.querySelector("[data-faixa]").textContent = nivel + " · " + FAIXAS[nivel];
      salvar.disabled = false;
      salvar.textContent = idDor ? "Atualizar a dor" : "Guardar a dor";
    }));
    salvar.onclick = async () => {
      const ok = tela.querySelector("[data-dor-ok]");
      if (idDor) {
        await api("PUT", `/api/dor/${idDor}`, { nivel });
        ok.textContent = "Atualizado: nível " + nivel + " (" + FAIXAS[nivel] + ").";
      } else {
        const d = await api("POST", `/api/conversas/${conversa.id}/dor`, { regiao: regiao ? regiao.codigo : "CORPO", lado, nivel });
        idDor = d.id;
        ok.textContent = "Guardado: " + d.regiao + ", nível " + d.nivel + " (" + d.faixa + "). Mudou? Toque em outro número e atualize.";
      }
      salvar.disabled = true;
    };
    tela.querySelectorAll("[data-a]").forEach((b) => (b.onclick = async () => {
      aval = b.dataset.a;
      tela.querySelectorAll("[data-a]").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
      const resp = await api("PUT", `/api/conversas/${conversa.id}/avaliacao`, { avaliacao: aval });
      tela.querySelector("[data-aval-resp]").textContent = resp.resposta;
      tela.querySelector("[data-coment]").hidden = false;
    }));
    tela.querySelector("[data-enviar-coment]").onclick = async () => {
      await api("PUT", `/api/conversas/${conversa.id}/avaliacao`, { avaliacao: aval, comentario: tela.querySelector("#coment").value });
      tela.querySelector("[data-aval-resp]").textContent = "Obrigada! Isso ajuda a Tami a melhorar.";
    };
    tela.querySelector("[data-nova]").onclick = () => { conversa = null; ir("#/para-quem"); };
    if (Number.isInteger(conversa.dor) && !conversa.dorGuardada) {   // a dor já veio da conversa: marca e guarda sozinha
      conversa.dorGuardada = true;
      const b = tela.querySelector(`[data-n="${conversa.dor}"]`);
      if (b) { b.click(); salvar.click(); }
    }
  }

  async function resumo(id) {
    const r = await api("GET", `/api/conversas/${id}` + (usuario ? `?usuario=${usuario.id}` : "")).catch(() => null);
    if (!r) return;
    const d = DESTINO[r.destino] || { titulo: "-" };
    tela.innerHTML = `<div class="coluna resumo">
      <div style="display:flex;align-items:center;gap:12px"><h1 style="flex:1">Resumo para o atendimento</h1><button class="btn btn-s nao-imprimir" onclick="print()">Imprimir</button></div>
      <p class="ajuda">${esc(r.aviso)}</p>
      <div class="cartao"><dl>
        <dt>Quando</dt><dd>${data(r.inicio)}</dd>
        <dt>Para quem</dt><dd>${esc(r.pessoa || "")} ${esc(r.idade)}</dd>
        ${r.regiao ? `<dt>Parte do corpo</dt><dd>${esc(r.regiao)}</dd>` : ""}
        <dt>Caminho sugerido</dt><dd><b>${d.titulo}</b>${r.mensagem ? "<br>" + esc(r.mensagem) : ""}</dd>
        ${r.dores.length ? `<dt>Dor</dt><dd>${r.dores.map((x) => `${esc(x.regiao)}${x.lado ? " (" + x.lado.toLowerCase() + ")" : ""}: ${x.nivel} de 10 · ${x.faixa}`).join("<br>")}</dd>` : ""}
        <dt>Perguntas</dt><dd>${r.perguntas}</dd>
      </dl></div>
      ${r.guardado ? `<div class="cartao"><span class="rotulo">O que foi contado</span><p style="margin-top:6px">${esc(r.relato || "")}</p></div>
        <div class="cartao"><span class="rotulo">Perguntas e respostas</span><ol style="margin-top:10px">${r.respostas.map((x) => `<li>${esc(x.pergunta)}<br><b>${esc(x.resposta)}</b>${x.origem !== "USUARIO" ? ' <span class="nota">(do que foi escrito)</span>' : ""}</li>`).join("")}</ol></div>`
        : '<p class="nota">O relato e as respostas não foram guardados, porque a permissão de dados de saúde estava desligada.</p>'}
      <div class="acoes nao-imprimir"><a class="btn btn-t" href="#/historico">Voltar ao histórico</a></div>
    </div>`;
  }

  async function historico() {
    if (!usuario) return precisaEntrar("ver o histórico");
    const h = await api("GET", `/api/usuarios/${usuario.id}/historico`).catch(() => []);
    tela.innerHTML = `<div class="coluna"><h1>Histórico</h1>
      ${h.length ? `<ul class="lista">${h.map((c) => `<li class="item"><span class="ponto cor-${c.destino}"></span><a href="#/resumo/${c.id}">${DESTINO[c.destino].titulo}<small>${data(c.inicio)}${c.pessoa ? " · " + esc(c.pessoa) : ""}${c.regiao ? " · " + esc(c.regiao) : ""}${c.relato ? " · “" + esc(c.relato.slice(0, 60)) + (c.relato.length > 60 ? "…" : "") + "”" : ""}</small></a>
        <button class="icone-b" data-apagar="${c.id}" aria-label="Apagar esta conversa">${ICONE.lixo}</button></li>`).join("")}</ul>`
        : '<div class="cartao vazio">Ainda não tem conversa guardada.<br><a href="#/">Começar uma</a></div>'}
    </div>`;
    tela.querySelectorAll("[data-apagar]").forEach((b) => (b.onclick = async () => {
      if (!(await confirmar("Apagar esta conversa?", "Ela sai do histórico e do banco. Não dá para desfazer."))) return;
      await api("DELETE", `/api/usuarios/${usuario.id}/conversas/${b.dataset.apagar}`);
      historico();
    }));
  }

  async function pessoas(editando) {
    if (!usuario) return precisaEntrar("cadastrar as pessoas que você cuida");
    const lista = await api("GET", `/api/usuarios/${usuario.id}/pessoas`).catch(() => []);
    const p = editando ? lista.find((x) => x.id === editando) : null;
    const rel = [["EU", "Eu"], ["FILHO", "Filho ou filha"], ["MAE", "Mãe"], ["PAI", "Pai"], ["AVO", "Avó ou avô"], ["PARCEIRO", "Parceiro ou parceira"], ["OUTRO", "Outra pessoa"]];
    tela.innerHTML = `<div class="coluna"><h1>Pessoas que eu cuido</h1>
      <p class="sub">Só guardo o que ajuda a orientar: idade, gravidez e diabetes.</p>
      ${lista.length ? `<ul class="lista">${lista.map((x) => `<li class="item pessoa"><span class="ini">${esc(x.apelido[0])}</span><span><b>${esc(x.apelido)}</b><small>${esc(x.relacaoTexto)} · ${esc(x.idade)}${x.gestante ? " · grávida" : ""}${x.diabetes ? " · diabetes" : ""}</small></span>
        <span style="display:flex;gap:6px"><button class="icone-b" data-editar="${x.id}" aria-label="Editar ${esc(x.apelido)}">${ICONE.lapis}</button><button class="icone-b" data-apagar="${x.id}" aria-label="Apagar ${esc(x.apelido)}">${ICONE.lixo}</button></span></li>`).join("")}</ul>` : ""}
      <form class="cartao" style="display:grid;gap:14px" data-form>
        <h2 style="font-size:22px">${p ? "Editar " + esc(p.apelido) : "Adicionar pessoa"}</h2>
        <div class="campo"><label for="ap">Nome ou apelido</label><input id="ap" name="apelido" required maxlength="60" value="${p ? esc(p.apelido) : ""}"></div>
        <div class="campo"><label for="rel">Quem é para você</label><select id="rel" name="relacao">${rel.map(([v, t]) => `<option value="${v}" ${p && p.relacao === v ? "selected" : ""}>${t}</option>`).join("")}</select></div>
        <div class="linha"><div class="campo"><label for="id2">Idade</label><input id="id2" name="idade" type="number" min="0" max="125" required value="${p ? (p.idadeMeses < 24 ? p.idadeMeses : Math.floor(p.idadeMeses / 12)) : ""}"></div>
          <div class="campo" style="flex:.7"><label for="un2" class="sr">Unidade</label><select id="un2" name="unidade"><option value="anos">anos</option><option value="meses" ${p && p.idadeMeses < 24 ? "selected" : ""}>meses</option></select></div></div>
        <label class="marcar"><input type="checkbox" name="gestante" ${p && p.gestante ? "checked" : ""}><span>Está grávida</span></label>
        <label class="marcar"><input type="checkbox" name="diabetes" ${p && p.diabetes ? "checked" : ""}><span>Tem diabetes</span></label>
        <div class="acoes"><button class="btn btn-p">${p ? "Salvar mudanças" : "Adicionar"}</button>${p ? '<a class="btn btn-t" href="#/pessoas">Cancelar</a>' : ""}</div>
      </form></div>`;
    tela.querySelector("[data-form]").onsubmit = async (e) => {
      e.preventDefault();
      const f = e.target;
      const corpo = { apelido: f.apelido.value, relacao: f.relacao.value, idade: +f.idade.value, unidade: f.unidade.value, gestante: f.gestante.checked, diabetes: f.diabetes.checked };
      if (p) await api("PUT", `/api/usuarios/${usuario.id}/pessoas/${p.id}`, corpo);
      else await api("POST", `/api/usuarios/${usuario.id}/pessoas`, corpo);
      pessoas();
    };
    tela.querySelectorAll("[data-editar]").forEach((b) => (b.onclick = () => pessoas(+b.dataset.editar)));
    tela.querySelectorAll("[data-apagar]").forEach((b) => (b.onclick = async () => {
      if (!(await confirmar("Apagar esta pessoa?", "As conversas dela continuam no histórico, sem o nome."))) return;
      await api("DELETE", `/api/usuarios/${usuario.id}/pessoas/${b.dataset.apagar}`);
      pessoas();
    }));
  }

  async function meusDados() {
    if (!usuario) return precisaEntrar("ver os seus dados");
    usuario = await api("GET", `/api/usuarios/${usuario.id}`).catch(() => usuario);
    guardado("usuario", usuario);
    tela.innerHTML = `<div class="coluna"><h1>Meus dados</h1>
      <div class="cartao" style="display:grid;gap:6px"><b>${esc(usuario.nome)}</b><span class="nota">${esc(usuario.contato)} · desde ${data(usuario.cadastro)}</span></div>
      <p class="sub">Seus dados ficam guardados no Brasil. Não vendo e não passo para ninguém.</p>
      <label class="marcar destaque"><input type="checkbox" data-saude ${usuario.consentimentoSaude ? "checked" : ""}><span><b>Guardar meus dados de saúde</b><br><span class="nota">${usuario.consentimentoSaude ? "Ligado desde " + data(usuario.dataConsentimento) + "." : "Desligado. A Tami orienta do mesmo jeito, só não guarda o histórico."}</span></span></label>
      <div class="acoes"><button class="btn btn-s" data-baixar>Baixar meus dados</button><button class="btn btn-s" data-sair>Sair</button></div>
      <div class="cartao" style="display:grid;gap:10px;border-color:#F5C2C4"><b>Apagar tudo</b><p class="nota">Apaga sua conta, as pessoas, as conversas e as avaliações. Não dá para desfazer.</p><button class="btn btn-v" data-apagar>Apagar tudo</button></div>
      <p class="nota"><a href="#/" data-politica>Política de privacidade</a></p></div>`;
    tela.querySelector("[data-saude]").onchange = async (e) => {
      usuario = await api("PUT", `/api/usuarios/${usuario.id}/consentimento`, { saude: e.target.checked });
      guardado("usuario", usuario);
      meusDados();
    };
    tela.querySelector("[data-baixar]").onclick = async () => {
      const d = await api("GET", `/api/usuarios/${usuario.id}/dados`);
      const a = document.createElement("a");
      a.href = URL.createObjectURL(new Blob([JSON.stringify(d, null, 2)], { type: "application/json" }));
      a.download = "meus-dados-tami.json";
      a.click();
    };
    tela.querySelector("[data-sair]").onclick = () => { usuario = null; guardado("usuario", null); guardado("termos", null); ir("#/"); };
    tela.querySelector("[data-apagar]").onclick = async () => {
      if (!(await confirmar("Apagar tudo?", "Sua conta e tudo ligado a ela saem do banco agora."))) return;
      await api("DELETE", `/api/usuarios/${usuario.id}`);
      usuario = null; guardado("usuario", null); guardado("termos", null);
      ir("#/");
    };
    tela.querySelector("[data-politica]").onclick = (e) => { e.preventDefault(); window.open("https://gabriell230g.github.io/tami/privacidade/", "_blank", "noopener"); };
  }

  function precisaEntrar(o) {
    tela.innerHTML = `<div class="coluna"><div class="cartao vazio" style="display:grid;gap:14px;justify-items:center">${rosto("oi", "#3F4ACB")}<p>Entre para ${o}.</p><a class="btn btn-p" href="#/entrar">Entrar</a></div></div>`;
  }

  function confirmar(titulo, texto) {
    return new Promise((ok) => {
      const m = document.createElement("div");
      m.className = "modal";
      m.innerHTML = `<div class="cartao" role="dialog" aria-modal="true" aria-labelledby="mt"><h2 id="mt" style="font-size:24px">${esc(titulo)}</h2><p>${esc(texto)}</p><div class="acoes"><button class="btn btn-v" data-s>Sim, apagar</button><button class="btn btn-s" data-n>Cancelar</button></div></div>`;
      document.body.appendChild(m);
      m.querySelector("[data-n]").focus();
      const fim = (v) => { m.remove(); ok(v); };
      m.querySelector("[data-s]").onclick = () => fim(true);
      m.querySelector("[data-n]").onclick = () => fim(false);
      m.onkeydown = (e) => { if (e.key === "Escape") fim(false); };
    });
  }

  // ---------------------------------------------------------------- painel de estatística
  const COR_MARCA = { SAMU_192: "#D63B40", UPA_HOJE: "#E58A00", UBS_EM_BREVE: "#2F8FD1", CUIDADO_EM_CASA: "#2E9E5B" };
  let abaPainel = "motor";

  function dica(el) {   // tooltip simples para qualquer [data-tip]
    let caixa = document.querySelector(".pn-dica");
    if (!caixa) { caixa = document.createElement("div"); caixa.className = "pn-dica"; caixa.setAttribute("role", "tooltip"); document.body.appendChild(caixa); }
    el.querySelectorAll("[data-tip]").forEach((m) => {
      const mostrar = () => { caixa.innerHTML = m.dataset.tip; caixa.hidden = false; const r = m.getBoundingClientRect(); caixa.style.left = Math.min(innerWidth - caixa.offsetWidth - 8, Math.max(8, r.left + r.width / 2 - caixa.offsetWidth / 2)) + "px"; caixa.style.top = (r.top + scrollY - caixa.offsetHeight - 8) + "px"; };
      m.addEventListener("mouseenter", mostrar); m.addEventListener("focus", mostrar);
      m.addEventListener("mouseleave", () => (caixa.hidden = true)); m.addEventListener("blur", () => (caixa.hidden = true));
    });
  }

  async function painel() {
    tela.innerHTML = '<div class="pn"><h1>Painel</h1><span class="pensando"><i></i><i></i><i></i></span></div>';
    const e = await api("GET", "/api/estatisticas").catch(() => null);
    if (!e) return;
    const v = e.validacao, u = e.uso;
    const medidor = (prop) => prop && prop.ic95 ? `<span class="pn-medidor" aria-hidden="true"><i class="pn-faixa" style="left:${100 * prop.ic95[0]}%;width:${Math.max(1, 100 * (prop.ic95[1] - prop.ic95[0]))}%"></i><i class="pn-ponto" style="left:${100 * prop.p}%"></i></span>` : "";
    const kpi = (rot, val, pe, extra = "", cls = "") => `<div class="pn-kpi ${cls}"><span class="pn-kpi-rot">${rot}</span><span class="pn-kpi-v">${val}</span>${extra}<span class="pn-kpi-pe">${pe}</span></div>`;
    const card = (titulo, sub, corpo, cls = "") => `<section class="pn-card ${cls}"><header><h2>${titulo}</h2>${sub ? `<p>${sub}</p>` : ""}</header>${corpo}</section>`;
    const barras = (itens, max, fmt) => `<div class="pn-barras">${itens.map((x) => `<div class="pn-barra" tabindex="0" data-tip="${esc(x.tip || x.rot + ": " + fmt(x.v))}"><span class="pn-barra-rot">${x.cor ? `<i class="pn-pino" style="background:${x.cor}"></i>` : ""}${esc(x.rot)}</span><span class="pn-trilho"><span class="pn-enche" style="width:${max ? 100 * x.v / max : 0}%;background:${x.cor || "var(--azul)"}"></span>${x.ic ? `<span class="pn-ic" style="left:${100 * x.ic[0] / max}%;width:${100 * (x.ic[1] - x.ic[0]) / max}%"></span>` : ""}</span><span class="pn-barra-v">${fmt(x.v)}</span></div>`).join("")}</div>`;
    const quiTexto = (q) => q && q.valido ? `<span class="pn-pilula ${q.valorP < 0.05 ? "sim" : ""}">χ² = ${num(q.quiQuadrado)} · gl ${q.grausLiberdade} · p ${q.valorP < 0.001 ? "&lt; 0,001" : "= " + num(q.valorP)} · V = ${num(q.vCramer)}</span><span class="pn-nota">${q.valorP < 0.05 ? "Há associação entre as duas variáveis (p &lt; 0,05)." : "Sem associação estatística (p ≥ 0,05)."}</span>` : '<span class="pn-nota">Poucos dados para o teste qui-quadrado.</span>';

    const abaMotor = () => {
      if (!v) return '<p class="vazio">Rode a validação (scripts/rodar validar) para ver os números do motor.</p>';
      const m = v.matriz, max = Math.max(...m.flat());
      const arm = Object.entries(v.porArmadilha).filter(([, x]) => x.casos >= 50).sort((a, b) => b[1].acerto.p - a[1].acerto.p);
      const NOME_ARM = { NENHUMA: "Texto direto", DIGITACAO: "Erro de digitação", RISCO_ESCONDIDO: "Risco escondido", NEGACAO: "Negação (não tenho...)", PARECE_GRAVE: "Parece grave, mas não é", MINIMIZA: "Minimiza (não é nada)", VAGO: "Texto vago", MISTURA: "Vários sintomas", PASSADO: "Fala do passado", TERCEIRO: "Fala de outra pessoa" };
      return `<div class="pn-kpis">
          ${kpi("Urgentes que foram para UPA ou SAMU", pct(v.sensibilidadeUrgente.p), `${ic(v.sensibilidadeUrgente)} · ${num(v.sensibilidadeUrgente.x)} de ${num(v.sensibilidadeUrgente.n)}`, medidor(v.sensibilidadeUrgente), "escuro")}
          ${kpi("Destino exato", pct(v.acuraciaExata.p), ic(v.acuraciaExata), medidor(v.acuraciaExata))}
          ${kpi("Grave mandado para mais leve", pct(v.subtriagemCritica.p, 2), `${ic(v.subtriagemCritica)} · ${v.subtriagemCritica.x} casos`, `<span class="pn-meta">quanto menor, melhor</span>`)}
          ${kpi("Concordância (kappa ponderado)", num(v.kappaPonderado), `kappa simples ${num(v.kappa)} · 1 é perfeita`, `<span class="pn-medidor" aria-hidden="true"><i class="pn-ponto" style="left:${100 * v.kappaPonderado}%"></i></span>`)}
        </div>
        <div class="pn-grade">
          ${card("Matriz de confusão", "Linha: o destino esperado. Coluna: o que a Tami deu.", `
            <div class="pn-matriz" style="grid-template-columns:auto repeat(${v.destinos.length},1fr)">
              <span></span>${v.destinos.map((d) => `<span class="pn-mcab">${DESTINO[d].curto}</span>`).join("")}
              ${m.map((l, i) => `<span class="pn-mlin">${DESTINO[v.destinos[i]].curto}</span>${l.map((x, j) => {
                const tipo = i === j ? "ok" : j < i ? "leve" : "cuidado";
                const a = x ? (i === j ? 0.18 + 0.82 * x / max : 0.14 + 0.7 * Math.sqrt(x / max)) : 0;
                return `<span class="pn-cel ${tipo}${a > 0.55 ? " claro" : ""}" tabindex="0" style="--a:${a.toFixed(2)}" data-tip="Esperado <b>${DESTINO[v.destinos[i]].curto}</b>, Tami deu <b>${DESTINO[v.destinos[j]].curto}</b>: ${num(x)} casos">${num(x)}</span>`;
              }).join("")}`).join("")}
            </div>
            <div class="pn-legenda"><span><i class="ok"></i>Acertou</span><span><i class="leve"></i>Mais leve que o esperado</span><span><i class="cuidado"></i>Mais cuidadoso (${pct(v.sobretriagem.p)})</span></div>`)}
          ${card("Acerto por tipo de texto", "Os casos de teste têm armadilhas de propósito.", barras(arm.map(([k, x]) => ({ rot: NOME_ARM[k] || k.toLowerCase(), v: x.acerto.p, tip: `${NOME_ARM[k] || k}: ${pct(x.acerto.p)} de acerto em ${num(x.casos)} casos · ${x.criticas} graves perdidos` })), 1, (p) => pct(p, 0)))}
        </div>
        <p class="pn-rodape">${esc(v.aviso)} Intervalos de confiança de 95% pelo método de Wilson. ${num(v.casos)} casos, ${num(v.msPorCaso)} ms por caso.</p>`;
    };

    const abaUso = () => {
      if (!u.conversas) return '<p class="vazio">Ainda não tem conversa no banco. Use o app ou gere conversas de teste.</p>';
      const maxD = Math.max(...ORDEM.map((d) => (u.destinos[d] || {}).ic95 ? u.destinos[d].ic95[1] : 0));
      const maxH = Math.max(1, ...u.porHora);
      const aval = ORDEM.filter((d) => u.avaliacaoPorDestino[d]);
      const AV = [["SIM", "Ajudou", "#2E9E5B"], ["MAIS_OU_MENOS", "Mais ou menos", "#E5B800"], ["NAO", "Não ajudou", "#D63B40"]];
      const faixas = Object.entries(u.faixaEtariaPorDestino);
      const maxF = Math.max(1, ...faixas.flatMap(([, m]) => ORDEM.map((d) => m[d] || 0)));
      return `<div class="pn-kpis">
          ${kpi("Conversas no banco", num(u.conversas), `${pct(u.comRegiao.p, 0)} com região do corpo achada`, "", "escuro")}
          ${kpi("Disseram que ajudou", pct(u.ajudou.p), `${ic(u.ajudou)} · ${num(u.ajudou.n)} avaliações`, medidor(u.ajudou))}
          ${kpi("Perguntas por conversa", num(u.perguntas.mediana), `mediana · metade fica entre ${num(u.perguntas.q1)} e ${num(u.perguntas.q3)}`)}
          ${kpi("Busca reduzida pela árvore", u.reducaoFiltroPct && u.reducaoFiltroPct.n ? num(Math.round(u.reducaoFiltroPct.mediana)) + "%" : "-", "dos sinais da base, quando a região é achada")}
        </div>
        <div class="pn-grade">
          ${card("Para onde a Tami mandou", "A faixa escura em cada barra é o intervalo de confiança de 95%.", barras(ORDEM.map((d) => ({ rot: DESTINO[d].curto, v: (u.destinos[d] || {}).p || 0, ic: (u.destinos[d] || {}).ic95, cor: COR_MARCA[d], tip: `${DESTINO[d].curto}: ${pct((u.destinos[d] || {}).p)} (${num((u.destinos[d] || {}).x || 0)} conversas) · ${ic(u.destinos[d])}` })), maxD, (p) => pct(p, 0)))}
          ${card("Conversas por hora do dia", "", `<div class="pn-colunas">${u.porHora.map((x, h) => `<span tabindex="0" data-tip="${h}h: ${x} conversas"><i style="height:${100 * x / maxH}%"></i></span>`).join("")}</div><div class="pn-eixo"><span>0h</span><span>6h</span><span>12h</span><span>18h</span><span>23h</span></div>`)}
          ${card("Avaliação por destino", "Cada barra soma 100% das avaliações daquele destino.", `<div class="pn-empilhado">${aval.map((d) => { const a = u.avaliacaoPorDestino[d]; const tot = AV.reduce((s, [k]) => s + (a[k] || 0), 0) || 1; return `<div class="pn-emp"><span class="pn-barra-rot"><i class="pn-pino" style="background:${COR_MARCA[d]}"></i>${DESTINO[d].curto}</span><span class="pn-emp-trilho">${AV.map(([k, n, c]) => a[k] ? `<i tabindex="0" style="flex:${a[k]};background:${c}" data-tip="${DESTINO[d].curto} · ${n}: ${a[k]} (${pct(a[k] / tot, 0)})"></i>` : "").join("")}</span><span class="pn-barra-v">${tot}</span></div>`; }).join("")}</div>
            <div class="pn-legenda">${AV.map(([, n, c]) => `<span><i style="background:${c}"></i>${n}</span>`).join("")}</div>${quiTexto(u.avaliacaoXDestino)}`)}
          ${card("Dor média por região", "Escala de 0 a 10, com o número de registros.", u.dorPorRegiao.length ? barras(u.dorPorRegiao.map((x) => ({ rot: x.regiao, v: x.media, tip: `${x.regiao}: média ${num(x.media)} em ${x.n} registros (de ${x.min} a ${x.max})` })), 10, (m) => num(m)) : '<p class="pn-nota">Sem dor registrada ainda.</p>')}
          ${card("Faixa etária e destino", "Quanto mais escuro, mais conversas.", `<div class="pn-calor" style="grid-template-columns:auto repeat(4,1fr)"><span></span>${ORDEM.map((d) => `<span class="pn-mcab">${DESTINO[d].curto}</span>`).join("")}
            ${faixas.map(([f, m]) => `<span class="pn-mlin">${esc(f)}</span>${ORDEM.map((d) => `<span class="pn-cel azul${(m[d] || 0) / maxF > 0.5 ? " claro" : ""}" tabindex="0" style="--a:${((m[d] || 0) ? 0.1 + 0.9 * (m[d] || 0) / maxF : 0).toFixed(2)}" data-tip="${esc(f)} · ${DESTINO[d].curto}: ${m[d] || 0}">${m[d] || 0}</span>`).join("")}`).join("")}</div>${quiTexto(u.faixaEtariaXDestino)}`, "largo2")}
        </div>
        <details class="pn-card pn-det"><summary><h2>Estatística descritiva</h2><span class="pn-nota">média, mediana, desvio padrão, quartis e IC 95%</span></summary>
          <table class="pn-tabela"><thead><tr><th>Medida</th><th>n</th><th>Média</th><th>Mediana</th><th>Desvio padrão</th><th>Q1 a Q3</th><th>IC 95% da média</th></tr></thead><tbody>
          ${[["Perguntas por conversa", u.perguntas], ["Duração (segundos)", u.segundos], ["Redução da busca pela árvore (%)", u.reducaoFiltroPct]].filter(([, d]) => d && d.n).map(([t, d]) => `<tr><td>${t}</td><td>${num(d.n)}</td><td>${num(d.media)}</td><td>${num(d.mediana)}</td><td>${num(d.desvioPadrao)}</td><td>${num(d.q1)} a ${num(d.q3)}</td><td>${d.ic95Media ? num(d.ic95Media[0]) + " a " + num(d.ic95Media[1]) : "-"}</td></tr>`).join("")}</tbody></table>
          <p class="pn-nota">Correlação de Pearson entre idade e número de perguntas: r = ${num(u.idadeXPerguntas)}. Crises com CVV 188 mostrado: ${u.crise.x}.</p></details>
        <p class="pn-rodape">Conversas de teste são de pessoas simuladas: o destino vem do motor de verdade, mas a avaliação e a dor são sorteadas.</p>`;
    };

    const abaBanco = () => {
      const maxS = Math.max(1, ...Object.values(u.sinaisPorRegiao));
      return `<div class="pn-tabelas">${Object.entries(u.tabelas).map(([t, n]) => `<div class="pn-tab"><code>${t}</code><b>${num(n)}</b></div>`).join("")}</div>
        <div class="pn-grade">
          ${card("Sinais da base por região do corpo", "Quantos sinais de alerta a árvore liga a cada região.", barras(Object.entries(u.sinaisPorRegiao).map(([k, x]) => ({ rot: k, v: x })), maxS, (x) => num(x)))}
          ${card("Regras mais acionadas", "As regras que mais decidiram destinos.", (u.regrasMaisAcionadas || []).length ? `<ol class="pn-lista">${u.regrasMaisAcionadas.slice(0, 10).map((r) => `<li><span><i class="pn-pino" style="background:${COR_MARCA[r.destino] || "var(--azul)"}"></i><code>${esc(r.regra)}</code> ➜ ${DESTINO[r.destino] ? DESTINO[r.destino].curto : esc(r.destino)}</span><b>${num(r.n)}</b></li>`).join("")}</ol>` : '<p class="pn-nota">Sem dados ainda.</p>')}
        </div>`;
    };

    const abaSimulados = () => {
      const s = e.simulados;
      if (!s) return '<p class="vazio">Rode ferramentas/simulados para ver as conversas simuladas.</p>';
      const a = s.antes, d = s.depois, num2 = (x) => num(Math.round(x * 100) / 100);
      const comp = (rot, k, inverso) => { const va = a[k].p, vd = d[k].p, melhor = inverso ? vd < va : vd > va;
        return `<div class="pn-kpi"><span class="pn-kpi-rot">${rot}</span><span class="pn-kpi-v">${pct(vd)}</span><span class="pn-kpi-pe">antes: ${pct(va)} <b class="pn-seta ${melhor ? "bom" : vd === va ? "" : "ruim"}">${vd === va ? "=" : melhor ? "melhorou" : "piorou"}</b></span></div>`; };
      const maxP = Math.max(1, ...s.problemas.map((p) => p.casos));
      return `<div class="pn-kpis">
          <div class="pn-kpi escuro"><span class="pn-kpi-rot">Nota de uma pessoa real (0 a 10)</span><span class="pn-kpi-v">${num2(d.nota_media)}</span><span class="pn-kpi-pe">antes: ${num2(a.nota_media)} · ${num(s.prova)} conversas da prova final</span></div>
          ${comp("Conversas com pergunta sem relação", "com_irrelevante", true)}
          ${comp("Urgentes que foram para UPA ou SAMU", "sensibilidade_urgente")}
          <div class="pn-kpi"><span class="pn-kpi-rot">Perguntas por conversa</span><span class="pn-kpi-v">${num2(d.perguntas_media)}</span><span class="pn-kpi-pe">antes: ${num2(a.perguntas_media)}</span></div>
        </div>
        <div class="pn-grade">
          ${card("Os 5 problemas que mais sobraram", "Na prova final, depois das correções.", barras(s.problemas.map((p) => ({ rot: p.problema, v: p.casos, tip: `${p.problema}: ${p.casos} conversas` })), maxP, (x) => num(x)), "largo2")}
          ${card("Nota por família de caso", "Antes e depois das correções (0 a 10).", `<table class="pn-tabela"><thead><tr><th>Família</th><th>Antes</th><th>Depois</th><th>Perguntas</th></tr></thead><tbody>${s.por_familia.map((f) => `<tr><td>${esc(f.familia)}</td><td>${num2(f.nota_antes)}</td><td><b>${num2(f.nota)}</b></td><td>${num(f.perguntas)}</td></tr>`).join("")}</tbody></table>`)}
          ${card("O que as pessoas simuladas disseram", "Comentários de conversas com nota 6 ou menos, e de nota 10.", `<ul class="pn-falas">${[...s.exemplos, ...s.bons].map((x) => `<li><span class="pn-nota-bola ${x.nota >= 9 ? "bom" : x.nota >= 6 ? "" : "ruim"}">${x.nota}</span><div><b>“${esc(x.relato)}”</b><br><span>${esc(x.comentario)}</span></div></li>`).join("")}</ul>`)}
        </div>
        <p class="pn-rodape">${num(s.casos)} casos simulados a partir da base de sinais (NHS, MedlinePlus, Ministério da Saúde). O destino esperado ainda precisa de revisão médica. A pessoa simulada responde com sinceridade, às vezes "Não sei" e às vezes se confunde.</p>`;
    };

    const ABAS = [["motor", "Motor", abaMotor], ["simulados", "Conversas simuladas", abaSimulados], ["uso", "Uso real", abaUso], ["banco", "Banco", abaBanco]];
    const desenhar = () => {
      tela.innerHTML = `<div class="pn">
        <header class="pn-topo"><div><span class="rotulo">Painel</span><h1>Como a Tami está indo</h1></div>
          <div class="pn-abas" role="tablist">${ABAS.map(([k, n]) => `<button role="tab" aria-selected="${abaPainel === k}" data-aba="${k}">${n}</button>`).join("")}</div>
          ${local ? '<button class="btn btn-s pn-gerar" data-simular>Gerar 200 conversas de teste</button>' : ""}</header>
        <div class="pn-corpo" role="tabpanel">${ABAS.find(([k]) => k === abaPainel)[2]()}</div></div>`;
      tela.querySelectorAll("[data-aba]").forEach((b) => (b.onclick = () => { abaPainel = b.dataset.aba; desenhar(); }));
      const sim = tela.querySelector("[data-simular]");
      if (sim) sim.onclick = async (ev) => {
        ev.target.disabled = true; ev.target.textContent = "Gerando...";
        await api("POST", "/api/demo/simular", { quantidade: 200 }).catch(() => {});
        abaPainel = "uso"; painel();
      };
      dica(tela);
    };
    desenhar();
  }

  rotear();
})();
