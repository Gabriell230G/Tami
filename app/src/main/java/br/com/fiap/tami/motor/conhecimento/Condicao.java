package br.com.fiap.tami.motor.conhecimento;

import br.com.fiap.tami.motor.Destino;

import java.util.List;
import java.util.Set;

/** Uma linha do condicoes.csv. {@code destinoMinimo}: piso quando a condição é reconhecida no texto. */
public record Condicao(String id, String nomePopular, Destino destinoPadrao, Destino destinoMinimo,
                       Set<String> gruposRisco, boolean casaSegura, List<String> expressoes, String sintomas) {
    public Condicao(String id, String nomePopular, Destino destinoPadrao, Destino destinoMinimo,
                    Set<String> gruposRisco, boolean casaSegura, List<String> expressoes) {
        this(id, nomePopular, destinoPadrao, destinoMinimo, gruposRisco, casaSegura, expressoes, "");
    }
}
