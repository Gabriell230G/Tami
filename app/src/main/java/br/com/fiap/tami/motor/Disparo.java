package br.com.fiap.tami.motor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Uma regra que disparou. O destino final é o mais grave de todos os disparos (M06).
 * {@code regra}: código rastreável (RA-FIL_01, RT-01, LX-AVC, FEB_AFIM-R4...).
 * {@code exige}: o que a tela de resultado precisa mostrar (MATERNIDADE, CVV188, BLK-...).
 */
public record Disparo(String regra, Destino destino, PrioridadeUbs prioridade, String motivo,
                      Set<String> exige, boolean porNaoSei) {

    public static Disparo de(String regra, Destino destino, String motivo, boolean porNaoSei) {
        PrioridadeUbs p = destino == Destino.UBS_EM_BREVE ? PrioridadeUbs.PROXIMOS_DIAS : null;
        return new Disparo(regra, destino, p, motivo, Set.of(), porNaoSei);
    }

    public static Disparo de(String regra, Destino destino, String motivo) {
        return de(regra, destino, motivo, false);
    }

    public static Disparo ubs(String regra, PrioridadeUbs prioridade, String motivo, boolean porNaoSei) {
        return new Disparo(regra, Destino.UBS_EM_BREVE, prioridade, motivo, Set.of(), porNaoSei);
    }

    public static Disparo ubs(String regra, PrioridadeUbs prioridade, String motivo) {
        return ubs(regra, prioridade, motivo, false);
    }

    public Disparo exigindo(String... itens) {
        Set<String> novo = new LinkedHashSet<>(exige);
        novo.addAll(java.util.Arrays.asList(itens));
        return new Disparo(regra, destino, prioridade, motivo, java.util.Collections.unmodifiableSet(novo), porNaoSei);
    }
}
