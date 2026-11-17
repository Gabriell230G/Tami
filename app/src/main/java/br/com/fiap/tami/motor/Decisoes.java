package br.com.fiap.tami.motor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Regras que precisam de conta e não cabem num botão: nós FIM, regras transversais (RT)
 * e pisos (M08). Cada disparo leva o código da regra do relatório "Roteiros" para o
 * médico achar a linha certa. RASCUNHO PARA REVISÃO MÉDICA.
 */
public final class Decisoes {

    private static final List<String> SINTOMAS_ARBO =
        List.of("arbo_corpo", "arbo_juntas", "arbo_cabeca", "arbo_olhos", "arbo_manchas", "arbo_enjoo");

    private final Map<String, Function<Sessao, List<Disparo>>> porNo = new LinkedHashMap<>();

    public Decisoes() {
        porNo.put("FAR_FIM", Decisoes::farFim);
        porNo.put("DPE_FIM", s -> List.of(Disparo.de("RT-11", Destino.UPA_HOJE, "DOR_PEITO").exigindo("BLK-EI-PEITO")));
        porNo.put("DCV_04", Decisoes::dcv04);
        porNo.put("DCV_FIM", s -> List.of(Disparo.de("RA-DCV_FIM", Destino.UPA_HOJE, "DESMAIO_RECENTE").exigindo("BLK-EI-DESMAIO")));
        porNo.put("INT_FIM", s -> List.of(Disparo.de("RA-INT_FIM", Destino.UPA_HOJE, "INTOXICACAO")
            .exigindo("DISQUE_INTOX", "BLK-EI-INTOX", "BLK-LEVAR-INTOX")));
        porNo.put("CRI_FIM", Decisoes::criFim);
        porNo.put("FEB_AFIM", Decisoes::febAdultoFim);
        porNo.put("GEN_FIM", Decisoes::genFim);
        porNo.put("LES_FIM", Decisoes::lesFim);
    }

    public Set<String> nosConhecidos() { return porNo.keySet(); }

    public List<Disparo> avaliar(String noId, Sessao s) {
        Function<Sessao, List<Disparo>> f = porNo.get(noId);
        if (f == null) throw new IllegalStateException("nó de decisão sem regra: " + noId);
        return f.apply(s);
    }

    // ---------- nós FIM ----------

    private static List<Disparo> farFim(Sessao s) {
        Perfil p = s.perfil();
        if (p.idoso()) return List.of(Disparo.de("RT-15", Destino.SAMU_192, "FALTA_AR_IDOSO"));
        if (p.gestante()) return List.of(Disparo.de("RA-FAR_FIM", Destino.UPA_HOJE, "FALTA_AR_GESTANTE").exigindo("MATERNIDADE"));
        // [INFERÊNCIA] enquanto o roteiro RES (respiração) não existe, falta de ar vai à UPA
        return List.of(Disparo.de("RA-FAR_FIM", Destino.UPA_HOJE, "FALTA_DE_AR").exigindo("BLK-EI-RESPIRACAO"));
    }

    private static List<Disparo> dcv04(Sessao s) {
        Perfil p = s.perfil();
        if (p.gestante()) return List.of(Disparo.de("RT-07", Destino.SAMU_192, "DESMAIO_OU_CONVULSAO_GESTANTE"));
        if (p.crianca() && s.flag("convulsao"))
            return List.of(Disparo.de("RT-07", Destino.SAMU_192, "CONVULSAO_CRIANCA").exigindo("BLK-EI-CONVULSAO"));
        return List.of();
    }

    private static List<Disparo> criFim(Sessao s) {
        if (horarioDeUbs(s.inicio()))
            return List.of(Disparo.ubs("RA-CRI_FIM", PrioridadeUbs.MESMO_DIA, "CRISE_SEM_PERIGO_AGORA").exigindo("CVV188", "CAPS"));
        return List.of(Disparo.de("RA-CRI_FIM", Destino.UPA_HOJE, "CRISE_FORA_DO_HORARIO").exigindo("CVV188"));
    }

    /** Dia útil das 7h às 19h. [INFERÊNCIA] o horário real de cada UBS muda. */
    static boolean horarioDeUbs(LocalDateTime t) {
        DayOfWeek d = t.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY && t.getHour() >= 7 && t.getHour() < 19;
    }

    /** FEB_AFIM: R1 a R9 em ordem, a primeira que bater vale. R7 (urinário) corre à parte. */
    private static List<Disparo> febAdultoFim(Sessao s) {
        List<Disparo> d = new ArrayList<>();
        int dias = s.var("dias", 1);
        long sintomas = SINTOMAS_ARBO.stream().filter(s::flag).count();
        boolean suspeita = s.flag("contextoArbo") && sintomas >= 2;
        boolean risco = s.flag("grupoRisco") || s.perfil().idoso() || s.perfil().diabetes();
        boolean social = s.flag("riscoSocial");

        if (s.flag("urinario"))   // R7 [INFERÊNCIA] até existir o roteiro URI: febre + urina vai à UPA
            d.add(Disparo.de("FEB_AFIM-R7", Destino.UPA_HOJE, "FEBRE_COM_SINTOMA_URINARIO"));

        Disparo principal;
        if (suspeita && (risco || social))
            principal = Disparo.de("FEB_AFIM-R1", Destino.UPA_HOJE, "SUSPEITA_DENGUE_COM_RISCO").exigindo("BLK-LEVAR-DENGUE");
        else if (suspeita && s.flag("febreBaixou"))
            principal = Disparo.ubs("FEB_AFIM-R2", PrioridadeUbs.MESMO_DIA, "SUSPEITA_DENGUE_FEBRE_BAIXOU").exigindo("BLK-SP-FEB");
        else if (suspeita && dias >= 5)
            principal = Disparo.ubs("FEB_AFIM-R3", PrioridadeUbs.ATE_48H, "SUSPEITA_ARBOVIROSE_5_DIAS");
        else if (suspeita)
            principal = Disparo.ubs("FEB_AFIM-R4", PrioridadeUbs.ATE_48H, "SUSPEITA_DENGUE").exigindo("BLK-HIDRATAR", "BLK-SP-FEB");
        else if (dias > 7)
            principal = Disparo.ubs("FEB_AFIM-R5", PrioridadeUbs.ATE_48H, "FEBRE_MAIS_DE_7_DIAS");
        else if (risco || social || dias >= 3)
            principal = Disparo.ubs("FEB_AFIM-R6", PrioridadeUbs.ATE_48H, "FEBRE_COM_RISCO_OU_3_DIAS");
        else if (s.flag("semFoco"))
            principal = Disparo.ubs("FEB_AFIM-R8", PrioridadeUbs.ATE_48H, "FEBRE_SEM_OUTRO_SINTOMA");
        else if (s.flag("resfriado") && dias <= 2) {
            principal = Disparo.de("FEB_AFIM-R9", Destino.CUIDADO_EM_CASA, "RESFRIADO_SEM_SINAL").exigindo("BLK-CASA-RES", "BLK-SP-FEB");
            if (s.flag("calor")) principal = principal.exigindo("BLK-CALOR");
        } else   // [INFERÊNCIA] nenhuma regra bateu: na dúvida, sobe
            principal = Disparo.ubs("FEB_AFIM-R10", PrioridadeUbs.ATE_48H, "FEBRE");
        d.add(principal);
        return d;
    }

    /** GEN_FIM: o caminho genérico nunca termina em casa. */
    private static List<Disparo> genFim(Sessao s) {
        boolean risco = s.flag("grupoRisco") || s.perfil().riscoNoPerfil();
        if (s.flag("febre") && risco)
            return List.of(Disparo.de("RA-GEN_FIM-1", Destino.UPA_HOJE, "FEBRE_EM_GRUPO_DE_RISCO"));
        if (risco)
            return List.of(Disparo.ubs("RA-GEN_FIM-2", PrioridadeUbs.MESMO_DIA, "GRUPO_DE_RISCO").exigindo("BLK-CASA-GERAL", "BLK-SP-GEN"));
        // D-006: condição estudada de cuidado em casa + tudo leve + ninguém de risco. Passa pela trava M12.
        if (s.condicaoCasa() != null && s.fatores().stream().allMatch(f -> f.equals("homem") || f.equals("mulher")) && !s.flag("febre") && !s.perfil().crianca()
                && !s.perfil().gestante() && !s.perfil().idoso()
                && s.var("dor", 99) <= 3 && s.efetiva("GEN_03").contains("N")
                && (s.condicaoCasa().startsWith("SA-")   // sinal que descreve a evolução normal: o tempo é o do procedimento
                    || s.efetiva("GEN_06").contains("hoje") || s.efetiva("GEN_06").contains("1_7_dias")))
            return List.of(Disparo.de((s.condicaoCasa().startsWith("SA-") ? "SA:" : "CO:") + s.condicaoCasa(), Destino.CUIDADO_EM_CASA, "CUIDADO_EM_CASA_SEM_SINAL")
                .exigindo("BLK-CASA-" + s.condicaoCasa(), "BLK-CASA-GERAL", "BLK-SP-GEN"));
        return List.of(Disparo.ubs("RA-GEN_FIM-3", PrioridadeUbs.PROXIMOS_DIAS, "AVALIACAO_NA_UBS").exigindo("BLK-CASA-GERAL", "BLK-SP-GEN"));
    }

    /**
     * LES_FIM (sprint 4): machucado sem sinal de alerta. Fonte: NHS, Sprains and strains (cuidados em casa nos primeiros
     * dias; UBS se não melhora). Criança, idoso, gestante e quem tem fator de risco vão à UBS com os mesmos cuidados.
     * Se a conversa já está na UPA, só junta o "enquanto vai para lá". RASCUNHO PARA REVISÃO MÉDICA.
     */
    private static List<Disparo> lesFim(Sessao s) {
        if (s.destinoAtual() == Destino.UPA_HOJE)
            return List.of(Disparo.de("LES_FIM-UPA", Destino.UPA_HOJE, "LESAO_COM_SINAL").exigindo("BLK-EI-LESAO"));
        Perfil p = s.perfil();
        Set<String> tempo = s.efetiva("GEN_06");
        boolean velha = tempo.contains("1_4_semanas") || tempo.contains("mais_1_mes");
        List<String> b = new ArrayList<>(List.of("BLK-CASA-LESAO"));
        if (s.flag("esporte")) b.add("BLK-VOLTA-TREINO");
        b.add("BLK-SP-LESAO");
        String[] blocos = b.toArray(String[]::new);
        boolean fator = s.fatores().stream().anyMatch(f -> !f.equals("homem") && !f.equals("mulher"));
        if (tempo.contains("mais_1_mes"))
            return List.of(Disparo.ubs("LES_FIM-1", PrioridadeUbs.PROXIMOS_DIAS, "LESAO_HA_MAIS_DE_1_MES").exigindo(blocos));
        if (velha)   // mais de uma semana e ainda incomodando a ponto de procurar ajuda: vale ser visto
            return List.of(Disparo.ubs("LES_FIM-2", s.flag("semMelhora") ? PrioridadeUbs.ATE_48H : PrioridadeUbs.PROXIMOS_DIAS, "LESAO_HA_MAIS_DE_UMA_SEMANA").exigindo(blocos));
        if (p.idoso() || p.gestante() || fator)
            return List.of(Disparo.ubs("LES_FIM-3", PrioridadeUbs.MESMO_DIA, "LESAO_EM_GRUPO_DE_RISCO").exigindo(blocos));
        if (p.crianca())
            return List.of(Disparo.ubs("LES_FIM-4", PrioridadeUbs.PROXIMOS_DIAS, "LESAO_EM_CRIANCA").exigindo(blocos));
        if (s.var("dor", 0) <= 6)
            return List.of(Disparo.de("LES_FIM-CASA", Destino.CUIDADO_EM_CASA, "LESAO_LEVE_SEM_SINAL").exigindo(blocos));
        return List.of(Disparo.ubs("LES_FIM-5", PrioridadeUbs.PROXIMOS_DIAS, "LESAO").exigindo(blocos));
    }

    // ---------- regras transversais, depois de cada resposta ----------

    public List<Disparo> aposResposta(Sessao s) {
        List<Disparo> d = new ArrayList<>();
        Perfil p = s.perfil();
        if (p.gestante() && s.flag("febre"))
            d.add(Disparo.de("RT-01", Destino.UPA_HOJE, "FEBRE_NA_GESTACAO").exigindo("MATERNIDADE"));
        Set<String> a05 = s.efetiva("FEB_A05");
        if (a05.contains("vomito_toda_hora") && a05.contains("dor_cabeca_forte"))
            d.add(Disparo.de("RT-06", Destino.SAMU_192, "FEBRE_VOMITO_E_DOR_DE_CABECA_FORTE"));
        if (p.idoso() && s.efetiva("RES_07").contains("S"))
            d.add(Disparo.de("RT-15", Destino.SAMU_192, "FALTA_AR_IDOSO"));
        return d;
    }

    // ---------- pisos, no fim (M08) ----------

    public List<Disparo> pisos(Sessao s) {
        List<Disparo> d = new ArrayList<>();
        Perfil p = s.perfil();
        if (s.flag("dorPeito")) d.add(Disparo.de("RT-11", Destino.UPA_HOJE, "DOR_PEITO"));
        if (p.gestante()) d.add(Disparo.ubs("RT-12", PrioridadeUbs.PROXIMOS_DIAS, "ACOMPANHAMENTO_PRE_NATAL"));
        if (p.menorDe3Meses()) d.add(Disparo.ubs("RT-13", PrioridadeUbs.MESMO_DIA, "BEBE_MENOR_DE_3_MESES"));
        return d;
    }
}
