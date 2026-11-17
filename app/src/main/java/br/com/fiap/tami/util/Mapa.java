package br.com.fiap.tami.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monta mapas ordenados de forma curta: Mapa.de("id", 1, "nome", "Ana"). Vira JSON na API. */
public final class Mapa {
    private Mapa() {}

    public static Map<String, Object> de(Object... chaveValor) {
        if (chaveValor.length % 2 != 0) throw new IllegalArgumentException("pares chave/valor incompletos");
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < chaveValor.length; i += 2) m.put((String) chaveValor[i], chaveValor[i + 1]);
        return m;
    }
}
