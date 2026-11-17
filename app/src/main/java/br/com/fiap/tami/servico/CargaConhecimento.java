package br.com.fiap.tami.servico;

import br.com.fiap.tami.dao.SinalDAO;
import br.com.fiap.tami.modelo.SinalCatalogo;
import br.com.fiap.tami.regiao.ArvoreRegioes;
import br.com.fiap.tami.regiao.NoRegiao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carga (ETL) da base de conhecimento: lê sinais_alerta.csv e condicoes.csv, descobre em que regiões do corpo
 * cada sinal faz sentido (pela árvore) e grava tudo no banco em lotes.
 */
public final class CargaConhecimento {

    public record Relatorio(int sinais, int condicoes, int expressoes, int vinculos, int semRegiao, long milissegundos) {}

    private final SinalDAO sinalDAO;
    private final ArvoreRegioes arvore;

    public CargaConhecimento(SinalDAO sinalDAO, ArvoreRegioes arvore) {
        this.sinalDAO = sinalDAO;
        this.arvore = arvore;
    }

    /** Carrega só se o banco ainda estiver vazio. */
    public Relatorio carregarSeVazio() {
        return sinalDAO.contar() > 0 ? null : carregar();
    }

    /** Carrega se o banco estiver vazio ou com outra quantidade de sinais (a base ganhou sinais novos). */
    public Relatorio carregarSeMudou() {
        int esperado = ler("/conhecimento/sinais_alerta.csv").size() + ler("/conhecimento/condicoes.csv").size();
        int noBanco = sinalDAO.contar();
        if (noBanco == esperado) return null;
        if (noBanco > 0) sinalDAO.apagarTudo();
        return carregar();
    }

    public Relatorio carregar() {
        long t0 = System.currentTimeMillis();
        List<SinalCatalogo> lista = new ArrayList<>();
        Map<String, Set<String>> regioes = new LinkedHashMap<>();
        int nSinais = 0, nCond = 0;

        // sinais_alerta.csv: id;area;descricao_popular;termo_medico;destino;vale_para;sobe_um_nivel_se;palavras_lexico;fonte_nome;fonte_url;...;revisar_medico
        for (Map<String, String> l : ler("/conhecimento/sinais_alerta.csv")) {
            List<String> expr = lista(l.get("palavras_lexico"));
            SinalCatalogo s = new SinalCatalogo(l.get("id"), "SINAL", l.get("descricao_popular"), l.get("termo_medico"), l.get("destino"),
                l.get("vale_para"), l.get("fonte_nome"), l.get("fonte_url"), "SIM".equals(l.get("revisar_medico")), expr);
            lista.add(s);
            regioes.put(s.codigo(), regioesDe(s.popular() + " . " + String.join(" . ", expr)));
            nSinais++;
        }
        // condicoes.csv: id;nome_popular;termo_medico;...;regioes_corpo;sintomas_tipicos;palavras_lexico;...;destino_padrao;...
        for (Map<String, String> l : ler("/conhecimento/condicoes.csv")) {
            List<String> expr = lista(l.get("palavras_lexico"));
            SinalCatalogo s = new SinalCatalogo(l.get("id"), "CONDICAO", l.get("nome_popular"), l.get("termo_medico"), l.get("destino_padrao"),
                l.get("grupos_risco"), l.get("fonte_nome"), l.get("fonte_url"), "SIM".equals(l.get("revisar_medico")), expr);
            lista.add(s);
            String texto = l.get("regioes_corpo").replace("|", " . ") + " . " + l.get("sintomas_tipicos").replace("|", " . ")
                + " . " + String.join(" . ", expr);
            regioes.put(s.codigo(), regioesDe(texto));
            nCond++;
        }
        sinalDAO.inserirTudo(lista, regioes);
        int expressoes = lista.stream().mapToInt(s -> s.expressoes().size()).sum();
        int vinculos = regioes.values().stream().mapToInt(Set::size).sum();
        int semRegiao = (int) regioes.values().stream().filter(Set::isEmpty).count();
        return new Relatorio(nSinais, nCond, expressoes, vinculos, semRegiao, System.currentTimeMillis() - t0);
    }

    /** Regiões citadas no texto do sinal. "Corpo todo" não entra: vale para tudo e não filtra nada. */
    private Set<String> regioesDe(String texto) {
        Set<String> out = new LinkedHashSet<>();
        for (ArvoreRegioes.Achado a : arvore.procurar(texto)) {
            NoRegiao n = a.regiao();
            if (n.getNivel() == 0) continue;
            out.add(n.getCodigo());
        }
        return out;
    }

    private static List<String> lista(String campo) {
        if (campo == null || campo.isBlank()) return List.of();
        return Arrays.stream(campo.split("\\|")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
    }

    /** Lê um CSV separado por ";" como uma lista de mapas coluna ➜ valor. */
    static List<Map<String, String>> ler(String caminho) {
        try (InputStream in = CargaConhecimento.class.getResourceAsStream(caminho)) {
            if (in == null) throw new IllegalStateException("Arquivo não encontrado: " + caminho);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String[] cab = br.readLine().split(";", -1);
            List<Map<String, String>> out = new ArrayList<>();
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] c = linha.split(";", -1);
                Map<String, String> m = new LinkedHashMap<>();
                for (int i = 0; i < cab.length; i++) m.put(cab[i], i < c.length ? c[i] : "");
                out.add(m);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
