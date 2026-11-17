package br.com.fiap.tami.motor;

import br.com.fiap.tami.motor.conhecimento.BaseConhecimento;
import br.com.fiap.tami.motor.lexico.DetectorAlerta;
import br.com.fiap.tami.motor.lexico.GrupoLexico;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

/**
 * O cérebro da Tami: decide a próxima pergunta e o destino. Java puro, sem Spring,
 * para ser testado sozinho. Regras do motor M01 a M12 do relatório "Roteiros".
 *
 * <p>Uso:
 * <pre>
 *   Sessao s = motor.iniciar(perfil, "to com aperto no peito", agora);
 *   Optional&lt;No&gt; no;
 *   while ((no = motor.proxima(s)).isPresent()) motor.responder(s, no.get().id(), List.of("N"));
 *   Resultado r = motor.resultado(s);
 * </pre>
 */
public final class MotorTriagem {

    public static final String NAO_SEI = "nao_sei";
    private static final Set<String> EXCLUSIVAS = Set.of("nenhum", "nenhuma", NAO_SEI);

    /** Grupos do texto que vão direto ao SAMU, com o "enquanto isso" de cada um. */
    private static final Map<GrupoLexico, String> TEXTO_SAMU = new EnumMap<>(Map.of(
        GrupoLexico.AR_GRAVE, "BLK-EI-RESPIRACAO",
        GrupoLexico.AVC, "BLK-EI-AVC",
        GrupoLexico.SANGUE, "BLK-EI-SANGRAMENTO",
        GrupoLexico.TRAUMA, "BLK-EI-TRAUMA",
        GrupoLexico.ALERGIA, "BLK-EI-ALERGIA"));

    private final CatalogoRoteiros catalogo;
    private final Decisoes decisoes;
    private final DetectorAlerta detector;
    private final Predicate<String> regraAprovada;
    private final BaseConhecimento conhecimento;   // null: só o léxico de emergência (comportamento original)

    /**
     * @param regraAprovada diz se uma regra já foi aprovada pelo médico. Regra que manda para casa
     *                      sem aprovação vira UBS (M12). Use {@code r -> false} até a revisão.
     */
    public MotorTriagem(CatalogoRoteiros catalogo, Predicate<String> regraAprovada) {
        this(catalogo, regraAprovada, null);
    }

    /** Com a base de conhecimento estudada: cada sinal de alerta reconhecido no texto vira disparo "SA:id". */
    public MotorTriagem(CatalogoRoteiros catalogo, Predicate<String> regraAprovada, BaseConhecimento conhecimento) {
        this.conhecimento = conhecimento;
        this.catalogo = catalogo;
        this.decisoes = new Decisoes();
        this.detector = new DetectorAlerta(catalogo.lexico());
        this.regraAprovada = regraAprovada;
        List<String> erros = catalogo.validar(decisoes.nosConhecidos());
        if (!erros.isEmpty()) throw new IllegalStateException("Roteiros com erro:\n" + String.join("\n", erros));
    }

    public DetectorAlerta detector() { return detector; }

    public Sessao iniciar(Perfil perfil, String textoLivre, LocalDateTime agora) {
        return iniciar(perfil, textoLivre, null, agora);
    }

    /**
     * M01: primeiro o texto livre, depois o filtro FIL, depois o roteiro da queixa.
     * @param queixaEscolhida "FEB" ou "GEN" quando a pessoa tocou num atalho; null para decidir pelo texto.
     */
    public Sessao iniciar(Perfil perfil, String textoLivre, String queixaEscolhida, LocalDateTime agora) {
        return iniciar(perfil, textoLivre, queixaEscolhida, agora, Set.of());
    }

    /**
     * @param fatores o que a pessoa contou de si no perfil: anticoagulado, quimioterapia, imunossuprimido,
     *                cardiopata, renal... Só a base de conhecimento usa (sobe_um_nivel_se).
     */
    public Sessao iniciar(Perfil perfil, String textoLivre, String queixaEscolhida, LocalDateTime agora, Set<String> fatores) {
        return iniciar(perfil, textoLivre, queixaEscolhida, agora, fatores, null);
    }

    /** @param regiao a região que a pessoa tocou no corpo 3D (nome popular), ou null. Ordena a lista de confirmação. */
    public Sessao iniciar(Perfil perfil, String textoLivre, String queixaEscolhida, LocalDateTime agora, Set<String> fatores, String regiao) {
        Objects.requireNonNull(perfil, "perfil");
        Objects.requireNonNull(agora, "agora");
        if (fatores == null) fatores = Set.of();   // revisão M7
        // sprint: texto muito longo (20 mil caracteres levava 3 s) ➜ guarda o começo e o fim, onde o grave costuma estar
        String inteiro = textoLivre;   // revisão A3: o detector de risco lê o texto inteiro; o corte vale só para a base
        if (textoLivre != null && textoLivre.length() > 4000)
            textoLivre = textoLivre.substring(0, 2000).replaceAll("\\S*$", "") + " . " + textoLivre.substring(textoLivre.length() - 2000).replaceAll("^\\S*", "");
        textoLivre = br.com.fiap.tami.motor.lexico.LeitorNumeros.completar(textoLivre);   // D-024: números de sinais vitais
        textoLivre = br.com.fiap.tami.motor.lexico.DetectorRisco.semFigurado(textoLivre);   // D-031: "morrer de rir" não é crise
        textoLivre = semHipotese(textoLivre);   // sprint 3 (C2): hipótese não é queixa
        DetectorAlerta.Analise a = detector.analisar(textoLivre);
        boolean febre = "FEB".equals(queixaEscolhida) || a.presente(GrupoLexico.FEBRE);
        // Só a trilha de febre do adulto está pronta; criança e gestante usam o genérico, que nunca manda para casa.
        String queixa = febre && !perfil.crianca() && !perfil.gestante() ? "FEB" : "GEN";
        // sprint 4: machucado (pancada, queda, torção, esporte) tem trilha própria; o resto segue igual
        String norm = br.com.fiap.tami.motor.lexico.Normalizador.normalizar(textoLivre == null ? "" : textoLivre);
        boolean semMachucar = SEM_LESAO.matcher(norm).find();
        boolean lesao = conhecimento != null && !febre && !semMachucar && !OPEROU.matcher(norm).find()
            && LESAO.matcher(FALSA_LESAO.matcher(norm).replaceAll(" ")).find();
        String grande = BaseConhecimento.grandeRegiao(regiao);
        boolean membro = grande != null && (grande.equals("bracos") || grande.equals("pernas") || grande.equals("costas"));
        if (lesao && grande != null && (grande.equals("peito") || grande.equals("barriga"))) lesao = false;   // pancada no tronco: o filtro geral pergunta peito e ar
        if (lesao) queixa = "LES";

        Sessao s = new Sessao(perfil, agora, textoLivre, a, queixa);
        s.regiao = regiao;
        s.fatoresInicio = fatores == null ? Set.of() : Set.copyOf(fatores);
        if (lesao) {
            String como = COMO_ESPORTE.matcher(norm).find() ? "esporte" : COMO_TORCAO.matcher(norm).find() ? "torcao"
                : COMO_QUEDA.matcher(norm).find() ? "queda" : COMO_PANCADA.matcher(norm).find() ? "pancada" : null;
            if (como != null) s.doTexto.put("LES_01", como);
        }

        // Texto livre (léxico): pode encerrar já no SAMU ou responder perguntas do filtro.
        for (var e : TEXTO_SAMU.entrySet())   // sprint 3 (C3): "tive um AVC em 2018" é história, não dispara o SAMU sozinho
            if (a.presente(e.getKey()) && !soNoPassado(a, e.getKey(), textoLivre))
                adicionar(s, Disparo.de("LX-" + e.getKey(), Destino.SAMU_192, e.getKey().name()).exigindo(e.getValue()));
        if (a.presente(GrupoLexico.CONVULSAO)) s.flags.add("convulsao");
        if (!a.passado()) {   // N5: se já passou, a pergunta FIL_03 confirma
            if (a.presente(GrupoLexico.CONSCIENCIA))
                adicionar(s, Disparo.de("LX-CONSCIENCIA", Destino.SAMU_192, "CONSCIENCIA").exigindo("BLK-EI-DESMAIO"));
            if (a.presente(GrupoLexico.CONVULSAO))
                adicionar(s, Disparo.de("LX-CONVULSAO", Destino.SAMU_192, "CONVULSAO").exigindo("BLK-EI-CONVULSAO"));
        }
        if (a.presente(GrupoLexico.PEITO) && !soNoPassado(a, GrupoLexico.PEITO, textoLivre)) s.doTexto.put("FIL_02", "S");
        // sprint 2 (C4): sintoma de resfriado escrito no texto responde a FEB_A09
        if ("FEB".equals(queixa) && java.util.regex.Pattern.compile("\\b(tosse|tossindo|coriza|nariz escorrendo|nariz entupido|espirr\\w*|dor de garganta|garganta)\\b")
                .matcher(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(textoLivre)).find())
            s.doTexto.put("FEB_A09", "resfriado");
        if (a.presente(GrupoLexico.VENENO)) {
            if (a.presente(GrupoLexico.PROPOSITO))
                adicionar(s, Disparo.de("RT-09", Destino.SAMU_192, "INTOXICACAO_INTENCIONAL").exigindo("CVV188", "BLK-EI-INTOX"));
            else s.doTexto.put("FIL_07", "S");
        }
        if (a.presente(GrupoLexico.SUICIDIO)) s.doTexto.put("FIL_08", "S");   // negado: FIL_08 pergunta (N7)
        s.fatores = fatores == null ? Set.of() : Set.copyOf(fatores);   // revisão M7
        if (conhecimento != null) {   // sinais e condições estudados em fonte oficial (base de conhecimento)
            for (BaseConhecimento.Achado k : conhecimento.procurar(textoLivre, perfil, fatores)) {
                if (k.negado()) { s.doTexto.put("FIL_08", "S"); continue; }   // D-012: vai para o roteiro CRI, que pergunta
                if (k.destino() == Destino.CUIDADO_EM_CASA) {   // D-006: sinal leve só libera o caminho de casa no GEN_FIM
                    if (s.condicaoCasa == null) s.condicaoCasa = k.sinal().id();
                    continue;
                }
                Disparo d = Disparo.de("SA:" + k.sinal().id(), k.destino(), k.sinal().termoMedico());
                if (k.sinal().naoNegavel()) { d = d.exigindo("CVV188"); s.doTexto.put("FIL_08", "S"); }   // sprint: risco de suicídio pela base sempre mostra o CVV 188
                else if (INTOX_TERMO.matcher(k.sinal().termoMedico().toLowerCase(java.util.Locale.ROOT)).find())
                    d = d.exigindo("DISQUE_INTOX");   // sprint: intoxicação pela base mostra o CIATox 0800 722 6001
                adicionar(s, k.subiu() ? d.exigindo("FATOR_DE_RISCO") : d);
            }
            // D-026: febre em quem tem imunidade baixa (transplante, quimio, imunossupressor) é UPA, mesmo sem outro sinal;
            // a trilha FEB não pergunta isso, então a base decide pelo perfil e pelo texto
            if (febre) {   // sprint: o mesmo vale para gestante (SA-D2-056) e bebê com menos de 3 meses (SA-D2-078)
                List<String> deFebre = new ArrayList<>(List.of("SA-I-016"));
                if (perfil.gestante()) deFebre.add("SA-D2-056");
                if (perfil.idadeMeses() < 3) deFebre.add("SA-D2-078");
                for (String id : deFebre) {
                    BaseConhecimento.Achado k = conhecimento.avaliar(id, textoLivre, perfil, fatores);
                    if (k != null) adicionar(s, Disparo.de("SA:" + id, k.destino(), k.sinal().termoMedico()));
                }
            }
            // D-031: detector de risco por raiz e contexto (crise não pode depender da frase exata)
            br.com.fiap.tami.motor.lexico.DetectorRisco.Resultado risco = br.com.fiap.tami.motor.lexico.DetectorRisco.analisar(inteiro);
            // sprint 3 (pedido P1 da Frente 1): convulsão descrita sem a palavra ("tremi inteira e fiquei fora do ar")
            // gestante: sempre SAMU (SA-D1-086), mesmo contado no passado; outros: SAMU agora, e o passado a FIL_03 confirma (N5)
            if (risco.convulsao()) {
                s.flags.add("convulsao");
                if (perfil.gestante())
                    adicionarSinal(s, "SA-D1-086", Destino.SAMU_192, "convulsão na gestante", textoLivre, perfil, fatores, "RISCO:");
                else if (!a.presente(GrupoLexico.CONVULSAO) && !a.passado())
                    adicionar(s, Disparo.de("LX-CONVULSAO", Destino.SAMU_192, "CONVULSAO").exigindo("BLK-EI-CONVULSAO"));
            }
            if (risco.suicidio()) {
                s.doTexto.put("FIL_08", "S");   // o roteiro de crise pergunta (D-012)
                if (!risco.suicidioNegado()) {
                    BaseConhecimento.Achado k = conhecimento.avaliar("SA-D3-058", textoLivre, perfil, fatores);
                    Destino dk = k != null ? k.destino() : Destino.UPA_HOJE;
                    adicionar(s, Disparo.de("SA:SA-D3-058", dk, "ideação suicida (orientar CVV 188)").exigindo("CVV188"));
                }
            }
            if (risco.intoxicacao() || risco.peconhento())
                adicionar(s, Disparo.de("RISCO-INTOX", Destino.UPA_HOJE, risco.peconhento() ? "ACIDENTE_POR_ANIMAL_PECONHENTO" : "INTOXICACAO").exigindo("DISQUE_INTOX"));
            if (risco.violenciaSexual()) {
                BaseConhecimento.Achado k = conhecimento.avaliar("SA-E-033", textoLivre, perfil, fatores);
                adicionar(s, Disparo.de("SA:SA-E-033", k != null ? k.destino() : Destino.UPA_HOJE, "violência sexual (PEP e cuidados)"));
            }
            if (risco.violenciaFisica()) {
                BaseConhecimento.Achado k = conhecimento.avaliar("SA-A-090", textoLivre, perfil, fatores);
                adicionar(s, Disparo.de("SA:SA-A-090", k != null ? k.destino() : Destino.UPA_HOJE, "violência física"));
            }
            Set<String> meus = conhecimento.eu(textoLivre, perfil, fatores);   // sprint: transplante, quimio, anticoagulante etc. também bloqueiam casa
            boolean recuperacao = RECUPERACAO.matcher(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(textoLivre)).find();
            for (var c : conhecimento.condicoesNoTexto(textoLivre)) {
                // D-019: "operei a apendicite faz 2 dias" ➜ a condição é história; os sinais de alerta continuam valendo
                if (recuperacao && c.destinoPadrao() != Destino.CUIDADO_EM_CASA) continue;
                if (c.destinoPadrao() != Destino.CUIDADO_EM_CASA)   // D-016: o caso típico, não o mínimo
                    adicionar(s, Disparo.de("CO:" + c.id(), c.destinoPadrao(), c.nomePopular()));
                else if (c.casaSegura() && (s.condicaoCasa == null || s.condicaoCasa.startsWith("SA-")) && Collections.disjoint(c.gruposRisco(), meus))
                    s.condicaoCasa = c.id();
            }
        }
        glicose(s, textoLivre, perfil, fatores);   // sprint 3 (item 6): o número da glicemia escrito no relato
        // sprint 4: escolhe a rota no fim, quando já se sabe tudo o que o texto disse
        boolean urgente = s.doTexto.keySet().stream().anyMatch(k -> k.startsWith("FIL_")) || s.flags.contains("convulsao")
            || URGENTES.stream().anyMatch(g -> a.presente(g) && !soNoPassado(a, g, s.textoLivre()));
        if ("LES".equals(s.queixa) && urgente) { s.queixa = "GEN"; s.doTexto.remove("LES_01"); }
        if ("LES".equals(s.queixa)) { rotaLesao(s); return s; }
        if (membro && !semMachucar && !urgente && "GEN".equals(s.queixa) && (s.destinoAtual() == null || s.destinoAtual().compareTo(Destino.UPA_HOJE) < 0)) {
            s.fila.add(ORIGEM);
            return s;
        }
        rotaGeral(s, Modo.GERAL);
        if (a.presente(GrupoLexico.AR)) {   // falta de ar: abre FAR antes do filtro, e o FIL_01 herda do FAR_01
            List<String> far = catalogo.roteiro("FAR");
            for (int i = far.size() - 1; i >= 0; i--) s.fila.addFirst(far.get(i));
        }
        return s;
    }

    // ---------- sprint 4: rota da lesão, pergunta da origem e lista de confirmação ----------

    /** Pergunta "Como isso começou?" quando a pessoa tocou num braço, perna ou costas e o texto não diz. */
    public static final String ORIGEM = "ORI_01";

    /** Grupos do texto que pedem o filtro geral completo, mesmo num machucado. */
    private static final List<GrupoLexico> URGENTES = List.of(GrupoLexico.PEITO, GrupoLexico.AR_GRAVE, GrupoLexico.AR,
        GrupoLexico.CONSCIENCIA, GrupoLexico.CONVULSAO, GrupoLexico.AVC, GrupoLexico.ALERGIA, GrupoLexico.VENENO,
        GrupoLexico.SUICIDIO, GrupoLexico.FEBRE);

    /** Palavras de machucado: pancada, queda, torção, esporte. */
    static final java.util.regex.Pattern LESAO = java.util.regex.Pattern.compile(
        "\\b(torci|torceu|torcao|torcido|torcida|entorse|luxei|luxou|luxacao|luxado|luxada|desloquei|deslocou|deslocad\\w*|"
        + "cai|caiu|queda|tombo|tropecei|tropecou|escorreguei|escorregou|pancada|bati|bateu (o|a|com|em|no|na|de)|batida no|"
        + "golpe|chute|chutaram|soco|socaram|levei um (golpe|chute|soco|tombo)|pisei num prego|pisou num prego|pulou de cabeca|pulei de cabeca|machuquei|machucou|machucad\\w*|lesionei|estiramento|distensao|"
        + "contusao|fratur\\w*|quebrei|quebrou|trinc\\w*|pisei errado|virei o pe|virou o pe|estalou|estralou|"
        + "no treino|treinando|no judo|no jogo|jogando bola|na luta|lutando|no futebol|na academia|correndo)\\b");
    /** Recuperação de cirurgia não é a trilha de machucado (D-019 cuida dela). */
    private static final java.util.regex.Pattern OPEROU = java.util.regex.Pattern.compile(
        "\\b(operei|operou|operad[oa]|cirurgia|artroscopia|pos operatorio|laqueadura|curetagem|plastica|lipo|protese|implante|colocaram (a|o|uma|um))\\b");
    /** "caiu a pressão", "quase caí", "a febre caiu": não é machucado. */
    private static final java.util.regex.Pattern FALSA_LESAO = java.util.regex.Pattern.compile(
        "\\b(quase (cai|caiu|desmaiei)|(pressao|febre|glicose|glicemia|ficha|cabelo|imunidade|saturacao|temperatura) (caiu|cai|caindo)|"
        + "(caiu|cai|caindo) (a|minha|o|meu) (pressao|febre|glicose|glicemia|cabelo|imunidade|saturacao|temperatura)|"
        + "coracao (bateu|batendo)|bateu (forte|acelerado|rapido|uma|um)|correndo (risco|atras)|lesao (de|na) pele|"
        + "caindo (o|os) (cabelo|cabelos|dente)|cai (no|na) (choro|sono))\\b");
    /** "sem ter batido", "não caí", "sem machucar": a pessoa diz que não foi machucado. */
    private static final java.util.regex.Pattern SEM_LESAO = java.util.regex.Pattern.compile(
        "\\b(sem (ter )?(batido|bater|cair|caido|machucar|pancada|torcer|torcido|motivo)|nao (bati|cai|caiu|machuquei|torci|levei pancada)|"
        + "nem (bati|cai|machuquei|torci)|do nada|sozinh[oa]|sem trauma)\\b");
    private static final java.util.regex.Pattern COMO_ESPORTE = java.util.regex.Pattern.compile(
        "\\b(treino|treinando|judo|jiu|jiujitsu|karate|muay|luta|lutando|futebol|bola|jogo|jogando|academia|musculacao|corrida|correndo|"
        + "basquete|volei|handebol|tenis|skate|bike|pedalando|crossfit|esporte|campeonato|aula de)\\b");
    private static final java.util.regex.Pattern COMO_TORCAO = java.util.regex.Pattern.compile(
        "\\b(torci|torceu|torcao|entorse|pisei errado|virei o pe|virou o pe)\\b");
    private static final java.util.regex.Pattern COMO_QUEDA = java.util.regex.Pattern.compile(
        "\\b(cai|caiu|queda|tombo|tropecei|tropecou|escorreguei|escorregou)\\b");
    private static final java.util.regex.Pattern COMO_PANCADA = java.util.regex.Pattern.compile(
        "\\b(pancada|bati|bateu|golpe|chute|soco)\\b");

    /** Sinais da base que já viram item das perguntas LES_02 e LES_03: não se repetem na lista. */
    private static final Set<String> SINAIS_DA_TRILHA = Set.of("SA-C-120", "SA-C-121", "SA-C-122", "SA-C-123", "SA-C-124",
        "SA-C-125", "SA-C-126", "SA-C-127", "SA-C-128", "SA-A-030", "SA-C-027", "SA-C-025");
    /** Sinal de machucado, queda ou esporte (pela descrição). */
    private static final java.util.regex.Pattern SINAL_TRAUMA = java.util.regex.Pattern.compile(
        "\\b(pancada|queda|caiu|cair|caido|tombo|torc\\w*|batida|bateu|bater|bati|acidente|lesao|estalo|chute|machuc\\w*|treino|jogo|"
        + "esporte|puxao|tranco|fratur\\w*|desloc\\w*|mergulhar|atropel\\w*|agredid\\w*|corte|ferida|apoiar|"
        + "pisar|torto|deformad\\w*|machucado|arremess\\w*|salto|giro|estalou|gesso|fisgada|musculo|osso)\\b");
    /** Mecanismo de machucado agora (para responder "Como isso começou?" na validação). */
    private static final java.util.regex.Pattern MECANISMO = java.util.regex.Pattern.compile(
        "\\b(pancada|queda|caiu|cair|caido|tombo|torc\\w*|batida|bateu|bater|bati|acidente|chute|machuc\\w*|estalo|fratur\\w*|desloc\\w*|"
        + "golpe|soco|puxao|tranco|fisgada|atropel\\w*|agredid\\w*|mergulh\\w*|engessad\\w*|gesso)\\b");
    /** Sinal que só vale para um grupo: só entra na lista se a pessoa for do grupo ou falar dele (grupo ➜ palavras no relato). */
    private static final Map<String, java.util.regex.Pattern> QUALIFICADORES = Map.of(
        "cancer|quimio\\w*", java.util.regex.Pattern.compile("\\b(cancer|tumor|quimio\\w*|radio\\w*|oncolog\\w*|metastase)"),
        "colesterol", java.util.regex.Pattern.compile("\\b(colesterol|estatina|sinvastatina|atorvastatina|rosuvastatina)"),
        "gravidez|gravida|gestante|gestacao|parto|amament\\w*", java.util.regex.Pattern.compile("\\b(gravid\\w*|gestant\\w*|gestacao|semanas|parto|pari|bebe (nao )?mex\\w*|pre natal|amament\\w*|puerper\\w*|resguardo|cesarea|cesaria|curetagem|aborto|abortei|perdi o bebe|gestacao|placenta|ultrassom|transvaginal|cesa\\w*|nasceu|tive (o |a |meu |minha )?(bebe|nenem|filh\\w*|fi\\w{2,3})|ganhei (o |a |meu |minha )?(bebe|nenem|filh\\w*))"),
        "transplant\\w*", java.util.regex.Pattern.compile("\\btransplant"),
        "dialise", java.util.regex.Pattern.compile("\\b(dialise|hemodialise|fistula|renal|rim)"),
        "hiv", java.util.regex.Pattern.compile("\\b(hiv|aids|soropositiv\\w*)"),
        "afinar o sangue|anticoagul\\w*", java.util.regex.Pattern.compile("\\b(afinar|afina o sangue|anticoag\\w*|varfarina|marevan|xarelto|rivaroxabana|apixabana|eliquis|clopidogrel|aas)"),
        "insulina|diabet\\w*", java.util.regex.Pattern.compile("\\b(insulina|diabet\\w*|glicose|glicemia|acucar no sangue)"));
    private static final java.util.regex.Pattern SO_IDOSO = java.util.regex.Pattern.compile("^(idoso|idosa|pessoa idosa)\\b");
    private static final java.util.regex.Pattern SO_CRIANCA = java.util.regex.Pattern.compile("^(crianca|bebe|adolescente)\\b");

    enum Modo { LESAO, SEM_LESAO, GERAL }

    /** Este sinal da base fala de machucado, queda ou esporte? (usado também pela validação para responder a ORI_01) */
    public boolean sinalDeTrauma(String id) {
        if (conhecimento == null) return false;
        var s = conhecimento.sinal(id);
        return s != null && MECANISMO.matcher(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(s.descricao())).find();
    }

    private boolean cabeNaLista(BaseConhecimento.Candidato c, Modo modo, String contexto, Perfil perfilAtual, String regiaoAtual, double topo) {
        var sinal = c.sinal();
        if (modo == Modo.LESAO && SINAIS_DA_TRILHA.contains(sinal.id())) return false;
        boolean forte = c.cobertura() >= 0.6;   // o relato descreve este sinal com clareza: entra sempre
        String d = br.com.fiap.tami.motor.lexico.Normalizador.normalizar(sinal.descricao());
        if (!forte && SO_IDOSO.matcher(d).find() && !perfilAtual.idoso()) return false;
        if (!forte)
            for (var q : QUALIFICADORES.entrySet())
                if (java.util.regex.Pattern.compile("\\b(" + q.getKey() + ")\\b").matcher(d).find() && !q.getValue().matcher(contexto).find()) return false;
        if (modo == Modo.GERAL) return true;
        boolean trauma = SINAL_TRAUMA.matcher(d).find();
        if (modo == Modo.LESAO) {
            if (!forte && !conhecimento.daRegiaoTocada(sinal.id(), regiaoAtual)) return false;
            if (!forte && CRONICO.matcher(d).find()) return false;   // machucado de agora: o que é de semanas fica de fora
            return trauma || forte || c.cobertura() >= 0.4;
        }
        return !trauma || forte || !DEPOIS_DE_TRAUMA.matcher(d).find();   // SEM_LESAO: a pessoa disse que começou sozinho
    }

    /** Sinal de problema que vem aos poucos ou se repete (não combina com um machucado de agora). */
    private static final java.util.regex.Pattern CRONICO = java.util.regex.Pattern.compile(
        "\\b(treina muito|em quem treina|que se repetem|(mais de|depois de) (duas|2|tres|3|quatro|4) semanas|veio aos poucos|de vez em quando|"
        + "sempre no exercicio|piora com o tempo|aparecem so no exercicio|desde uma torcao antiga|perda de peso|piora a noite|caroco)\\b");

    /** Sinal que só existe depois de um machucado ("depois de pancada", "depois de queda"). */
    private static final java.util.regex.Pattern DEPOIS_DE_TRAUMA = java.util.regex.Pattern.compile(
        "\\b(depois (de|da|do|que)|apos|durante|no|na) (uma |um |a |o )?(pancada|queda|torc\\w*|batida|bater|lesao|acidente|machuc\\w*|"
        + "caiu|cair|tombo|jogo|treino|esporte|exercicio|mergulhar|chute|tranco|arremess\\w*|giro|salto)|\\bcaiu\\b|\\bbateu\\b|\\bbater\\b|\\bcair\\b|pancada");

    /** Texto do relato com o perfil e os fatores, para saber se a pessoa é de um grupo citado num sinal. */
    private static String contexto(Sessao s) {
        StringBuilder b = new StringBuilder(br.com.fiap.tami.motor.lexico.Normalizador.normalizar(s.textoLivre() == null ? "" : s.textoLivre()));
        Perfil p = s.perfil();
        if (p.gestante()) b.append(" gravida gravidez gestante gestacao parto amament");
        if (p.diabetes()) b.append(" diabet insulina");
        for (String f : s.fatoresInicio) {
            b.append(' ').append(f);
            if (f.startsWith("anticoag")) b.append(" afinar o sangue anticoag");
            if (f.startsWith("quimio") || f.startsWith("imuno")) b.append(" cancer quimio");
            if (f.startsWith("diabet")) b.append(" diabet insulina");
            if (f.startsWith("renal")) b.append(" dialise");
            if (f.startsWith("transpl")) b.append(" transplant");
        }
        return b.toString();
    }

    /** Machucado: como aconteceu, sinais graves, lista da base, sinais de UPA, dor, tempo, evolução. */
    private void rotaLesao(Sessao s) {
        boolean lista = montarLista(s, Modo.LESAO);
        s.fila.add("LES_01");
        s.fila.add("LES_02");
        if (lista) s.fila.add(CONF_SINAIS);
        s.fila.add("LES_03");
        s.fila.add("GEN_02");
        s.fila.add("GEN_06");
        s.fila.add("LES_04");
        s.fila.add("LES_FIM");
    }

    /** O caminho de antes: lista da base, filtro FIL e o roteiro da queixa (FAR antes, se falta de ar). */
    private void rotaGeral(Sessao s, Modo modo) {
        s.fila.addAll(catalogo.roteiro("FIL"));
        s.fila.addAll(catalogo.roteiro(s.queixa));
        if (montarLista(s, modo)) s.fila.addFirst(CONF_SINAIS);
        if (s.analise().presente(GrupoLexico.AR)) {   // falta de ar: abre FAR antes do filtro, e o FIL_01 herda do FAR_01
            List<String> far = catalogo.roteiro("FAR");
            for (int i = far.size() - 1; i >= 0; i--) s.fila.addFirst(far.get(i));
        }
    }

    /** D-015: a pessoa confirma sinais parecidos com o relato. Devolve true se montou a lista. */
    private boolean montarLista(Sessao s, Modo modo) {
        if (conhecimento == null || LISTA_DE_CONFIRMACAO <= 0) return false;
        String textoLivre = s.textoLivre();
        Perfil perfil = s.perfil();
        Set<String> fatores = s.fatoresInicio;
        String regiao = s.regiao;
        String ctx = contexto(s);
        int tamanho = LISTA_DE_CONFIRMACAO;
            Set<String> ja = new HashSet<>();
            for (Disparo d : s.disparos) if (d.regra().startsWith("SA:")) ja.add(d.regra().substring(3));
            List<BaseConhecimento.Candidato> todos = conhecimento.candidatosComNota(textoLivre, perfil, fatores, ja, 6 * tamanho, regiao);
            double topo = todos.isEmpty() ? 0 : todos.get(0).nota();
            List<BaseConhecimento.Candidato> comNota = todos.stream().filter(c -> cabeNaLista(c, modo, ctx, perfil, regiao, topo)).limit(2 * tamanho).toList();
            List<br.com.fiap.tami.motor.conhecimento.SinalAlerta> cand = new ArrayList<>();
            // sprint 3 (item 7): o sinal grave que o relato descreve com boa confiança vem já marcado, no topo da lista
            for (var c : preMarcar(comNota, textoLivre, perfil, fatores)) { cand.add(c); s.flags.add(PRE + c.id()); }
            for (var c : comNota) if (!cand.contains(c.sinal())) cand.add(c.sinal());
            // sprint 2 (C1): até 3 itens de casa a mais, no fim da 2ª página; a lista cresce e nenhum sinal grave sai dela
            if (!cand.isEmpty()) {
                cand = new ArrayList<>(cand);
                int extra = 0;
                for (var cn : conhecimento.candidatosComNota(textoLivre, perfil, fatores, ja, 60, regiao)) {
                    var c = cn.sinal();
                    if (cabeNaLista(cn, modo, ctx, perfil, regiao, topo) && c.destino() == Destino.CUIDADO_EM_CASA && !cand.contains(c) && extra < 3) { cand.add(c); extra++; }
                }
            }
            if (cand.size() > tamanho)   // 2ª página, só se a pessoa marcar "nenhum" na 1ª, só se a pessoa marcar "nenhum" na 1ª
                s.dinamicos.put(CONF_SINAIS_2, listaDeConfirmacao(CONF_SINAIS_2,
                    cand.subList(tamanho, cand.size()), "Nenhum destes também? Veja mais alguns:", s));
            if (!cand.isEmpty()) {
                s.dinamicos.put(CONF_SINAIS, listaDeConfirmacao(CONF_SINAIS,
                    cand.subList(0, Math.min(tamanho, cand.size())),
                    modo == Modo.LESAO ? "Algum destes combina com o que aconteceu?" : "Algum destes está acontecendo?", s));
            }
        return s.dinamicos.containsKey(CONF_SINAIS);
    }

    /** A próxima pergunta, ou vazio quando a conversa acabou. Chamar de novo devolve a mesma pergunta. */
    /** Relato de procedimento ou tratamento já feito (D-019). */
    private static final java.util.regex.Pattern RECUPERACAO = java.util.regex.Pattern.compile(
        "\\b(operei|operou|operada|operado|opere[i]?|cirurgia|tirei o|tirei a|tirou o|tirou a|fiz a cirurgia|engessad[oa]|gesso|tipoia|tala|"
        + "protese|pino|placa no|parafuso|pos operatorio|depois de operar|depois que operei|reduziram|colocaram no lugar)\\b");

    /** Sprint 3 (C2): "achei que era infarto", "medo de ser apendicite": hipótese não é queixa. */
    public static boolean HIPOTESE_LIGADA = true;
    private static final java.util.regex.Pattern HIPOTESE = java.util.regex.Pattern.compile(
        "(?iu)\\b((achei|achando|imaginei)\\s+(que|q)\\s+(era|fosse|tinha)|fiquei com medo de ser|com medo de ser|medo de ser|medo que seja|medo que fosse|li que)\\b");

    static String semHipotese(String texto) {
        return HIPOTESE_LIGADA && texto != null ? HIPOTESE.matcher(texto).replaceAll(" nao ") : texto;
    }

    /** Sprint 3 (C3): marcas fortes de passado no trecho. */
    public static boolean PASSADO_LIGADO = true;
    private static final java.util.regex.Pattern PASSADO_FORTE = java.util.regex.Pattern.compile(
        "\\b(mes passado|ano passado|anos atras|meses atras|faz anos|ha anos|(faz|ha) (\\d+|uns|umas|dois|duas|tres|quatro|cinco|seis) (anos|meses)|"
        + "em (19|20)\\d\\d|em (janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro))\\b");
    /** No mesmo trecho, o que diz que é de agora vence o passado ("tive AVC em 2018 e agora a boca entortou"). */
    private static final java.util.regex.Pattern AGORA = java.util.regex.Pattern.compile(
        "\\b(agora|agr|hoje|hj|ontem|derrepente|repente|agorinha|atualmente|comecou|voltou|de novo|denovo|outra vez|desde|ainda|ta|to|estou|esta)\\b");

    /** O termo do grupo só aparece em trechos com passado forte e sem marca de agora. */
    static boolean soNoPassado(DetectorAlerta.Analise a, GrupoLexico g, String texto) {
        if (!PASSADO_LIGADO) return false;
        List<String[]> trechos = br.com.fiap.tami.motor.lexico.Normalizador.trechos(texto);
        boolean algum = false;
        for (var si : a.sinais()) {
            if (si.grupo() != g || si.negado()) continue;
            algum = true;
            boolean noPassado = false;
            for (String[] t : trechos) {
                String frase = " " + String.join(" ", t) + " ";
                if (!frase.contains(" " + si.trecho() + " ")) continue;
                if (PASSADO_FORTE.matcher(frase).find() && !AGORA.matcher(frase).find()) noPassado = true;
                else return false;   // o termo aparece num trecho que é de agora
            }
            if (!noPassado) return false;
        }
        return algum;
    }

    // ---------- sprint 3, item 6: glicemia escrita no relato ----------

    /** Sinal novo da Frente 4: glicose muito alta com sintomas (UPA). */
    static final String SINAL_GLICOSE_ALTA = "SA-D1-102";
    private static final String NUM_CENTENA = "(cem|cento|duzentos|trezentos|quatrocentos|quinhentos|seiscentos|setecentos|oitocentos|novecentos|"
        + "duzentas|trezentas|quatrocentas|quinhentas|seiscentas|quatro centos|tres centos|cinco centos|seis centos)";
    private static final String NUM_DEZENA = "(dez|vinte|trinta|quarenta|cinquenta|sessenta|setenta|oitenta|noventa)";
    private static final String NUM_UNIDADE = "(um|uma|dois|duas|tres|quatro|cinco|seis|sete|oito|nove)";
    private static final java.util.regex.Pattern NUM_EXTENSO = java.util.regex.Pattern.compile(
        "\\b" + NUM_CENTENA + "(?: e " + NUM_DEZENA + ")?(?: e " + NUM_UNIDADE + ")?\\b|\\b" + NUM_DEZENA + "(?: e " + NUM_UNIDADE + ")?\\b");
    /** Palavra da glicose, até 6 palavras antes do número, ou "de glicose / mg dl" logo depois. */
    private static final java.util.regex.Pattern GLIC_ANTES = java.util.regex.Pattern.compile(
        "\\b(glicose|glicemia|glicemi\\w*|glicoze|dextro|hgt|acucar|asucar|glicosimetro|aparelh\\w*|ponta de dedo|diabete\\w*|diabetic\\w*|diabetes)\\b"
        + "((?: \\w+){0,6}?) (\\d{2,3}|hi|high)\\b(?! ?(dias?|anos?|meses|mes|semanas?|horas?|hrs?|h|min|minutos?|reais|kg|quilos?|vezes|unidades?|ui|u|mg(?! dl)|ml|comprimidos?|anos de|por|x)\\b)");
    private static final java.util.regex.Pattern GLIC_DEPOIS = java.util.regex.Pattern.compile(
        "\\b(\\d{2,3}) (de glicose|de glicemia|de acucar|mg dl|mgdl)\\b");
    /** Entre a palavra da glicose e o número não pode haver outra medida. */
    private static final java.util.regex.Pattern OUTRA_MEDIDA = java.util.regex.Pattern.compile(
        "\\b(pressao|febre|temperatura|graus|grau|batimento\\w*|saturacao|oxigenio|peso|insulina|unidades|metformina|comprimido\\w*|anos|idade)\\b");
    private static final java.util.regex.Pattern MEDIU = java.util.regex.Pattern.compile(
        "\\b(deu|mediu|medi|medimos|marcou|marcando|bateu|ta em|esta em|tava em|estava em|subiu|subiu pra|chegou)\\b");
    private static final java.util.regex.Pattern NEG = java.util.regex.Pattern.compile("\\b(nao|n|sem|nem|nenhum|nenhuma|nunca)\\b");
    private static final java.util.regex.Pattern SINTOMA_ALTA = java.util.regex.Pattern.compile(
        "\\b(sede|sedenta|sedento|boca seca|xixi toda hora|xixi o tempo todo|muito xixi|xixi demais|mijando|mijano|mijo toda hora|urinando|urinano|urina muito|urinar muito|"
        + "fraco|fraca|fraqueza|sem forca|mole|molinho|molinha|vomit\\w*|vomt\\w*|bomit\\w*|gomit\\w*|enjo\\w*|nausea\\w*|confus\\w*|confuso|desorientad\\w*|"
        + "estranh\\w*|sonolent\\w*|grogue|falando coisa sem sentido|nao fala coisa com coisa)\\b");
    private static final java.util.regex.Pattern SINTOMA_CONSCIENCIA = java.util.regex.Pattern.compile(
        "\\b(confus\\w*|desorientad\\w*|estranh\\w*|sonolent\\w*|grogue|nao acorda|desmai\\w*|apagou|apaguei|convuls\\w*|"
        + "fala enrolada|falando enrolado|enrolando a lingua|nao responde|falando coisa sem sentido|nao fala coisa com coisa|dormindo demais|muito sonolent\\w*)\\b");
    private static final java.util.regex.Pattern SINTOMA_CETO = java.util.regex.Pattern.compile(
        "\\b(respira\\w* (rapid\\w*|funda|fundo|ofegante|acelerad\\w*)|respiracao (rapida|funda|acelerada)|ofegante|falta de ar|dor (na|de) barriga|hálito|halito|cheiro de fruta)\\b");
    private static final java.util.regex.Pattern SINTOMA_BAIXA = java.util.regex.Pattern.compile(
        "\\b(trem\\w*|suand\\w*|suor\\w*|sudorese|fome|tont\\w*|fraco|fraca|fraqueza|mole|palpita\\w*|coracao acelerado|tremedeira|"
        + "confus\\w*|desorientad\\w*|estranh\\w*|sonolent\\w*|grogue|nao acorda|desmai\\w*|apagou|convuls\\w*|fala enrolada|nao responde|vista embac\\w*|visao embac\\w*)\\b");

    /** Valor da glicemia citada (o maior, na dúvida o mais urgente), ou -1. "HI" do aparelho vale 600. */
    static int glicemiaNoTexto(String texto) {
        String n = br.com.fiap.tami.motor.lexico.Normalizador.normalizar(texto);
        n = porExtenso(n);
        int maior = -1, menor = Integer.MAX_VALUE;
        var m = GLIC_ANTES.matcher(n);
        while (m.find()) {
            boolean soDiabetes = m.group(1).startsWith("diabet");   // "diabética, 55" é idade: com "diabetes" precisa do verbo da medida
            if (OUTRA_MEDIDA.matcher(m.group(2)).find() || (soDiabetes && !MEDIU.matcher(m.group(2)).find())) {
                m.region(m.start(3), n.length());
                continue;
            }
            int v = m.group(3).startsWith("h") ? 600 : Integer.parseInt(m.group(3));
            if (v >= 20) { maior = Math.max(maior, v); menor = Math.min(menor, v); }
            m.region(m.end(3), n.length());
        }
        var d = GLIC_DEPOIS.matcher(n);
        while (d.find()) {
            int v = Integer.parseInt(d.group(1));
            if (v >= 20) { maior = Math.max(maior, v); menor = Math.min(menor, v); }
        }
        if (maior < 0) return -1;
        return maior >= 300 ? maior : menor;   // alta vence; se nenhuma é alta, a mais baixa (hipoglicemia)
    }

    /** "quatrocentos e cinquenta" ➜ "450" (só para a glicemia; a Frente 3 cuida do resto). */
    static String porExtenso(String n) {
        var m = NUM_EXTENSO.matcher(n);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int v = 0;
            if (m.group(1) != null) {
                v = centena(m.group(1));
                if (m.group(2) != null) v += dezena(m.group(2));
                if (m.group(3) != null) v += unidade(m.group(3));
            } else {
                v = dezena(m.group(4));
                if (m.group(5) != null) v += unidade(m.group(5));
            }
            m.appendReplacement(sb, String.valueOf(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static int centena(String w) {
        w = w.replace(" ", "");
        if (w.equals("cem") || w.equals("cento")) return 100;
        if (w.startsWith("duzent")) return 200;
        if (w.startsWith("trezent") || w.startsWith("trescent")) return 300;
        if (w.startsWith("quatrocent")) return 400;
        if (w.startsWith("quinhent") || w.startsWith("cincocent")) return 500;
        if (w.startsWith("seiscent")) return 600;
        if (w.startsWith("setecent")) return 700;
        if (w.startsWith("oitocent")) return 800;
        return 900;
    }

    private static int dezena(String w) {
        return switch (w) { case "dez" -> 10; case "vinte" -> 20; case "trinta" -> 30; case "quarenta" -> 40; case "cinquenta" -> 50;
            case "sessenta" -> 60; case "setenta" -> 70; case "oitenta" -> 80; default -> 90; };
    }

    private static int unidade(String w) {
        return switch (w) { case "um", "uma" -> 1; case "dois", "duas" -> 2; case "tres" -> 3; case "quatro" -> 4; case "cinco" -> 5;
            case "seis" -> 6; case "sete" -> 7; case "oito" -> 8; default -> 9; };
    }

    /** O padrão aparece no texto sem "não/sem/nem" nas 3 palavras antes. */
    static boolean afirmado(java.util.regex.Pattern p, String n) {
        var m = p.matcher(n);
        while (m.find()) {
            String antes = n.substring(0, m.start()).trim();
            String[] w = antes.isEmpty() ? new String[0] : antes.split(" ");
            boolean neg = false;
            for (int i = Math.max(0, w.length - 3); i < w.length; i++) neg |= NEG.matcher(w[i]).matches();
            if (!neg) return true;
        }
        return false;
    }

    /**
     * Sprint 3 (item 6). Glicemia >= 300 com sede, xixi toda hora, fraqueza, vômito ou confusão ➜ SA-D1-102 (UPA);
     * com confusão ou sonolência ➜ também o SAMU (SA-D1-094); com vômito e respiração rápida ou dor na barriga ➜ SA-D3-044.
     * Glicemia >= 300 sozinha ➜ UBS no mesmo dia, no mínimo. Glicemia <= 60 com sintoma ➜ o sinal de hipoglicemia da base.
     */
    private void glicose(Sessao s, String texto, Perfil perfil, Set<String> fatores) {
        int g = glicemiaNoTexto(texto);
        if (g < 0) return;
        String n = br.com.fiap.tami.motor.lexico.Normalizador.normalizar(texto);
        if (g >= 300) {
            if (afirmado(SINTOMA_ALTA, n)) {
                adicionarSinal(s, SINAL_GLICOSE_ALTA, Destino.UPA_HOJE, "glicose muito alta com sintomas", texto, perfil, fatores);
                if (afirmado(SINTOMA_CONSCIENCIA, n))
                    adicionarSinal(s, "SA-D1-094", Destino.SAMU_192, "glicose muito alta com confusão", texto, perfil, fatores);
                if (afirmado(java.util.regex.Pattern.compile("\\b(vomit\\w*|vomt\\w*|bomit\\w*|gomit\\w*)\\b"), n) && afirmado(SINTOMA_CETO, n))
                    adicionarSinal(s, "SA-D3-044", Destino.SAMU_192, "glicose muito alta com vômito e respiração rápida", texto, perfil, fatores);
            } else adicionar(s, Disparo.ubs("GLIC-300", PrioridadeUbs.MESMO_DIA, "glicose muito alta (300 ou mais)"));
        } else if (g <= 60 && afirmado(SINTOMA_BAIXA, n)) {
            if (afirmado(SINTOMA_CONSCIENCIA, n)) adicionarSinal(s, "SA-I-011", Destino.SAMU_192, "glicose baixa com confusão", texto, perfil, fatores);
            else adicionarSinal(s, "SA-D3-041", Destino.CUIDADO_EM_CASA, "glicose baixa com sintomas", texto, perfil, fatores);
        }
    }

    /**
     * Dispara o sinal da base; se ele não existe ou não vale para a pessoa, dispara a regra "GLIC:" com o destino
     * mínimo (nunca mais leve que o mínimo). Sinal de casa só preenche a condição de casa (D-006).
     */
    private void adicionarSinal(Sessao s, String id, Destino minimo, String motivo, String texto, Perfil perfil, Set<String> fatores) {
        adicionarSinal(s, id, minimo, motivo, texto, perfil, fatores, "GLIC:");
    }

    private void adicionarSinal(Sessao s, String id, Destino minimo, String motivo, String texto, Perfil perfil, Set<String> fatores, String prefixo) {
        BaseConhecimento.Achado k = conhecimento == null ? null : conhecimento.avaliar(id, texto, perfil, fatores);
        if (minimo == Destino.CUIDADO_EM_CASA) {
            if (k != null && k.destino() == Destino.CUIDADO_EM_CASA) { if (s.condicaoCasa == null) s.condicaoCasa = id; return; }
            if (k != null) adicionar(s, Disparo.de("SA:" + id, k.destino(), k.sinal().termoMedico()));
            else adicionar(s, Disparo.de(prefixo + id, Destino.UPA_HOJE, motivo));   // bebê ou sinal ausente: na dúvida, UPA
            return;
        }
        if (k != null && k.destino().compareTo(minimo) >= 0)
            adicionar(s, Disparo.de("SA:" + id, k.destino(), k.sinal().termoMedico()));
        else adicionar(s, Disparo.de(prefixo + id, minimo, motivo));   // fallback registrado: o sinal não existe ou não vale para a pessoa
    }

    /** Pergunta da lista de confirmação (D-015). */
    /** revisão M6: termo médico de intoxicação ou bicho peçonhento (sem "ingest", que pegava "engoliu moeda"). */
    private static final java.util.regex.Pattern INTOX_TERMO = java.util.regex.Pattern.compile(
        "intoxica|envenena|superdosagem|monoxido|monóxido|peçonh|peconh|escorpi|cobra|serpente|aranha|cáustic|caustic|lonomia|lagarta|abelha|vespa");
    public static final String CONF_SINAIS = "CONF_SINAIS";
    /** Quantos sinais parecidos mostrar (0 desliga a lista). */
    public static int LISTA_DE_CONFIRMACAO = 8;

    public static final String CONF_SINAIS_2 = "CONF_SINAIS_2";

    private static No listaDeConfirmacao(String id, List<br.com.fiap.tami.motor.conhecimento.SinalAlerta> cand, String pergunta, Sessao s) {
        List<Opcao> ops = new ArrayList<>();
        boolean algumMarcado = false;
        for (var c : cand) {
            String d = c.descricao();
            ops.add(new Opcao(c.id(), d.isEmpty() ? d : Character.toUpperCase(d.charAt(0)) + d.substring(1), 1, List.of()));
            algumMarcado |= s.flags.contains(PRE + c.id());
        }
        ops.add(new Opcao("nenhum", "Nenhum destes", 0, List.of()));
        return new No(id, "CONF", 0, No.Tipo.MULTI, false, true, null, null, pergunta,
            algumMarcado ? "Já deixei marcado o que entendi do que você contou. Se não estiver acontecendo, desmarque. "
                + "Marque também os outros que valem para você agora."
                : "Marque todos que valem para você agora.", ops);
    }

    // ---------- sprint 3, item 7: lista com o sinal grave já marcado ----------

    /** Pergunta curta que aparece quando a pessoa desmarca um sinal de SAMU que a Tami tinha marcado. */
    public static final String CONF_DESMARCOU = "CONF_DESMARCOU";
    private static final String PRE = "pre:";
    /** No máximo quantos sinais vêm já marcados (só SAMU ou UPA). */
    public static int PRE_MARCADOS_MAX = 1;
    /** Só olha os primeiros da lista: com 1, só o item do topo (o mais parecido com o relato) pode vir marcado. Ver sprint3_decisoes_F2.md. */
    public static int PRE_MARCADOS_POSICAO = 1;
    /** Parte mínima de uma expressão do sinal achada num mesmo trecho do relato, sem negação (0 a 1). */
    public static double PRE_MARCADOS_COBERTURA = 1.0;
    /** Peso mínimo (soma do idf) da expressão achada: "pressão alta" sozinha é comum demais para marcar. */
    public static double PRE_MARCADOS_PESO = 8.0;
    /** Desmarcar um sinal de UPA também pede a confirmação (o pedido do sprint é só para o SAMU). */
    public static boolean CONFIRMAR_DESMARCAR_UPA = false;

    /** Os ids que a Tami deixou marcados nesta pergunta (CONF_SINAIS ou CONF_SINAIS_2), na ordem da tela. */
    public List<String> preMarcados(Sessao s, String noId) {
        No n = s.dinamicos.get(noId);
        if (n == null || !noId.startsWith(CONF_SINAIS)) return List.of();
        return n.opcoes().stream().map(Opcao::codigo).filter(c -> s.flags.contains(PRE + c)).toList();
    }

    private List<br.com.fiap.tami.motor.conhecimento.SinalAlerta> preMarcar(List<BaseConhecimento.Candidato> comNota,
            String texto, Perfil perfil, Set<String> fatores) {
        List<br.com.fiap.tami.motor.conhecimento.SinalAlerta> out = new ArrayList<>();
        for (int i = 0; i < comNota.size() && i < PRE_MARCADOS_POSICAO && out.size() < PRE_MARCADOS_MAX; i++) {
            var c = comNota.get(i);
            if (c.cobertura() < PRE_MARCADOS_COBERTURA || c.pesoDaExpressao() < PRE_MARCADOS_PESO) continue;
            var k = conhecimento.avaliar(c.sinal().id(), texto, perfil, fatores);
            if (k == null || k.destino().compareTo(Destino.UPA_HOJE) < 0) continue;
            out.add(c.sinal());
        }
        return out;
    }

    /** Monta a confirmação para os sinais graves que a pessoa desmarcou (null se nenhum pede confirmação). */
    private No confirmacaoDeDesmarcar(Sessao s, String noId, List<String> marcados) {
        List<Opcao> ops = new ArrayList<>();
        for (String id : preMarcados(s, noId)) {
            if (marcados.contains(id)) continue;
            var k = conhecimento.avaliar(id, s.textoLivre(), s.perfil(), s.fatores);
            if (k == null) continue;
            if (k.destino() == Destino.SAMU_192 || (CONFIRMAR_DESMARCAR_UPA && k.destino() == Destino.UPA_HOJE))
                ops.add(new Opcao(id, k.sinal().descricao(), 1, List.of()));
        }
        if (ops.isEmpty()) return null;
        ops.add(new Opcao("nenhum", ops.size() == 1 ? "Não está acontecendo" : "Nenhum destes está acontecendo", 0, List.of()));
        ops.add(new Opcao(NAO_SEI, "Não sei", 0, List.of()));
        return new No(CONF_DESMARCOU, "CONF", 0, No.Tipo.MULTI, true, true, null, null,
            "Você tirou a marca. Só para ter certeza: isto está acontecendo agora?",
            "Pelo que você escreveu, parecia que sim. Se estiver acontecendo, mesmo que pouco, marque.", ops);
    }

    private No no(Sessao s, String id) {
        No d = s.dinamicos.get(id);
        return d != null ? d : catalogo.no(id);
    }

    public Optional<No> proxima(Sessao s) {
        if (s.pendente != null) return Optional.of(no(s, s.pendente));
        while (true) {
            if (s.destinoAtual() == Destino.SAMU_192) {   // M05: SAMU encerra na hora
                s.fila.clear();
                s.encerrada = true;
                return Optional.empty();
            }
            String id = s.fila.pollFirst();
            if (id == null) { s.encerrada = true; return Optional.empty(); }
            No no = no(s, id);
            if (no.tipo() == No.Tipo.DECISAO) {
                decisoes.avaliar(id, s).forEach(d -> adicionar(s, d));
                continue;
            }
            if (s.respostas.containsKey(id) || !Condicoes.mostrar(no.mostrarSe(), s)) continue;
            // M05: depois de um UPA_HOJE, só seguem as perguntas que ainda podem dar SAMU
            if (s.destinoAtual() == Destino.UPA_HOJE && !no.podeSamu()) continue;
            String doTexto = s.doTexto.get(id);
            if (doTexto != null) { aplicar(s, no, List.of(doTexto), RespostaDada.Origem.TEXTO); continue; }
            List<String> herdada = no.herdaDe() == null ? null : s.respostas.get(no.herdaDe());
            if (herdada != null && herdada.stream().allMatch(c -> no.opcao(c).isPresent())) {
                aplicar(s, no, herdada, RespostaDada.Origem.HERDADA);   // M07
                continue;
            }
            s.pendente = id;
            return Optional.of(no);
        }
    }

    public void responder(Sessao s, String noId, Collection<String> codigos) {
        if (s.pendente == null || !s.pendente.equals(noId))
            throw new IllegalStateException("a pergunta da vez é " + s.pendente + ", não " + noId);
        No no = no(s, noId);
        List<String> lista = List.copyOf(new LinkedHashSet<>(codigos));
        if (lista.isEmpty()) throw new IllegalArgumentException("escolha pelo menos uma opção");
        if (no.tipo() == No.Tipo.UNICA && lista.size() != 1)
            throw new IllegalArgumentException(noId + " aceita só uma opção");
        if (lista.size() > 1 && lista.stream().anyMatch(EXCLUSIVAS::contains))
            throw new IllegalArgumentException("'Nenhum' e 'Não sei' não podem vir junto com outras opções");
        for (String c : lista)
            if (no.opcao(c).isEmpty()) throw new IllegalArgumentException(noId + " não tem a opção " + c);
        s.pendente = null;
        aplicar(s, no, lista, RespostaDada.Origem.USUARIO);
        if (noId.equals(ORIGEM)) {   // sprint 4: só agora se sabe se foi machucado
            if (s.flag("lesao")) { s.queixa = "LES"; rotaLesao(s); }
            else rotaGeral(s, s.respostas.get(ORIGEM).contains("sozinho") ? Modo.SEM_LESAO : Modo.GERAL);
            return;
        }
        if (noId.equals(CONF_SINAIS) && lista.equals(List.of("nenhum")) && s.dinamicos.containsKey(CONF_SINAIS_2))
            s.fila.addFirst(CONF_SINAIS_2);
        if (noId.startsWith(CONF_SINAIS) && conhecimento != null) {   // sprint 3 (item 7): desmarcou um SAMU ➜ confirma
            No conf = confirmacaoDeDesmarcar(s, noId, lista);
            if (conf != null) {
                s.dinamicos.put(CONF_DESMARCOU, conf);
                s.respostas.remove(CONF_DESMARCOU);
                s.fila.addFirst(CONF_DESMARCOU);
            }
        }
        List<String> confirmados = lista;
        if (noId.equals(CONF_DESMARCOU) && lista.contains(NAO_SEI))   // na dúvida, o mais urgente: vale para todos da pergunta
            confirmados = no.opcoes().stream().map(Opcao::codigo).toList();
        if ((noId.startsWith(CONF_SINAIS) || noId.equals(CONF_DESMARCOU)) && conhecimento != null)   // cada sinal confirmado vira disparo
            for (String c : confirmados) {
                var k = conhecimento.avaliar(c, s.textoLivre(), s.perfil(), s.fatores);
                if (k == null) continue;
                if (k.destino() == Destino.CUIDADO_EM_CASA) { if (s.condicaoCasa == null) s.condicaoCasa = c; continue; }
                Disparo d = Disparo.de((c.startsWith("CO-") ? "CO:" : "SA:") + c, k.destino(), k.sinal().termoMedico());
                adicionar(s, k.subiu() ? d.exigindo("FATOR_DE_RISCO") : d);
            }
    }

    private void aplicar(Sessao s, No no, List<String> codigos, RespostaDada.Origem origem) {
        List<Opcao> escolhidas = codigos.stream().map(c -> no.opcao(c).orElseThrow()).toList();
        boolean porNaoSei = false;
        List<Opcao> efetivas = new ArrayList<>();
        for (Opcao o : escolhidas) {
            if (o.codigo().equals(NAO_SEI) && no.alarme()) {
                porNaoSei = true;
                // M02/M03: "Não sei" vale a resposta positiva mais leve, a menos que a planilha diga outra coisa
                efetivas.add(o.efeitos().isEmpty() ? positivaMaisLeve(no) : o);
            } else efetivas.add(o);
        }
        s.respostas.put(no.id(), codigos);
        Set<String> cod = new LinkedHashSet<>();
        efetivas.forEach(o -> cod.add(o.codigo()));
        s.efetivas.put(no.id(), cod);
        s.historico.add(new RespostaDada(no.id(), no.pergunta(), codigos,
            escolhidas.stream().map(Opcao::rotulo).toList(), origem, porNaoSei));

        List<String> abrir = new ArrayList<>();
        for (Opcao o : efetivas)
            for (Efeito e : o.efeitos()) {
                switch (e.acao()) {
                    case FLAG -> s.flags.add(e.valor());
                    case VAR -> {
                        String[] kv = e.valor().split("=");
                        s.vars.put(kv[0], Integer.parseInt(kv[1]));
                    }
                    case EXIGE -> s.exige.add(e.valor());
                    case IR -> abrir.add(e.valor());
                    case SINAL -> {   // sprint 4: o item da pergunta aponta para um sinal da base; o destino respeita idade e grupo
                        var k = conhecimento == null ? null : conhecimento.avaliar(e.valor(), s.textoLivre(), s.perfil(), s.fatores);
                        if (k != null) adicionar(s, Disparo.de("SA:" + e.valor(), k.destino(), k.sinal().termoMedico(), porNaoSei));
                        else adicionar(s, Disparo.de("RA-" + no.id(), o.gravidade() >= 3 ? Destino.SAMU_192 : Destino.UPA_HOJE, e.valor(), porNaoSei));
                    }
                    default -> adicionar(s, e.comoDisparo("RA-" + no.id(), porNaoSei));
                }
            }
        for (int i = abrir.size() - 1; i >= 0; i--) {
            List<String> sub = catalogo.aPartirDe(abrir.get(i));
            for (int j = sub.size() - 1; j >= 0; j--) s.fila.addFirst(sub.get(j));
        }
        decisoes.aposResposta(s).forEach(d -> adicionar(s, d));
    }

    private static Opcao positivaMaisLeve(No no) {
        return no.opcoes().stream().filter(Opcao::positiva)
            .min(Comparator.comparingInt(Opcao::gravidade)).orElseThrow();
    }

    private static void adicionar(Sessao s, Disparo d) {
        boolean repetido = s.disparos.stream().anyMatch(x -> x.regra().equals(d.regra())
            && x.destino() == d.destino() && Objects.equals(x.motivo(), d.motivo()));
        if (!repetido) s.disparos.add(d);
    }

    /** Fecha a conversa: aplica pisos (M08), a trava de revisão (M12) e escolhe o mais grave (M06). */
    public Resultado resultado(Sessao s) {
        if (proxima(s).isPresent()) throw new IllegalStateException("a conversa ainda tem pergunta: " + s.pendente);
        List<Disparo> todos = new ArrayList<>(s.disparos);
        todos.addAll(decisoes.pisos(s));

        List<Disparo> finais = new ArrayList<>();
        for (Disparo d : todos) {
            if (d.destino() == Destino.CUIDADO_EM_CASA && !regraAprovada.test(d.regra()))
                finais.add(Disparo.ubs("M12", PrioridadeUbs.PROXIMOS_DIAS, "REGRA_EM_REVISAO:" + d.regra()));
            else finais.add(d);
        }
        if (finais.isEmpty())   // não deveria acontecer: todo roteiro termina num FIM
            finais.add(Disparo.ubs("SEGURANCA", PrioridadeUbs.PROXIMOS_DIAS, "SEM_DISPARO"));

        Destino destino = null;
        for (Disparo d : finais) destino = Destino.maisGrave(destino, d.destino());
        final Destino fim = destino;
        List<Disparo> doNivel = finais.stream().filter(d -> d.destino() == fim).toList();

        PrioridadeUbs prioridade = null;
        if (fim == Destino.UBS_EM_BREVE)
            prioridade = doNivel.stream().map(Disparo::prioridade).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(PrioridadeUbs.PROXIMOS_DIAS);

        Set<String> exige = new LinkedHashSet<>(s.exige);
        doNivel.forEach(d -> exige.addAll(d.exige()));
        // revisão A1: CVV 188 e CIATox nunca se perdem, mesmo que o destino final venha de outro disparo (ex.: SAMU)
        for (Disparo d : finais) for (String x : d.exige()) if (x.equals("CVV188") || x.equals("DISQUE_INTOX")) exige.add(x);
        if ("S".equals(s.doTexto.get("FIL_08"))) exige.add("CVV188");
        if ("LES".equals(s.queixa) && fim == Destino.SAMU_192) exige.add("BLK-EI-TRAUMA");   // sprint 4: não mexer em quem caiu forte
        List<String> motivos = doNivel.stream().map(Disparo::motivo).distinct().toList();
        boolean porNaoSei = doNivel.stream().anyMatch(Disparo::porNaoSei);

        return new Resultado(fim, prioridade, motivos, Collections.unmodifiableSet(exige),
            mensagem(fim, prioridade, exige), exige.contains("CVV188"), porNaoSei,
            List.copyOf(finais), s.historico(), s.analise().sinais());
    }

    private static String mensagem(Destino d, PrioridadeUbs p, Set<String> exige) {
        return switch (d) {
            case SAMU_192 -> "MSG-SAMU";
            case UPA_HOJE -> exige.contains("MATERNIDADE") ? "MSG-UPA-MATERNIDADE"
                : exige.contains("DISQUE_INTOX") ? "MSG-UPA-INTOX" : "MSG-UPA";
            case UBS_EM_BREVE -> "MSG-UBS-" + p;
            case CUIDADO_EM_CASA -> "MSG-CASA";
        };
    }
}
