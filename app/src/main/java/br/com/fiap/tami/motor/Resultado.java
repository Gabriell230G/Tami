package br.com.fiap.tami.motor;

import br.com.fiap.tami.motor.lexico.Sinal;

import java.util.List;
import java.util.Set;

/**
 * O que a Tami entrega no fim. Os textos vêm de mensagens fixas revisadas ({@code mensagemId}),
 * nunca gerados na hora.
 */
public record Resultado(Destino destino, PrioridadeUbs prioridadeUbs, List<String> motivos,
                        Set<String> exige, String mensagemId, boolean telaCrise, boolean porNaoSei,
                        List<Disparo> disparos, List<RespostaDada> respostas, List<Sinal> sinaisDoTexto) {}
