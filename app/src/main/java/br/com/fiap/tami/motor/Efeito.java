package br.com.fiap.tami.motor;

import java.util.ArrayList;
import java.util.List;

/**
 * O que uma resposta faz. Gramática da coluna "efeito" do opcoes.csv, separada por " & ":
 * SAMU:MOTIVO, UPA:MOTIVO, UPA_MAT:MOTIVO, UBS_MESMO_DIA:MOTIVO, UBS_48H:MOTIVO,
 * UBS_PROX:MOTIVO, CASA:MOTIVO, IR:NO, FLAG:nome, VAR:nome=numero, EXIGE:CODIGO,
 * SINAL:ID (o destino vem do sinal da base de conhecimento, respeitando idade e grupo).
 */
public record Efeito(Acao acao, String valor) {

    public enum Acao { SAMU, UPA, UPA_MAT, UBS_MESMO_DIA, UBS_48H, UBS_PROX, CASA, IR, FLAG, VAR, EXIGE, SINAL }

    public static List<Efeito> lerLista(String texto) {
        List<Efeito> lista = new ArrayList<>();
        if (texto == null || texto.isBlank()) return lista;
        for (String parte : texto.split("&")) {
            String p = parte.trim();
            int dois = p.indexOf(':');
            if (dois <= 0 || dois == p.length() - 1) throw new IllegalArgumentException("efeito inválido: " + p);
            Acao acao = Acao.valueOf(p.substring(0, dois).trim());
            String valor = p.substring(dois + 1).trim();
            if (acao == Acao.VAR && !valor.matches("[A-Za-z]+=-?\\d+"))
                throw new IllegalArgumentException("VAR precisa ser nome=numero: " + p);
            lista.add(new Efeito(acao, valor));
        }
        return lista;
    }

    /** Converte um efeito de destino num disparo. Devolve null se não for destino. */
    public Disparo comoDisparo(String regra, boolean porNaoSei) {
        return switch (acao) {
            case SAMU -> Disparo.de(regra, Destino.SAMU_192, valor, porNaoSei);
            case UPA -> Disparo.de(regra, Destino.UPA_HOJE, valor, porNaoSei);
            case UPA_MAT -> Disparo.de(regra, Destino.UPA_HOJE, valor, porNaoSei).exigindo("MATERNIDADE");
            case UBS_MESMO_DIA -> Disparo.ubs(regra, PrioridadeUbs.MESMO_DIA, valor, porNaoSei);
            case UBS_48H -> Disparo.ubs(regra, PrioridadeUbs.ATE_48H, valor, porNaoSei);
            case UBS_PROX -> Disparo.ubs(regra, PrioridadeUbs.PROXIMOS_DIAS, valor, porNaoSei);
            case CASA -> Disparo.de(regra, Destino.CUIDADO_EM_CASA, valor, porNaoSei);
            default -> null;
        };
    }
}
