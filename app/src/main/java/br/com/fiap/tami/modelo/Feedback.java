package br.com.fiap.tami.modelo;

import java.time.LocalDateTime;

/** Resposta ao "Isso te ajudou?". Uma por conversa; a pessoa pode mudar depois. */
public record Feedback(Long id, Long idConversa, Avaliacao avaliacao, String comentario, LocalDateTime data, LocalDateTime atualizacao) {
    public enum Avaliacao { SIM, MAIS_OU_MENOS, NAO }

    public Feedback {
        if (avaliacao == null) throw new IllegalArgumentException("Escolha Sim, Mais ou menos ou Não.");
        if (comentario != null && comentario.length() > 1000) throw new IllegalArgumentException("O comentário pode ter até 1000 letras.");
    }
}
