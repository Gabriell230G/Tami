package br.com.fiap.tami.modelo;

/** Uma regra do motor que levou a um destino. Serve para explicar a orientação. */
public record RegraAcionada(Long id, Long idConversa, String codigoRegra, String destino, String motivo) {}
