package br.com.fiap.tami.motor;

import java.util.List;

/** Um botão de resposta. {@code gravidade > 0} marca resposta positiva (usada pelo M02 e M03). */
public record Opcao(String codigo, String rotulo, int gravidade, List<Efeito> efeitos) {
    public boolean positiva() { return gravidade > 0; }
}
