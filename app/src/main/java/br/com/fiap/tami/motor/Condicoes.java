package br.com.fiap.tami.motor;

import java.util.Set;

/** Valores aceitos na coluna "mostrar_se" do nos.csv. */
final class Condicoes {

    static final Set<String> CONHECIDAS = Set.of("NAO_HOJE", "CRIANCA", "MENOR_3M", "MENOR_2M", "GESTANTE",
        "IDOSO_OU_DIABETES", "NAO_IDOSO", "CRIANCA_SEM_CONVULSAO", "SEM_RISCO_NO_PERFIL");

    private Condicoes() {}

    static boolean valida(String c) {
        return c == null || c.isBlank() || CONHECIDAS.contains(c) || c.matches("FLAG:[A-Za-z_]+");
    }

    static boolean mostrar(String c, Sessao s) {
        if (c == null || c.isBlank()) return true;
        Perfil p = s.perfil();
        if (c.startsWith("FLAG:")) return s.flag(c.substring(5));
        return switch (c) {
            case "CRIANCA" -> p.crianca();
            case "NAO_HOJE" -> !s.efetiva("GEN_06").isEmpty() && !s.efetiva("GEN_06").contains("hoje");
            case "MENOR_3M" -> p.menorDe3Meses();
            case "MENOR_2M" -> p.menorDe2Meses();
            case "GESTANTE" -> p.gestante();
            case "IDOSO_OU_DIABETES" -> p.idoso() || p.diabetes();
            case "NAO_IDOSO" -> !p.idoso();
            case "CRIANCA_SEM_CONVULSAO" -> p.crianca() && !s.flag("convulsao");
            case "SEM_RISCO_NO_PERFIL" -> !p.riscoNoPerfil();
            default -> throw new IllegalStateException("condição desconhecida: " + c);
        };
    }
}
