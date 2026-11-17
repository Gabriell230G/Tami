package br.com.fiap.tami.servico;

import br.com.fiap.tami.dao.ConversaDAO;
import br.com.fiap.tami.dao.DorDAO;
import br.com.fiap.tami.dao.FeedbackDAO;
import br.com.fiap.tami.dao.UsuarioDAO;
import br.com.fiap.tami.modelo.Conversa;
import br.com.fiap.tami.modelo.Feedback;
import br.com.fiap.tami.modelo.RegistroDor;
import br.com.fiap.tami.modelo.RegraAcionada;
import br.com.fiap.tami.modelo.RespostaRegistrada;
import br.com.fiap.tami.modelo.Usuario;
import br.com.fiap.tami.motor.Destino;
import br.com.fiap.tami.motor.Disparo;
import br.com.fiap.tami.motor.MotorTriagem;
import br.com.fiap.tami.motor.No;
import br.com.fiap.tami.motor.RespostaDada;
import br.com.fiap.tami.motor.Resultado;
import br.com.fiap.tami.motor.Sessao;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.regiao.NoRegiao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Gera uso de DEMONSTRAÇÃO para o painel: pega casos de teste ao acaso e roda cada um no motor de verdade,
 * gravando no banco como conversas de "pessoas simuladas" (contato terminando em @tami.demo).
 * O destino vem do motor; a avaliação e o nível da dor são sorteados. Nada disso é dado de gente real.
 */
public final class Simulador {

    private final MotorTriagem motor;
    private final ArvoreRegioes arvore;
    private final ConversaDAO conversaDAO;
    private final UsuarioDAO usuarioDAO;
    private final DorDAO dorDAO;
    private final FeedbackDAO feedbackDAO;
    private final Textos textos;
    private br.com.fiap.tami.dao.SinalDAO sinalDAO;

    /** Para gravar quanto a árvore do corpo enxugou a busca em cada conversa simulada. */
    public Simulador comFiltro(br.com.fiap.tami.dao.SinalDAO dao) { this.sinalDAO = dao; return this; }

    public Simulador(MotorTriagem motor, ArvoreRegioes arvore, ConversaDAO conversaDAO, UsuarioDAO usuarioDAO,
                     DorDAO dorDAO, FeedbackDAO feedbackDAO, Textos textos) {
        this.motor = motor;
        this.arvore = arvore;
        this.conversaDAO = conversaDAO;
        this.usuarioDAO = usuarioDAO;
        this.dorDAO = dorDAO;
        this.feedbackDAO = feedbackDAO;
        this.textos = textos;
    }

    public int simular(int quantas, long semente) {
        Random rnd = new Random(semente);
        List<Validacao.Caso> casos = Validacao.carregar();
        List<Usuario> usuarios = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, quantas / 4); i++) {
            String contato = "simulado-" + semente + "-" + i + "@tami.demo";
            final int num = i;
            Usuario u = usuarioDAO.buscarPorContato(contato).orElseGet(() -> usuarioDAO.inserir(new Usuario("Pessoa simulada " + num, contato)));
            usuarios.add(u);
        }
        int feitas = 0;
        for (int i = 0; i < quantas; i++) {
            Validacao.Caso caso = casos.get(rnd.nextInt(casos.size()));
            Optional<ArvoreRegioes.Achado> achado = arvore.principal(caso.texto());
            NoRegiao no = achado.map(ArvoreRegioes.Achado::regiao).filter(n -> n.getNivel() > 0).orElse(null);
            // horários mais comuns à noite e no começo da manhã; últimos 14 dias
            int hora = new int[]{7, 8, 9, 10, 12, 14, 17, 19, 20, 21, 22, 23, 0, 2}[rnd.nextInt(14)];
            LocalDateTime inicio = LocalDateTime.now().minusDays(rnd.nextInt(14)).withHour(hora).withMinute(rnd.nextInt(60));
            Sessao s = motor.iniciar(caso.perfil(), caso.texto(), null, inicio, caso.fatores(), ArvoreRegioes.nomeParaMotor(no));
            Optional<No> prox;
            int voltas = 0;
            while ((prox = motor.proxima(s)).isPresent() && voltas++ < 60) motor.responder(s, prox.get().id(), Validacao.responder(caso, prox.get(), motor));
            Resultado r = motor.resultado(s);

            Conversa cv = new Conversa();
            cv.setIdUsuario(usuarios.get(rnd.nextInt(usuarios.size())).getId());
            cv.setIdadeMeses(caso.perfil().idadeMeses());
            cv.setGuardar(false);   // simulado: não guarda relato nem respostas
            cv.setCodigoRegiao(no == null ? null : no.getCodigo());
            cv.setQueixa(s.queixa());
            cv.setInicio(inicio);
            if (sinalDAO != null) {
                cv.setSinaisTotal(sinalDAO.contar());
                if (no != null) cv.setSinaisRegiao(sinalDAO.filtrar(arvore.codigosDaSubArvore(no.getCodigo())).sinais());
            }
            conversaDAO.inserir(cv);
            int perguntas = (int) r.respostas().stream().filter(d -> d.origem() == RespostaDada.Origem.USUARIO).count();
            cv.setPerguntas(perguntas);
            cv.setFim(inicio.plusSeconds(20 + perguntas * (6L + rnd.nextInt(10))));
            cv.setDestino(r.destino());
            cv.setPrioridadeUbs(r.prioridadeUbs() == null ? null : r.prioridadeUbs().name());
            cv.setCodigoMensagem(r.mensagemId());
            cv.setCrise(r.telaCrise());
            cv.setPorNaoSei(r.porNaoSei());
            List<RegraAcionada> regras = new ArrayList<>();
            for (Disparo d : r.disparos()) regras.add(new RegraAcionada(null, cv.getId(), d.regra(), d.destino().name(), d.motivo()));
            conversaDAO.finalizar(cv, List.<RespostaRegistrada>of(), regras);
            if (no != null && rnd.nextDouble() < 0.6) {
                int base = switch (r.destino()) { case SAMU_192 -> 7; case UPA_HOJE -> 6; case UBS_EM_BREVE -> 4; case CUIDADO_EM_CASA -> 2; };
                int nivel = Math.max(0, Math.min(10, base + rnd.nextInt(5) - 2));
                String lado = new String[]{"DIREITO", "ESQUERDO", "MEIO", null}[rnd.nextInt(4)];
                dorDAO.inserir(new RegistroDor(null, cv.getId(), no.getCodigo(), lado, nivel, null, null));
            }
            if (rnd.nextDouble() < 0.7) {
                double x = rnd.nextDouble();
                Feedback.Avaliacao a = x < 0.72 ? Feedback.Avaliacao.SIM : x < 0.9 ? Feedback.Avaliacao.MAIS_OU_MENOS : Feedback.Avaliacao.NAO;
                feedbackDAO.salvar(cv.getId(), a, null);
            }
            feitas++;
        }
        return feitas;
    }

    public static boolean simulado(Usuario u) { return u.getContato().endsWith("@tami.demo"); }

    static Destino destino(String s) { return Destino.valueOf(s); }
}
