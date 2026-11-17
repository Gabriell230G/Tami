package br.com.fiap.tami.motor.lexico;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Procura sinais de emergência no texto livre (regras N1 a N8 do relatório "Roteiros").
 * Regra de ouro: na dúvida, casa. Falso positivo é aceitável, falso negativo não.
 */
public final class DetectorAlerta {

    /** Resultado da leitura do texto. {@code passado}: o texto fala de algo que já passou (N5). */
    public record Analise(List<Sinal> sinais, boolean passado) {
        public boolean presente(GrupoLexico g) {
            return sinais.stream().anyMatch(s -> s.grupo() == g && !s.negado());
        }
        public boolean negado(GrupoLexico g) {
            return sinais.stream().anyMatch(s -> s.grupo() == g && s.negado());
        }
    }

    private static final int JANELA = 3;
    private static final Set<String> GATILHOS =
        Set.of("nao", "nem", "sem", "nunca", "nenhum", "nenhuma", "jamais", "zero", "nada", "quase");
    private static final Set<String> TERMINADORES =
        Set.of("mas", "porem", "entretanto", "contudo", "agora", "hoje");
    private static final Set<String> NAO_QUE_REFORCA =
        Set.of("aguento", "agento", "aguenta", "suporto", "consigo", "consegue", "para", "paro", "passa", "melhora");
    private static final Set<String> INCERTEZA_SOZINHA = Set.of("parece", "talvez", "tipo", "acho");
    /** Palavras que podem aparecer no meio de um termo: "garganta [ta] fechando". */
    private static final Set<String> RECHEIO = Set.of(
        "ta", "esta", "to", "estou", "bem", "muito", "meio", "um", "uma", "o", "a",
        "toda", "todo", "minha", "meu", "forte", "leve", "fraca", "fraco", "ficando");
    private static final List<String[]> PASSADO = List.of(
        new String[]{"passou"}, new String[]{"ontem"}, new String[]{"ja", "acordou"},
        new String[]{"ja", "acordei"}, new String[]{"ja", "voltou"}, new String[]{"ja", "melhorou"},
        new String[]{"semana", "passada"}, new String[]{"mais", "cedo"}, new String[]{"ano", "passado"},
        new String[]{"mes", "passado"}, new String[]{"anos", "atras"}, new String[]{"faz", "anos"});
    /** Palavras do português que ficam a 1 ou 2 letras de um termo, mas são outra coisa ("derramei" ≠ "derrame"). */
    private static final Set<String> FALSOS_AMIGOS = Set.of(
        "derramei>derrame", "derramou>derrame", "derramar>derrame", "derramo>derrame",
        "soltando>surtando", "solta>surta", "infartei>infarto");

    private final List<TermoLexico> comNao;
    private final List<TermoLexico> outros;

    public DetectorAlerta(List<TermoLexico> lexico) {
        Comparator<TermoLexico> maisLongoPrimeiro =
            Comparator.comparingInt((TermoLexico t) -> t.palavras().length).reversed();
        this.comNao = lexico.stream()
            .filter(t -> t.tipo() == TermoLexico.Tipo.POSITIVO_COM_NAO)
            .sorted(maisLongoPrimeiro).toList();
        this.outros = lexico.stream()
            .filter(t -> t.tipo() != TermoLexico.Tipo.POSITIVO_COM_NAO)
            .sorted(maisLongoPrimeiro).toList();
    }

    public Analise analisar(String texto) {
        List<Sinal> sinais = new ArrayList<>();
        boolean passado = false;
        for (String[] t : Normalizador.trechos(texto)) {
            passado |= contemAlguma(t, PASSADO);
            boolean[] usado = new boolean[t.length];
            varrer(t, usado, comNao, sinais, false);   // N1: frases que já têm "não"
            varrer(t, usado, outros, sinais, true);    // N2 a N4, N8: com janela de negação
        }
        return new Analise(List.copyOf(sinais), passado);
    }

    private void varrer(String[] t, boolean[] usado, List<TermoLexico> termos,
                        List<Sinal> saida, boolean checarNegacao) {
        int i = 0;
        while (i < t.length) {
            if (usado[i]) { i++; continue; }
            int fim = -1;
            TermoLexico achado = null;
            for (TermoLexico termo : termos) {
                fim = casar(t, usado, i, termo.palavras());
                if (fim > 0) { achado = termo; break; }
            }
            if (achado == null) { i++; continue; }
            boolean negado = checarNegacao && negado(t, usado, i);
            String trecho = String.join(" ", java.util.Arrays.copyOfRange(t, i, fim));
            saida.add(new Sinal(achado.codigo(), achado.grupo(), trecho, negado));
            for (int k = i; k < fim; k++) usado[k] = true;
            i = fim;
        }
    }

    /** Devolve onde o termo termina no texto, ou -1. Aceita uma palavra de recheio entre as palavras do termo. */
    static int casar(String[] t, boolean[] usado, int inicio, String[] palavras) {
        int pos = inicio;
        for (int j = 0; j < palavras.length; j++) {
            if (j > 0 && pos + 1 < t.length && !parecida(t[pos], palavras[j])
                    && RECHEIO.contains(t[pos]) && parecida(t[pos + 1], palavras[j])) {
                pos++;
            }
            if (pos >= t.length || usado[pos] || !parecida(t[pos], palavras[j])) return -1;
            pos++;
        }
        return pos;
    }

    /** Igual, ou com erro de digitação: até 1 letra em palavras de 5+, até 2 em palavras de 8+. */
    /**
     * Palavras escritas corretamente que a base conhece (D-021). Uma palavra conhecida nunca é tratada como erro de
     * digitação de OUTRA palavra: "escada" não vira "escara", "tanto" não vira "tonto".
     */
    private static volatile Set<String> conhecidas = Set.of();

    public static void registrarConhecidas(Set<String> palavras) {
        Set<String> s = new java.util.HashSet<>(conhecidas);
        s.addAll(palavras);
        conhecidas = Set.copyOf(s);
    }

    static boolean parecida(String palavra, String doTermo) {
        if (palavra.equals(doTermo)) return true;
        if (conhecidas.contains(palavra)) return false;
        if (FALSOS_AMIGOS.contains(palavra + ">" + doTermo)) return false;
        int n = doTermo.length();
        if (n < 5 || palavra.length() < 4 || palavra.charAt(0) != doTermo.charAt(0)) return false;
        // 2 erros só em palavra longa e de tamanho parecido ("amarelando" não é "amarelao")
        int limite = n >= 8 && Math.abs(palavra.length() - n) <= 1 ? 2 : 1;
        if (Math.abs(palavra.length() - n) > limite) return false;
        return levenshtein(palavra, doTermo) <= limite;
    }

    static int levenshtein(String a, String b) {
        int[] ant = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) ant[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, ant[j] + 1), ant[j - 1] + custo);
            }
            int[] tmp = ant; ant = atual; atual = tmp;
        }
        return ant[b.length()];
    }

    /**
     * Janela de 3 palavras antes do termo. Para em "mas", "agora", no termo anterior
     * ou no começo do trecho. Incerteza nunca nega (N4) e negação dupla vira incerteza (N8).
     */
    static boolean negado(String[] t, boolean[] usado, int inicio) {
        int negacoes = 0;
        boolean incerto = false;
        for (int j = inicio - 1; j >= Math.max(0, inicio - JANELA); j--) {
            if (usado[j]) break;
            String p = t[j];
            if (TERMINADORES.contains(p)) break;
            if (p.equals("que") && j > 0 && t[j - 1].equals("so")) break;          // "só que"
            if (p.equals("que") && j > 0 && Set.of("acho", "sera", "meio").contains(t[j - 1])) incerto = true;
            if (INCERTEZA_SOZINHA.contains(p)) incerto = true;
            if (GATILHOS.contains(p)) {
                boolean naoSeiSe = p.equals("nao") && j + 1 < t.length && t[j + 1].equals("sei");
                // "não aguento", "não consigo", "não para": o "não" reforça o sintoma, não nega o que vem depois
                boolean reforco = p.equals("nao") && j + 1 < t.length && NAO_QUE_REFORCA.contains(t[j + 1]);
                if (naoSeiSe) incerto = true; else if (!reforco) negacoes++;
            }
        }
        return negacoes == 1 && !incerto;
    }

    private static boolean contemAlguma(String[] t, List<String[]> sequencias) {
        for (String[] seq : sequencias)
            for (int i = 0; i + seq.length <= t.length; i++) {
                boolean ok = true;
                for (int k = 0; k < seq.length && ok; k++) ok = t[i + k].equals(seq[k]);
                if (ok) return true;
            }
        return false;
    }
}
