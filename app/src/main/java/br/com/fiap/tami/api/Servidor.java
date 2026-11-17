package br.com.fiap.tami.api;

import br.com.fiap.tami.dao.ConexaoFactory;
import br.com.fiap.tami.dao.ConversaDAO;
import br.com.fiap.tami.dao.DaoException;
import br.com.fiap.tami.dao.PessoaDAO;
import br.com.fiap.tami.dao.UsuarioDAO;
import br.com.fiap.tami.modelo.Feedback;
import br.com.fiap.tami.modelo.Pessoa;
import br.com.fiap.tami.modelo.Relacao;
import br.com.fiap.tami.modelo.Usuario;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.regiao.NoRegiao;
import br.com.fiap.tami.servico.ConversaService;
import br.com.fiap.tami.servico.EstatisticaService;
import br.com.fiap.tami.servico.Simulador;
import br.com.fiap.tami.util.Mapa;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API REST da Tami, feita com o servidor HTTP que já vem no Java (com.sun.net.httpserver), sem framework.
 * Também entrega as telas do app (pasta web/).
 */
public final class Servidor {

    /** Uma rota: método HTTP, caminho com {parâmetros} e o que fazer. */
    private record Rota(String metodo, Pattern caminho, Acao acao) {}

    @FunctionalInterface
    private interface Acao { Object executar(Requisicao r) throws Exception; }

    /** O que chega numa requisição, já separado. */
    public record Requisicao(Map<String, String> params, Map<String, String> query, Map<String, Object> corpo) {
        String texto(String k) { Object v = corpo.get(k); return v == null ? null : String.valueOf(v); }
        boolean bool(String k) { return Boolean.TRUE.equals(corpo.get(k)) || "true".equals(String.valueOf(corpo.get(k))); }
        Integer inteiro(String k) {
            Object v = corpo.get(k);
            if (v == null || String.valueOf(v).isBlank()) return null;
            return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v).strip());
        }
        Long id(String k) { Object v = corpo.get(k); return v == null ? null : v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)); }
        long p(String k) { return Long.parseLong(params.get(k)); }
        Long q(String k) { String v = query.get(k); return v == null || v.isBlank() ? null : Long.parseLong(v); }
    }

    private final List<Rota> rotas = new ArrayList<>();

    // ---------- sessão simples: cada pessoa só mexe nos próprios dados ----------
    /** Chave do servidor (variável TAMI_SEGREDO; sem ela, uma aleatória a cada vez que o app sobe). */
    private static final byte[] SEGREDO = segredo();

    private static byte[] segredo() {
        String env = System.getenv("TAMI_SEGREDO");
        if (env != null && env.length() >= 16) return env.getBytes(StandardCharsets.UTF_8);
        byte[] b = new byte[32];
        new java.security.SecureRandom().nextBytes(b);
        return b;
    }

    /** Token do usuário: HMAC-SHA256 do id com a chave do servidor. Não precisa guardar nada no banco. */
    static String token(long idUsuario) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(SEGREDO, "HmacSHA256"));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("usuario:" + idUsuario).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** O usuário que a requisição diz ser (no caminho, na query ou no corpo), ou null. */
    private static Long usuarioDaRequisicao(String caminho, Map<String, String> query, Map<String, Object> corpo) {
        Matcher m = Pattern.compile("^/api/usuarios/(\\d+)").matcher(caminho);
        if (m.find()) return Long.parseLong(m.group(1));
        String q = query.get("usuario");
        if (q != null && !q.isBlank()) return Long.parseLong(q);
        Object c = corpo.get("usuario");
        if (c != null && !String.valueOf(c).isBlank() && !"null".equals(String.valueOf(c))) return Long.parseLong(String.valueOf(c).replaceAll("\\.0$", ""));
        return null;
    }

    private static class NaoAutorizado extends RuntimeException {
        NaoAutorizado() { super("Sua sessão expirou. Entre de novo."); }
    }
    private final Path pastaWeb;
    private HttpServer http;

    public Servidor(Path pastaWeb, ConexaoFactory fabrica, ArvoreRegioes arvore, ConversaService conversas,
                    EstatisticaService estatistica, Simulador simulador) {
        this.pastaWeb = pastaWeb;
        UsuarioDAO usuarios = new UsuarioDAO(fabrica);
        PessoaDAO pessoas = new PessoaDAO(fabrica);
        ConversaDAO conversaDAO = new ConversaDAO(fabrica);

        get("/api/status", r -> Mapa.de("ok", true, "banco", fabrica.nome(), "conversasAbertas", conversas.abertas(),
            "local", "local".equals(r.query().get("_origem"))));

        // ---------- usuário ----------
        post("/api/entrar", r -> {   // cria na primeira vez; depois só lê (o código por e-mail fica para a próxima versão)
            Usuario novo = new Usuario(r.texto("nome"), r.texto("contato"));
            Usuario u = usuarios.buscarPorContato(novo.getContato()).orElseGet(() -> usuarios.inserir(novo));
            return usuario(u, pessoas.listarPorUsuario(u.getId()).size());
        });
        get("/api/usuarios/{id}", r -> usuario(achar(usuarios, r.p("id")), pessoas.listarPorUsuario(r.p("id")).size()));
        put("/api/usuarios/{id}/consentimento", r -> {
            achar(usuarios, r.p("id"));
            usuarios.atualizarConsentimento(r.p("id"), r.bool("saude"));
            return usuario(achar(usuarios, r.p("id")), 0);
        });
        put("/api/usuarios/{id}", r -> {
            Usuario u = achar(usuarios, r.p("id"));
            u.setNome(r.texto("nome"));
            usuarios.atualizarNome(u.getId(), u.getNome());
            return usuario(u, 0);
        });
        delete("/api/usuarios/{id}", r -> Mapa.de("apagado", usuarios.apagar(r.p("id"))));
        get("/api/usuarios/{id}/dados", r -> {   // LGPD: a pessoa baixa tudo o que existe sobre ela
            Usuario u = achar(usuarios, r.p("id"));
            List<Object> conv = new ArrayList<>();
            for (Map<String, Object> h : conversas.historico(u.getId())) conv.add(conversas.resumo(((Number) h.get("id")).longValue(), u.getId()));
            return Mapa.de("usuario", usuario(u, 0), "pessoas", pessoas.listarPorUsuario(u.getId()).stream().map(Servidor::pessoa).toList(),
                "conversas", conv, "geradoEm", java.time.LocalDateTime.now().toString());
        });

        // ---------- pessoas que eu cuido ----------
        get("/api/usuarios/{id}/pessoas", r -> pessoas.listarPorUsuario(r.p("id")).stream().map(Servidor::pessoa).toList());
        post("/api/usuarios/{id}/pessoas", r -> {
            achar(usuarios, r.p("id"));
            Pessoa p = new Pessoa(r.p("id"), r.texto("apelido"), Relacao.valueOf(r.texto("relacao")), idadeMeses(r));
            p.setGestante(r.bool("gestante"));
            p.setDiabetes(r.bool("diabetes"));
            return pessoa(pessoas.inserir(p));
        });
        put("/api/usuarios/{id}/pessoas/{pid}", r -> {
            Pessoa p = pessoas.buscarPorId(r.p("pid")).filter(x -> x.getIdUsuario() == r.p("id"))
                .orElseThrow(() -> new NoSuchElementException("Pessoa não encontrada."));
            if (r.corpo().containsKey("apelido")) p.setApelido(r.texto("apelido"));
            if (r.corpo().containsKey("relacao")) p.setRelacao(Relacao.valueOf(r.texto("relacao")));
            if (r.corpo().containsKey("idade") || r.corpo().containsKey("idadeMeses")) p.setIdadeMeses(idadeMeses(r));
            if (r.corpo().containsKey("gestante")) p.setGestante(r.bool("gestante"));
            if (r.corpo().containsKey("diabetes")) p.setDiabetes(r.bool("diabetes"));
            pessoas.atualizar(p);
            return pessoa(pessoas.buscarPorId(p.getId()).orElseThrow());
        });
        delete("/api/usuarios/{id}/pessoas/{pid}", r -> Mapa.de("apagado", pessoas.apagar(r.p("pid"), r.p("id"))));

        // ---------- histórico ----------
        get("/api/usuarios/{id}/historico", r -> conversas.historico(r.p("id")));
        delete("/api/usuarios/{id}/conversas/{cid}", r -> Mapa.de("apagado", conversaDAO.apagar(r.p("cid"), r.p("id"))));

        // ---------- árvore do corpo ----------
        get("/api/regioes", r -> no(arvore.raiz()));
        post("/api/filtro", r -> conversas.filtro(r.texto("texto"), r.texto("regiao")));

        // ---------- conversa ----------
        post("/api/conversas", r -> conversas.iniciar(new ConversaService.Inicio(r.id("usuario"), r.id("pessoa"), idadeOuNulo(r),
            r.bool("gestante"), r.bool("diabetes"), r.texto("relato"), r.texto("regiao"), r.bool("guardar"))));
        post("/api/conversas/{id}/respostas", r -> {
            @SuppressWarnings("unchecked") List<Object> cods = (List<Object>) r.corpo().get("codigos");
            if (cods == null || cods.isEmpty()) throw new IllegalArgumentException("Escolha uma opção.");
            return conversas.responder(r.p("id"), r.texto("pergunta"), cods.stream().map(String::valueOf).toList());
        });
        get("/api/conversas/{id}", r -> conversas.resumo(r.p("id"), r.q("usuario")));
        post("/api/conversas/{id}/dor", r -> conversas.registrarDor(r.p("id"), r.texto("regiao"), r.texto("lado"),
            r.inteiro("nivel") == null ? -1 : r.inteiro("nivel"), r.texto("tipo")));
        put("/api/dor/{id}", r -> {   // UPDATE: a pessoa mudou o nível da dor
            Integer nivel = r.inteiro("nivel");
            if (nivel == null) throw new IllegalArgumentException("Diga o nível da dor, de 0 a 10.");
            if (!new br.com.fiap.tami.dao.DorDAO(fabrica).atualizarNivel(r.p("id"), nivel)) throw new NoSuchElementException("Registro de dor não encontrado.");
            return Mapa.de("id", r.p("id"), "nivel", nivel);
        });
        put("/api/conversas/{id}/avaliacao", r -> conversas.avaliar(r.p("id"), Feedback.Avaliacao.valueOf(r.texto("avaliacao")), r.texto("comentario")));

        // ---------- estatística ----------
        get("/api/estatisticas", r -> Mapa.de("uso", estatistica.uso(), "validacao", estatistica.validacao(), "simulados", estatistica.simulados()));
        post("/api/demo/simular", r -> {   // só na máquina de quem roda o app: no link público não dá para encher o banco
            if (!"local".equals(r.query().get("_origem"))) throw new IllegalStateException("Conversas de teste só podem ser geradas rodando o app no computador.");
            Integer n = r.inteiro("quantidade");
            int feitas = simulador.simular(Math.max(1, Math.min(n == null ? 200 : n, 2000)), System.currentTimeMillis());
            return Mapa.de("geradas", feitas);
        });
    }

    // ---------------------------------------------------------------- formato das respostas

    private static Map<String, Object> usuario(Usuario u, int pessoas) {
        return Mapa.de("id", u.getId(), "nome", u.getNome(), "contato", u.getContato(), "consentimentoSaude", u.isConsentimentoSaude(),
            "dataConsentimento", u.getDataConsentimento() == null ? null : u.getDataConsentimento().toString(),
            "cadastro", String.valueOf(u.getDataCadastro()), "pessoas", pessoas, "token", token(u.getId()));
    }

    private static Map<String, Object> pessoa(Pessoa p) {
        return Mapa.de("id", p.getId(), "apelido", p.getApelido(), "relacao", p.getRelacao().name(), "relacaoTexto", p.getRelacao().getRotulo(),
            "idadeMeses", p.getIdadeMeses(), "idade", p.idadeEscrita(), "gestante", p.isGestante(), "diabetes", p.isDiabetes());
    }

    private static Map<String, Object> no(NoRegiao n) {
        return Mapa.de("codigo", n.getCodigo(), "nome", n.getNome(), "nivel", n.getNivel(),
            "filhos", n.getFilhos().stream().map(Servidor::no).toList());
    }

    private static Usuario achar(UsuarioDAO dao, long id) {
        return dao.buscarPorId(id).orElseThrow(() -> new NoSuchElementException("Usuário não encontrado."));
    }

    /** Idade em meses: aceita "idadeMeses" ou "idade" + "unidade" (anos ou meses). */
    private static int idadeMeses(Requisicao r) {
        Integer v = idadeOuNulo(r);
        if (v == null) throw new IllegalArgumentException("Diga a idade.");
        return v;
    }

    private static Integer idadeOuNulo(Requisicao r) {
        Integer m = r.inteiro("idadeMeses");
        if (m != null) return m;
        Integer i = r.inteiro("idade");
        if (i == null) return null;
        return "meses".equals(r.texto("unidade")) ? i : i * 12;
    }

    // ---------------------------------------------------------------- roteamento

    private void get(String c, Acao a) { rota("GET", c, a); }
    private void post(String c, Acao a) { rota("POST", c, a); }
    private void put(String c, Acao a) { rota("PUT", c, a); }
    private void delete(String c, Acao a) { rota("DELETE", c, a); }

    private void rota(String metodo, String caminho, Acao acao) {
        String regex = caminho.replaceAll("\\{(\\w+)}", "(?<$1>[0-9A-Za-z_]+)");
        rotas.add(new Rota(metodo, Pattern.compile("^" + regex + "$"), acao));
    }

    public void iniciar(int porta) throws IOException {
        http = HttpServer.create(new InetSocketAddress(porta), 0);
        http.createContext("/", this::atender);
        http.setExecutor(Executors.newFixedThreadPool(8));
        http.start();
    }

    public void parar() { if (http != null) http.stop(0); }

    private void atender(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            if (!caminho.startsWith("/api/")) { arquivo(ex, caminho); return; }
            for (Rota rota : rotas) {
                Matcher m = rota.caminho().matcher(caminho);
                if (!m.matches()) continue;
                String metodo = "HEAD".equals(ex.getRequestMethod()) ? "GET" : ex.getRequestMethod();
                if (!rota.metodo().equals(metodo)) continue;
                Map<String, String> params = new HashMap<>();
                for (String nome : nomesDosGrupos(rota.caminho().pattern())) params.put(nome, m.group(nome));
                String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> q = new HashMap<>(query(ex.getRequestURI().getRawQuery()));
                q.remove("_origem");
                if (ex.getRemoteAddress().getAddress().isLoopbackAddress() && ex.getRequestHeaders().getFirst("X-Forwarded-For") == null)
                    q.put("_origem", "local");
                Map<String, Object> mc = corpo.isBlank() ? Map.of() : Json.objeto(corpo);
                Long quem = caminho.equals("/api/entrar") ? null : usuarioDaRequisicao(caminho, q, mc);
                if (quem != null && !java.security.MessageDigest.isEqual(token(quem).getBytes(StandardCharsets.UTF_8),
                        String.valueOf(ex.getRequestHeaders().getFirst("X-Tami-Token")).getBytes(StandardCharsets.UTF_8)))
                    throw new NaoAutorizado();
                Object resultado = rota.acao().executar(new Requisicao(params, q, mc));
                responder(ex, 200, Json.escrever(resultado));
                return;
            }
            responder(ex, 404, Json.escrever(Mapa.de("erro", "Endereço não encontrado: " + ex.getRequestMethod() + " " + caminho)));
        } catch (NaoAutorizado e) {
            responder(ex, 401, Json.escrever(Mapa.de("erro", e.getMessage())));
        } catch (IllegalArgumentException e) {
            responder(ex, 400, Json.escrever(Mapa.de("erro", e.getMessage())));
        } catch (NoSuchElementException e) {
            responder(ex, 404, Json.escrever(Mapa.de("erro", e.getMessage())));
        } catch (IllegalStateException e) {
            responder(ex, 409, Json.escrever(Mapa.de("erro", e.getMessage())));
        } catch (DaoException e) {
            System.err.println("[banco] " + e.getMessage());
            responder(ex, 500, Json.escrever(Mapa.de("erro", "Problema no banco de dados. Tente de novo.")));
        } catch (Exception e) {
            e.printStackTrace();
            responder(ex, 500, Json.escrever(Mapa.de("erro", "Algo deu errado aqui. Tente de novo.")));
        } finally {
            ex.close();
        }
    }

    private static List<String> nomesDosGrupos(String regex) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\\(\\?<(\\w+)>").matcher(regex);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static Map<String, String> query(String q) {
        Map<String, String> out = new HashMap<>();
        if (q == null) return out;
        for (String par : q.split("&")) {
            int i = par.indexOf('=');
            if (i > 0) out.put(URLDecoder.decode(par.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static void responder(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        enviar(ex, status, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Manda a resposta. HEAD vai sem corpo (o Render e outros proxies usam HEAD para ver se o app está de pé), e a conexão
     * fecha no fim: atrás de um proxy, conexão reaproveitada que o servidor do Java já fechou virava erro de vez em quando.
     */
    private static void enviar(HttpExchange ex, int status, byte[] b) throws IOException {
        ex.getResponseHeaders().set("Connection", "close");
        if ("HEAD".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static final Map<String, String> TIPOS = Map.of("html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
        "js", "text/javascript; charset=utf-8", "svg", "image/svg+xml", "png", "image/png", "webp", "image/webp", "woff2", "font/woff2",
        "json", "application/json; charset=utf-8", "ico", "image/x-icon");

    private void arquivo(HttpExchange ex, String caminho) throws IOException {
        if (caminho.equals("/") || caminho.isBlank()) caminho = "/index.html";
        Path p = pastaWeb.resolve(caminho.substring(1)).normalize();
        if (!p.startsWith(pastaWeb) || !Files.isRegularFile(p)) {
            enviar(ex, 404, "Não encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String nome = p.getFileName().toString();
        String ext = nome.substring(nome.lastIndexOf('.') + 1);
        byte[] b = Files.readAllBytes(p);
        ex.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(ext, "application/octet-stream"));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        enviar(ex, 200, b);
    }
}
