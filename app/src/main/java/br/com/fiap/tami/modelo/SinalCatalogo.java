package br.com.fiap.tami.modelo;

import java.util.List;

/** Um sinal de alerta ou condição, como fica no banco (carregado dos arquivos com fonte). */
public record SinalCatalogo(String codigo, String tipo, String popular, String termo, String destino, String valePara,
                            String fonte, String fonteUrl, boolean revisar, List<String> expressoes) {}
