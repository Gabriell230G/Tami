package br.com.fiap.tami.regiao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Um nó da árvore do corpo (padrão Composite): "Corpo todo" ➜ "Braços e mãos" ➜ "Braço".
 * Cada nó conhece a região mãe e as filhas, e guarda as palavras que o identificam no texto.
 */
public class NoRegiao {

    private final String codigo;
    private final String nome;
    private final int nivel;
    private final List<String> palavras;
    private NoRegiao pai;
    private final List<NoRegiao> filhos = new ArrayList<>();

    public NoRegiao(String codigo, String nome, int nivel, List<String> palavras) {
        this.codigo = codigo;
        this.nome = nome;
        this.nivel = nivel;
        this.palavras = List.copyOf(palavras);
    }

    void adicionarFilho(NoRegiao filho) {
        filho.pai = this;
        filhos.add(filho);
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public int getNivel() { return nivel; }
    public List<String> getPalavras() { return palavras; }
    public NoRegiao getPai() { return pai; }
    public List<NoRegiao> getFilhos() { return Collections.unmodifiableList(filhos); }
    public boolean folha() { return filhos.isEmpty(); }

    /** Do corpo todo até aqui: [Corpo todo, Braços e mãos, Braço]. */
    public List<NoRegiao> caminho() {
        List<NoRegiao> c = new ArrayList<>();
        for (NoRegiao n = this; n != null; n = n.pai) c.add(0, n);
        return c;
    }

    /** Este nó e todos abaixo dele (busca em profundidade, recursiva). */
    public List<NoRegiao> subArvore() {
        List<NoRegiao> out = new ArrayList<>();
        out.add(this);
        for (NoRegiao f : filhos) out.addAll(f.subArvore());
        return out;
    }

    /** A grande região (nível 1) a que este nó pertence. */
    public NoRegiao grandeRegiao() {
        NoRegiao n = this;
        while (n.pai != null && n.nivel > 1) n = n.pai;
        return n;
    }

    @Override
    public String toString() { return nome; }
}
