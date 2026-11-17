package br.com.fiap.tami.motor;

import java.util.List;

/** Uma resposta registrada, para o resumo do médico e para auditoria. */
public record RespostaDada(String noId, String pergunta, List<String> codigos, List<String> rotulos,
                           Origem origem, boolean porNaoSei) {

    /** USUARIO tocou no botão; TEXTO veio do que a pessoa escreveu; HERDADA veio de outra pergunta (M07). */
    public enum Origem { USUARIO, TEXTO, HERDADA }
}
