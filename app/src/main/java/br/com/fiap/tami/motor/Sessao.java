package br.com.fiap.tami.motor;

import br.com.fiap.tami.motor.lexico.DetectorAlerta;

import java.time.LocalDateTime;
import java.util.*;

/** Estado de uma conversa. Só o {@link MotorTriagem} muda o estado. */
public final class Sessao {

    private final Perfil perfil;
    private final LocalDateTime inicio;
    private final String textoLivre;
    private final DetectorAlerta.Analise analise;
    String queixa;

    final Deque<String> fila = new ArrayDeque<>();
    String pendente;
    boolean encerrada;
    final Map<String, List<String>> respostas = new LinkedHashMap<>();
    final Map<String, Set<String>> efetivas = new HashMap<>();
    final Map<String, String> doTexto = new HashMap<>();
    final Set<String> flags = new HashSet<>();
    final Map<String, Integer> vars = new HashMap<>();
    final List<Disparo> disparos = new ArrayList<>();
    final Set<String> exige = new LinkedHashSet<>();
    final List<RespostaDada> historico = new ArrayList<>();
    /** O que a pessoa contou de si (anticoagulado, quimioterapia...). */
    Set<String> fatores = Set.of();
    /** Condição de cuidado em casa reconhecida no texto (D-006); null se nenhuma. */
    String condicaoCasa;
    /** Região tocada no corpo (nome popular), para montar a lista de confirmação depois da pergunta da origem. */
    String regiao;
    /** Fatores do perfil no início, guardados para a lista montada mais tarde. */
    Set<String> fatoresInicio = Set.of();
    /** Perguntas montadas na hora (ex.: CONF_SINAIS, a lista de sinais parecidos com o relato). */
    final Map<String, No> dinamicos = new HashMap<>();

    Sessao(Perfil perfil, LocalDateTime inicio, String textoLivre, DetectorAlerta.Analise analise, String queixa) {
        this.perfil = perfil;
        this.inicio = inicio;
        this.textoLivre = textoLivre == null ? "" : textoLivre;
        this.analise = analise;
        this.queixa = queixa;
    }

    public Perfil perfil() { return perfil; }
    public LocalDateTime inicio() { return inicio; }
    public String textoLivre() { return textoLivre; }
    public DetectorAlerta.Analise analise() { return analise; }
    /** Roteiro da queixa: FEB (febre adulto), LES (machucado: pancada, queda, torção) ou GEN (caminho genérico). */
    public String queixa() { return queixa; }
    public boolean encerrada() { return encerrada; }
    public List<Disparo> disparos() { return List.copyOf(disparos); }
    public List<RespostaDada> historico() { return List.copyOf(historico); }

    public boolean flag(String nome) { return flags.contains(nome); }
    public Set<String> fatores() { return fatores; }
    public String condicaoCasa() { return condicaoCasa; }
    public int var(String nome, int padrao) { return vars.getOrDefault(nome, padrao); }

    /** Códigos que valeram de fato, já com o "Não sei" convertido (M02 e M03). */
    public Set<String> efetiva(String noId) { return efetivas.getOrDefault(noId, Set.of()); }

    public Destino destinoAtual() {
        Destino d = null;
        for (Disparo x : disparos) d = Destino.maisGrave(d, x.destino());
        return d;
    }

    /** Lista de perguntas mostradas à pessoa, na ordem. */
    public List<String> perguntasFeitas() {
        return historico.stream().filter(r -> r.origem() == RespostaDada.Origem.USUARIO)
            .map(RespostaDada::noId).toList();
    }
}
