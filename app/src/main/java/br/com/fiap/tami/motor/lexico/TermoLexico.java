package br.com.fiap.tami.motor.lexico;

/** Uma linha do lexico.csv (no banco: tabela regra_lexico). */
public record TermoLexico(String codigo, GrupoLexico grupo, Tipo tipo, String termo) {

    /** POSITIVO_COM_NAO: o "não" faz parte do sintoma ("não consigo respirar"). */
    public enum Tipo { POSITIVO_COM_NAO, NORMAL, VAGO }

    public String[] palavras() { return termo.split(" "); }
}
