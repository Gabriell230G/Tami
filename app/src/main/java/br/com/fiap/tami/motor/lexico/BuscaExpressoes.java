package br.com.fiap.tami.motor.lexico;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Procura expressões livres (as palavras_lexico da base de conhecimento) no texto da pessoa,
 * com as MESMAS regras do detector de emergência: erro de digitação, palavra de recheio,
 * janela de negação (N2 a N4, N8, com o alcance curto do sprint 3) e trechos separados por pontuação.
 */
public final class BuscaExpressoes {

    /** Uma expressão a procurar. {@code dono}: id de quem a expressão representa (ex.: SA-A-001). */
    /** {@code exata}: forma verbal gerada; só vale escrita igual (sem tolerância a erro de digitação). */
    public record Expressao(String dono, String texto, String[] palavras, boolean exata) {
        public Expressao(String dono, String texto, String[] palavras) { this(dono, texto, palavras, false); }

        public static Expressao de(String dono, String texto) {
            String n = Normalizador.normalizar(texto);
            return new Expressao(dono, texto, n.isEmpty() ? new String[0] : n.split(" "));
        }

        /**
         * A expressão e as mesmas palavras com o verbo em outra pessoa/tempo comum no relato:
         * "bateu a cabeca" ➜ "bati a cabeca", "bate a cabeca", "batendo a cabeca". Uma palavra trocada por vez.
         */
        public static List<Expressao> comFormasVerbais(String dono, String texto) {
            Expressao base = de(dono, texto);
            List<Expressao> out = new ArrayList<>(List.of(base));
            java.util.Set<String> vistas = new java.util.HashSet<>();
            vistas.add(String.join(" ", base.palavras()));
            for (int i = 0; i < base.palavras().length; i++)
                for (String f : formas(base.palavras()[i])) {
                    String[] p = base.palavras().clone();
                    p[i] = f;
                    if (vistas.add(String.join(" ", p))) out.add(new Expressao(dono, texto, p, true));
                }
            return out;
        }

        static List<String> formas(String w) {
            if (w.length() < 4) return List.of();
            String r;
            if (w.endsWith("eu")) { r = w.substring(0, w.length() - 2); return List.of(r + "i", r + "e", r + "endo"); }
            if (w.endsWith("ou")) { r = w.substring(0, w.length() - 2); return List.of(r + "ei", r + "a", r + "ando"); }
            if (w.endsWith("iu")) { r = w.substring(0, w.length() - 2); return List.of(r + "i", r + "e", r + "indo"); }
            if (w.endsWith("ando")) { r = w.substring(0, w.length() - 4); return List.of(r + "ou", r + "ei", r + "a"); }
            if (w.endsWith("endo")) { r = w.substring(0, w.length() - 4); return List.of(r + "eu", r + "i", r + "e"); }
            return List.of();
        }
    }

    /** Achado no texto: {@code negado} quando a pessoa disse que NÃO tem. */
    public record Achado(String dono, String expressao, String trecho, boolean negado) {}

    private final List<Expressao> expressoes;
    /** true: devolve todas as expressões parecidas que casam no mesmo ponto (usado para os agravantes). */
    private final boolean todosNoPonto;

    public BuscaExpressoes(List<Expressao> lista) { this(lista, false); }

    /** Índices: expressões pela 1ª letra da 1ª palavra; palavras-chave pré-calculadas para a busca solta. */
    private final java.util.Map<Character, List<Expressao>> porLetra = new java.util.HashMap<>();
    private final java.util.Map<Expressao, String[]> chaves = new java.util.IdentityHashMap<>();

    public BuscaExpressoes(List<Expressao> lista, boolean todosNoPonto) {
        this.todosNoPonto = todosNoPonto;
        this.expressoes = lista.stream().filter(e -> e.palavras().length > 0)
            .sorted(Comparator.comparingInt((Expressao e) -> e.palavras().length).reversed()).toList();
        for (Expressao e : expressoes) {
            porLetra.computeIfAbsent(e.palavras()[0].charAt(0), k -> new ArrayList<>()).add(e);
            List<String> ch = new ArrayList<>();
            boolean pode = true;
            for (String p : e.palavras()) {
                if (p.equals("nao") || p.equals("sem")) { pode = false; break; }
                if (!VAZIAS.contains(p)) ch.add(p);
            }
            if (pode && ch.size() >= 2) chaves.put(e, ch.toArray(new String[0]));
        }
    }

    /** Palavras que não contam na busca por palavras-chave (só ligam as outras). */
    public static final java.util.Set<String> VAZIAS = java.util.Set.of(
        "de", "da", "do", "das", "dos", "a", "o", "as", "os", "e", "em", "no", "na", "nos", "nas", "um", "uma",
        "uns", "umas", "com", "pra", "pro", "para", "por", "pelo", "pela", "que", "q", "se", "me", "meu", "minha",
        "ta", "to", "esta", "estou", "tou", "ao", "foi", "ficou", "fica", "tem", "tenho",
        "ele", "ela", "eu", "isso", "isto", "esse", "essa", "la", "aqui", "bem", "so", "ja", "depois", "quando",
        "como", "tipo", "assim", "agora", "hoje", "faz", "vez", "coisa", "tudo", "todo", "toda");

    /** Mesma palavra com erro de digitação, ou a mesma raiz (5 primeiras letras) em palavras de 5+ letras. */
    public static boolean mesmaRaiz(String palavra, String doTermo) {
        if (DetectorAlerta.parecida(palavra, doTermo)) return true;
        if (palavra.length() < 5 || doTermo.length() < 5) return false;
        String a = radical(palavra), b = radical(doTermo);
        return a.length() >= 4 && a.equals(b);
    }

    /** Sufixos comuns do português, do mais longo ao mais curto (redutor simples, uma retirada só). */
    private static final String[] SUFIXOS = {"issimas", "issimos", "issima", "issimo", "amentos", "amento", "imento", "acoes", "acao", "ando", "endo", "indo",
        "ados", "adas", "idos", "idas", "ado", "ada", "ido", "ida", "aco", "inho", "inha", "ei", "ou", "iu", "am", "em",
        "ar", "er", "ir", "as", "os", "es", "a", "o", "e", "s"};

    private static final java.util.concurrent.ConcurrentHashMap<String, String> RADICAIS = new java.util.concurrent.ConcurrentHashMap<>();

    static String radical(String w) {
        return RADICAIS.computeIfAbsent(w, BuscaExpressoes::calcularRadical);
    }

    private static String calcularRadical(String w) {
        for (String suf : SUFIXOS)
            if (w.endsWith(suf) && w.length() - suf.length() >= 4) return w.substring(0, w.length() - suf.length());
        return w;
    }

    /** Marcas fortes de passado no trecho: o que vem nele é história, não queixa de agora. */
    private static final List<String[]> PASSADO_FORTE = List.of(new String[]{"ano", "passado"}, new String[]{"mes", "passado"},
        new String[]{"anos", "atras"}, new String[]{"meses", "atras"}, new String[]{"faz", "anos"});

    private static final java.util.Set<String> AGORA = java.util.Set.of("agora", "hoje", "ontem", "derrepente", "repente", "agorinha", "atualmente", "hj", "agr");

    private static boolean temPassadoForte(String[] t) {
        for (String[] m : PASSADO_FORTE)
            for (int i = 0; i + m.length <= t.length; i++) {
                boolean ok = true;
                for (int k = 0; k < m.length && ok; k++) ok = t[i + k].equals(m[k]);
                if (ok) return true;
            }
        return false;
    }

    /**
     * Busca por palavras-chave: todas as palavras de conteúdo da expressão aparecem no mesmo trecho, em qualquer
     * ordem, num espaço de até 10 palavras. Se alguma delas vem negada ("sem bolha", "não sangrou"), o achado é negado.
     * Expressões que têm "nao" dentro (positivas com não) só valem na busca exata.
     */
    private static int[] porPalavrasChave(String[] t, String[] chaveArr, boolean exata, java.util.Set<Character> letras) {
        if (chaveArr == null) return null;
        for (String c : chaveArr) if (!letras.contains(c.charAt(0))) return null;   // toda casada exige a mesma 1ª letra
        List<String> chave = java.util.Arrays.asList(chaveArr);
        int[] pos = new int[chave.size()];
        boolean[] tomada = new boolean[t.length];   // cada palavra-chave precisa de uma palavra própria do texto
        for (int k = 0; k < chave.size(); k++) {
            pos[k] = -1;
            for (int i = 0; i < t.length; i++)
                if (!tomada[i] && (exata ? t[i].equals(chave.get(k)) : mesmaRaiz(t[i], chave.get(k)))) { pos[k] = i; tomada[i] = true; break; }
            if (pos[k] < 0) return null;
        }
        int min = java.util.Arrays.stream(pos).min().getAsInt(), max = java.util.Arrays.stream(pos).max().getAsInt();
        int marcas = 0;   // a marca " / " de nova mensagem não conta como palavra
        for (int i = min; i < max; i++) if (t[i].equals(Normalizador.MARCA_BARRA)) marcas++;
        return max - min - marcas <= 10 ? pos : null;
    }

    public List<Achado> procurar(String texto) {
        List<Achado> saida = new ArrayList<>();
        for (String[] t : Normalizador.trechos(texto)) {
            boolean historia = temPassadoForte(t) && !java.util.Arrays.asList(t).stream().anyMatch(AGORA::contains);   // revisão M2
            java.util.Set<Character> letras = new java.util.HashSet<>();
            for (String w : t) if (!w.isEmpty()) letras.add(w.charAt(0));
            java.util.Set<String> donosExatos = new java.util.HashSet<>();   // dono já achado afirmado: não procura de novo
            java.util.Set<String> donosNegados = new java.util.HashSet<>();  // dono achado negado: ainda pode aparecer afirmado
            for (Achado a : procurarExato(t)) {
                boolean doPassado = a.expressao().matches(".*\\b(passado|atras|atrás|faz anos)\\b.*");   // sinal que é sobre o passado
                Achado b = historia && !doPassado ? new Achado(a.dono(), a.expressao(), a.trecho(), true) : a;
                if (b.negado() && (donosNegados.contains(b.dono()) || donosExatos.contains(b.dono()))) continue;
                saida.add(b);
                (b.negado() ? donosNegados : donosExatos).add(b.dono());
            }
            for (Expressao e : expressoes) {
                if (donosExatos.contains(e.dono())) continue;
                int[] pos = porPalavrasChave(t, chaves.get(e), e.exata(), letras);
                if (pos == null || !gatilhoNoMesmoJeito(t, pos, chaves.get(e), e.palavras())) continue;
                int ini = java.util.Arrays.stream(pos).min().getAsInt(), fim = java.util.Arrays.stream(pos).max().getAsInt() + 1;
                // sprint 3: a negação vale para a expressão inteira só se vem antes dela (ou "X nenhum" logo depois);
                // um "não"/"sem" no meio nega só uma parte: aí esta expressão não decide (nem afirma, nem nega)
                boolean[] chave = new boolean[t.length];
                for (int p : pos) chave[p] = !GATILHOS.contains(t[p]);   // "nada" que é palavra-chave ainda nega o que vem depois
                boolean negIni = negado(t, new boolean[t.length], ini, fim);
                boolean parte = false;
                for (int p : pos) if (p != ini && negado(t, chave, p, p + 1)) parte = true;
                if (parte && !negIni) continue;
                boolean neg = historia && !e.texto().matches(".*\\b(passado|atras|atrás|faz anos)\\b.*") || negIni;
                if (neg && donosNegados.contains(e.dono())) continue;
                saida.add(new Achado(e.dono(), e.texto(), String.join(" ", java.util.Arrays.copyOfRange(t, ini, fim)), neg));
                (neg ? donosNegados : donosExatos).add(e.dono());
            }
        }
        return saida;
    }

    /**
     * Palavra-chave que é palavra de negação ("cansaço do nada", "nunca me senti assim") só vale escrita do mesmo jeito:
     * "do nada" casa com "do nada", não com "não vou fazer nada".
     */
    private static boolean gatilhoNoMesmoJeito(String[] t, int[] pos, String[] chave, String[] palavras) {
        for (int k = 0; k < chave.length; k++) {
            if (!GATILHOS.contains(chave[k])) continue;
            int i = java.util.Arrays.asList(palavras).indexOf(chave[k]);
            if (i > 0 && (pos[k] == 0 || !t[pos[k] - 1].equals(palavras[i - 1]))) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sprint 3: alcance da negação

    private static final int JANELA = 4;
    private static final java.util.Set<String> GATILHOS =
        java.util.Set.of("nao", "nem", "sem", "nunca", "nenhum", "nenhuma", "jamais", "zero", "nada");   // "quase" não nega: "quase desmaiei" é sintoma
    /** Intenção antes de "me"/"se": "não vou me matar", "não penso em me matar" (a negação passa por elas). */
    private static final java.util.Set<String> INTENCAO = java.util.Set.of("vou", "vai", "vamos", "quero", "quer", "penso", "pensei",
        "pensa", "pensando", "vontade", "ia", "iria", "pretendo", "consigo");
    private static final java.util.Set<String> TERMINADORES =
        java.util.Set.of("mas", "porem", "entretanto", "contudo", "agora", "hoje", Normalizador.MARCA_BARRA);
    private static final java.util.Set<String> NAO_QUE_REFORCA =
        java.util.Set.of("aguento", "agento", "aguenta", "suporto", "consigo", "consegue", "para", "paro", "passa", "melhora");
    private static final java.util.Set<String> INCERTEZA = java.util.Set.of("parece", "talvez", "tipo", "acho");
    /** Palavras que ficam entre a negação e o sintoma sem ser outro sintoma: "não [tenho mais] dor", "sem [nenhuma] febre". */
    private static final java.util.Set<String> LIGACAO = java.util.Set.of(
        "tenho", "tem", "tinha", "tive", "teve", "tido", "ter", "temos", "sinto", "senti", "sente", "sentiu", "sentindo", "sentia",
        "estou", "esta", "estava", "tava", "to", "ta", "tou", "fiquei", "ficou", "fica", "ficando", "apresenta", "apresentou",
        "notei", "notou", "vi", "viu", "percebi", "percebeu", "houve", "deu", "da", "dando", "mais", "ainda", "mesmo", "muito",
        "muita", "sequer", "qualquer", "algum", "alguma", "aquela", "aquele", "nenhum", "nenhuma", "nada", "sinal", "sinais",
        "meio", "bem", "forte", "leve", "fraca", "fraco", "toda", "todo", "minha", "meu", "um", "uma", "o", "a", "ele", "ela",
        "dele", "dela", "nele", "nela", "isso", "ficar", "sentir", "estar", "apresentar", "aparecer", "notar", "ver", "aparece", "apareceu", "veio", "vem", "voltou", "volta", "e");

    private static boolean conteudo(String p) {
        return !VAZIAS.contains(p) && !LIGACAO.contains(p) && !GATILHOS.contains(p) && !INCERTEZA.contains(p)
            && !p.chars().allMatch(Character::isDigit);
    }

    /**
     * O termo que começa em {@code inicio} (e acaba antes de {@code fim}) está negado? Regras de DetectorAlerta (N2 a N4,
     * N8) com o alcance curto do sprint 3:
     * <ul>
     *   <li>olha no máximo 4 palavras antes do termo, e para em pontuação (outro trecho), em "mas/agora/hoje", em outro
     *       termo já achado ({@code usado});</li>
     *   <li>para no sintoma anterior: em "não tenho febre dor no peito", o "não" fica com a febre e não chega na dor;</li>
     *   <li>"sem X e sem Y": o "sem" mais perto já negou, o "e" para a contagem (não vira negação dupla);</li>
     *   <li>"nenhum" depois de uma palavra ("jeito nenhum", "dor nenhuma") nega a palavra de antes, não o que vem depois;
     *       e "dor nenhuma" nega a dor.</li>
     * </ul>
     * Incerteza ("acho", "não sei se") e negação dupla continuam não negando (na dúvida, o mais urgente).
     */
    private static final java.util.Set<String> VERBO_DEPOIS = java.util.Set.of("tenho", "tem", "tive", "teve", "tinha", "sinto", "senti", "sente", "sentiu");
    /** Depois de "febre não tenho": o que mostra que a frase acabou ali (fim do trecho, nova mensagem, outra frase). */
    private static final java.util.Set<String> FECHA_A_IDEIA = java.util.Set.of(Normalizador.MARCA_BARRA, "e", "to", "ta", "estou",
        "esta", "mas", "so", "ja", "agora", "hoje", "graças", "gracas", "ainda", "tambem");

    /**
     * Negação depois do sintoma: "febre nenhuma", "febre não tenho", "febre nunca tive". Só quando o "não tenho" fecha a
     * ideia: no fim do trecho ou antes de outra coisa que não muda o sentido ("não tem fim", "não tem parado" não negam).
     */
    private static boolean negadoDepois(String[] t, boolean[] usado, int fim) {
        if (fim >= t.length || usado[fim]) return false;
        if (t[fim].equals("nenhum") || t[fim].equals("nenhuma")) return true;
        if (!t[fim].equals("nao") && !t[fim].equals("nunca")) return false;
        int j = fim + 1;
        if (j < t.length && (t[j].equals("assim") || t[j].equals("eu"))) j++;   // "febre nao assim tenho" (voz), "febre nao eu tenho"
        if (j >= t.length || !VERBO_DEPOIS.contains(t[j])) return false;
        j++;
        return j >= t.length || FECHA_A_IDEIA.contains(t[j]);   // "febre nao tenho to na quimio"; "vomitei nunca senti uma dor" não
    }

    /** A palavra j vem logo antes de "me"/"se", ou de "em me"/"de me" ("penso em me matar", "vontade de me matar"). */
    private static boolean antesDeMeSe(String[] t, int j) {
        if (j + 1 < t.length && (t[j + 1].equals("me") || t[j + 1].equals("se"))) return true;
        return j + 2 < t.length && (t[j + 1].equals("em") || t[j + 1].equals("de")) && (t[j + 2].equals("me") || t[j + 2].equals("se"));
    }

    /** "nada", "nenhum" junto de "não"/"nem"/"sem" é a mesma negação ("nem nada estranho", "não tenho nenhuma dor"). */
    private static final java.util.Set<String> CONCORDA = java.util.Set.of("nada", "nenhum", "nenhuma", "zero");

    public static boolean negado(String[] t, boolean[] usado, int inicio, int fim) {
        int negacoes = 0, concordantes = 0;
        boolean incerto = false;
        for (int j = inicio - 1; j >= Math.max(0, inicio - JANELA); j--) {
            if (usado[j]) break;
            String p = t[j];
            if (TERMINADORES.contains(p)) break;
            if (p.equals("que") && j > 0 && t[j - 1].equals("so")) break;          // "só que"
            if (negacoes > 0 && (p.equals("e") || p.equals("ou"))) break;         // "sem X e sem Y"
            if (p.equals("que") && j > 0 && java.util.Set.of("acho", "sera", "meio").contains(t[j - 1])) incerto = true;
            if (INCERTEZA.contains(p)) { incerto = true; continue; }
            if (GATILHOS.contains(p) && !(p.equals("nada") && j > 0 && (t[j - 1].equals("do") || t[j - 1].equals("de")))) {   // "do nada" = de repente
                if ((p.equals("nenhum") || p.equals("nenhuma") || p.equals("nada")) && j > 0 && conteudo(t[j - 1])) break;   // "jeito nenhum", "não dói nada"
                boolean naoSeiSe = p.equals("nao") && j + 1 < t.length && t[j + 1].equals("sei");
                boolean reforco = p.equals("nao") && j + 1 < t.length && NAO_QUE_REFORCA.contains(t[j + 1]);
                if (naoSeiSe) incerto = true; else if (reforco) { } else if (CONCORDA.contains(p)) concordantes++; else negacoes++;
                continue;
            }
            if (INTENCAO.contains(p) && antesDeMeSe(t, j)) continue;   // "não vou me matar": o "vou" não corta a negação
            if (conteudo(p)) break;   // o sintoma de antes é o que foi negado
        }
        if (negacoes == 0) negacoes = Math.min(concordantes, 2);
        if (negacoes == 0 && !incerto && negadoDepois(t, usado, fim)) return true;
        return negacoes == 1 && !incerto;
    }

    private static int casarIgual(String[] t, boolean[] usado, int inicio, String[] palavras) {
        if (inicio + palavras.length > t.length) return -1;
        for (int j = 0; j < palavras.length; j++)
            if (usado[inicio + j] || !t[inicio + j].equals(palavras[j])) return -1;
        return inicio + palavras.length;
    }

    private List<Achado> procurarExato(String[] t) {
        List<Achado> saida = new ArrayList<>();
        {
            boolean[] usado = new boolean[t.length];
            int i = 0;
            while (i < t.length) {
                if (usado[i]) { i++; continue; }
                Expressao achada = null;
                int fim = -1;
                List<Expressao> daLetra = t[i].isEmpty() ? List.of() : porLetra.getOrDefault(t[i].charAt(0), List.of());
                for (Expressao e : daLetra) {
                    fim = e.exata() ? casarIgual(t, usado, i, e.palavras()) : DetectorAlerta.casar(t, usado, i, e.palavras());
                    if (fim > 0) { achada = e; break; }
                }
                if (achada == null) { i++; continue; }
                boolean negado = negado(t, usado, i, fim);
                String trecho = String.join(" ", java.util.Arrays.copyOfRange(t, i, fim));
                // todas as expressões que casam neste ponto (mesma ou parecida: "molinho"/"molinha"), de todos os donos
                for (Expressao e : daLetra) {
                    boolean junto = java.util.Arrays.equals(e.palavras(), achada.palavras());
                    if (!junto && todosNoPonto)
                        junto = (e.exata() ? casarIgual(t, usado, i, e.palavras()) : DetectorAlerta.casar(t, usado, i, e.palavras())) > 0;
                    if (junto) saida.add(new Achado(e.dono(), e.texto(), trecho, negado));
                }
                for (int k = i; k < fim; k++) usado[k] = true;
                i = fim;
            }
        }
        return saida;
    }
}
