package br.com.fiap.tami.modelo;

import java.time.LocalDateTime;

/** O que a pessoa marcou no "Onde dói?" e na escala de 0 a 10. */
public record RegistroDor(Long id, Long idConversa, String codigoRegiao, String lado, int nivel, String tipo, LocalDateTime data) {
    public RegistroDor {
        if (nivel < 0 || nivel > 10) throw new IllegalArgumentException("O nível da dor vai de 0 a 10.");
        if (lado != null && !lado.matches("DIREITO|ESQUERDO|MEIO")) throw new IllegalArgumentException("Lado inválido.");
    }

    /** Faixa da escala, a mesma do app: 0 sem dor, 1 a 3 fraco, 4 a 6 médio, 7 a 9 forte, 10 muito forte. */
    public String faixa() {
        if (nivel == 0) return "Sem dor";
        if (nivel <= 3) return "Fraco";
        if (nivel <= 6) return "Médio";
        if (nivel <= 9) return "Forte";
        return "Muito forte";
    }
}
