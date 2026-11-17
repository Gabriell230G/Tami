package br.com.fiap.tami.modelo;

/** Quem é a pessoa para o usuário. */
public enum Relacao {
    EU("Você"), FILHO("Filho ou filha"), MAE("Mãe"), PAI("Pai"), AVO("Avó ou avô"), PARCEIRO("Parceiro ou parceira"), OUTRO("Outra pessoa");

    private final String rotulo;
    Relacao(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
