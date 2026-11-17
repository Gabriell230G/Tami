package br.com.fiap.tami.motor;

/** Os quatro destinos, do mais leve ao mais grave. A ordem do enum é a gravidade (M06). */
public enum Destino {
    CUIDADO_EM_CASA, UBS_EM_BREVE, UPA_HOJE, SAMU_192;

    public static Destino maisGrave(Destino a, Destino b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }
}
