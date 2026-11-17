package br.com.fiap.tami.motor.conhecimento;

import br.com.fiap.tami.motor.Destino;

import java.util.List;
import java.util.Set;

/** Uma linha do sinais_alerta.csv: um sinal estudado em fonte oficial, com o destino que ele pede. */
public record SinalAlerta(String id, String area, String descricao, String termoMedico, Destino destino,
                          Set<String> valePara, Set<String> sobeSe, List<String> expressoes,
                          String fonte, boolean revisarMedico) {

    /** Sinais de risco à própria vida (marcados com "CVV 188" no termo médico) valem mesmo com "não" antes (D-012). */
    public boolean naoNegavel() { return termoMedico.contains("CVV 188"); }
}
