package br.com.fiap.tami.motor.lexico;

/**
 * Um termo achado no texto livre, já com a decisão sobre negação.
 * Negado nunca quer dizer "descartado": a pergunta do filtro sobre aquele sinal
 * continua sendo feita (regras N5 e N7).
 */
public record Sinal(String codigo, GrupoLexico grupo, String trecho, boolean negado) {}
