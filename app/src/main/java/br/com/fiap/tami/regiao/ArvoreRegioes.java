package br.com.fiap.tami.regiao;

import br.com.fiap.tami.motor.lexico.BuscaExpressoes;
import br.com.fiap.tami.motor.lexico.Normalizador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A árvore das regiões do corpo, montada a partir da tabela T_TAMI_REGIAO (cada linha aponta para a mãe).
 * Ela acha a região no que a pessoa escreveu: "meu bracinho tá doendo" ➜ Corpo todo › Braços e mãos › Braço.
 */
public final class ArvoreRegioes {

    /** Uma linha da tabela T_TAMI_REGIAO. */
    public record Linha(String codigo, String codigoPai, String nome, int nivel, String palavras) {}

    /** Onde a região apareceu no texto. */
    public record Achado(NoRegiao regiao, String trecho, int posicao) {}

    private final Map<String, NoRegiao> porCodigo = new LinkedHashMap<>();
    private NoRegiao raiz;

    public ArvoreRegioes(Collection<Linha> linhas) {
        for (Linha l : linhas) {
            List<String> pal = l.palavras() == null || l.palavras().isBlank() ? List.of()
                : Arrays.stream(l.palavras().split("\\|")).map(String::strip).filter(p -> !p.isEmpty()).toList();
            porCodigo.put(l.codigo(), new NoRegiao(l.codigo(), l.nome(), l.nivel(), pal));
        }
        for (Linha l : linhas) {
            NoRegiao n = porCodigo.get(l.codigo());
            if (l.codigoPai() == null) {
                if (raiz != null) throw new IllegalArgumentException("A árvore tem duas raízes: " + raiz.getCodigo() + " e " + l.codigo());
                raiz = n;
            } else {
                NoRegiao pai = porCodigo.get(l.codigoPai());
                if (pai == null) throw new IllegalArgumentException("Região " + l.codigo() + " aponta para uma mãe que não existe: " + l.codigoPai());
                pai.adicionarFilho(n);
            }
        }
        if (raiz == null) throw new IllegalArgumentException("A árvore de regiões está vazia.");
    }

    public NoRegiao raiz() { return raiz; }
    public int tamanho() { return porCodigo.size(); }
    public Optional<NoRegiao> buscar(String codigo) { return Optional.ofNullable(codigo == null ? null : porCodigo.get(codigo)); }
    public Collection<NoRegiao> todas() { return porCodigo.values(); }

    /** Todas as regiões citadas no texto, na ordem em que aparecem (a mais específica ganha no mesmo lugar). */
    public List<Achado> procurar(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        String t = " " + Normalizador.normalizar(texto) + " ";
        String[] palavrasTexto = t.strip().split(" ");
        List<Achado> achados = new ArrayList<>();
        for (NoRegiao n : porCodigo.values())
            for (String p : n.getPalavras()) {
                int i = t.indexOf(" " + p + " ");
                if (i >= 0) { achados.add(new Achado(n, p, i)); continue; }
                if (!p.contains(" ") && p.length() >= 5)   // "bracinho", "joelhos": mesma raiz
                    for (int k = 0, pos = 0; k < palavrasTexto.length; pos += palavrasTexto[k].length() + 1, k++)
                        if (palavrasTexto[k].length() >= 4 && palavrasTexto[k].regionMatches(0, p, 0, 4) && BuscaExpressoes.mesmaRaiz(palavrasTexto[k], p)) {
                            achados.add(new Achado(n, palavrasTexto[k], pos));
                            break;
                        }
            }
        // no mesmo trecho, vale a expressão mais longa ("pé da barriga" ganha de "pé")
        achados.sort(Comparator.comparingInt(Achado::posicao).thenComparing(a -> -a.trecho().length()));
        List<Achado> out = new ArrayList<>();
        Set<String> vistas = new LinkedHashSet<>();
        int fimUltimo = -1;
        for (Achado a : achados) {
            if (a.posicao() < fimUltimo) continue;
            if (vistas.add(a.regiao().getCodigo())) out.add(a);
            fimUltimo = a.posicao() + a.trecho().length() + 1;
        }
        return out;
    }

    /** A primeira região do texto (a queixa principal costuma vir primeiro), se houver. */
    public Optional<Achado> principal(String texto) {
        return procurar(texto).stream().findFirst();
    }

    /** Códigos da região e de tudo que está abaixo dela: o filtro usado no banco. */
    public List<String> codigosDaSubArvore(String codigo) {
        return buscar(codigo).map(n -> n.subArvore().stream().map(NoRegiao::getCodigo).toList()).orElse(List.of());
    }

    /**
     * Nome da região no formato que o motor de regras entende (ele dá prioridade aos sinais dessa parte do corpo).
     * Pele e corpo todo não têm grande região no motor.
     */
    public static String nomeParaMotor(NoRegiao n) {
        if (n == null) return null;
        return switch (n.grandeRegiao().getCodigo()) {
            case "CABECA_PESCOCO" -> "Cabeça";
            case "TRONCO_PEITO" -> n.getCodigo().equals("COSTELAS") ? "Costelas" : "Peito";
            case "BARRIGA_GRUPO" -> "Barriga";
            case "COSTAS_GRUPO" -> n.getCodigo().equals("QUADRIL") ? "Quadril" : "Costas";
            case "BRACOS" -> "Braço";
            case "PERNAS" -> "Perna";
            default -> null;
        };
    }
}
