package br.com.fiap.tami.motor;

import java.util.List;
import java.util.Optional;

/**
 * Uma pergunta (ou um nó de decisão) de um roteiro.
 * {@code alarme} = [A]: "Não sei" vale "Sim". {@code podeSamu} = [S]: continua sendo perguntada
 * mesmo depois de um UPA_HOJE (M05).
 */
public record No(String id, String roteiro, int ordem, Tipo tipo, boolean alarme, boolean podeSamu,
                 String mostrarSe, String herdaDe, String pergunta, String ajuda, List<Opcao> opcoes) {

    public enum Tipo { UNICA, MULTI, DECISAO }

    public Optional<Opcao> opcao(String codigo) {
        return opcoes.stream().filter(o -> o.codigo().equals(codigo)).findFirst();
    }
}
