package br.com.fiap.tami.motor;

import br.com.fiap.tami.motor.lexico.GrupoLexico;
import br.com.fiap.tami.motor.lexico.TermoLexico;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Carrega os roteiros a partir de três planilhas CSV (separador ";"), que o médico revisor
 * consegue abrir no Excel: nos.csv, opcoes.csv e lexico.csv. No app, as mesmas linhas
 * ficam nas tabelas no_roteiro, opcao_no e regra_lexico.
 */
public final class CatalogoRoteiros {

    private final Map<String, No> nos;
    private final List<TermoLexico> lexico;

    private CatalogoRoteiros(Map<String, No> nos, List<TermoLexico> lexico) {
        this.nos = Collections.unmodifiableMap(nos);
        this.lexico = List.copyOf(lexico);
    }

    public static CatalogoRoteiros carregarDoClasspath() {
        try (Reader n = abrir("/roteiros/nos.csv");
             Reader o = abrir("/roteiros/opcoes.csv");
             Reader l = lexicoComExtra()) {
            return carregar(n, o, l);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** lexico.csv + conhecimento/lexico_extra.csv (linguagem popular estudada, área J), se existir. */
    private static Reader lexicoComExtra() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(abrir("/roteiros/lexico.csv"))) {
            r.lines().forEach(x -> sb.append(x).append('\n'));
        }
        InputStream extra = CatalogoRoteiros.class.getResourceAsStream("/conhecimento/lexico_extra.csv");
        if (extra != null)
            try (BufferedReader r = new BufferedReader(new InputStreamReader(extra, StandardCharsets.UTF_8))) {
                r.lines().skip(1).filter(x -> !x.isBlank()).forEach(x -> sb.append(x).append('\n'));
            }
        return new java.io.StringReader(sb.toString());
    }

    private static Reader abrir(String caminho) {
        InputStream in = CatalogoRoteiros.class.getResourceAsStream(caminho);
        if (in == null) throw new IllegalStateException("arquivo não encontrado no classpath: " + caminho);
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    public static CatalogoRoteiros carregar(Reader nosCsv, Reader opcoesCsv, Reader lexicoCsv) throws IOException {
        Map<String, List<Opcao>> opcoesPorNo = new HashMap<>();
        for (String[] c : linhas(opcoesCsv, 5)) {
            List<Opcao> lista = opcoesPorNo.computeIfAbsent(c[0], k -> new ArrayList<>());
            if (lista.stream().anyMatch(o -> o.codigo().equals(c[1])))
                throw new IllegalArgumentException("opção repetida: " + c[0] + "/" + c[1]);
            lista.add(new Opcao(c[1], c[2], Integer.parseInt(c[3]), Efeito.lerLista(c[4])));
        }
        Map<String, No> nos = new LinkedHashMap<>();
        for (String[] c : linhas(nosCsv, 10)) {
            if (nos.containsKey(c[0])) throw new IllegalArgumentException("nó repetido: " + c[0]);
            List<Opcao> ops = List.copyOf(opcoesPorNo.getOrDefault(c[0], List.of()));
            nos.put(c[0], new No(c[0], c[1], Integer.parseInt(c[2]), No.Tipo.valueOf(c[3]),
                "S".equals(c[4]), "S".equals(c[5]), vazioViraNull(c[6]), vazioViraNull(c[7]),
                c[8], vazioViraNull(c[9]), ops));
        }
        for (String id : opcoesPorNo.keySet())
            if (!nos.containsKey(id)) throw new IllegalArgumentException("opção para nó que não existe: " + id);
        List<TermoLexico> lexico = new ArrayList<>();
        for (String[] c : linhas(lexicoCsv, 4))
            lexico.add(new TermoLexico(c[0], GrupoLexico.valueOf(c[1]), TermoLexico.Tipo.valueOf(c[2]), c[3]));
        return new CatalogoRoteiros(nos, lexico);
    }

    private static List<String[]> linhas(Reader r, int colunas) throws IOException {
        List<String[]> lista = new ArrayList<>();
        BufferedReader br = new BufferedReader(r);
        String linha = br.readLine();                       // cabeçalho
        int n = 1;
        while ((linha = br.readLine()) != null) {
            n++;
            if (linha.isBlank() || linha.startsWith("#")) continue;
            String[] c = linha.split(";", -1);
            if (c.length != colunas)
                throw new IllegalArgumentException("linha " + n + " tem " + c.length + " colunas, esperado " + colunas + ": " + linha);
            for (int i = 0; i < c.length; i++) c[i] = c[i].trim();
            lista.add(c);
        }
        return lista;
    }

    private static String vazioViraNull(String s) { return s == null || s.isBlank() ? null : s; }

    public No no(String id) {
        No no = nos.get(id);
        if (no == null) throw new IllegalArgumentException("nó desconhecido: " + id);
        return no;
    }

    public boolean existe(String id) { return nos.containsKey(id); }

    public Collection<No> todos() { return nos.values(); }

    public List<TermoLexico> lexico() { return lexico; }

    /** Todos os nós de um roteiro, na ordem. */
    public List<String> roteiro(String roteiro) {
        return nos.values().stream().filter(n -> n.roteiro().equals(roteiro))
            .sorted(Comparator.comparingInt(No::ordem)).map(No::id).toList();
    }

    /** O nó e os seguintes do mesmo roteiro: é o que entra na fila quando um sub-fluxo abre. */
    public List<String> aPartirDe(String id) {
        No inicio = no(id);
        return nos.values().stream()
            .filter(n -> n.roteiro().equals(inicio.roteiro()) && n.ordem() >= inicio.ordem())
            .sorted(Comparator.comparingInt(No::ordem)).map(No::id).toList();
    }

    /** Confere a planilha inteira. Erro aqui impede o app de subir com roteiro quebrado. */
    public List<String> validar(Set<String> decisoesConhecidas) {
        List<String> erros = new ArrayList<>();
        for (No no : nos.values()) {
            if (!Condicoes.valida(no.mostrarSe())) erros.add(no.id() + ": mostrar_se desconhecido " + no.mostrarSe());
            if (no.herdaDe() != null && !nos.containsKey(no.herdaDe())) erros.add(no.id() + ": herda_de inexistente");
            if (no.tipo() == No.Tipo.DECISAO) {
                if (!no.opcoes().isEmpty()) erros.add(no.id() + ": nó de decisão não tem botões");
                if (!decisoesConhecidas.contains(no.id())) erros.add(no.id() + ": decisão sem regra no código");
                continue;
            }
            if (no.pergunta().isBlank()) erros.add(no.id() + ": pergunta vazia");
            if (no.opcoes().size() < 2) erros.add(no.id() + ": precisa de pelo menos 2 botões");
            if (no.alarme() && no.opcoes().stream().noneMatch(Opcao::positiva))
                erros.add(no.id() + ": pergunta de alarme sem resposta positiva");
            for (Opcao o : no.opcoes())
                for (Efeito e : o.efeitos())
                    if (e.acao() == Efeito.Acao.IR && !nos.containsKey(e.valor()))
                        erros.add(no.id() + "/" + o.codigo() + ": IR para nó inexistente " + e.valor());
        }
        for (String d : decisoesConhecidas)
            if (!nos.containsKey(d)) erros.add(d + ": regra no código sem nó na planilha");
        return erros;
    }
}
