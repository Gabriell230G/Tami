package br.com.fiap.tami.servico;

import br.com.fiap.tami.dao.ConversaDAO;
import br.com.fiap.tami.dao.DorDAO;
import br.com.fiap.tami.dao.FeedbackDAO;
import br.com.fiap.tami.dao.PessoaDAO;
import br.com.fiap.tami.dao.SinalDAO;
import br.com.fiap.tami.dao.UsuarioDAO;
import br.com.fiap.tami.modelo.Conversa;
import br.com.fiap.tami.modelo.Feedback;
import br.com.fiap.tami.modelo.Pessoa;
import br.com.fiap.tami.modelo.RegistroDor;
import br.com.fiap.tami.modelo.RegraAcionada;
import br.com.fiap.tami.modelo.RespostaRegistrada;
import br.com.fiap.tami.modelo.Usuario;
import br.com.fiap.tami.motor.Destino;
import br.com.fiap.tami.motor.Disparo;
import br.com.fiap.tami.motor.MotorTriagem;
import br.com.fiap.tami.motor.No;
import br.com.fiap.tami.motor.Opcao;
import br.com.fiap.tami.motor.Perfil;
import br.com.fiap.tami.motor.RespostaDada;
import br.com.fiap.tami.motor.Resultado;
import br.com.fiap.tami.motor.Sessao;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.regiao.NoRegiao;
import br.com.fiap.tami.util.Mapa;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regra de negócio da conversa: liga a árvore do corpo (filtro), o motor de regras (as perguntas) e o banco (DAOs).
 * A sessão do motor fica na memória enquanto a conversa acontece; o banco guarda o começo e o fim.
 */
public final class ConversaService {

    /** O que o app manda para começar. */
    public record Inicio(Long idUsuario, Long idPessoa, Integer idadeMeses, boolean gestante, boolean diabetes,
                         String relato, String codigoRegiao, boolean guardar) {}

    private record Andamento(Conversa conversa, Sessao sessao, LocalDateTime criada) {}

    private static final Duration VALIDADE = Duration.ofHours(2);

    private final MotorTriagem motor;
    private final ArvoreRegioes arvore;
    private final Textos textos;
    private final SinalDAO sinalDAO;
    private final ConversaDAO conversaDAO;
    private final UsuarioDAO usuarioDAO;
    private final PessoaDAO pessoaDAO;
    private final DorDAO dorDAO;
    private final FeedbackDAO feedbackDAO;
    private final Map<Long, Andamento> andamento = new ConcurrentHashMap<>();
    private volatile int totalSinais = -1;

    public ConversaService(MotorTriagem motor, ArvoreRegioes arvore, Textos textos, SinalDAO sinalDAO, ConversaDAO conversaDAO,
                           UsuarioDAO usuarioDAO, PessoaDAO pessoaDAO, DorDAO dorDAO, FeedbackDAO feedbackDAO) {
        this.motor = motor;
        this.arvore = arvore;
        this.textos = textos;
        this.sinalDAO = sinalDAO;
        this.conversaDAO = conversaDAO;
        this.usuarioDAO = usuarioDAO;
        this.pessoaDAO = pessoaDAO;
        this.dorDAO = dorDAO;
        this.feedbackDAO = feedbackDAO;
    }

    // ---------------------------------------------------------------- filtro pela árvore

    /** "Escreveu braço": acha a região, sobe a árvore para mostrar o caminho e conta no banco o que sobra. */
    public Map<String, Object> filtro(String texto, String codigoRegiao) {
        Optional<NoRegiao> no = codigoRegiao != null && !codigoRegiao.isBlank()
            ? arvore.buscar(codigoRegiao) : arvore.principal(texto).map(ArvoreRegioes.Achado::regiao);
        if (totalSinais < 0) totalSinais = sinalDAO.contar();
        int totalExpr = sinalDAO.contarExpressoes();
        if (no.isEmpty() || no.get().getNivel() == 0)
            return Mapa.de("regiao", null, "sinaisTotal", totalSinais, "expressoesTotal", totalExpr);
        NoRegiao n = no.get();
        List<String> codigos = arvore.codigosDaSubArvore(n.getCodigo());
        SinalDAO.Filtro f = sinalDAO.filtrar(codigos);
        List<Map<String, Object>> exemplos = new ArrayList<>();
        for (String[] e : sinalDAO.exemplos(codigos, 6)) exemplos.add(Mapa.de("codigo", e[0], "texto", e[1], "destino", e[2]));
        List<String> outras = arvore.procurar(texto).stream().map(a -> a.regiao().getNome()).filter(x -> !x.equals(n.getNome())).toList();
        return Mapa.de(
            "regiao", Mapa.de("codigo", n.getCodigo(), "nome", n.getNome(), "nivel", n.getNivel()),
            "caminho", n.caminho().stream().map(NoRegiao::getNome).toList(),
            "subregioes", codigos.size(),
            "sinaisTotal", totalSinais, "sinaisRegiao", f.sinais(),
            "expressoesTotal", totalExpr, "expressoesRegiao", f.expressoes(),
            "porDestino", f.porDestino(), "exemplos", exemplos, "outrasRegioes", outras);
    }

    // ---------------------------------------------------------------- conversa

    public Map<String, Object> iniciar(Inicio in) {
        limparVelhas();
        Perfil perfil;
        Long idPessoa = in.idPessoa();
        if (idPessoa != null) {
            Pessoa p = pessoaDAO.buscarPorId(idPessoa).orElseThrow(() -> new NoSuchElementException("Pessoa não encontrada."));
            if (in.idUsuario() == null || !p.getIdUsuario().equals(in.idUsuario())) throw new IllegalArgumentException("Essa pessoa não é sua.");
            perfil = p.comoPerfil();
        } else {
            if (in.idadeMeses() == null) throw new IllegalArgumentException("Diga a idade de quem precisa de ajuda.");
            perfil = new Perfil(in.idadeMeses(), in.gestante(), in.diabetes());
        }
        String relato = in.relato() == null ? "" : in.relato().strip();
        if (relato.length() > 4000) relato = relato.substring(0, 4000);
        boolean guardar = in.guardar() && in.idUsuario() != null
            && usuarioDAO.buscarPorId(in.idUsuario()).map(Usuario::isConsentimentoSaude).orElse(false);

        Map<String, Object> filtro = filtro(relato, in.codigoRegiao());
        @SuppressWarnings("unchecked") Map<String, Object> reg = (Map<String, Object>) filtro.get("regiao");
        NoRegiao no = reg == null ? null : arvore.buscar((String) reg.get("codigo")).orElse(null);

        LocalDateTime agora = LocalDateTime.now();
        Sessao s = motor.iniciar(perfil, relato, null, agora, Set.of(), ArvoreRegioes.nomeParaMotor(no));

        Conversa cv = new Conversa();
        cv.setIdUsuario(in.idUsuario());
        cv.setIdPessoa(idPessoa);
        cv.setIdadeMeses(perfil.idadeMeses());
        cv.setGuardar(guardar);
        cv.setRelato(relato);
        cv.setCodigoRegiao(no == null ? null : no.getCodigo());
        cv.setSinaisTotal((Integer) filtro.get("sinaisTotal"));
        cv.setSinaisRegiao((Integer) filtro.get("sinaisRegiao"));
        cv.setQueixa(s.queixa());
        cv.setInicio(agora);
        conversaDAO.inserir(cv);
        andamento.put(cv.getId(), new Andamento(cv, s, agora));

        Map<String, Object> out = proximoPasso(cv.getId());
        out.put("filtro", filtro);
        out.put("guardando", guardar);
        out.put("abertura", textos.texto("POS_RELATO"));
        return out;
    }

    public Map<String, Object> responder(long idConversa, String noId, List<String> codigos) {
        Andamento a = andamento.get(idConversa);
        if (a == null) throw new NoSuchElementException("Essa conversa já acabou ou expirou. Comece de novo.");
        synchronized (a) {
            motor.responder(a.sessao(), noId, codigos);
            return proximoPasso(idConversa);
        }
    }

    /** A próxima pergunta, ou o resultado (e aí a conversa é gravada no banco). */
    private Map<String, Object> proximoPasso(long idConversa) {
        Andamento a = andamento.get(idConversa);
        Optional<No> prox = motor.proxima(a.sessao());
        if (prox.isPresent()) {
            No n = prox.get();
            return Mapa.de("conversa", idConversa, "fim", false, "pergunta", pergunta(a.sessao(), n),
                "respondidas", a.sessao().historico().size());
        }
        Resultado r = motor.resultado(a.sessao());
        finalizar(a, r);
        andamento.remove(idConversa);
        return Mapa.de("conversa", idConversa, "fim", true, "resultado", resultado(r, a.conversa()),
            "respondidas", a.sessao().historico().size());
    }

    private Map<String, Object> pergunta(Sessao s, No n) {
        String texto = textos.pergunta(n.id(), n.pergunta());
        // "Você tem algum destes? • A • B • C" vira enunciado + lista
        String[] partes = texto.split("\\s*•\\s*");
        List<String> itens = new ArrayList<>();
        for (int i = 1; i < partes.length; i++) if (!partes[i].isBlank()) itens.add(partes[i].strip());
        List<Map<String, Object>> ops = new ArrayList<>();
        for (Opcao o : n.opcoes()) ops.add(Mapa.de("codigo", o.codigo(), "rotulo", o.rotulo()));
        String intro = n.id().equals(MotorTriagem.CONF_SINAIS) ? textos.texto("LISTA_CONFIRMACAO")
            : n.id().equals(MotorTriagem.CONF_SINAIS_2) ? textos.texto("LISTA_CONFIRMACAO_2") : null;
        return Mapa.de("id", n.id(), "texto", partes[0].strip(), "itens", itens, "ajuda", n.ajuda(),
            "multipla", n.tipo() == No.Tipo.MULTI, "alarme", n.alarme(), "opcoes", ops,
            "preMarcados", motor.preMarcados(s, n.id()), "intro", intro);
    }

    private Map<String, Object> resultado(Resultado r, Conversa cv) {
        List<Map<String, Object>> blocos = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (String x : r.exige()) {
            String t = textos.texto(x);
            if (t != null && vistos.add(x)) blocos.add(Mapa.de("codigo", x, "texto", t));
        }
        List<String> porque = new ArrayList<>();
        for (RespostaDada d : r.respostas())
            if (d.origem() != RespostaDada.Origem.HERDADA && !d.codigos().contains("nenhum") && !d.codigos().contains("N")
                && d.noId().startsWith("CONF"))
                porque.addAll(d.rotulos());
        return Mapa.de("destino", r.destino().name(), "mensagem", textos.texto(r.mensagemId()), "codigoMensagem", r.mensagemId(),
            "prioridadeUbs", r.prioridadeUbs() == null ? null : r.prioridadeUbs().name(), "blocos", blocos,
            "crise", r.telaCrise(), "porNaoSei", r.porNaoSei(), "sinaisConfirmados", porque,
            "fechamento", textos.texto("FECHAMENTO"), "perguntas", cv.getPerguntas());
    }

    private void finalizar(Andamento a, Resultado r) {
        Conversa cv = a.conversa();
        cv.setFim(LocalDateTime.now());
        cv.setDestino(r.destino());
        cv.setPrioridadeUbs(r.prioridadeUbs() == null ? null : r.prioridadeUbs().name());
        cv.setCodigoMensagem(r.mensagemId());
        cv.setCrise(r.telaCrise());
        cv.setPorNaoSei(r.porNaoSei());
        List<RespostaRegistrada> resp = new ArrayList<>();
        int ordem = 0;
        for (RespostaDada d : r.respostas()) {
            if (d.origem() == RespostaDada.Origem.USUARIO) cv.setPerguntas(cv.getPerguntas() + 1);
            resp.add(new RespostaRegistrada(null, cv.getId(), ++ordem, d.noId(), textos.pergunta(d.noId(), d.pergunta()),
                String.join(", ", d.rotulos()), d.origem().name(), d.porNaoSei(), LocalDateTime.now()));
        }
        List<RegraAcionada> regras = new ArrayList<>();
        for (Disparo d : r.disparos())
            regras.add(new RegraAcionada(null, cv.getId(), d.regra(), d.destino().name(), d.motivo()));
        conversaDAO.finalizar(cv, resp, regras);
    }

    private void limparVelhas() {
        LocalDateTime limite = LocalDateTime.now().minus(VALIDADE);
        andamento.values().removeIf(a -> a.criada().isBefore(limite));
    }

    // ---------------------------------------------------------------- depois da conversa

    /** O resumo para mostrar no atendimento (lido do banco). */
    public Map<String, Object> resumo(long idConversa, Long idUsuario) {
        Conversa cv = conversaDAO.buscarPorId(idConversa).orElseThrow(() -> new NoSuchElementException("Conversa não encontrada."));
        if (cv.getIdUsuario() != null && !cv.getIdUsuario().equals(idUsuario)) throw new IllegalArgumentException("Essa conversa não é sua.");
        List<Map<String, Object>> resp = new ArrayList<>();
        for (RespostaRegistrada r : conversaDAO.respostas(idConversa))
            resp.add(Mapa.de("pergunta", r.pergunta(), "resposta", r.resposta(), "origem", r.origem(), "naoSei", r.naoSei()));
        List<Map<String, Object>> dores = new ArrayList<>();
        for (RegistroDor d : dorDAO.listarPorConversa(idConversa))
            dores.add(Mapa.de("id", d.id(), "regiao", arvore.buscar(d.codigoRegiao()).map(NoRegiao::getNome).orElse(d.codigoRegiao()),
                "lado", d.lado(), "nivel", d.nivel(), "faixa", d.faixa(), "tipo", d.tipo()));
        Optional<Feedback> fb = feedbackDAO.buscarPorConversa(idConversa);
        String pessoa = cv.getIdPessoa() == null ? null : pessoaDAO.buscarPorId(cv.getIdPessoa()).map(Pessoa::getApelido).orElse(null);
        return Mapa.de("id", cv.getId(), "inicio", String.valueOf(cv.getInicio()), "segundos", cv.segundos(),
            "pessoa", pessoa, "idade", idadeEscrita(cv.getIdadeMeses()), "guardado", cv.isGuardar(),
            "relato", cv.getRelato(), "regiao", cv.getCodigoRegiao() == null ? null : arvore.buscar(cv.getCodigoRegiao()).map(NoRegiao::getNome).orElse(null),
            "destino", cv.getDestino() == null ? null : cv.getDestino().name(),
            "mensagem", cv.getCodigoMensagem() == null ? null : textos.texto(cv.getCodigoMensagem()),
            "crise", cv.isCrise(), "perguntas", cv.getPerguntas(), "respostas", resp, "dores", dores,
            "avaliacao", fb.map(f -> f.avaliacao().name()).orElse(null),
            "aviso", "A Tami não faz diagnóstico. Este resumo é só o que foi contado para ela.");
    }

    public List<Map<String, Object>> historico(long idUsuario) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Conversa cv : conversaDAO.listarPorUsuario(idUsuario, 50)) {
            String pessoa = cv.getIdPessoa() == null ? null : pessoaDAO.buscarPorId(cv.getIdPessoa()).map(Pessoa::getApelido).orElse(null);
            out.add(Mapa.de("id", cv.getId(), "inicio", String.valueOf(cv.getInicio()), "destino", cv.getDestino().name(),
                "pessoa", pessoa, "relato", cv.getRelato(), "regiao", cv.getCodigoRegiao() == null ? null
                    : arvore.buscar(cv.getCodigoRegiao()).map(NoRegiao::getNome).orElse(null),
                "perguntas", cv.getPerguntas(), "avaliacao", feedbackDAO.buscarPorConversa(cv.getId()).map(f -> f.avaliacao().name()).orElse(null)));
        }
        return out;
    }

    public Map<String, Object> registrarDor(long idConversa, String codigoRegiao, String lado, int nivel, String tipo) {
        conversaDAO.buscarPorId(idConversa).orElseThrow(() -> new NoSuchElementException("Conversa não encontrada."));
        NoRegiao n = arvore.buscar(codigoRegiao).orElseThrow(() -> new IllegalArgumentException("Região do corpo desconhecida."));
        RegistroDor d = dorDAO.inserir(new RegistroDor(null, idConversa, n.getCodigo(), lado, nivel, tipo, null));
        return Mapa.de("id", d.id(), "regiao", n.getNome(), "nivel", d.nivel(), "faixa", d.faixa());
    }

    public Map<String, Object> avaliar(long idConversa, Feedback.Avaliacao avaliacao, String comentario) {
        conversaDAO.buscarPorId(idConversa).orElseThrow(() -> new NoSuchElementException("Conversa não encontrada."));
        Feedback f = feedbackDAO.salvar(idConversa, avaliacao, comentario);
        String resposta = switch (avaliacao) {
            case SIM -> textos.texto("FECHAMENTO_SIM");
            case NAO, MAIS_OU_MENOS -> textos.texto("FECHAMENTO_NAO");
        };
        return Mapa.de("id", f.id(), "avaliacao", f.avaliacao().name(), "atualizada", f.atualizacao() != null, "resposta", resposta);
    }

    public boolean emAndamento(long idConversa) { return andamento.containsKey(idConversa); }

    public int abertas() { return andamento.size(); }

    static String idadeEscrita(int meses) {
        if (meses < 24) return meses + (meses == 1 ? " mês" : " meses");
        return (meses / 12) + " anos";
    }

    public Destino[] destinos() { return Destino.values(); }
}
