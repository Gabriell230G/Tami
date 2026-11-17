package br.com.fiap.tami.motor.conhecimento;

import br.com.fiap.tami.motor.Destino;
import br.com.fiap.tami.motor.Perfil;
import br.com.fiap.tami.motor.lexico.BuscaExpressoes;
import java.util.Comparator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A base de conhecimento estudada (src/main/resources/conhecimento). Hoje: os sinais de alerta.
 * Cada sinal reconhecido no texto vira um disparo rastreável (regra "SA:" + id) no motor.
 */
public final class BaseConhecimento {

    /** Um sinal que apareceu no texto e vale para esta pessoa, já com o destino ajustado. */
    /** {@code negado}: só para sinais não negáveis (D-012); o motor manda para a pergunta em vez de disparar. */
    public record Achado(SinalAlerta sinal, Destino destino, boolean subiu, String trecho, boolean negado) {}

    private final Map<String, SinalAlerta> sinais;
    private final BuscaExpressoes busca;
    private final Map<String, Condicao> condicoes;
    private final BuscaExpressoes buscaCondicoes;
    private final BuscaExpressoes buscaAgravantes;   // termos livres de sobe_um_nivel_se (D-010)

    public BaseConhecimento(Collection<SinalAlerta> lista) { this(lista, List.of()); }

    public BaseConhecimento(Collection<SinalAlerta> lista, Collection<Condicao> conds) {
        Map<String, Condicao> mc = new LinkedHashMap<>();
        List<BuscaExpressoes.Expressao> ec = new ArrayList<>();
        for (Condicao c : conds) {
            if (mc.put(c.id(), c) != null) throw new IllegalArgumentException("condição repetida: " + c.id());
            for (String e : c.expressoes()) ec.addAll(BuscaExpressoes.Expressao.comFormasVerbais(c.id(), e));
        }
        this.condicoes = Collections.unmodifiableMap(mc);
        Set<String> vocab = new HashSet<>();   // D-021: palavras corretas que a base conhece
        for (SinalAlerta s : lista) {
            List<String> t = new ArrayList<>(s.expressoes()); t.add(s.descricao());
            for (String x : t) vocab.addAll(Arrays.asList(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(x).split(" ")));
        }
        for (Condicao c : conds) {
            List<String> t = new ArrayList<>(c.expressoes()); t.add(c.nomePopular()); t.add(c.sintomas());
            for (String x : t) vocab.addAll(Arrays.asList(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(x).split(" ")));
        }
        vocab.removeIf(w -> w.length() < 4);
        br.com.fiap.tami.motor.lexico.DetectorAlerta.registrarConhecidas(vocab);
        this.buscaCondicoes = new BuscaExpressoes(ec);
        Map<String, SinalAlerta> m = new LinkedHashMap<>();
        List<BuscaExpressoes.Expressao> ex = new ArrayList<>();
        for (SinalAlerta s : lista) {
            if (m.put(s.id(), s) != null) throw new IllegalArgumentException("sinal repetido: " + s.id());
            for (String e : s.expressoes())
                for (String v : semMarcaDeGrupo(e, s.valePara()))
                    ex.addAll(BuscaExpressoes.Expressao.comFormasVerbais(s.id(), v));
        }
        Set<String> livres = new LinkedHashSet<>();
        for (SinalAlerta s : lista) livres.addAll(s.sobeSe());
        List<BuscaExpressoes.Expressao> ag = new ArrayList<>(livres.stream().flatMap(t -> BuscaExpressoes.Expressao.comFormasVerbais(t, t).stream()).toList());
        Map<String, List<String>> sin = sinonimosDeAgravantes();   // D-025: o agravante dito com outras palavras
        for (String t : livres)
            for (String alt : sin.getOrDefault(t, List.of())) ag.addAll(BuscaExpressoes.Expressao.comFormasVerbais(t, alt));
        this.buscaAgravantes = new BuscaExpressoes(ag, true);
        this.sinais = Collections.unmodifiableMap(m);
        this.busca = new BuscaExpressoes(ex);
    }

    private static Map<String, List<String>> sinonimosAgravantes;

    /** agravantes_sinonimos.csv: agravantes (separados por |);expressoes_populares. Vazio se o arquivo não existe. */
    static synchronized Map<String, List<String>> sinonimosDeAgravantes() {
        if (sinonimosAgravantes != null) return sinonimosAgravantes;
        Map<String, List<String>> m = new HashMap<>();
        InputStream in = BaseConhecimento.class.getResourceAsStream("/conhecimento/agravantes_sinonimos.csv");
        if (in != null)
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                br.readLine();
                String linha;
                while ((linha = br.readLine()) != null) {
                    String[] c = linha.split(";", -1);
                    if (c.length < 2) continue;
                    for (String canon : c[0].split("\\|"))
                        for (String alt : c[1].split("\\|"))
                            if (!alt.isBlank()) m.computeIfAbsent(normal(canon).replace('_', ' '), k -> new ArrayList<>()).add(normal(alt));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        return sinonimosAgravantes = m;
    }

    public static BaseConhecimento carregarDoClasspath() {
        InputStream in = BaseConhecimento.class.getResourceAsStream("/conhecimento/sinais_alerta.csv");
        InputStream ic = BaseConhecimento.class.getResourceAsStream("/conhecimento/condicoes.csv");
        if (in == null) throw new IllegalStateException("conhecimento/sinais_alerta.csv não está no classpath");
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            BaseConhecimento so = carregar(r);
            if (ic == null) return so;
            try (Reader rc = new InputStreamReader(ic, StandardCharsets.UTF_8)) {
                return new BaseConhecimento(so.sinais.values(), carregarCondicoes(rc));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** condicoes.csv (D-007): id;nome_popular;...;sintomas_tipicos;palavras_lexico;...;destino_padrao;destino_minimo;grupos_risco;cuidado_casa_seguro;... */
    public static List<Condicao> carregarCondicoes(Reader csv) throws IOException {
        BufferedReader br = new BufferedReader(csv);
        String cab = br.readLine();
        if (cab == null) return List.of();
        List<String> col = Arrays.asList(cab.split(";", -1));
        int iId = col.indexOf("id"), iNome = col.indexOf("nome_popular"), iPal = col.indexOf("palavras_lexico"),
            iPad = col.indexOf("destino_padrao"), iMin = col.indexOf("destino_minimo"), iGr = col.indexOf("grupos_risco"),
            iCasa = col.indexOf("cuidado_casa_seguro"), iSint = col.indexOf("sintomas_tipicos");
        if (Math.min(Math.min(iId, iPal), Math.min(iPad, Math.min(iMin, iCasa))) < 0)
            throw new IllegalArgumentException("condicoes.csv com cabeçalho inesperado");
        List<Condicao> out = new ArrayList<>();
        String linha;
        while ((linha = br.readLine()) != null) {
            if (linha.isBlank()) continue;
            String[] c = linha.split(";", -1);
            if (c.length != col.size()) throw new IllegalArgumentException("condicoes.csv: linha com " + c.length + " colunas: " + linha);
            out.add(new Condicao(c[iId], c[iNome], Destino.valueOf(c[iPad]), Destino.valueOf(c[iMin]), conjunto(c[iGr]),
                "SIM".equals(c[iCasa]),
                Arrays.stream(c[iPal].split("\\|")).map(String::trim).filter(x -> !x.isEmpty()).toList(),
                iSint < 0 ? "" : c[iSint].replace("|", ", ")));
        }
        return out;
    }

    public Map<String, Condicao> condicoes() { return condicoes; }

    /** Condições citadas (não negadas) no texto, na ordem em que aparecem. */
    public List<Condicao> condicoesNoTexto(String texto) {
        List<Condicao> out = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (BuscaExpressoes.Achado a : buscaCondicoes.procurar(texto))
            if (!a.negado() && vistos.add(a.dono())) out.add(condicoes.get(a.dono()));
        return out;
    }

    public static BaseConhecimento carregar(Reader csv) throws IOException {
        BufferedReader br = new BufferedReader(csv);
        String cab = br.readLine();
        if (cab == null || !cab.startsWith("id;area;descricao_popular;"))
            throw new IllegalArgumentException("sinais_alerta.csv com cabeçalho inesperado");
        List<SinalAlerta> lista = new ArrayList<>();
        String linha;
        while ((linha = br.readLine()) != null) {
            if (linha.isBlank()) continue;
            String[] c = linha.split(";", -1);
            if (c.length != 14) throw new IllegalArgumentException("linha com " + c.length + " colunas: " + linha);
            lista.add(new SinalAlerta(c[0], c[1], c[2], c[3], Destino.valueOf(c[4]), conjunto(c[5]), conjunto(c[6]),
                Arrays.stream(c[7].split("\\|")).map(String::trim).filter(x -> !x.isEmpty()).toList(),
                c[8], "SIM".equals(c[13])));
        }
        return new BaseConhecimento(lista);
    }

    private static final Map<String, Set<String>> MARCAS_DE_GRUPO = Map.of(
        "gestante", Set.of("gravida", "gestante", "gravidez", "gestacao", "gravidas"),
        "idoso", Set.of("idoso", "idosa", "vovo", "vova", "avo", "velhinho", "velhinha", "idosos"),
        "imunossuprimido", Set.of("transplantado", "transplantada", "transplante", "imunossuprimido", "imunossuprimida"));
    private static final Set<String> LIGACOES = Set.of("na", "no", "de", "da", "do", "em", "e", "com");

    /**
     * A expressão e, se o sinal só vale para gestante (ou só para idoso), a mesma expressão sem a palavra do grupo:
     * o grupo já vem do perfil ("34 semanas" + "dor de cabeça forte que não passa"). Precisa sobrar 2+ palavras.
     */
    static List<String> semMarcaDeGrupo(String expressao, Set<String> valePara) {
        List<String> out = new ArrayList<>(List.of(expressao));
        for (var g : MARCAS_DE_GRUPO.entrySet()) {
            if (!valePara.equals(Set.of(g.getKey()))) continue;
            List<String> p = new ArrayList<>(Arrays.asList(normal(expressao).split(" ")));
            if (!p.removeIf(g.getValue()::contains)) continue;
            while (!p.isEmpty() && LIGACOES.contains(p.get(p.size() - 1))) p.remove(p.size() - 1);
            while (!p.isEmpty() && LIGACOES.contains(p.get(0))) p.remove(0);
            if (p.size() >= 2) out.add(String.join(" ", p));
        }
        return out;
    }

    private static Set<String> conjunto(String campo) {
        Set<String> s = new LinkedHashSet<>();
        for (String x : campo.split("\\|")) {
            if (x.isBlank()) continue;
            String n = normal(x).replace('_', ' ').trim();
            s.add(n);
            if (n.contains(" ou "))   // "rosto ou maos" ➜ "rosto", "maos", "mao"
                for (String alt : n.split(" ou ")) {
                    alt = alt.trim();
                    if (alt.isEmpty()) continue;
                    s.add(alt);
                    if (alt.endsWith("s") && alt.length() > 3) s.add(alt.substring(0, alt.length() - 1));
                }
        }
        return Collections.unmodifiableSet(s);
    }

    private static String normal(String x) {
        return java.text.Normalizer.normalize(x.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    }

    public Map<String, SinalAlerta> sinais() { return sinais; }

    /** Grupos de idade e condição da pessoa, nos mesmos nomes da coluna vale_para. */
    public static Set<String> grupos(Perfil p) {
        Set<String> g = new HashSet<>();
        int m = p.idadeMeses();
        g.add(m < 24 ? "bebe" : m < 144 ? "crianca" : m < 216 ? "adolescente" : m < 720 ? "adulto" : "idoso");
        if (p.gestante()) g.add("gestante");
        if (p.diabetes()) g.add("diabetico");
        return g;
    }

    /**
     * Sinais presentes (não negados) no texto que valem para a pessoa. {@code fatores}: o que ela contou
     * de si (anticoagulado, quimioterapia, imunossuprimido, cardiopata, renal...). Se um fator ou grupo
     * aparece em sobe_um_nivel_se, o destino sobe um nível (nunca acima do SAMU).
     */
    public List<Achado> procurar(String texto, Perfil perfil, Set<String> fatores) {
        Set<String> eu = eu(texto, perfil, fatores);   // D-010 e D-017
        List<Achado> out = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        List<BuscaExpressoes.Achado> lidos = busca.procurar(texto);
        Set<String> afirmados = new HashSet<>();   // revisão A2: o afirmado depois vence o negado antes
        for (BuscaExpressoes.Achado a : lidos) if (!a.negado()) afirmados.add(a.dono());
        for (BuscaExpressoes.Achado a : lidos) {
            if (a.negado() && afirmados.contains(a.dono())) continue;
            SinalAlerta s = sinais.get(a.dono());
            // D-012: risco de suicídio/autolesão citado com "não" não some: volta marcado como negado
            if ((a.negado() && !s.naoNegavel()) || !vistos.add(a.dono())) continue;
            if (!valeParaMim(s, eu)) continue;
            boolean sobe = !Collections.disjoint(s.sobeSe(), eu);
            Destino d = sobe ? subirSinal(s, texto) : s.destino();
            out.add(new Achado(s, d, sobe && d != s.destino(), a.trecho(), a.negado()));
        }
        return out;
    }


    /** Mesmo fato escrito de jeitos diferentes: se um aparece, valem todos (D-017). */
    private static final List<Set<String>> SINONIMOS = List.of(
        Set.of("fumante", "tabagismo", "fumo", "cigarro", "fuma", "fumar"),
        Set.of("cardiopata", "doenca do coracao", "problema no coracao", "insuficiencia cardiaca", "arritmia",
            "fibrilacao atrial", "doenca do coracao ou do pulmao", "cardiaco", "cardiaca"),
        Set.of("diabetes", "diabetico", "diabetica", "insulina"),
        Set.of("imunossuprimido", "imunidade baixa", "transplante", "transplantado", "hiv", "quimioterapia", "quimio", "imunossupressor"),
        Set.of("renal", "doenca renal", "dialise", "rim unico"),
        Set.of("anticoagulado", "anticoagulante", "remedio para afinar o sangue"));
    private static final java.util.regex.Pattern SEMANAS = java.util.regex.Pattern.compile("\\b(\\d{1,2}) semanas\\b");
    private static final java.util.regex.Pattern MESES_GRAVIDEZ = java.util.regex.Pattern.compile("\\b(\\d) mes(es)?\\b");

    /** Grupos, fatores, idade calculada, semanas de gestação e sinônimos: o "eu" usado na subida de nível. */
    public Set<String> eu(String texto, Perfil perfil, Set<String> fatores) {
        if (fatores == null) fatores = Set.of();
        Set<String> eu = new HashSet<>(grupos(perfil));
        for (String f : fatores) eu.add(normal(f));
        for (BuscaExpressoes.Achado a : buscaAgravantes.procurar(texto)) if (!a.negado()) eu.add(a.dono());
        int m = perfil.idadeMeses();
        if (m < 3) eu.addAll(Set.of("menor de 3 meses", "menos de 3 meses", "bebe menor de 3 meses"));
        if (m < 6) eu.addAll(Set.of("menor de 6 meses", "menos de 6 meses"));
        if (m < 12) eu.add("menor de 1 ano");
        if (m < 60) eu.add("crianca pequena");
        if (m >= 600) eu.add("maior de 50 anos");
        String n = br.com.fiap.tami.motor.lexico.Normalizador.normalizar(texto);
        if (perfil.gestante()) {
            var sem = SEMANAS.matcher(n);
            while (sem.find()) if (Integer.parseInt(sem.group(1)) < 37) eu.add("menos de 37 semanas");
            var mes = MESES_GRAVIDEZ.matcher(n);
            while (mes.find()) if (Integer.parseInt(mes.group(1)) <= 8) eu.add("menos de 37 semanas");
        }
        Set<String> palavras = new HashSet<>(Arrays.asList(n.split(" ")));
        // D-023: agravantes abstratos ditos com outras palavras no relato
        if (!Collections.disjoint(palavras, Set.of("bebi", "bebeu", "bebendo", "bebida", "cerveja", "cervejas", "cachaca",
                "pinga", "vinho", "whisky", "alcool", "droga", "drogas", "maconha", "cocaina", "crack", "bebado", "bebada")))
            eu.addAll(Set.of("usou alcool ou drogas", "usou alcool", "bebeu", "alcool", "drogas"));
        if (!Collections.disjoint(palavras, Set.of("sozinho", "sozinha")))
            eu.addAll(Set.of("esta sozinho", "sozinho", "mora sozinho"));
        if (n.matches(".*\\b(cachorro|cao|gato|cachorra) de rua\\b.*") || n.contains("cachorro desconhecido"))
            eu.addAll(Set.of("animal de rua", "animal desconhecido"));
        for (Set<String> g : SINONIMOS) {
            boolean tem = false;
            for (String x : g) tem |= eu.contains(x) || (!x.contains(" ") && palavras.contains(x)) || (x.contains(" ") && n.contains(x));
            for (String x : eu) for (String y : g) tem |= x.contains(y) && y.length() >= 5;
            if (tem) eu.addAll(g);
        }
        return eu;
    }

    /** Destino de um sinal para esta pessoa (com a subida de nível), ou null se o sinal não vale para ela. */
    /** O sinal pelo id (ou o item de condição), ou null. */
    public SinalAlerta sinal(String id) {
        indexar();
        SinalAlerta s = sinais.get(id);
        return s != null ? s : itensDeCondicao.get(id);
    }

    public Achado avaliar(String sinalId, String texto, Perfil perfil, Set<String> fatores) {
        indexar();
        if (itensDeCondicao.containsKey(sinalId)) {   // condição confirmada: vale o destino padrão (D-016)
            SinalAlerta c = itensDeCondicao.get(sinalId);
            Condicao cond = condicoes.get(sinalId);
            if (c.destino() == Destino.CUIDADO_EM_CASA   // condição leve: só libera casa se a pessoa não é do grupo de risco dela
                    && !Collections.disjoint(cond.gruposRisco(), eu(texto, perfil, fatores))) return null;
            return new Achado(c, c.destino(), false, "", false);
        }
        SinalAlerta s = sinais.get(sinalId);
        if (s == null) return null;
        Set<String> eu = eu(texto, perfil, fatores);
        if (!valeParaMim(s, eu)) return null;
        boolean sobe = !Collections.disjoint(s.sobeSe(), eu);
        Destino d = sobe ? subirSinal(s, texto) : s.destino();
        return new Achado(s, d, sobe && d != s.destino(), "", false);
    }

    // ---------- lista de confirmação: sinais parecidos com o relato (busca aproximada por palavras) ----------
    private Map<String, Set<String>> palavrasDoSinal;
    private Map<String, Double> peso;
    // D-0xx (ranking da lista): BM25 com o sinal como documento, e as expressões do sinal separadas
    private Map<String, Map<String, Integer>> frequenciaNoSinal;
    private Map<String, Integer> tamanhoDoSinal;
    private Map<String, List<Set<String>>> expressoesDoSinal;
    private Map<String, Double> idfBm25;
    private double tamanhoMedio;

    /** Condições com destino padrão UPA ou SAMU, vistas como itens da lista (rótulo: sintomas típicos). D-020. */
    private Map<String, SinalAlerta> itensDeCondicao;

    private synchronized void indexar() {   // revisão A4: seguro entre threads
        if (palavrasDoSinal != null) return;
        itensDeCondicao = new LinkedHashMap<>();
        for (Condicao c : condicoes.values())
            if ((c.destinoPadrao().compareTo(Destino.UPA_HOJE) >= 0 || c.casaSegura()) && !c.sintomas().isBlank()) {
                List<String> ex = new ArrayList<>(c.expressoes());
                ex.add(c.sintomas());
                itensDeCondicao.put(c.id(), new SinalAlerta(c.id(), "CO", c.sintomas(), c.nomePopular(), c.destinoPadrao(),
                    Set.of("todos"), Set.of(), ex, "", true));
            }
        Set<String> marcas = new HashSet<>();
        MARCAS_DE_GRUPO.values().forEach(marcas::addAll);
        Map<String, Set<String>> palavras = new HashMap<>();
        frequenciaNoSinal = new HashMap<>();
        tamanhoDoSinal = new HashMap<>();
        expressoesDoSinal = new HashMap<>();
        Map<String, Integer> df = new HashMap<>();
        long somaTamanhos = 0;
        List<SinalAlerta> todos = new ArrayList<>(sinais.values());
        todos.addAll(itensDeCondicao.values());
        for (SinalAlerta s : todos) {
            Set<String> w = new HashSet<>();
            Map<String, Integer> tf = new HashMap<>();
            List<Set<String>> exprs = new ArrayList<>();
            int tamanho = 0;
            List<String> textos = new ArrayList<>(s.expressoes());
            textos.add(s.descricao());
            for (String t : textos) {
                Set<String> e = new LinkedHashSet<>();
                for (String p : br.com.fiap.tami.motor.lexico.Normalizador.normalizar(t).split(" "))
                    if (p.length() >= 3 && !BuscaExpressoes.VAZIAS.contains(p) && !marcas.contains(p)
                            && !p.equals("nao") && !p.equals("sem")) {
                        w.add(p);
                        e.add(p);
                        tf.merge(p, 1, Integer::sum);
                        tamanho++;
                    }
                if (!e.isEmpty()) exprs.add(e);
            }
            palavras.put(s.id(), w);
            frequenciaNoSinal.put(s.id(), tf);
            tamanhoDoSinal.put(s.id(), tamanho);
            expressoesDoSinal.put(s.id(), exprs);
            somaTamanhos += tamanho;
            for (String p : w) df.merge(p, 1, Integer::sum);
        }
        peso = new HashMap<>();
        idfBm25 = new HashMap<>();
        int n = todos.size();
        tamanhoMedio = somaTamanhos / (double) n;
        df.forEach((p, k) -> {
            peso.put(p, Math.log(1.0 + n / (double) k));
            idfBm25.put(p, Math.log(1.0 + (n - k + 0.5) / (k + 0.5)));
        });
        palavrasDoSinal = palavras;   // por último: é a marca de "já indexado"
    }


    /**
     * Até {@code k} sinais (que valem para a pessoa e ainda não dispararam) mais parecidos com o relato, do mais
     * parecido ao menos; no empate, o mais grave primeiro. A pessoa confirma quais tem (pergunta CONF_SINAIS).
     */
    public List<SinalAlerta> candidatos(String texto, Perfil perfil, Set<String> fatores, Set<String> jaDisparados, int k) {
        return candidatos(texto, perfil, fatores, jaDisparados, k, null);
    }

    /** Palavras de cada grande região do corpo 3D (D-018). */
    private static final Map<String, List<String>> PALAVRAS_DA_REGIAO = Map.of(
        "cabeca", List.of("cabeca", "rosto", "olho", "olhos", "boca", "dente", "ouvido", "nariz", "garganta", "pescoco",
            "nuca", "fala", "visao", "vista", "lingua", "labio", "testa", "mandibula", "queixo", "gengiva", "engolir", "amigdala"),
        "peito", List.of("peito", "costela", "coracao", "respirar", "respiracao", "tosse", "catarro", "pulmao", "mama", "seio", "folego"),
        "barriga", List.of("barriga", "estomago", "umbigo", "vomito", "vomitando", "diarreia", "fezes", "coco", "xixi", "urina",
            "virilha", "intestino", "gases", "vagina", "calcinha", "bolsa", "contracao", "evacuar", "abdome"),
        "costas", List.of("costas", "lombar", "coluna", "bumbum", "quadril", "nadega", "coccix", "rim"),
        "bracos", List.of("braco", "ombro", "cotovelo", "antebraco", "mao", "punho", "dedo", "clavicula"),
        "pernas", List.of("perna", "coxa", "joelho", "canela", "panturrilha", "batata", "tornozelo", "pe", "calcanhar", "dedo"));

    /** Grande região a partir do nome da região tocada (qualquer um dos 3 nomes do corpo 3D), ou null. */
    public static String grandeRegiao(String regiao) {
        if (regiao == null) return null;
        String r = normal(regiao);
        if (r.contains("cabeca") || r.contains("rosto") || r.contains("ouvido") || r.contains("garganta")
            || r.contains("nuca") || r.contains("pescoco")) return "cabeca";
        if (r.contains("peito") || r.contains("costela")) return r.contains("embaixo das costelas") ? "barriga" : "peito";
        if (r.contains("barriga") || r.contains("estomago") || r.contains("umbigo") || r.contains("virilha")) return "barriga";
        if (r.contains("costas") || r.contains("lombar") || r.contains("bumbum") || r.contains("quadril")) return "costas";
        if (r.contains("ombro") || r.contains("braco") || r.contains("cotovelo") || r.contains("mao")) return "bracos";
        if (r.contains("coxa") || r.contains("joelho") || r.contains("canela") || r.contains("perna")
            || r.contains("tornozelo") || r.equals("pe") || r.startsWith("pe ") || r.contains("pe direito") || r.contains("pe esquerdo")) return "pernas";
        return null;
    }

    /** O sinal fala de alguma parte da grande região tocada? (true se não há região) */
    public boolean daRegiaoTocada(String sinalId, String regiao) {
        indexar();
        String g = grandeRegiao(regiao);
        if (g == null || !palavrasDoSinal.containsKey(sinalId) || daRegiao(sinalId, g)) return true;
        for (String outra : PALAVRAS_DA_REGIAO.keySet()) if (daRegiao(sinalId, outra)) return false;
        return true;   // o sinal não fala de nenhuma parte do corpo: vale para qualquer região
    }

    private boolean daRegiao(String sinalId, String grande) {
        for (String w : palavrasDoSinal.get(sinalId))
            for (String k : PALAVRAS_DA_REGIAO.get(grande))
                if (w.equals(k) || (k.length() >= 5 && BuscaExpressoes.mesmaRaiz(w, k))) return true;
        return false;
    }

    /** Igual ao outro, com bônus para os sinais da grande região que a pessoa tocou no corpo 3D (D-018). */
    /** Igual ao outro, com bônus para os sinais da grande região que a pessoa tocou no corpo 3D (D-018). */
    public List<SinalAlerta> candidatos(String texto, Perfil perfil, Set<String> fatores, Set<String> jaDisparados, int k, String regiao) {
        return candidatosComNota(texto, perfil, fatores, jaDisparados, k, regiao).stream().map(Candidato::sinal).toList();
    }

    /**
     * Item da lista de confirmação com a força do achado. {@code nota}: a nota do ranking. {@code cobertura}: a maior parte
     * (pesada pelo idf) de UMA expressão do sinal que aparece num MESMO trecho do relato, sem negação (0 a 1).
     */
    public record Candidato(SinalAlerta sinal, double nota, double cobertura, String expressao, double pesoDaExpressao) {}

    public List<Candidato> candidatosComNota(String texto, Perfil perfil, Set<String> fatores, Set<String> jaDisparados, int k, String regiao) {
        indexar();
        String grande = grandeRegiao(regiao);
        Set<String> eu = eu(texto, perfil, fatores);
        List<Set<String>> porPalavra = new ArrayList<>();   // vocabulário que casa com cada palavra do relato, na ordem do relato
        for (String p : NEGADO_NA_LISTA.matcher(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(texto)).replaceAll(" ").split(" "))
            if (p.length() >= 3 && !BuscaExpressoes.VAZIAS.contains(p)) porPalavra.add(vocabularioQueCasa(p));
        if (porPalavra.isEmpty()) return List.of();
        Set<String> casadas = new HashSet<>();   // palavras do vocabulário dos sinais que aparecem no relato
        porPalavra.forEach(casadas::addAll);
        List<Candidato> notas = new ArrayList<>();
        List<SinalAlerta> todos = new ArrayList<>(sinais.values());
        todos.addAll(itensDeCondicao.values());
        for (SinalAlerta s : todos) {
            if (jaDisparados.contains(s.id())) continue;
            if (!valeParaMim(s, eu)) continue;
            double v = notaDoRelato(s.id(), casadas, porPalavra);
            if (v > 0 && grande != null && daRegiao(s.id(), grande)) v *= 1.6;
            if (v > 0 && !s.valePara().contains("todos")) v *= 1.5;   // D-018: sinal específico do grupo da pessoa
            if (v > 0 && s.destino() == Destino.CUIDADO_EM_CASA && s.area().equals("CO")) v *= 0.5;   // D-020: leve nunca na frente
            // D-027: na disputa por lugar na lista, o mais urgente pesa mais (a pessoa ainda precisa confirmar)
            if (v > 0) v *= switch (s.destino()) { case SAMU_192 -> 1.3; case UPA_HOJE -> 1.15; case UBS_EM_BREVE -> 1.0; case CUIDADO_EM_CASA -> 0.85; };
            if (v > 0) notas.add(new Candidato(s, v, 0, "", 0));   // o BM25 já normaliza pelo tamanho do sinal
        }
        notas.sort(Comparator.comparingDouble((Candidato x) -> -x.nota()).thenComparing(x -> -x.sinal().destino().ordinal()));
        List<Candidato> out = new ArrayList<>();
        List<List<Palavra>> porTrecho = null;
        for (Candidato c : notas) {
            if (out.size() >= k) break;
            if (c.sinal().destino().compareTo(Destino.UPA_HOJE) < 0) { out.add(c); continue; }   // cobertura só importa para o grave
            if (porTrecho == null) porTrecho = palavrasPorTrecho(texto);
            String[] melhor = {""};
            double[] peso = {0};
            double cob = coberturaNoTrecho(c.sinal().id(), porTrecho, melhor, peso);
            out.add(new Candidato(c.sinal(), c.nota(), cob, melhor[0], peso[0]));
        }
        return out;
    }

    /** Palavra do relato com o vocabulário que casa com ela e se ela está negada ("não", "sem", "nem" logo antes). */
    private record Palavra(Set<String> casa, boolean negada) {}

    private static final Set<String> NEGACOES = Set.of("nao", "n", "sem", "nem", "nenhum", "nenhuma", "nunca", "jamais");
    private static final Set<String> PARA_NEGACAO = Set.of("mas", "porem", "so", "agora", "hoje", "entao");

    /** Por trecho do relato (a pontuação separa): cada palavra de conteúdo, o vocabulário que casa e a negação. */
    private List<List<Palavra>> palavrasPorTrecho(String texto) {
        List<List<Palavra>> out = new ArrayList<>();
        for (String[] t : br.com.fiap.tami.motor.lexico.Normalizador.trechos(texto)) {
            List<Palavra> l = new ArrayList<>();
            int ultimaNegacao = -99;
            int i = 0;
            for (String p : t) {
                if (NEGACOES.contains(p)) { ultimaNegacao = i; i++; continue; }
                if (PARA_NEGACAO.contains(p)) ultimaNegacao = -99;
                if (p.length() >= 3 && !BuscaExpressoes.VAZIAS.contains(p))
                    l.add(new Palavra(vocabularioQueCasa(p), i - ultimaNegacao <= 3));
                i++;
            }
            if (!l.isEmpty()) out.add(l);
        }
        return out;
    }

    /**
     * Sprint 3 (item 7): a maior parte de UMA expressão do sinal (2+ palavras, pesada pelo idf) que aparece dentro de um
     * só trecho, num espaço de até 12 palavras. A palavra do relato que vem negada não conta; na expressão positiva com
     * "não" ("bombinha não resolve"), a palavra depois do "não" só conta se vier negada no relato também.
     */
    private double coberturaNoTrecho(String id, List<List<Palavra>> porTrecho, String[] qual, double[] pesoDaMelhor) {
        SinalAlerta s = sinais.containsKey(id) ? sinais.get(id) : itensDeCondicao.get(id);
        if (s == null) return 0;
        Set<String> marcas = new HashSet<>();
        MARCAS_DE_GRUPO.values().forEach(marcas::addAll);
        double melhor = 0;
        for (String ex : s.expressoes()) {
            if (ex.matches(".*\\d.*")) continue;   // "39 graus", "3 a 6 meses": o número é o que importa, e ele não entra aqui
            List<String> pal = new ArrayList<>();
            List<Boolean> neg = new ArrayList<>();
            boolean proximaNegada = false;
            for (String p : br.com.fiap.tami.motor.lexico.Normalizador.normalizar(ex).split(" ")) {
                if (NEGACOES.contains(p)) { proximaNegada = true; continue; }
                if (p.length() < 3 || BuscaExpressoes.VAZIAS.contains(p) || marcas.contains(p) || !peso.containsKey(p)) continue;
                pal.add(p);
                neg.add(proximaNegada);
                proximaNegada = false;
            }
            if (pal.size() < 2) continue;
            double total = 0;
            for (String w : pal) total += peso.get(w);
            for (List<Palavra> trecho : porTrecho) {
                double m = 0;
                int primeiro = Integer.MAX_VALUE, ultimo = -1;
                boolean[] usada = new boolean[trecho.size()];   // uma palavra do relato vale para uma palavra da expressão só
                for (int k = 0; k < pal.size(); k++) {
                    for (int i = 0; i < trecho.size(); i++) {
                        Palavra x = trecho.get(i);
                        if (!usada[i] && x.negada() == neg.get(k) && x.casa().contains(pal.get(k))) {
                            usada[i] = true;
                            m += peso.get(pal.get(k));
                            primeiro = Math.min(primeiro, i);
                            ultimo = Math.max(ultimo, i);
                            break;
                        }
                    }
                }
                if (ultimo - primeiro > 12) continue;
                double c = m / total;
                if (c > melhor || (c == melhor && c > 0 && total > pesoDaMelhor[0])) { melhor = c; qual[0] = String.join(" ", pal); pesoDaMelhor[0] = total; }
            }
        }
        return melhor;
    }


    /**
     * D-0xx: quanto o relato parece com o sinal. BM25 (k1 = 1,2; b = 0,75; o sinal é o documento) vezes
     * (1 + cobertura da expressão do sinal mais coberta pelo relato), mais meio ponto de idf por par de palavras
     * vizinhas do relato que aparecem juntas numa mesma expressão do sinal ("dor no peito" vale mais que "dor" + "peito" soltos).
     */
    private double notaDoRelato(String id, Set<String> casadas, List<Set<String>> porPalavra) {
        Map<String, Integer> tf = frequenciaNoSinal.get(id);
        double norma = 1.2 * (1 - 0.75 + 0.75 * tamanhoDoSinal.get(id) / tamanhoMedio);
        double bm25 = 0;
        for (String w : casadas) {
            Integer f = tf.get(w);
            if (f != null) bm25 += idfBm25.get(w) * f * (1.2 + 1) / (f + norma);
        }
        if (bm25 == 0) return 0;
        List<Set<String>> exprs = expressoesDoSinal.get(id);
        double cobertura = 0;
        for (Set<String> e : exprs) {
            double m = 0, t = 0;
            for (String w : e) {
                t += peso.get(w);
                if (casadas.contains(w)) m += peso.get(w);
            }
            cobertura = Math.max(cobertura, m / Math.max(1.0, t));
        }
        double pares = 0;
        for (int i = 0; i + 1 < porPalavra.size(); i++) {
            Set<String> a = porPalavra.get(i), b = porPalavra.get(i + 1);
            if (a.isEmpty() || b.isEmpty()) continue;
            double melhor = 0;
            for (Set<String> e : exprs)
                for (String x : a)
                    if (e.contains(x))
                        for (String y : b)
                            if (!x.equals(y) && e.contains(y)) melhor = Math.max(melhor, peso.get(x) + peso.get(y));
            pares += melhor;
        }
        return bm25 * (1 + cobertura) + 0.5 * pares;
    }


    private final Map<String, Set<String>> memoCasa = new java.util.concurrent.ConcurrentHashMap<>();

    /** Palavras do vocabulário que casam com o token (igual, mesmo radical ou erro de digitação), com memória. */
    private Set<String> vocabularioQueCasa(String t) {
        return memoCasa.computeIfAbsent(t, x -> {
            Set<String> r = new HashSet<>();
            for (String w : peso.keySet()) if (BuscaExpressoes.mesmaRaiz(x, w)) r.add(w);
            return r;
        });
    }

    private double total(String id) {
        double t = 0;
        for (String w : palavrasDoSinal.get(id)) t += peso.get(w);
        return Math.max(1.0, t);
    }

    /**
     * D-029: sinal de casa com um sinal de alarme dito no relato (sonolento, desmaio, sangue...) vai para a UPA, não só
     * para a UBS. Subida só por grupo ou fator (idoso, anticoagulado) continua subindo um nível.
     */
    Destino subirSinal(SinalAlerta s, String texto) {
        if (s.destino() == Destino.CUIDADO_EM_CASA) {
            Set<String> doTexto = new HashSet<>();
            for (BuscaExpressoes.Achado a : buscaAgravantes.procurar(texto)) if (!a.negado()) doTexto.add(a.dono());
            for (String ag : s.sobeSe())
                if (doTexto.contains(ag) && !AGRAVANTE_DE_TEMPO.matcher(ag).matches()) return Destino.UPA_HOJE;
        }
        return subir(s.destino());
    }

    /** Sprint 2: idoso também vale como adulto quando o sinal é de UPA ou SAMU (o grave do adulto não some aos 60+). */
    static boolean valeParaMim(SinalAlerta s, Set<String> eu) {
        if (s.valePara().contains("todos") || !Collections.disjoint(s.valePara(), eu)) return true;
        return eu.contains("idoso") && s.valePara().contains("adulto") && s.destino().compareTo(Destino.UPA_HOJE) >= 0;
    }

    /** Sprint 2: sintoma negado ("não vomitei", "sem tosse") não pontua a lista de confirmação. */
    private static final java.util.regex.Pattern NEGADO_NA_LISTA = java.util.regex.Pattern.compile(
        "\\b(nao|sem|nem|nunca)\\s+(tenho |teve |tive |tem |sinto |senti |estou com |to com |ta com |deu |teve |houve )?(febre|tosse|vomito|vomitei|vomitou|vomitando|vomitos|diarreia|coriza|catarro|nausea|enjoo|dor de cabeca|manchas|coceira|sangue|sangramento|tontura|falta de ar|dor)\\b");

    /** D-029 refinada: estes agravantes falam de tempo, não de alarme; sobem só um nível. */
    private static final java.util.regex.Pattern AGRAVANTE_DE_TEMPO = java.util.regex.Pattern.compile(
        ".*\\b(semana|semanas|dias|mes|meses|nao melhora|piorou depois|nao solta gases|mais de|volta sempre|repete)\\b.*");

    static Destino subir(Destino d) {
        return switch (d) {
            case CUIDADO_EM_CASA -> Destino.UBS_EM_BREVE;
            case UBS_EM_BREVE -> Destino.UPA_HOJE;
            case UPA_HOJE, SAMU_192 -> Destino.SAMU_192;
        };
    }
}
