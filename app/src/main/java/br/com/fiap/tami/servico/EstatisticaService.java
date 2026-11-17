package br.com.fiap.tami.servico;

import br.com.fiap.tami.dao.EstatisticaDAO;
import br.com.fiap.tami.dao.SinalDAO;
import br.com.fiap.tami.motor.Destino;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.regiao.NoRegiao;
import br.com.fiap.tami.util.Estatistica;
import br.com.fiap.tami.util.Mapa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Junta as consultas do banco e os cálculos estatísticos para o painel e para a documentação. */
public final class EstatisticaService {

    private final EstatisticaDAO dao;
    private final SinalDAO sinalDAO;
    private final ArvoreRegioes arvore;
    private volatile Map<String, Object> validacao;

    public EstatisticaService(EstatisticaDAO dao, SinalDAO sinalDAO, ArvoreRegioes arvore) {
        this.dao = dao;
        this.sinalDAO = sinalDAO;
        this.arvore = arvore;
    }

    public void guardarValidacao(Map<String, Object> v) { this.validacao = v; }

    public Map<String, Object> validacao() { return validacao; }

    private volatile Map<String, Object> simulados;

    /** Resultado das conversas simuladas (ferramentas/simulados), para a aba do painel. */
    public void guardarSimulados(Map<String, Object> s) { this.simulados = s; }

    public Map<String, Object> simulados() { return simulados; }

    /** Estatística do uso, calculada sobre o que está no banco agora. */
    public Map<String, Object> uso() {
        List<EstatisticaDAO.LinhaConversa> cs = dao.conversas();
        int n = cs.size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("conversas", n);
        out.put("tabelas", Mapa.de(
            "T_TAMI_USUARIO", dao.contar("T_TAMI_USUARIO"), "T_TAMI_PESSOA", dao.contar("T_TAMI_PESSOA"),
            "T_TAMI_CONVERSA", dao.contar("T_TAMI_CONVERSA"), "T_TAMI_RESPOSTA", dao.contar("T_TAMI_RESPOSTA"),
            "T_TAMI_REGRA_ACIONADA", dao.contar("T_TAMI_REGRA_ACIONADA"), "T_TAMI_DOR", dao.contar("T_TAMI_DOR"),
            "T_TAMI_FEEDBACK", dao.contar("T_TAMI_FEEDBACK"), "T_TAMI_SINAL", dao.contar("T_TAMI_SINAL"),
            "T_TAMI_EXPRESSAO", dao.contar("T_TAMI_EXPRESSAO"), "T_TAMI_SINAL_REGIAO", dao.contar("T_TAMI_SINAL_REGIAO"),
            "T_TAMI_REGIAO", dao.contar("T_TAMI_REGIAO")));

        Map<String, Object> destinos = new LinkedHashMap<>();
        for (Destino d : Destino.values()) {
            long x = cs.stream().filter(c -> d.name().equals(c.destino())).count();
            destinos.put(d.name(), Estatistica.proporcao(x, n));
        }
        out.put("destinos", destinos);
        out.put("perguntas", Estatistica.descrever(cs.stream().map(c -> (double) c.perguntas()).toList()));
        out.put("segundos", Estatistica.descrever(cs.stream().filter(c -> c.segundos() != null).map(c -> (double) c.segundos()).toList()));

        // quanto a árvore do corpo enxugou a busca: 1 − (sinais da região / sinais totais)
        List<Double> reducao = cs.stream().filter(c -> c.sinaisRegiao() != null && c.sinaisTotal() != null && c.sinaisTotal() > 0)
            .map(c -> 100.0 * (1 - (double) c.sinaisRegiao() / c.sinaisTotal())).toList();
        out.put("reducaoFiltroPct", Estatistica.descrever(reducao));
        out.put("comRegiao", Estatistica.proporcao(cs.stream().filter(c -> c.regiao() != null).count(), n));

        Map<String, Integer> porRegiao = new TreeMap<>();
        for (var c : cs) {
            if (c.regiao() == null) continue;
            String g = arvore.buscar(c.regiao()).map(NoRegiao::grandeRegiao).map(NoRegiao::getNome).orElse(c.regiao());
            porRegiao.merge(g, 1, Integer::sum);
        }
        out.put("porGrandeRegiao", porRegiao);

        String[] faixas = {"Bebê (0 a 1 ano)", "Criança (2 a 12)", "Adolescente (13 a 17)", "Adulto (18 a 59)", "Idoso (60 ou mais)"};
        Map<String, Map<String, Integer>> faixaDestino = new LinkedHashMap<>();
        for (String f : faixas) faixaDestino.put(f, new LinkedHashMap<>());
        for (var c : cs) {
            int anos = c.idadeMeses() / 12;
            String f = anos < 2 ? faixas[0] : anos < 13 ? faixas[1] : anos < 18 ? faixas[2] : anos < 60 ? faixas[3] : faixas[4];
            faixaDestino.get(f).merge(c.destino(), 1, Integer::sum);
        }
        out.put("faixaEtariaPorDestino", faixaDestino);
        out.put("faixaEtariaXDestino", Estatistica.quiQuadrado(tabela(faixaDestino)));

        int[] horas = new int[24];
        for (var c : cs) if (c.hora() >= 0 && c.hora() < 24) horas[c.hora()]++;
        List<Integer> h = new ArrayList<>();
        for (int x : horas) h.add(x);
        out.put("porHora", h);
        out.put("crise", Estatistica.proporcao(cs.stream().filter(EstatisticaDAO.LinhaConversa::crise).count(), n));
        out.put("porNaoSei", Estatistica.proporcao(cs.stream().filter(EstatisticaDAO.LinhaConversa::porNaoSei).count(), n));
        out.put("idadeXPerguntas", Estatistica.pearson(cs.stream().map(c -> c.idadeMeses() / 12.0).toList(),
            cs.stream().map(c -> (double) c.perguntas()).toList()));

        Map<String, Map<String, Integer>> av = dao.avaliacaoPorDestino();
        out.put("avaliacaoPorDestino", av);
        out.put("avaliacaoXDestino", Estatistica.quiQuadrado(tabela(av)));
        long sim = av.values().stream().mapToLong(m -> m.getOrDefault("SIM", 0)).sum();
        long totalAv = av.values().stream().flatMap(m -> m.values().stream()).mapToLong(Integer::longValue).sum();
        out.put("ajudou", Estatistica.proporcao(sim, totalAv));

        List<Map<String, Object>> dor = new ArrayList<>();
        for (Object[] d : dao.dorPorRegiao())
            dor.add(Mapa.de("regiao", d[0], "n", d[1], "media", Estatistica.arred((Double) d[2], 2), "min", d[3], "max", d[4]));
        out.put("dorPorRegiao", dor);
        List<Map<String, Object>> regras = new ArrayList<>();
        for (Object[] r : dao.regrasMaisAcionadas(10)) regras.add(Mapa.de("regra", r[0], "destino", r[1], "n", r[2]));
        out.put("regrasMaisAcionadas", regras);
        out.put("sinaisPorRegiao", sinaisPorGrandeRegiao());
        return out;
    }

    private Map<String, Integer> sinaisPorGrandeRegiao() {
        Map<String, Integer> out = new LinkedHashMap<>();
        Map<String, Integer> porCod = sinalDAO.sinaisPorRegiao();
        for (NoRegiao g : arvore.raiz().getFilhos()) {
            int soma = 0;
            for (NoRegiao n : g.subArvore()) soma += porCod.getOrDefault(n.getCodigo(), 0);
            out.put(g.getNome(), soma);
        }
        return out;
    }

    /** Mapa linha ➜ (coluna ➜ contagem) para matriz, com as colunas na ordem dos destinos e das avaliações. */
    private static long[][] tabela(Map<String, Map<String, Integer>> m) {
        List<String> cols = new ArrayList<>();
        for (Map<String, Integer> l : m.values()) for (String c : l.keySet()) if (!cols.contains(c)) cols.add(c);
        long[][] t = new long[m.size()][cols.size()];
        int i = 0;
        for (Map<String, Integer> l : m.values()) {
            for (int j = 0; j < cols.size(); j++) t[i][j] = l.getOrDefault(cols.get(j), 0);
            i++;
        }
        return t;
    }
}
