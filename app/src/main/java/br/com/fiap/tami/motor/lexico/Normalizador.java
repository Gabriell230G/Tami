package br.com.fiap.tami.motor.lexico;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Limpa o texto livre antes da busca: minúsculas, sem acento, letras repetidas
 * juntadas ("peitoooo" vira "peito") e abreviações trocadas ("vc" vira "voce").
 *
 * <p>Sprint 3 (Frente 3, texto e voz):
 * <ul>
 *   <li>tira as muletas da fala ("né", "ééé", "hum", "tipo assim", "então") e a palavra repetida ("meu meu");</li>
 *   <li>"de jeito nenhum" não é negação do que vem depois ("não para de jeito nenhum, já encharquei...");</li>
 *   <li>grafia pelo som vira a forma certa: tabela própria (estrupo, vista dobrada, sói cídio...) e uma regra geral
 *       pelo som ("isquerdo", "formigano", "ospital", "com vulsão", "e corpião", "in toxicação");</li>
 *   <li>"num"/"nu" falado no lugar de "não", só antes de um verbo que a base usa depois de "não" ("num consigo respirar").</li>
 * </ul>
 * A regra geral pelo som só troca uma palavra que a base NÃO conhece por uma palavra que a base conhece, e só quando
 * o som leva a uma única palavra. Palavra que existe e é outra coisa ("saiu", "ouço", "cinto") não é trocada: essa
 * dúvida é da tela de conferência da voz, não do motor.
 */
public final class Normalizador {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern NAO_ALFANUM = Pattern.compile("[^a-z0-9 ]+");
    private static final Pattern REPETIDAS = Pattern.compile("([a-z])\\1{2,}");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    /** Fim de trecho: a negação nunca atravessa pontuação nem vírgula. */
    private static final Pattern FIM_DE_TRECHO = Pattern.compile("(?<!\\d)[.,](?!\\d)|[.,](?=\\s)|[;:!?\\n]+");   // "37,8" e "1.5" não quebram o trecho
    /** " / " separa mensagens: vira a palavra-marca "/" dentro do trecho. A negação para nela; a busca por palavras-chave não. */
    private static final Pattern BARRA = Pattern.compile("(?<!\\d)/|/(?!\\d)");
    public static final String MARCA_BARRA = "/";

    /**
     * Muletas da fala, com ou sem acento. "é" sozinho fica (pode ser o verbo: "não é xixi"); só sai junto de outra
     * muleta ("é né", "éé"). "hã" sai, mas "ha" sem acento fica (pode ser "há 3 dias"). "ó" sai ("olha"); "o" fica.
     */
    private static final Pattern MULETA = Pattern.compile(
        "\\s*(?<![\\p{L}\\p{N}])(?:né|ne|hã|ã|ó|é(?=\\s+(?:né|ne|éé+|hã|hum)(?![\\p{L}\\p{N}]))|é{2,}|e{2,}|h+u+m+|h+m+|a+h+n+|u+h+m+|então|entao|tipo assim)(?![\\p{L}\\p{N}])");

    /** "de jeito nenhum" reforça o que vem antes; não nega o que vem depois. */
    private static final Pattern JEITO_NENHUM = Pattern.compile("\\b(de|por|em) (jeito|forma|maneira|modo) (nenhum|nenhuma|algum|alguma)\\b");

    private static final Map<String, String> ABREVIACOES = Map.ofEntries(
        Map.entry("n", "nao"), Map.entry("nn", "nao"), Map.entry("naum", "nao"),
        Map.entry("vc", "voce"), Map.entry("q", "que"), Map.entry("tb", "tambem"),
        Map.entry("tbm", "tambem"), Map.entry("mt", "muito"), Map.entry("mto", "muito"),
        Map.entry("d", "de"), Map.entry("dps", "depois"), Map.entry("pq", "porque"),
        Map.entry("p", "para"), Map.entry("pra", "pra"), Map.entry("hj", "hoje"),
        Map.entry("vo", "vou"), Map.entry("c", "com"), Map.entry("cm", "com"), Map.entry("qnd", "quando"), Map.entry("qdo", "quando"), Map.entry("msm", "mesmo"),
        Map.entry("cmg", "comigo"), Map.entry("td", "tudo"), Map.entry("tds", "todos"), Map.entry("agr", "agora"),
        Map.entry("dnv", "de novo"), Map.entry("bb", "bebe"),
        // diminutivo que minimiza não esconde o sintoma (Etapa 5)
        Map.entry("dorzinha", "dor"), Map.entry("dorzinhas", "dor"), Map.entry("barriguinha", "barriga"), Map.entry("bucho", "barriga"),
        Map.entry("estorou", "estourou"), Map.entry("qro", "quero"), Map.entry("kero", "quero"), Map.entry("estoro", "estourou"), Map.entry("vcs", "voces"), Map.entry("fikei", "fiquei"));

    /**
     * Tabela própria de grafias pelo som (sprint 3). Vale sempre, antes da regra geral. Chave e valor já sem acento.
     * Várias palavras: troca o trecho inteiro.
     */
    private static final Map<String, String> PELO_SOM = Map.ofEntries(
        // violência sexual (troca de letras)
        Map.entry("estrupo", "estupro"), Map.entry("estrupada", "estuprada"), Map.entry("estrupado", "estuprado"),
        Map.entry("estrupou", "estuprou"), Map.entry("estrupador", "estuprador"), Map.entry("estrupar", "estuprar"),
        Map.entry("estruparam", "estupraram"), Map.entry("estrupro", "estupro"), Map.entry("estrupa", "estupra"),
        // suicídio dito ou ouvido separado
        Map.entry("sui cidio", "suicidio"), Map.entry("soi cidio", "suicidio"), Map.entry("soicidio", "suicidio"),
        Map.entry("suecidio", "suicidio"), Map.entry("suicidiu", "suicidio"), Map.entry("sucidio", "suicidio"),
        // convulsão com "ç" ou partida
        Map.entry("convulcao", "convulsao"), Map.entry("convursao", "convulsao"), Map.entry("combulsao", "convulsao"),
        Map.entry("com vulcao", "convulsao"), Map.entry("convulcoes", "convulsoes"), Map.entry("convulssao", "convulsao"),
        // visão dupla dita do jeito popular
        Map.entry("vista dobrada", "visao dupla"), Map.entry("vista dobrando", "visao dupla"), Map.entry("visao dobrada", "visao dupla"),
        Map.entry("vista duplicada", "visao dupla"), Map.entry("vista duplicando", "visao dupla"),
        // febre: troca de letra e corte da voz
        Map.entry("frebe", "febre"), Map.entry("frebre", "febre"), Map.entry("febrr", "febre"), Map.entry("feb", "febre"),
        Map.entry("fevre", "febre"),
        // escorpião e intoxicação faladas
        Map.entry("iscorpiao", "escorpiao"), Map.entry("escorpiam", "escorpiao"), Map.entry("in toxicacao", "intoxicacao"),
        Map.entry("in toxicado", "intoxicado"), Map.entry("in toxicada", "intoxicada"), Map.entry("intossicacao", "intoxicacao"),
        Map.entry("entoxicacao", "intoxicacao"), Map.entry("intoxicassao", "intoxicacao"),
        // AVC falado (a tabela vale mesmo se a base tiver a grafia pelo som: a expressão da base também passa por aqui)
        Map.entry("isquerdo", "esquerdo"), Map.entry("isquerda", "esquerda"), Map.entry("esquerdu", "esquerdo"),
        Map.entry("formigano", "formigando"), Map.entry("formigamentu", "formigamento"), Map.entry("dormenti", "dormente"),
        Map.entry("imagem dobrada", "visao dupla"), Map.entry("vendo dobrada", "vendo dobrado"), Map.entry("derrami", "derrame"));

    private static final Set<String> ANTES_DE_AVO = Set.of("meu", "minha", "seu", "sua", "teu", "tua", "nosso", "nossa", "o", "a",
        "do", "da", "no", "na", "ao", "pro", "pra", "para", "pelo", "pela", "com", "de", "dele", "dela");

    /**
     * Tabela da Frente 1 (pedido P-F1-5): as mesmas formas que o `DetectorRisco` junta só para ele, agora no texto do
     * motor todo (a base também vê). Aplicada no texto já sem acento.
     */
    private static final Pattern[] SOM_RE = {
        Pattern.compile("\\bs[uo]i\\s?(?:c|s|ss|sc)id(?:i?o|iu)\\b"), Pattern.compile("\\bsu\\s?(?:c|s|ss)id(?:io|ar|a)\\b"),
        Pattern.compile("\\b(?:auto|alto) (lesa\\w*|lesion\\w*|mutil\\w*|exterm\\w*)"),
        Pattern.compile("\\b[ei]st?r?upr?(o|os|a|ada|adas|ado|ados|aram|ou|ar|ador|ando)\\b"),
        Pattern.compile("\\b(?:com|con|cum) ?v[uo]l[sc]a(?:o|oes)\\b|\\bconvul(?:c|ss)a(?:o|oes)\\b|\\bcombul[sc]a(?:o|oes)\\b|\\bconvur[sc]a(?:o|oes)\\b"),
        Pattern.compile("\\b(?:in|en) ?to(?:x|ss|cs|ks)ic"),
        Pattern.compile("\\b(?:[ei] ?s?|s)?c[oa]rpi(ao|oes)\\b|\\bescopi(ao|oes)\\b"),
        Pattern.compile("\\bagua ?sa\\w*ari[ao]\\b"),
        Pattern.compile("\\b(me|se) mantar\\b")};
    private static final String[] SOM_POR = {"suicidio", "suicidio", "auto$1", "estupr$1", "convulsao", "intoxic",
        "escorpi$1", "agua sanitaria", "$1 matar"};

    private static String somDaFrente1(String s) {
        for (int i = 0; i < SOM_RE.length; i++) {
            Matcher m = SOM_RE[i].matcher(s);
            String por = SOM_POR[i];
            if (m.find()) s = m.replaceAll(r -> Matcher.quoteReplacement(
                por.contains("$1") ? por.replace("$1", r.group(1) == null ? "ao" : r.group(1)) : por));
        }
        return s;
    }

    /** Maior número de palavras numa chave da tabela. */
    private static final int MAIOR_CHAVE = 2;

    /** Números por extenso nunca viram outra palavra pelo som ("cento" não é "sento"). */
    static final Set<String> NUMEROS = Set.of("um", "uma", "dois", "duas", "tres", "quatro", "cinco", "seis", "sete", "oito",
        "nove", "dez", "onze", "doze", "treze", "catorze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
        "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa", "cem", "cento", "duzentos",
        "duzentas", "trezentos", "trezentas", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos",
        "novecentos", "mil", "meio", "meia", "zero");

    /**
     * Palavras de verdade que soam parecido com uma palavra da base, mas são outra coisa. Nunca trocadas pelo som
     * (a tela de conferência pergunta). A regra geral já não junta a maioria; esta lista é a trava extra.
     */
    private static final Set<String> PROTEGIDAS = Set.of("saiu", "sair", "ouco", "ouca", "baco", "briga", "brigas", "obra",
        "ficou", "beber", "bebeu", "cinto", "cintos", "sela", "cela", "censo", "senso", "conserto", "concerto",
        "auto", "alto", "mau", "mal", "caro", "cara", "coral", "aranha", "arranha", "arranhou", "carinho", "carrinho",
        "morre", "morri", "morro", "mata", "matar", "matou", "tremi", "corri", "senti", "sinto", "nada", "nado",
        "acento", "assento", "cassar", "cacar", "sessao", "secao", "cessao", "tacha", "taxa", "cheque", "xeque");

    private Normalizador() {}

    /** Tira as muletas da fala do texto em minúsculas (com ou sem acento). Mantém a pontuação. */
    public static String semMuletas(String minusculo) {
        if (minusculo == null || minusculo.isEmpty()) return minusculo;
        Matcher m = MULETA.matcher(minusculo);
        if (!m.find()) return minusculo;
        return m.replaceAll("").replaceFirst("^\\s+", "");
    }

    public static String normalizar(String entrada) {
        if (entrada == null) return "";
        String s = semMuletas(entrada.toLowerCase(Locale.ROOT));
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = MARCAS.matcher(s).replaceAll("");
        s = NAO_ALFANUM.matcher(s).replaceAll(" ");
        s = semMuletas(s);   // de novo, já sem acento ("ne", "eee", "hum")
        s = REPETIDAS.matcher(s).replaceAll("$1");
        s = ESPACOS.matcher(s).replaceAll(" ").trim();
        if (s.isEmpty()) return s;
        List<String> p = new ArrayList<>();
        String[] ws = s.split(" ");
        for (int i = 0; i < ws.length; i++) {
            String w = ws[i];
            // P-F1-4: "vo" depois de "minha", "meu", "da", "do"... é a avó ou o avô ("da minha vó"), não "vou"
            if (w.equals("vo") && i > 0 && ANTES_DE_AVO.contains(ws[i - 1])) { p.add(w); continue; }
            String a = ABREVIACOES.getOrDefault(w, w);
            for (String x : a.split(" ")) p.add(x);
        }
        p = semRepeticao(p);
        String junto = String.join(" ", p);
        if (JEITO_NENHUM.matcher(junto).find()) junto = JEITO_NENHUM.matcher(junto).replaceAll("$1 $2");
        junto = somDaFrente1(junto);
        p = new ArrayList<>(List.of(junto.split(" ")));
        p = pelaTabela(p);
        p = peloSom(p);
        p = numViraNao(p);
        p = cemViraSem(p);
        p = verboPartido(p);
        p = semRepeticao(p);
        return String.join(" ", p);
    }

    /** Divide o texto em trechos (por pontuação) e cada trecho em palavras normalizadas. */
    public static List<String[]> trechos(String texto) {
        List<String[]> lista = new ArrayList<>();
        if (texto == null) return lista;
        for (String parte : FIM_DE_TRECHO.split(texto)) {
            List<String> p = new ArrayList<>();
            for (String msg : BARRA.split(parte)) {
                String n = normalizar(msg);
                if (n.isEmpty()) continue;
                if (!p.isEmpty()) p.add(MARCA_BARRA);
                for (String w : n.split(" ")) p.add(w);
            }
            if (!p.isEmpty()) lista.add(p.toArray(new String[0]));
        }
        return lista;
    }

    // ------------------------------------------------------------------------------------------------------------
    // repetição e tabela

    /** "meu meu filho" ➜ "meu filho". Número e negação repetidos ficam ("não, não tenho" é cuidado, não erro). */
    private static List<String> semRepeticao(List<String> p) {
        List<String> out = new ArrayList<>(p.size());
        for (String w : p) {
            if (!out.isEmpty() && w.equals(out.get(out.size() - 1)) && !w.chars().allMatch(Character::isDigit)
                    && !NEGACOES.contains(w) && !RISO.contains(w)) continue;
            out.add(w);
        }
        return out;
    }

    private static final Set<String> RISO = Set.of("k", "kk", "ks", "rs", "ha", "he", "hi", "hua", "kkk");
    private static final Set<String> NEGACOES = Set.of("nao", "nem", "sem", "nunca", "nenhum", "nenhuma", "jamais", "nada");

    private static List<String> pelaTabela(List<String> p) {
        List<String> out = new ArrayList<>(p.size());
        int i = 0;
        while (i < p.size()) {
            String troca = null;
            int usado = 0;
            for (int n = Math.min(MAIOR_CHAVE, p.size() - i); n >= 1 && troca == null; n--) {
                String k = String.join(" ", p.subList(i, i + n));
                if (PELO_SOM.containsKey(k)) { troca = PELO_SOM.get(k); usado = n; }
            }
            if (troca == null) { out.add(p.get(i)); i++; continue; }
            for (String x : troca.split(" ")) out.add(x);
            i += usado;
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------------------
    // regra geral pelo som

    /**
     * Chave de som de uma palavra (português do Brasil, sem acento). Duas palavras com a mesma chave soam igual ou quase:
     * h mudo no começo; ch/x; lh soa "i" (vermeio/vermelho); qu e c duro = k; g/j antes de e/i; c antes de e/i, ss e ç = s;
     * s entre vogais = z; rr e r no começo = R (caro ≠ carro); começo e/i + consoante (isquerdo/esquerdo,
     * iscorpião/escorpião, inchaço/enchaço); m/n antes de consoante; gerúndio sem d (formigano); e/o finais átonos.
     * O c no começo antes de e/i fica separado do s (cinto ≠ sinto).
     */
    private static final Map<String, String> CHAVES = new ConcurrentHashMap<>();

    static String chave(String w) {
        if (CHAVES.size() > 50_000) CHAVES.clear();   // texto muito estranho não enche a memória
        return CHAVES.computeIfAbsent(w, Normalizador::calcularChave);
    }

    private static String calcularChave(String w) {
        String k = w;
        if (k.startsWith("h")) k = k.substring(1);
        k = k.replace("ph", "f").replace("ch", "X").replace("sh", "X").replace("lh", "i").replace("nh", "N");
        k = k.replaceAll("ndo(s?)$", "no$1");
        k = k.replaceAll("^[ei][sx]?(?=[^aeiou])", "I");
        k = k.replaceAll("qu(?=[ei])", "k").replace("qu", "kw");
        k = k.replaceAll("gu(?=[ei])", "G").replaceAll("g(?=[ei])", "J").replace("j", "J").replace("g", "G");
        k = k.replaceAll("^c(?=[ei])", "C");
        k = k.replaceAll("[sx]c(?=[ei])", "S").replaceAll("c(?=[ei])", "S").replace("c", "k");
        k = k.replace("ss", "S").replaceAll("(?<=[aeiou])s(?=[aeiou])", "Z").replaceAll("z$", "S").replace("z", "Z").replace("s", "S");
        k = k.replace("rr", "R").replaceAll("^r", "R");
        k = k.replace("w", "v").replace("y", "i");
        k = k.replaceAll("[mn](?=[^aeiouIN]|$)", "N");
        k = k.replaceAll("([a-zIXNSZRJGC])\\1+", "$1");
        k = k.replaceAll("o(S?)$", "u$1").replaceAll("e(S?)$", "i$1");
        return k;
    }

    /** Palavras que a base conhece, o índice de som e os verbos que a base usa depois de "não". Carrega uma vez. */
    private static final class Vocabulario {
        static final Set<String> PALAVRAS = new HashSet<>();
        static final Map<String, String> INDICE = new HashMap<>();   // chave de som ➜ palavra ("" se mais de uma)
        static final Set<String> DEPOIS_DO_NAO = new HashSet<>(Set.of("consigo", "consegue", "conseguia", "consegui",
            "conseguiu", "para", "parou", "aguento", "aguenta", "acorda", "acordou", "responde", "respondeu", "respira",
            "respirou", "sabe", "sei", "mexe", "mexo", "fala", "falou", "enxergo", "enxerga", "sente", "sinto", "senti",
            "reconhece", "tem", "tenho", "tive", "teve", "to", "ta", "esta", "estou", "segura", "engole", "engulo", "come",
            "dorme", "levanta", "anda", "urina", "mama", "chora", "melhora", "passa", "sai", "desce", "volta", "quero", "quer",
            "vejo", "devia", "deveria"));
        static final Set<String> PROTEGIDAS_RISCO = new HashSet<>();

        static {
            String[][] fontes = {
                {"/conhecimento/sinais_alerta.csv", "2,6,7"}, {"/conhecimento/condicoes.csv", "1,6,7"},
                {"/conhecimento/lexico_extra.csv", "3"}, {"/roteiros/lexico.csv", "3"},
                {"/conhecimento/agravantes_sinonimos.csv", "0,1"}};
            for (String[] f : fontes) {
                try (InputStream in = Normalizador.class.getResourceAsStream(f[0])) {
                    if (in == null) continue;
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String linha;
                    boolean cab = true;
                    while ((linha = br.readLine()) != null) {
                        if (cab) { cab = false; continue; }
                        String[] c = linha.split(";", -1);
                        for (String col : f[1].split(",")) {
                            int i = Integer.parseInt(col);
                            if (i < c.length) for (String txt : c[i].split("\\|")) aprender(txt);
                        }
                    }
                } catch (Exception e) { /* sem a base, sem regra geral pelo som */ }
            }
            for (String w : List.of("suicidio", "suicida", "estupro", "estuprada", "estuprador", "intoxicacao", "intoxicado",
                    "intoxicada", "envenenamento", "envenenado", "convulsao", "convulsoes", "convulsionando", "escorpiao",
                    "escorpioes", "esquerdo", "esquerda", "direito", "direita", "formigando", "formigamento", "dormente",
                    "hospital", "visao", "dupla"))
                PALAVRAS.add(w);
            // palavras dos padrões do detector de risco: nunca trocadas pelo som (lidas dos padrões, sem mudar o detector)
            try {
                for (java.lang.reflect.Field fd : DetectorRisco.class.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(fd.getModifiers())) continue;
                    fd.setAccessible(true);
                    Object v = fd.get(null);
                    String txt = v instanceof Pattern pt ? pt.pattern() : v instanceof String st ? st : null;
                    if (txt == null) continue;
                    Matcher m = Pattern.compile("[a-z]{4,}").matcher(txt);
                    while (m.find()) PROTEGIDAS_RISCO.add(m.group());
                }
            } catch (Throwable e) { /* sem a trava extra */ }
            for (String w : PALAVRAS)
                if (w.length() >= 5 && !NUMEROS.contains(w)) INDICE.merge(chave(w), w, (a, b) -> a.equals(b) ? a : "");
        }

        private static void aprender(String txt) {
            String[] p = basico(txt).split(" ");
            for (int i = 0; i < p.length; i++) {
                if (p[i].isEmpty()) continue;
                PALAVRAS.add(p[i]);
                if (i > 0 && p[i - 1].equals("nao") && p[i].length() >= 4) DEPOIS_DO_NAO.add(p[i]);
            }
        }
    }

    /** Minúsculas, sem acento, só letras e números, letras repetidas juntadas, abreviações (sem a parte pelo som). */
    static String basico(String txt) {
        String s = Normalizer.normalize(txt.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        s = MARCAS.matcher(s).replaceAll("");
        s = NAO_ALFANUM.matcher(s).replaceAll(" ");
        s = REPETIDAS.matcher(s).replaceAll("$1");
        s = ESPACOS.matcher(s).replaceAll(" ").trim();
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(ABREVIACOES.getOrDefault(w, w));
        }
        return sb.toString();
    }

    /** true: a palavra é conhecida (base, números, travas) e nunca é trocada pelo som. */
    static boolean conhecida(String w) {
        return Vocabulario.PALAVRAS.contains(w) || NUMEROS.contains(w) || PROTEGIDAS.contains(w)
            || Vocabulario.PROTEGIDAS_RISCO.contains(w) || w.chars().anyMatch(Character::isDigit);
    }

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /** A palavra da base com o mesmo som, ou a própria palavra. */
    static String corrigirPeloSom(String w) {
        if (w.length() < 5 || conhecida(w)) return w;
        if (CACHE.size() > 50_000) CACHE.clear();
        return CACHE.computeIfAbsent(w, Normalizador::calcularPeloSom);
    }

    private static String calcularPeloSom(String w) {
        String t = Vocabulario.INDICE.get(chave(w));
        if (t != null && !t.isEmpty()) return aceita(w, t) ? t : w;
        // "convulcao" (o ç virou c depois de consoante) ➜ "convulsao"
        if (w.matches(".*[lnr]coe?s?$|.*[lnr]cao$")) {
            String v = w.replaceAll("([lnr])c(oe?s?|ao)$", "$1s$2");
            if (Vocabulario.PALAVRAS.contains(v)) return v;
        }
        return w;
    }

    /** Travas da troca pelo som: não inventa vogal final átona ("morre" ➜ "morri") nem troca verbo no passado ("tremi" ➜ "treme"). */
    private static boolean aceita(String w, String t) {
        char a = w.charAt(w.length() - 1), b = t.charAt(t.length() - 1);
        if ((a == 'e' || a == 'o') && (b == 'i' || b == 'u')) return false;
        if (w.endsWith("es") && t.endsWith("is") || w.endsWith("os") && t.endsWith("us")) return false;
        if (a == 'i' && b == 'e') {   // "febri" ➜ "febre" sim; "corri" ➜ "corre" não (é o verbo no passado)
            String r = w.substring(0, w.length() - 1);
            for (String fim : List.of("eu", "iu", "er", "ir", "endo", "indo", "ia", "ido", "ida"))
                if (Vocabulario.PALAVRAS.contains(r + fim)) return false;
            if (w.endsWith("ei")) return false;
        }
        return true;
    }

    /**
     * Junta pedaço de palavra que a voz separou ("com vulsao", "sui sidio", "in toxicacao", "e corpiao", "cor acao").
     * Só junta quando o 2º pedaço não é palavra conhecida e a junção é uma palavra da base (pelo som).
     */
    private static String juntar(String a, String b) {
        if (a.length() > 4 || b.length() < 3 || conhecida(b) || a.chars().anyMatch(Character::isDigit)) return null;
        if (a.equals("a") || a.equals("o")) return null;
        if (a.equals("e")) {   // "e corpiao" ➜ "escorpiao": só o "es"/"ex" do começo que a voz perdeu
            String t = Vocabulario.INDICE.get(chave("es" + b));
            return t != null && !t.isEmpty() && (t.startsWith("es") || t.startsWith("ex")) ? t : null;
        }
        String junto = a + (b.startsWith("s") && !b.startsWith("ss") ? "s" : "") + b;   // "sui"+"sidio": o s do começo soa s
        if (Vocabulario.PALAVRAS.contains(a + b) && (a + b).length() >= 6) return a + b;
        if (junto.length() < 6) return null;
        String t = Vocabulario.INDICE.get(chave(junto));
        return t != null && !t.isEmpty() && aceita(junto, t) ? t : null;
    }

    private static List<String> peloSom(List<String> p) {
        List<String> out = new ArrayList<>(p.size());
        int i = 0;
        while (i < p.size()) {
            String w = p.get(i);
            if (i + 1 < p.size() && corrigirPeloSom(p.get(i + 1)).equals(p.get(i + 1))) {
                String j = juntar(w, p.get(i + 1));
                if (j != null) { out.add(j); i += 2; continue; }
            }
            out.add(corrigirPeloSom(w));
            i++;
        }
        return out;
    }

    private static final Set<String> ARTIGO_DEPOIS = Set.of("a", "o", "as", "os", "na", "no", "nas", "nos", "um", "uma",
        "com", "em", "de", "do", "da", "pra", "para", "pelo", "pela", "minha", "meu", "sua", "seu");

    /**
     * "bate o a testa" ➜ "bateu a testa": a voz partiu o "eu"/"iu" do fim do verbo. Só quando o verbo com "u" é da base
     * e o "o" vem colado em outro artigo ou preposição (frase que não existe sem a troca).
     */
    private static List<String> verboPartido(List<String> p) {
        List<String> out = new ArrayList<>(p.size());
        for (int i = 0; i < p.size(); i++) {
            String w = p.get(i);
            if (i + 2 < p.size() && (w.endsWith("e") || w.endsWith("i")) && w.length() >= 3 && p.get(i + 1).equals("o")
                    && ARTIGO_DEPOIS.contains(p.get(i + 2)) && Vocabulario.PALAVRAS.contains(w + "u")) {
                out.add(w + "u");
                i++;
                continue;
            }
            out.add(w);
        }
        return out;
    }

    /** Depois de "cem" de verdade vem número, unidade ou coisa contada no plural ("cem reais", "cem mil", "cem por cento"). */
    private static final Pattern DEPOIS_DE_CEM = Pattern.compile("\\d+|.*s|mil|por|e|gramas?|metros?|ml|mg|kg|km|real|reais|grau|graus|vezes|anos?");

    /** "cem febre" ➜ "sem febre": a voz troca "sem" por "cem" (som igual). Só antes de palavra da base que não é número nem plural. */
    private static List<String> cemViraSem(List<String> p) {
        List<String> out = new ArrayList<>(p);
        for (int i = 0; i + 1 < out.size(); i++)
            if (out.get(i).equals("cem") && Vocabulario.PALAVRAS.contains(out.get(i + 1)) && !DEPOIS_DE_CEM.matcher(out.get(i + 1)).matches())
                out.set(i, "sem");
        return out;
    }

    /** "num consigo respirar" ➜ "nao consigo respirar". Só antes de verbo que a base usa depois de "não". */
    private static List<String> numViraNao(List<String> p) {
        List<String> out = new ArrayList<>(p);
        for (int i = 0; i + 1 < out.size(); i++)
            if ((out.get(i).equals("num") || out.get(i).equals("nu")) && Vocabulario.DEPOIS_DO_NAO.contains(out.get(i + 1)))
                out.set(i, "nao");
        return out;
    }
}
