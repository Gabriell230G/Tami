package br.com.fiap.tami.motor.lexico;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D-024: lê números de sinais vitais no texto livre e põe, logo depois do número, as palavras que a base de
 * conhecimento já conhece ("termometro deu 38,4" ➜ "termometro deu 38,4 graus febre"). Fica no mesmo trecho,
 * então a busca por palavras-chave junta o número com o resto da frase. Nunca tira nada do texto.
 * Limiares (cautelosos): febre a partir de 37,8 · febre alta a partir de 39,5 · saturação até 94 · batimento
 * em repouso a partir de 130 · glicemia até 70 (baixa) ou a partir de 250/"HI" (alta) · pressão a partir de
 * 14 por 9 (alta), 18 por 11 (muito alta) ou máxima até 9 (baixa).
 */
public final class LeitorNumeros {

    private LeitorNumeros() {}

    private static final String NAO_E_TEMPERATURA = "(?!\\s*(anos?|semanas?|sem|dias?|mes|meses|kg|quilos?|kilos?|minutos?|min|horas?|h|hs|vezes|x|por|mil|reais|cm|mg|ml|batimentos?|bpm|filhos?|pessoas?|anos de idade)\\b|\\s*[/%])";   // revisão A6: com fim de palavra ("mesmo" não é "mes")
    /** 38,4 / 38.4 / 39 e meio */
    private static final Pattern TEMP_DECIMAL = Pattern.compile("(?<!r\\$ )(?<!r\\$)(?<!peso )(?<!nota )(?<!kg )\\b(3[5-9]|4[0-3])\\s*[,.]\\s*(\\d)\\b" + NAO_E_TEMPERATURA);   // "R$ 38,5", "peso 38,5" não são febre
    private static final Pattern TEMP_MEIO = Pattern.compile("\\b(3[7-9]|4[0-2]) e meio\\b");
    /** 38 inteiro só com contexto de temperatura antes (termometro deu 38, com 38, febre de 39) */
    private static final Pattern TEMP_INTEIRO = Pattern.compile(
        "\\b(termometro|temperatura|febre|febre de|de febre|deu|marcando|marcou|marca|com|ta|esta|chegou a|bateu)\\s+(?:a\\s+|em\\s+)?(3[89]|4[0-2])\\b(?!\\s*[,.]\\s*\\d)(?!\\s+e\\s+\\d+\\s+(anos?|dias?|meses|semanas?|horas?|filhos?|vezes|reais)\\b)" + NAO_E_TEMPERATURA);
    /** temperatura baixa inteira só com contexto explícito de termômetro ("termometro deu 34", "temperatura 35") */
    private static final Pattern TEMP_BAIXA_INT = Pattern.compile(
        "\\b(termometro|temperatura|temp)\\s+(?:deu\\s+|marcou\\s+|marcando\\s+|ta\\s+|de\\s+|esta\\s+|em\\s+)?(3[0-5])(?:\\s*[,.]\\s*(\\d))?\\b" + NAO_E_TEMPERATURA);
    private static final Pattern GRAUS = Pattern.compile("(?<!fazendo )(?<!faz )(?<!calor de )\\b(3[7-9]|4[0-3])\\s*(graus|°|º)");   // só febre; frio é pelo termômetro

    private static final Pattern SATURACAO = Pattern.compile(
        "\\b(saturacao|saturando|saturou|satura|sat|oxigenio|oximetro|oxigenacao|o2)\\b\\D{0,20}?\\b([5-9]\\d)\\b\\s*%?");
    private static final Pattern BATIMENTO_A = Pattern.compile("\\b(\\d{2,3})\\s*(bpm|batimentos?|batidas?|por minuto de pulso)\\b");
    private static final Pattern BATIMENTO_B = Pattern.compile(
        "\\b(batimentos?|pulso|frequencia cardiaca|coracao|relogio|smartwatch)\\D{0,25}?\\b(\\d{2,3})\\b(?!\\s*[:h]\\d)(?!\\s*(anos?|semanas?|dias?|meses|kg|minutos?|min\\b|horas?|segundos?|vezes|por\\s+\\d|x\\s*\\d))");
    private static final Pattern GLICEMIA = Pattern.compile(
        "\\b(glicemia|glicose|dextro|hgt|acucar|glicosimetro|aparelhinho do acucar|ponta de dedo)\\D{0,20}?\\b(\\d{2,3}|hi|high|lo)\\b(?!\\s*(dias?|anos?|meses|semanas?|horas?|reais|kg|vezes))");
    private static final Pattern PRESSAO = Pattern.compile(
        "\\b(pressao|pa)\\b\\D{0,20}?\\b(\\d{1,3})\\s*(?:por|x|/|com)\\s*(\\d{1,3})\\b(?!\\s*(horas?|h|dias?|em))");
    private static final Pattern PRESSAO_SO_MAXIMA = Pattern.compile(
        "\\b(pressao|pa)\\b\\s+(?:deu\\s+|ta\\s+|esta\\s+|de\\s+|em\\s+|a\\s+|marcou\\s+|marcando\\s+)?(\\d{1,3})\\b(?!\\s*(?:por|x|/|com|anos?|semanas?|dias?|meses|em|horas?)\\b)");

    /** O texto com as palavras dos sinais vitais acrescentadas depois de cada número reconhecido. */
    public static String completar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        // sprint 3: muletas da fala saem antes (o "é" e o "ó" só dá para separar com o acento)
        String s = Normalizer.normalize(Normalizador.semMuletas(texto.toLowerCase(Locale.ROOT)), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = falados(s);   // sprint 3: "trinta e oito e quatro" ➜ "38,4"; "38 e 5" com febre ➜ "38,5"; "34 sem anos" ➜ "34 semanas"
        // "39sem" e "39 sem," ➜ "39 semanas": "sem" solto seria lido como negação ("sem bolsa estourou")
        s = s.replaceAll("\\b(\\d{1,2})\\s?sem\\b(?=\\s*([.,;:!?]|$|e\\b|de gesta|de gravidez|gravida|gestante|bolsa|com\\b|to\\b|ta\\b))", "$1 semanas");
        s = s.replaceAll("\\b(\\d{1,2})sem\\b", "$1 semanas");
        s = s.replaceAll("\\b(gravida|gestante|gravidez|gestacao)( de| com)? (\\d{1,2}) sem\\b", "$1$2 $3 semanas");   // "gravida de 30 sem a perna..."
        s = s.replaceAll("(\\d)(semanas\\b)", "$1 $2");
        java.util.List<int[]> usados = new java.util.ArrayList<>();
        java.util.TreeMap<Integer, String> extras = new java.util.TreeMap<>();
        marcar(s, PRESSAO, m -> pressao(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))), usados, extras);
        marcar(s, PRESSAO_SO_MAXIMA, m -> pressao(Integer.parseInt(m.group(2)), -1), usados, extras);
        marcar(s, SATURACAO, m -> Integer.parseInt(m.group(2)) <= 94 ? "saturacao baixa oxigenio baixo" : null, usados, extras);
        marcar(s, BATIMENTO_A, m -> batimento(Integer.parseInt(m.group(1))), usados, extras);
        marcar(s, BATIMENTO_B, m -> batimento(Integer.parseInt(m.group(2))), usados, extras);
        marcar(s, GLICEMIA, m -> glicemia(m.group(2)), usados, extras);
        marcar(s, TEMP_DECIMAL, m -> temperatura(Double.parseDouble(m.group(1) + "." + m.group(2))), usados, extras);
        marcar(s, TEMP_MEIO, m -> temperatura(Integer.parseInt(m.group(1)) + 0.5), usados, extras);
        marcar(s, TEMP_INTEIRO, m -> temperatura(Integer.parseInt(m.group(2))), usados, extras);
        marcar(s, TEMP_BAIXA_INT, m -> temperatura(Double.parseDouble(m.group(2) + "." + (m.group(3) == null ? "0" : m.group(3)))), usados, extras);
        marcar(s, GRAUS, m -> temperatura(Integer.parseInt(m.group(1))), usados, extras);
        StringBuilder out = new StringBuilder();
        int ultimo = 0;
        for (var e : extras.entrySet()) {
            out.append(s, ultimo, e.getKey()).append(' ').append(e.getValue()).append(' ');
            ultimo = e.getKey();
        }
        out.append(s.substring(ultimo));
        s = out.toString();
        return s;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sprint 3 (voz): números falados

    private static final Map<String, Integer> UNIDADES = Map.ofEntries(
        Map.entry("zero", 0), Map.entry("um", 1), Map.entry("uma", 1), Map.entry("dois", 2), Map.entry("duas", 2),
        Map.entry("tres", 3), Map.entry("quatro", 4), Map.entry("cinco", 5), Map.entry("seis", 6), Map.entry("sete", 7),
        Map.entry("oito", 8), Map.entry("nove", 9), Map.entry("dez", 10), Map.entry("onze", 11), Map.entry("doze", 12),
        Map.entry("treze", 13), Map.entry("catorze", 14), Map.entry("quatorze", 14), Map.entry("quinze", 15),
        Map.entry("dezesseis", 16), Map.entry("dezasseis", 16), Map.entry("dezessete", 17), Map.entry("dezoito", 18),
        Map.entry("dezenove", 19));
    private static final Map<String, Integer> DEZENAS = Map.of("vinte", 20, "trinta", 30, "quarenta", 40,
        "cinquenta", 50, "sessenta", 60, "setenta", 70, "oitenta", 80, "noventa", 90);
    private static final Map<String, Integer> CENTENAS = Map.ofEntries(Map.entry("cem", 100), Map.entry("cento", 100),
        Map.entry("duzentos", 200), Map.entry("duzentas", 200), Map.entry("trezentos", 300), Map.entry("trezentas", 300),
        Map.entry("quatrocentos", 400), Map.entry("quatrocentas", 400), Map.entry("quinhentos", 500), Map.entry("quinhentas", 500),
        Map.entry("seiscentos", 600), Map.entry("setecentos", 700), Map.entry("oitocentos", 800), Map.entry("novecentos", 900));

    private static final Pattern PALAVRA = Pattern.compile("[a-z0-9]+");
    /** Antes do número: sinal vital ou "o termômetro deu/marcou" (até 25 letras antes, sem pontuação no meio). */
    private static final Pattern VITAL_ANTES = Pattern.compile(
        "\\b(termometro|temperatura|temp|febre|feb|frebe|saturacao|saturando|saturou|satura|sat|oxigenio|oximetro|oxigenacao|o2|pressao|pa"
        + "|glicemia|glicose|dextro|hgt|acucar|glicosimetro|batimentos?|pulso|frequencia|deu|marcou|marcando|mediu|medi|chegou a"
        + "|bateu|subiu|esta com|ta com|to com|estou com|tava com|estava com)\\b[^.,;:!?]{0,25}$");
    /** Depois do número: unidade de tempo, de peso ou de gente. Aí o número fica como está (a base tem "cinco dias"). */
    private static final Pattern UNIDADE_DEPOIS = Pattern.compile(
        "^\\s+(anos?|dias?|semanas?|mes|meses|horas?|minutos?|segundos?|vezes|quilos?|kg|filhos?|filhas?|pessoas?|reais|comprimidos?|passos|metros?|andares)\\b");
    private static final Pattern GESTACAO_PERTO = Pattern.compile("\\b(gravida|gestante|gravidez|gestacao|gesta|bebe|neném|nenem|barriga|pre natal|prenatal)\\b");

    /** Número por extenso de 0 a 999 a partir da palavra i. {valor, palavras usadas} ou null. */
    private static int[] lerExtenso(List<String> w, int i) {
        int j = i, v = 0;
        if (j < w.size() && CENTENAS.containsKey(w.get(j))) {
            v = CENTENAS.get(w.get(j));
            j++;
            if (w.get(j - 1).equals("cem")) return new int[] {v, 1};
            if (j + 1 < w.size() && w.get(j).equals("e") && (DEZENAS.containsKey(w.get(j + 1)) || UNIDADES.containsKey(w.get(j + 1)))) j++;
            else return w.get(j - 1).equals("cento") ? null : new int[] {v, j - i};
        }
        if (j < w.size() && DEZENAS.containsKey(w.get(j))) {
            v += DEZENAS.get(w.get(j));
            j++;
            if (j + 1 < w.size() && w.get(j).equals("e") && UNIDADES.containsKey(w.get(j + 1)) && UNIDADES.get(w.get(j + 1)) < 10
                    && UNIDADES.get(w.get(j + 1)) > 0) { v += UNIDADES.get(w.get(j + 1)); j += 2; }
            return new int[] {v, j - i};
        }
        if (j < w.size() && UNIDADES.containsKey(w.get(j))) return new int[] {v + UNIDADES.get(w.get(j)), j - i + 1};
        return j > i ? new int[] {v, j - i} : null;
    }

    /**
     * Números falados. Número composto ("trinta e oito", "cento e vinte") vira dígito sempre. Número de uma palavra só
     * vira dígito perto de sinal vital ("pressão doze por oito"), antes de "graus" e de "semanas" de gestação; perto de
     * "dias", "minutos", "vezes" fica por extenso, porque a base tem "cinco dias", "duas vezes".
     */
    static String falados(String s) {
        List<String> w = new ArrayList<>();
        List<int[]> pos = new ArrayList<>();
        Matcher m = PALAVRA.matcher(s);
        while (m.find()) { w.add(m.group()); pos.add(new int[] {m.start(), m.end()}); }
        StringBuilder out = new StringBuilder();
        int ultimo = 0;
        for (int i = 0; i < w.size(); i++) {
            int[] lido = lerExtenso(w, i);
            if (lido == null) continue;
            int n = lido[1], v = lido[0];
            int ini = pos.get(i)[0], fim = pos.get(i + n - 1)[1];
            // as palavras do número precisam estar juntas (só espaço entre elas)
            if (!s.substring(ini, fim).matches("[a-z ]+")) continue;
            String antes = s.substring(Math.max(0, ini - 40), ini), depois = s.substring(fim);
            boolean vira;
            if (n >= 3) vira = true;   // composto
            else if (v <= 1 && !w.get(i).equals("zero")) vira = false;   // "um", "uma": artigo
            else if (depois.matches("^\\s*(graus|°|º)[\\s\\S]*") || depois.matches("^\\s+por cento\\b[\\s\\S]*")) vira = true;
            else if (depois.matches("^\\s+(semanas?|sem anos?|sem ana)\\b[\\s\\S]*"))
                vira = v >= 10 || GESTACAO_PERTO.matcher(s.substring(Math.max(0, ini - 60), Math.min(s.length(), fim + 60))).find();
            else vira = VITAL_ANTES.matcher(antes).find() && !UNIDADE_DEPOIS.matcher(depois).find();
            if (!vira) continue;
            out.append(s, ultimo, ini).append(v);
            ultimo = fim;
            i += n - 1;
        }
        out.append(s.substring(ultimo));
        s = out.toString();
        // "38 e quatro", "38 virgula cinco": a casa decimal falada depois de um número de temperatura
        s = DECIMAL_FALADA.matcher(s).replaceAll(r -> r.group(1) + " " + r.group(2) + " " + UNIDADES.get(r.group(3)));
        // "34 sem anos" ➜ "34 semanas" (a voz partiu a palavra)
        s = s.replaceAll("\\b(\\d{1,2})\\s+sem\\s+(anos?|ana)\\b", "$1 semanas");
        // "3 8,4" ➜ "38,4"
        s = PARTIDO.matcher(s).replaceAll(r -> {
            double t = Double.parseDouble(r.group(1) + r.group(2).replace(',', '.'));
            return t >= 34 && t < 43 ? r.group(1) + r.group(2) : r.group();
        });
        // "38 e 5", "38 5", "38 virgula 5" ➜ "38,5" (com contexto de temperatura, ou "virgula"/"ponto" dito)
        Matcher d = DECIMAL.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (d.find()) {
            boolean dito = d.group(2).contains("virgula") || d.group(2).contains("ponto");
            String antes = s.substring(Math.max(0, d.start() - 30), d.start());
            String depois = s.substring(d.end());
            boolean contexto = dito || TEMP_ANTES.matcher(antes).find() || TEMP_DEPOIS.matcher(depois).find();
            d.appendReplacement(sb, contexto ? d.group(1) + "," + d.group(3) : Matcher.quoteReplacement(d.group()));
        }
        d.appendTail(sb);
        s = sb.toString();
        // "febre de 385", "termometro deu 392" ➜ 38,5 / 39,2
        s = COLADO.matcher(s).replaceAll("$1$2,$3");
        return s;
    }

    private static final Pattern DECIMAL_FALADA = Pattern.compile(
        "\\b(3[4-9]|4[0-3]) (e|virgula|ponto) (zero|um|dois|tres|quatro|cinco|seis|sete|oito|nove)\\b(?!\\s+e\\s+(um|dois|tres|quatro|cinco|seis|sete|oito|nove)\\b)");
    private static final Pattern PARTIDO = Pattern.compile("(?<![\\d,.])\\b([34]) (\\d,\\d)\\b(?![,.]?\\d)");
    private static final Pattern DECIMAL = Pattern.compile(
        "(?<![\\d,.])\\b(3[4-9]|4[0-3])(\\s+e\\s+|\\s+virgula\\s+|\\s+ponto\\s+|\\s+)(\\d)\\b(?!\\s*[,.]\\s*\\d)(?!\\s*\\d)" + NAO_E_TEMPERATURA);
    private static final Pattern TEMP_ANTES = Pattern.compile(
        "\\b(termometro|temperatura|temp|febre|feb|frebe|fevre|graus|deu|marcou|marcando|mediu|medi|com|chegou|bateu|subiu|esta|ta|to|tava|passou de)\\b[^.,;:!?]{0,12}$");
    private static final Pattern TEMP_DEPOIS = Pattern.compile("^\\s*(graus|°|º|de febre|de temperatura)");
    private static final Pattern COLADO = Pattern.compile(
        "\\b((?:termometro|temperatura|febre)\\s+(?:deu\\s+|de\\s+|marcou\\s+|marcando\\s+|em\\s+|a\\s+|ta\\s+|esta\\s+)?)(3[5-9]|4[0-2])(\\d)\\b(?!\\s*[,.]\\d)" + NAO_E_TEMPERATURA);

    static String temperatura(double t) {
        if (t >= 39.5) return "graus febre alta";
        if (t >= 37.8) return "graus febre";
        if (t <= 35.0) return "graus corpo gelado temperatura baixa";
        return null;
    }

    static String batimento(int b) {
        if (b >= 130 && b <= 260) return "coracao disparado taquicardia batimento acelerado";
        if (b >= 20 && b <= 40) return "coracao muito lento pulso fraco";
        return null;
    }

    static String glicemia(String v) {
        if (v.equals("hi") || v.equals("high")) return "glicose muito alta glicemia alta";
        if (v.equals("lo")) return "glicose baixa hipoglicemia";
        int g = Integer.parseInt(v);
        if (g <= 70 && g >= 10) return "glicose baixa hipoglicemia";
        if (g >= 250) return "glicose muito alta glicemia alta";
        return null;
    }

    /** Aceita 18 por 11 e 180 por 110. diastolica -1 quando só veio a máxima. */
    static String pressao(int sis, int dia) {
        if (sis >= 50) sis = Math.round(sis / 10f);
        if (dia >= 30) dia = Math.round(dia / 10f);
        if (sis < 5 || sis > 30) return null;
        if (sis >= 18 || dia >= 11) return "pressao muito alta pressao alta pressao nas alturas";
        if (sis >= 14 || dia >= 9) return "pressao alta";
        if (sis <= 9 && dia <= 6) return "pressao baixa";
        if (sis <= 8 && dia < 0) return "pressao baixa";
        return null;
    }

    private interface Regra { String palavras(Matcher m); }

    /** Cada pedaço do texto original é lido por uma regra só (a primeira, na ordem acima). */
    private static void marcar(String s, Pattern p, Regra r, java.util.List<int[]> usados, java.util.Map<Integer, String> extras) {
        Matcher m = p.matcher(s);
        while (m.find()) {
            int a = m.start(), b = m.end();
            boolean sobrepoe = false;
            for (int[] u : usados) sobrepoe |= a < u[1] && u[0] < b;
            if (sobrepoe) continue;
            String extra;
            try { extra = r.palavras(m); } catch (NumberFormatException e) { extra = null; }
            usados.add(new int[] {a, b});   // número lido (mesmo normal) não é relido por outra regra
            if (extra == null) continue;
            // não repete palavra que o texto já tem ali perto ("febre de 38" ➜ só "graus"): não afasta as outras palavras
            String perto = " " + s.substring(Math.max(0, a - 30), Math.min(s.length(), b + 15)) + " ";
            StringBuilder novo = new StringBuilder();
            for (String w : extra.split(" "))
                if (!w.isBlank() && !perto.matches("(?s).*\\b" + w + "\\b.*")) novo.append(w).append(' ');
            if (novo.length() > 0) extras.merge(b, novo.toString().trim(), (x, y) -> x + " " + y);
        }
    }
}
