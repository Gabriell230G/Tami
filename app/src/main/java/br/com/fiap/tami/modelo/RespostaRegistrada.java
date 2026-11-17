package br.com.fiap.tami.modelo;

import java.time.LocalDateTime;

/** Uma pergunta respondida, como fica guardada no banco (só com a permissão de dados de saúde). */
public record RespostaRegistrada(Long id, Long idConversa, int ordem, String codigoPergunta, String pergunta,
                                 String resposta, String origem, boolean naoSei, LocalDateTime data) {}
