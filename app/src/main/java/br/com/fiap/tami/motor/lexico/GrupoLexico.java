package br.com.fiap.tami.motor.lexico;

/**
 * Grupos do léxico de emergência (relatório "Roteiros", seção Léxico).
 * {@code negavel = false}: a negação nunca descarta o grupo sozinha (suicídio, regra N7).
 */
public enum GrupoLexico {
    PEITO(true), PEITO_VAGO(true),
    AR_GRAVE(true), AR(true),
    CONSCIENCIA(true), CONSCIENCIA_VAGO(true), CONVULSAO(true),
    AVC(true), AVC_VAGO(true),
    SANGUE(true), TRAUMA(true), ALERGIA(true),
    VENENO(true), PROPOSITO(true),
    GESTANTE(true),
    SUICIDIO(false),
    FEBRE(true);

    private final boolean negavel;

    GrupoLexico(boolean negavel) { this.negavel = negavel; }

    public boolean negavel() { return negavel; }
}
