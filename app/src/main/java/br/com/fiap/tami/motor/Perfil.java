package br.com.fiap.tami.motor;

/**
 * Quem está sendo avaliado. Idade sempre em meses, arredondada para baixo (M10).
 * Gestante "Não sei" deve chegar aqui como gestante = true (RT-14).
 */
public record Perfil(int idadeMeses, boolean gestante, boolean diabetes) {

    public Perfil {
        if (idadeMeses < 0 || idadeMeses > 1500) throw new IllegalArgumentException("idade em meses inválida: " + idadeMeses);
    }

    public static Perfil adulto(int anos) { return new Perfil(anos * 12, false, false); }

    public boolean menorDe3Meses() { return idadeMeses < 3; }
    public boolean menorDe2Meses() { return idadeMeses < 2; }
    public boolean crianca() { return idadeMeses < 156; }
    public boolean idoso() { return idadeMeses >= 720; }
    /** Grupo de risco do caminho genérico: criança até 15 anos, 60+, gestante ou diabetes. */
    public boolean riscoNoPerfil() { return idadeMeses < 192 || idoso() || gestante || diabetes; }
}
