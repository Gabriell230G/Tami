package br.com.fiap.tami.app;

import br.com.fiap.tami.api.Json;
import br.com.fiap.tami.api.Servidor;
import br.com.fiap.tami.dao.BancoDeDados;
import br.com.fiap.tami.dao.ConexaoFactory;
import br.com.fiap.tami.dao.ConversaDAO;
import br.com.fiap.tami.dao.DorDAO;
import br.com.fiap.tami.dao.EstatisticaDAO;
import br.com.fiap.tami.dao.FeedbackDAO;
import br.com.fiap.tami.dao.PessoaDAO;
import br.com.fiap.tami.dao.RegiaoDAO;
import br.com.fiap.tami.dao.SinalDAO;
import br.com.fiap.tami.dao.UsuarioDAO;
import br.com.fiap.tami.motor.CatalogoRoteiros;
import br.com.fiap.tami.motor.MotorTriagem;
import br.com.fiap.tami.motor.conhecimento.BaseConhecimento;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.servico.CargaConhecimento;
import br.com.fiap.tami.servico.ConversaService;
import br.com.fiap.tami.servico.EstatisticaService;
import br.com.fiap.tami.servico.Simulador;
import br.com.fiap.tami.servico.Textos;
import br.com.fiap.tami.servico.Validacao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Ponto de entrada da Tami.
 *   java ... br.com.fiap.tami.app.App            ➜ sobe o app em http://localhost:8080
 *   java ... br.com.fiap.tami.app.App validar    ➜ roda os 10.000 casos de teste e grava dados/validacao.json
 *   java ... br.com.fiap.tami.app.App simular 300 ➜ grava 300 conversas de demonstração (pessoas simuladas)
 *   java ... br.com.fiap.tami.app.App zerar      ➜ apaga e recria as tabelas
 */
public final class App {

    public static void main(String[] args) throws Exception {
        String comando = args.length > 0 ? args[0] : "servidor";
        Path raiz = Path.of("").toAbsolutePath();
        Config config = new Config(raiz.resolve("tami.properties"));
        ConexaoFactory fabrica = new ConexaoFactory(config);
        Files.createDirectories(raiz.resolve("dados"));

        System.out.println("Tami · banco: " + fabrica.nome());
        BancoDeDados banco = new BancoDeDados(fabrica);
        esperarBanco(banco);
        if (comando.equals("zerar")) { banco.apagarTudo(); System.out.println("Tabelas apagadas."); }
        if (banco.preparar()) System.out.println("Tabelas criadas (11) e árvore do corpo carregada.");

        ArvoreRegioes arvore = new RegiaoDAO(fabrica).carregarArvore();
        SinalDAO sinalDAO = new SinalDAO(fabrica);
        CargaConhecimento.Relatorio carga = new CargaConhecimento(sinalDAO, arvore).carregarSeMudou();
        if (carga != null)
            System.out.printf("Base carregada: %d sinais + %d condições, %d expressões, %d ligações com o corpo (%d ms)%n",
                carga.sinais(), carga.condicoes(), carga.expressoes(), carga.vinculos(), carga.milissegundos());
        System.out.printf("Árvore: %d regiões · base: %d sinais e %d expressões no banco%n", arvore.tamanho(), sinalDAO.contar(), sinalDAO.contarExpressoes());

        // Regras ainda sem revisão médica. "demonstracao": todos os caminhos ativos. "producao": "cuidar em casa" vira UBS (trava M12).
        boolean producao = "producao".equalsIgnoreCase(config.modo());
        MotorTriagem motor = new MotorTriagem(CatalogoRoteiros.carregarDoClasspath(), regra -> !producao, BaseConhecimento.carregarDoClasspath());
        Textos textos = new Textos();

        switch (comando) {
            case "validar" -> {
                int limite = args.length > 1 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE;
                var casos = Validacao.carregar();
                if (casos.size() > limite) casos = casos.subList(0, limite);
                System.out.println("Rodando " + casos.size() + " casos de teste...");
                Map<String, Object> v = new Validacao(motor).avaliar(casos);
                Files.writeString(raiz.resolve("dados/validacao.json"), Json.escrever(v), StandardCharsets.UTF_8);
                System.out.println("Acurácia: " + v.get("acuraciaExata") + "\nSensibilidade (UPA ou SAMU): " + v.get("sensibilidadeUrgente")
                    + "\nGravado em dados/validacao.json");
                return;
            }
            case "simular" -> {
                int n = args.length > 1 ? Integer.parseInt(args[1]) : 300;
                Simulador sim = new Simulador(motor, arvore, new ConversaDAO(fabrica), new UsuarioDAO(fabrica), new DorDAO(fabrica), new FeedbackDAO(fabrica), textos).comFiltro(sinalDAO);
                System.out.println("Conversas de demonstração gravadas: " + sim.simular(n, 2026));
                return;
            }
            default -> { }
        }

        ConversaService conversas = new ConversaService(motor, arvore, textos, sinalDAO, new ConversaDAO(fabrica), new UsuarioDAO(fabrica),
            new PessoaDAO(fabrica), new DorDAO(fabrica), new FeedbackDAO(fabrica));
        EstatisticaService estatistica = new EstatisticaService(new EstatisticaDAO(fabrica), sinalDAO, arvore);
        Path val = raiz.resolve("dados/validacao.json");
        if (Files.exists(val)) {
            @SuppressWarnings("unchecked") Map<String, Object> v = (Map<String, Object>) Json.ler(Files.readString(val, StandardCharsets.UTF_8));
            estatistica.guardarValidacao(v);
        }
        Path sim = raiz.resolve("dados/simulados/painel.json");
        if (Files.exists(sim)) {
            @SuppressWarnings("unchecked") Map<String, Object> v = (Map<String, Object>) Json.ler(Files.readString(sim, StandardCharsets.UTF_8));
            estatistica.guardarSimulados(v);
        }
        Simulador simulador = new Simulador(motor, arvore, new ConversaDAO(fabrica), new UsuarioDAO(fabrica), new DorDAO(fabrica), new FeedbackDAO(fabrica), textos).comFiltro(sinalDAO);
        Servidor servidor = new Servidor(raiz.resolve("web"), fabrica, arvore, conversas, estatistica, simulador);
        servidor.iniciar(config.porta());
        manterBancoAcordado(banco);
        System.out.println("Tami no ar: http://localhost:" + config.porta() + (producao ? "  (modo produção)" : "  (modo demonstração)"));
    }

    /**
     * Enquanto o app estiver ligado, faz uma consulta leve no banco a cada 6 horas.
     * O Supabase grátis pausa o projeto depois de 7 dias sem consultas. O Render grátis
     * dorme quando ninguém usa, então quem garante de verdade é o GitHub Actions
     * (.github/workflows/manter-vivo.yml), que chama /api/vivo todo dia; isto aqui é a segunda camada.
     */
    /**
     * Se o banco estiver acordando (Supabase saindo da pausa), tenta de novo por até 3 minutos
     * antes de desistir. Assim um deploy feito nesse meio tempo não falha à toa.
     */
    private static void esperarBanco(BancoDeDados banco) throws InterruptedException {
        for (int tentativa = 1; ; tentativa++) {
            try { banco.tabelasExistem(); return; }
            catch (RuntimeException e) {
                if (tentativa >= 12) throw e;
                System.err.println("Banco ainda não respondeu (tentativa " + tentativa + " de 12). Tento de novo em 15 s.");
                Thread.sleep(15_000);
            }
        }
    }

    private static void manterBancoAcordado(BancoDeDados banco) {
        java.util.concurrent.ScheduledExecutorService agenda = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tami-batimento");
            t.setDaemon(true);
            return t;
        });
        agenda.scheduleAtFixedRate(() -> {
            try { banco.batimento(); }
            catch (RuntimeException e) { System.err.println("Batimento do banco falhou: " + e.getMessage()); }
        }, 1, 6 * 60, java.util.concurrent.TimeUnit.MINUTES);
    }
}
