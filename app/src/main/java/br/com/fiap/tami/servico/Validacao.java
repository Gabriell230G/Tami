package br.com.fiap.tami.servico;

import br.com.fiap.tami.motor.Destino;
import br.com.fiap.tami.motor.MotorTriagem;
import br.com.fiap.tami.motor.No;
import br.com.fiap.tami.motor.Opcao;
import br.com.fiap.tami.motor.Perfil;
import br.com.fiap.tami.motor.Resultado;
import br.com.fiap.tami.motor.Sessao;
import br.com.fiap.tami.util.Estatistica;
import br.com.fiap.tami.util.Mapa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Validação do motor com os 10.000 casos de teste (vinhetas_10k.csv): cada caso é uma pessoa inventada, com relato,
 * respostas e o destino esperado. O gabarito foi escrito a partir das fontes públicas e AINDA NÃO foi revisado por médico.
 */
public final class Validacao {

    /** Terça-feira, 10h: horário com UBS aberta (o mesmo dos testes do motor). */
    public static final LocalDateTime TERCA_10H = LocalDateTime.of(2026, 11, 10, 10, 0);

    public record Caso(String id, Perfil perfil, Set<String> fatores, String regiao, String texto,
                       Map<String, List<String>> respostas, Destino esperado, String sinal, String armadilha) {}

    public record Saida(Caso caso, Destino obtido, int perguntas, String erro) {
        public int diferenca() { return obtido == null ? -9 : obtido.compareTo(caso.esperado()); }
    }

    private final MotorTriagem motor;

    public Validacao(MotorTriagem motor) { this.motor = motor; }

    public static List<Caso> carregar() {
        List<Caso> out = new ArrayList<>();
        for (Map<String, String> l : CargaConhecimento.ler("/conhecimento/vinhetas_10k.csv")) {
            Set<String> fat = fatores(l.get("comorbidades"), l.get("remedios_em_uso"));
            fat.add("M".equals(l.get("sexo")) ? "homem" : "mulher");
            Perfil p = new Perfil(Integer.parseInt(l.get("idade_meses")), "S".equals(l.get("gestante")), fat.contains("diabetico"));
            Map<String, List<String>> resp = new HashMap<>();
            String r = l.get("respostas");
            if (r != null && !r.isBlank())
                for (String par : r.split("\\|")) {
                    String[] kv = par.split("=");
                    if (kv.length == 2) resp.put(kv[0], List.of(kv[1].split("\\+")));
                }
            out.add(new Caso(l.get("id"), p, fat, l.get("regiao_marcada"), l.get("relato_usuario"), resp,
                Destino.valueOf(l.get("destino_esperado")), l.get("sinal_que_decide"), l.get("armadilha")));
        }
        return out;
    }

    static Set<String> fatores(String comorbidades, String remedios) {
        Set<String> f = new LinkedHashSet<>();
        for (String campo : new String[]{comorbidades, remedios}) {
            if (campo == null) continue;
            for (String x : campo.split("\\|")) {
                String n = x.trim().toLowerCase(Locale.ROOT);
                if (n.isEmpty()) continue;
                f.add(n);
                if (n.contains("anticoag")) f.add("anticoagulado");
                if (n.contains("quimio")) f.add("quimioterapia");
                if (n.contains("imunossupr") || n.contains("transplant")) f.add("imunossuprimido");
                if (n.contains("cardi") || n.contains("insuficiencia cardiaca")) f.add("cardiopata");
                if (n.contains("renal") || n.contains("dialise")) f.add("renal");
                if (n.contains("diabet") || n.contains("insulina")) f.add("diabetico");
            }
        }
        return f;
    }

    /** Como uma pessoa sincera responderia: marca o sinal que tem, responde o que está no caso e "não" no resto. */
    /** Sinais parecidos com os itens das perguntas LES_02 e LES_03: quem tem um deles marcaria o item também. */
    private static final Map<String, Set<String>> PARECIDOS = Map.ofEntries(
        Map.entry("estalo", Set.of("SA-C-012", "SA-C-016", "SA-C-017", "SA-C-107", "SA-C-112")),
        Map.entry("apoiar", Set.of("SA-A-015", "SA-C-003", "SA-C-019", "SA-C-114")),
        Map.entry("frio", Set.of("SA-C-020")),
        Map.entry("coluna", Set.of("SA-A-031")),
        Map.entry("cabeca", Set.of("SA-A-018", "SA-A-020")),
        Map.entry("cabeca_grave", Set.of("SA-A-016", "SA-A-019", "SA-A-024")),
        // sprint 6: itens das trilhas URI, PEL e BAR
        Map.entry("rim", Set.of("SA-C-045")),
        Map.entry("preso", Set.of("SA-G-009")),
        Map.entry("coagulo", Set.of("SA-D2-039", "SA-I-004")),
        Map.entry("copo", Set.of("SA-D1-089", "SA-E-037", "SA-D3-050")),
        Map.entry("incha", Set.of("SA-I-001", "SA-I-018", "SA-H-010")),
        Map.entry("espalha", Set.of("SA-D3-002", "SA-D3-005")),
        Map.entry("bolhas", Set.of("SA-F2-045")),
        Map.entry("sangue", Set.of("SA-D2-007", "SA-D2-008")),
        Map.entry("gases", Set.of("SA-D2-026")),
        Map.entry("direita", Set.of("SA-D2-030")));

    public static List<String> responder(Caso caso, No n) { return responder(caso, n, null); }

    /** @param motor para a pergunta da origem (ORI_01): quem tem um sinal de machucado responde "machuquei". */
    public static List<String> responder(Caso caso, No n, MotorTriagem motor) {
        Set<String> sinaisDoCaso = Set.of(caso.sinal().split("\\|"));
        if (n.id().equals(MotorTriagem.TELA_EMERGENCIA)) {   // os casos não respondem as perguntas de emergência: "nenhum", como antes
            List<String> r = new ArrayList<>();
            for (String fil : List.of("FIL_01", "FIL_02", "FIL_03", "FIL_04", "FIL_05")) {
                List<String> v = caso.respostas().get(fil);
                if (v != null && !v.isEmpty() && !v.get(0).equals("N")) r.add(switch (fil) { case "FIL_01" -> "ar"; case "FIL_02" -> "peito";
                    case "FIL_03" -> v.get(0).equals("passou") ? "apagou_passou" : "apagou_agora"; case "FIL_04" -> "avc"; default -> "sangue"; });
            }
            r.removeIf(c -> n.opcoes().stream().noneMatch(o -> o.codigo().equals(c)));
            return r.isEmpty() ? List.of("nenhum") : r;
        }
        if (n.id().equals(MotorTriagem.ORIGEM)) {
            boolean machucou = motor != null && sinaisDoCaso.stream().anyMatch(motor::sinalDeTrauma);
            return List.of(machucou ? "machucou" : "sozinho");
        }
        if (n.opcoes().stream().anyMatch(o -> o.efeitos().stream().anyMatch(e -> e.acao() == br.com.fiap.tami.motor.Efeito.Acao.SINAL))) {   // o item aponta para um sinal da base (efeito SINAL)
            List<String> r = new ArrayList<>();
            for (Opcao o : n.opcoes())
                for (var e : o.efeitos())
                    if (e.acao() == br.com.fiap.tami.motor.Efeito.Acao.SINAL
                            && (sinaisDoCaso.contains(e.valor()) || PARECIDOS.getOrDefault(o.codigo(), Set.of()).stream().anyMatch(sinaisDoCaso::contains)
                                || (o.codigo().equals("osso_dor") && motor != null && sinaisDoCaso.stream().anyMatch(motor::sinalDeFratura))))
                        r.add(o.codigo());
            return r.isEmpty() ? List.of("nenhum") : r;
        }
        if (n.id().equals("GEN_02")) {   // os casos foram escritos com as faixas antigas
            List<String> r = caso.respostas().get("GEN_02");
            String faixa = r == null || r.isEmpty() ? "0_3" : r.get(0);
            return List.of(switch (faixa) { case "4_6" -> "5"; case "7_10" -> "8"; default -> faixa.matches("\\d+") ? faixa : "2"; });
        }
        if (n.id().startsWith(MotorTriagem.CONF_SINAIS)) {
            List<String> r = n.opcoes().stream().map(Opcao::codigo).filter(sinaisDoCaso::contains).toList();
            return r.isEmpty() ? List.of("nenhum") : r;
        }
        List<String> r = caso.respostas().get(n.id());
        if (r != null && r.stream().allMatch(c -> n.opcao(c).isPresent())) return r;
        Optional<Opcao> nao = n.opcoes().stream().filter(o -> !o.positiva() && !o.codigo().equals(MotorTriagem.NAO_SEI)).findFirst();
        return List.of(nao.orElse(n.opcoes().get(0)).codigo());
    }

    public Saida rodar(Caso caso) {
        try {
            Sessao s = motor.iniciar(caso.perfil(), caso.texto(), null, TERCA_10H, caso.fatores(), caso.regiao());
            Optional<No> no;
            int voltas = 0;
            while ((no = motor.proxima(s)).isPresent()) {
                if (++voltas > 60) throw new IllegalStateException("conversa sem fim");
                motor.responder(s, no.get().id(), responder(caso, no.get(), motor));
            }
            Resultado r = motor.resultado(s);
            return new Saida(caso, r.destino(), voltas, null);
        } catch (RuntimeException e) {
            return new Saida(caso, null, 0, e.toString());
        }
    }

    /** Roda todos os casos e calcula as métricas (matriz de confusão, acurácia, sub e sobretriagem, sensibilidade, kappa). */
    public Map<String, Object> avaliar(List<Caso> casos) {
        long t0 = System.nanoTime();
        List<Saida> saidas = casos.stream().map(this::rodar).toList();
        double ms = (System.nanoTime() - t0) / 1e6;
        Destino[] ds = Destino.values();
        int k = ds.length;
        long[][] matriz = new long[k][k];   // [esperado][obtido]
        int erros = 0;
        for (Saida s : saidas) {
            if (s.obtido() == null) { erros++; continue; }
            matriz[s.caso().esperado().ordinal()][s.obtido().ordinal()]++;
        }
        long n = saidas.size() - erros;
        long exato = 0, sub = 0, sobre = 0, criticas = 0, esperadoUrgente = 0;
        for (int e = 0; e < k; e++)
            for (int o = 0; o < k; o++) {
                long v = matriz[e][o];
                if (e == o) exato += v; else if (o < e) sub += v; else sobre += v;
                if (o < e && e >= Destino.UPA_HOJE.ordinal()) criticas += v;
                if (e >= Destino.UPA_HOJE.ordinal()) esperadoUrgente += v;
            }
        // binário: "precisa de atendimento hoje" (UPA ou SAMU) contra "pode esperar" (UBS ou casa)
        long vp = 0, fn = 0, fp = 0, vn = 0;
        int corte = Destino.UPA_HOJE.ordinal();
        for (int e = 0; e < k; e++)
            for (int o = 0; o < k; o++) {
                boolean urgE = e >= corte, urgO = o >= corte;
                if (urgE && urgO) vp += matriz[e][o];
                else if (urgE) fn += matriz[e][o];
                else if (urgO) fp += matriz[e][o];
                else vn += matriz[e][o];
            }
        List<List<Long>> m = new ArrayList<>();
        for (long[] linha : matriz) { List<Long> l = new ArrayList<>(); for (long v : linha) l.add(v); m.add(l); }

        Map<String, Map<String, Object>> porArmadilha = new TreeMap<>();
        Map<String, long[]> arm = new HashMap<>();
        for (Saida s : saidas) {
            String a = s.caso().armadilha() == null || s.caso().armadilha().isBlank() ? "NENHUMA" : s.caso().armadilha();
            long[] c = arm.computeIfAbsent(a, x -> new long[3]);
            c[0]++;
            if (s.diferenca() == 0) c[1]++;
            if (s.obtido() != null && s.diferenca() < 0 && s.caso().esperado().ordinal() >= corte) c[2]++;
        }
        for (var e : arm.entrySet())
            porArmadilha.put(e.getKey(), Mapa.de("casos", e.getValue()[0], "acerto", Estatistica.proporcao(e.getValue()[1], e.getValue()[0]),
                "criticas", e.getValue()[2]));

        List<Double> perguntas = saidas.stream().filter(s -> s.obtido() != null).map(s -> (double) s.perguntas()).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("casos", saidas.size());
        out.put("errosExecucao", erros);
        out.put("milissegundos", Math.round(ms));
        out.put("msPorCaso", Math.round(ms / Math.max(1, saidas.size()) * 100) / 100.0);
        out.put("destinos", List.of(ds).stream().map(Enum::name).toList());
        out.put("matriz", m);
        out.put("acuraciaExata", Estatistica.proporcao(exato, n));
        out.put("subtriagem", Estatistica.proporcao(sub, n));
        out.put("subtriagemCritica", Estatistica.proporcao(criticas, esperadoUrgente));
        out.put("sobretriagem", Estatistica.proporcao(sobre, n));
        out.put("sensibilidadeUrgente", Estatistica.proporcao(vp, vp + fn));
        out.put("especificidadeUrgente", Estatistica.proporcao(vn, vn + fp));
        out.put("vppUrgente", Estatistica.proporcao(vp, vp + fp));
        out.put("vpnUrgente", Estatistica.proporcao(vn, vn + fn));
        out.put("kappa", Estatistica.kappa(matriz));
        out.put("kappaPonderado", Estatistica.kappaPonderado(matriz));
        out.put("perguntas", Estatistica.descrever(perguntas));
        out.put("porArmadilha", porArmadilha);
        out.put("aviso", "O destino esperado de cada caso foi escrito a partir de fontes públicas (NHS, MedlinePlus, Ministério da Saúde) "
            + "e ainda não foi revisado por médico. Os números medem se o motor segue as regras, não se a orientação é clinicamente correta.");
        return out;
    }
}
