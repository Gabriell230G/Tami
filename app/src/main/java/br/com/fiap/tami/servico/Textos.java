package br.com.fiap.tami.servico;

import java.util.HashMap;
import java.util.Map;

/** Textos fixos e revisados da Tami (textos_tami.csv): mensagens de resultado, blocos de cuidado e telas. Nada é gerado na hora. */
public final class Textos {

    private final Map<String, String> porCodigo = new HashMap<>();

    public Textos() {
        for (Map<String, String> l : CargaConhecimento.ler("/roteiros/textos_tami.csv")) {
            String t = l.getOrDefault("texto_tami", "");
            if (t.isBlank()) t = l.getOrDefault("texto_atual", "");
            if (!t.isBlank()) porCodigo.put(l.get("codigo"), t.strip());
        }
    }

    /** O texto pelo código, ou null se não existir. */
    public String texto(String codigo) { return codigo == null ? null : porCodigo.get(codigo); }

    /** A versão revisada da pergunta, se houver; senão a do roteiro. */
    public String pergunta(String id, String doRoteiro) { return porCodigo.getOrDefault(id, doRoteiro); }

    public int tamanho() { return porCodigo.size(); }
}
