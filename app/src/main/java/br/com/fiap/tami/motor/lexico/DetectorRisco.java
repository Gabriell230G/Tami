package br.com.fiap.tami.motor.lexico;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D-031: detector de risco por raiz e contexto, para o que não pode escapar (crise). O léxico só reconhece a frase
 * exata; aqui a regra olha a raiz da palavra e o que está perto dela. Na dúvida, liga: o roteiro de crise pergunta
 * antes de qualquer conclusão. Sentido figurado comum ("morrer de rir", "o trânsito vai me matar") é tirado antes.
 *
 * <p>Sprint 3 (Frente 1): o léxico foi escrito por CATEGORIA, não por frase:
 * <ul>
 *   <li>grafia pelo som e de áudio ("sui sídio", "estrupo", "com vulsão", "e corpião") é juntada antes da busca;</li>
 *   <li>suicídio: despedida, doação de pertences, "não devia ter nascido", "ir embora deste mundo", automutilação,
 *       frase em inglês (só ideia; nenhum método é procurado ou mostrado);</li>
 *   <li>intoxicação: produto de casa, remédio em quantidade ou repetido, gás e fumaça em lugar fechado, droga com sintoma;</li>
 *   <li>violência: ameaça, pedido de socorro, abuso de criança por eufemismo, estrangulamento, maus-tratos de idoso;</li>
 *   <li>convulsão descrita sem a palavra ("tremeu inteira e ficou fora do ar");</li>
 *   <li>exagero com humor ("quero morrer kkk", "vou matar meu filho kkk") é figurado, MAS só quando não há
 *       sinal de que é sério ("todo dia", "na real", "mentira... ou não"): na dúvida, risco.</li>
 * </ul>
 */
public final class DetectorRisco {

    private DetectorRisco() {}

    /** {@code convulsao}: crise convulsiva descrita sem a palavra (sprint 3, item 5); o motor decide o destino. */
    public record Resultado(boolean suicidio, boolean suicidioNegado, boolean intoxicacao, boolean peconhento,
                            boolean violenciaFisica, boolean violenciaSexual, boolean convulsao) {
        public Resultado(boolean suicidio, boolean suicidioNegado, boolean intoxicacao, boolean peconhento,
                         boolean violenciaFisica, boolean violenciaSexual) {
            this(suicidio, suicidioNegado, intoxicacao, peconhento, violenciaFisica, violenciaSexual, false);
        }
        public boolean algum() { return suicidio || intoxicacao || peconhento || violenciaFisica || violenciaSexual || convulsao; }
    }

    // =====================================================================================================
    // Grafia pelo som e de áudio: junta as formas antes da busca (só dentro do detector; o texto do motor não muda)
    // =====================================================================================================
    private record Troca(Pattern p, String por) {}
    private static Troca t(String re, String por) { return new Troca(Pattern.compile(re), por); }
    private static final Troca[] SOM = {
        t("\\bs[uo]i\\s?(?:c|s|ss|sc)id(?:i?o|iu)\\b", "suicidio"),              // sui sídio, sói cídio, suicido
        t("\\bsu\\s?(?:c|s|ss)id(?:io|ar|a)\\b", "suicidio"),
        t("\\b(?:auto|alto) (lesa\\w*|lesion\\w*|mutil\\w*|exterm\\w*)", "auto$1"),   // auto lesão, auto mutilando
        t("\\b[ei]st?r?upr?(o|os|a|ada|adas|ado|ados|aram|ou|ar|ador|ando)\\b", "estupr$1"),   // estrupo, istrupada
        t("\\b(?:com|con|cum) ?v[uo]l[sc]a(o|oes)\\b|\\bconvul(?:c|ss|ç)a(o|oes)\\b|\\bcombul[sc]a(o|oes)\\b|\\bconvur[sc]a(o|oes)\\b", "convulsao"),
        t("\\b(?:in|en) ?to(?:x|ss|cs|ks)ic", "intoxic"),                         // in toxicação
        t("\\b(?:[ei] ?s?|s)?c[oa]rpi(?:ao|oes)\\b|\\bescopi(?:ao|oes)\\b", "escorpiao"),   // e corpião, iscorpião
        t("\\bagua ?sa\\w*ari[ao]\\b", "agua sanitaria"),                       // água sanguinária (corretor)
        t("\\b(me|se) mantar\\b", "$1 matar"),
        t("\\bnum (quero|quer|vejo|aguento|aguenta|tenho|consigo|sei|devia|deveria)\\b", "nao $1"),
        t("\\bpensano\\b", "pensando"), t("\\bqueru\\b", "quero"), t("\\bmorer\\b", "morrer"),
    };

    private static String som(String n) {
        for (Troca x : SOM) n = x.p().matcher(n).replaceAll(x.por());
        return n;
    }

    // =====================================================================================================
    // Sentido figurado: sai do texto antes da busca
    // =====================================================================================================
    /** Atividade comum depois de "me matar de" / "morrer de": exagero. O que lembra método ou "de vez" fica de fora. */
    private static final String ATIVIDADE = "(?:tanto )?(?!(?:tomar|beber|pular|jogar|atirar|enforcar|cortar|envenenar|afogar|queimar|vez|verdade|qualquer|lugar|mulher|colher|bar|mar|par)\\b)"
        + "(?:\\w+(?:ar|er|ir)|rir|ri|raiva|vergonha|susto|saudade|calor|frio|fome|sono|cansaco|trabalho|estudo|academia|treino|tedio|preguica|ciume|inveja)";

    private static final Pattern FIGURADO = Pattern.compile(
        "\\bmorr\\w* (?=de dor\\b)|\\bmorr\\w* de (rir|fome|sono|vergonha|calor|frio|medo|tedio|saudade|inveja|cansaco|preguica|amor|ansiedade|curiosidade|raiva|tanto rir|dar risada|vontade|dor de cotovelo|ciume|susto)\\b"
        + "|\\bmorto de (cansaco|fome|sono|vergonha|rir)\\b"
        + "|\\b(transito|prova|fila|calor|frio|chefe|trabalho|saudade|isso|essa|esse|professor\\w*|onibus|metro|conta|fatura|ansiedade|curiosidade|dieta|academia|treino|crianca|filho|filha) (ainda )?(vai|vao|ta|esta|tao) (me )?mat\\w*"
        + "|\\bmat\\w* (a|o) (aula|saudade|tempo|fome|sede|charada|servico|trabalho|vontade|curiosidade|jogo|serie|prova)\\b"
        + "|\\bmatei (aula|a saudade|o tempo)\\b|\\bmorrendo de\\b|\\bmorri de\\b"
        + "|\\b(transito|prova|fila|calor|chefe|trabalho|semana|segunda|faculdade|escola|conta|fatura|dieta|academia|treino|saudade) (ainda )?(vai|vao|ta|esta) acaba\\w* comigo"
        // sprint 3 (categoria "exagero"): "me matar de estudar/trabalhar/rir", "isso me mata", "vai acabar comigo"
        + "|\\b(me|se|te|nos) mat(ar|a|o|ando|ei|ou|ava|aria|amos)? de " + ATIVIDADE + "\\b"
        + "|\\b(ta|esta|tao|estao|vai|vao|ainda vai|quase|so) me mat(a|ar|ando|ou|aram)\\b"
        + "|(?<!\\b(?:em|de|vou|vo|quero|queria|ia|eu|tentar|tentei|pra|para|que|so|e|nao))(?<!\\bpensando em) me mata\\b"
        + "|\\b(vai|vao|ta|esta|tao|estao|quase|ainda vai) acaba\\w* comigo\\b"
        + "|\\b(e|eh|seria|foi|isso e|que) (um )?suicidio (\\w+(ar|er|ir))\\b|\\bsuicidio (financeiro|social|politico|profissional|academico)\\b"
        // gíria que soa como trauma, droga ou acidente
        + "|\\bfacada (no|na|do|da) (preco|conta|valor|orcamento|bolso|carteira|cartao|fatura|imposto|aluguel|mecanico|dentista|oficina|loja|mercado|feira|posto)\\b"
        + "|\\btiro e queda\\b|\\btiro no pe\\b|\\btiro certo\\b"
        + "|\\btir(o|ei|ou|amos|aram|a|ar) (um |uma |nota |so |a nota )?(\\d+|zero|dez|nove|oito|sete|seis|cinco|quatro|tres|dois|um)\\b(?! (tiros?|vezes|vez|dias))"
        + "|\\boverdose de (serie|series|netflix|filme|filmes|novela|novelas|chocolate|doce|doces|fofura|amor|trabalho|informacao|reuniao|reunioes|memes?|tiktok|instagram|redes? sociais?|carboidrato|musica|futebol|anime|livros?|estudo|jogo|games|beleza)\\b"
        + "|\\b(bati|bateu|batemos|capotei|capotou|atropelei|atropelou|morri|morreu|levei|levou|tomei|tomou|me mataram|mataram|matei|matou|cai|caiu)\\b(\\s+\\w+){0,4}?\\s+(no|na|do|da) (jogo de (corrida|tiro|luta|carro|guerra|videogame|celular|computador)|videogame|video game|game|fortnite|free fire|freefire|gta|minecraft|roblox|fifa|counter strike|valorant|mario kart|simulador|playstation|xbox)\\b"
        + "|\\benforc\\w* (a|o) (aula|trabalho|escola|faculdade|servico|dia|expediente|plantao|reuniao|academia|treino)\\b"
        + "|\\b(sumir|desaparecer|me enterrar|enfiar a cara|abrir um buraco) (de|da) (tanta |muita )?vergonha\\b|\\bsumir (\\w+ )?de ferias\\b"
        + "|\\bacabar com (tudo )?(que|o que) (tem|tinha|sobrou)\\b|\\bacabar com (ess[ae]|est[ae]|o|a|meu|minha) (gripe|tosse|febre|fome|sede|preguica|virus|bacteria|alergia|coceira|espinha|acne|celulite|barriga|gordura|bagunca|sujeira|conta|divida|caspa|piolho|mosquito|barata|rato|formiga|cupim|infeccao|inflamacao|ressaca|enxaqueca|dieta|festa|cerveja|pizza|bolo|comida|garrafa|estoque)\\b"
        + "|\\bapanh\\w* de (\\d+|um|dois|tres|quatro|cinco|goleada|lavada)\\b|\\b(time|equipe|selecao) apanhou\\b"
        + "|\\bapanh\\w* (muito |bastante )?(no|na) (jiu|jiu jitsu|treino|luta|jogo|aula|academia|muay|boxe|judo|karate|capoeira)\\b");

    /** Riso e emoji de piada no texto. 🙃 fica de fora: costuma ser ironia triste. */
    private static final Pattern RISO = Pattern.compile("(^| )(k|kk|ks|ksks\\w*|(rs)+r?|(ha){2,}h?|(he){2,}|(hu){2,}|lol|lmao|kkk\\w*)( |$)");
    private static final Pattern EMOJI_RISO = Pattern.compile("[\\x{1F602}\\x{1F923}\\x{1F605}\\x{1F606}\\x{1F639}\\x{1F921}\\x{1F61C}\\x{1F61D}\\x{1F92A}\\x{1F648}\\x{1F601}\\x{1F604}\\x{1F480}\\x{1F62D}]");
    /** O que mostra que não é piada: com isto o humor não apaga nada. */
    private static final Pattern SERIO = Pattern.compile(
        "\\b(serio|seria mesmo|de verdade|di verdade|nao e brincadeira|nao to brincando|nao estou brincando|sem brincadeira|nao e zoeira|na real|real oficial|mentira|ou nao"
        + "|todo (santo )?dia|todos os dias|toda (santa )?noite|toda hora|toda vez|sempre|faz (uns )?(dias|semanas|meses)|desde|socorro|me ajuda|ajuda"
        + "|nao aguento mais|cansad[oa] de viver|ninguem liga|ninguem se importa|despedi\\w*|pensando nisso|so penso nisso)\\b");
    /** Exagero que só é figurado com riso e sem sinal de que é sério. */
    private static final Pattern FIGURADO_HUMOR = Pattern.compile(
        "\\b(quero|queria|vou|ia|to querendo|da vontade de|vontade de|so quero|so queria|vou acabar) morrer\\b"
        + "|\\b(vou|quero|queria|vontade de|da vontade de|ia|to com vontade de) matar (o|a|os|as|meu|minha|meus|minhas|esse|essa|esses|essas|aquele|aquela|ele|ela|eles|elas|alguem|todo mundo|quem|um|uma|geral|voce|tu|te)\\b"
        + "|\\b(vai|vao|ia|quer) matar (quem|todo mundo|geral|o primeiro|a primeira)\\b"
        + "|\\b(e|eh) suicidio\\b");
    /** "X vai me matar kkk": exagero com riso, a não ser que seja ameaça contada ("ele disse que vai me matar"). */
    private static final Pattern FIGURADO_HUMOR_CONTRA_MIM = Pattern.compile(
        "\\b(vai|vao|ia|quase|ta|tava|esta) me mat(ar|a|ando|ou)\\b|\\bme mata\\b");
    /** Ameaça relatada ("ele disse que vai me matar") nunca vira piada, mesmo com riso. */
    private static final Pattern AMEACA_RELATADA = Pattern.compile(
        "\\b(disse|falou|jurou|ameac\\w*|mandou (msg|mensagem|mensagens)|fica (dizendo|falando|mandando)|vive dizendo|promete)\\b");

    private static boolean humor(String original, String n) {
        return (RISO.matcher(n).find() || EMOJI_RISO.matcher(original).find()) && !SERIO.matcher(n).find();
    }

    /** Tira só o exagero com riso (o texto continua com "vai me matar" de quem ameaça de verdade). */
    private static String tirarHumor(String original, String n) {
        if (!humor(original, n)) return n;
        boolean relatada = AMEACA_RELATADA.matcher(n).find();
        n = FIGURADO_HUMOR.matcher(n).replaceAll(" . ");
        return relatada ? n : FIGURADO_HUMOR_CONTRA_MIM.matcher(n).replaceAll(" . ");
    }

    private static String tirarFigurado(String original, String n) {
        return FIGURADO.matcher(tirarHumor(original, n)).replaceAll(" . ");
    }

    // =====================================================================================================
    // Suicídio e autolesão (só ideia; nenhum método é procurado ou mostrado)
    // =====================================================================================================
    /** Quem anuncia a ideia: "quero", "vou", "penso em", "vontade de"... */
    private static final String LEAD = "(?:quero|queria|quiria|vou|ia|iria|vontade de|da vontade de|penso em|pensando em|pensei em|pensa em|pensamento de|pensamentos de|ideia de|desejo de|planejando|planejo|tenho vontade de|to com vontade de|so queria|so quero|preciso|devia|deveria)";

    private static final Pattern SUICIDIO = Pattern.compile(
        "\\bsuicid\\w*|\\bautolesa\\w*|\\bautolesion\\w*|\\bautomutil\\w*|\\bautoexterm\\w*"
        + "|\\b" + LEAD + " (morrer|morre|suicidar|nao existir|nao existir mais|sumir pra sempre|desaparecer pra sempre)\\b"
        + "|\\b" + LEAD + " me (matar|mata|mato)\\b"
        + "|\\b" + LEAD + " matar\\s*$|\\b(em|de|pra|para) me (matar|mata)\\b"
        + "|\\b(quer|queria|vai|disse que vai|falou que vai|ameacou|ameaca|ameacando|tentou|tenta|pensa em|fala em|falou em|falando em) se (matar|mata|suicidar)\\b"
        + "|\\b(quer|queria|disse que vai|falou que vai|ameacou|ameaca|ameacando|tentou|tenta|pensa em|fala em|falou em|falando em|vai) (morrer|suicidar)\\b"
        + "|(?<!\\b(?:vai|vao|iam|querem|quer|tentou|tentaram|ameacou|ameacaram|ameaca|jurou|pode|podem|consegue|conseguiu|quase|ele ia|ela ia)) me matar\\b(?! (de|a|o) )"
        + "|\\bse mata(r)?\\b(?! (de|a|o) )|\\bmatar me\\b"
        + "|\\bvontade de morrer|\\bpensamentos? de morte|\\bdesejo de morte|\\bdesisti de viver|\\bcansad[oa] de viver|\\bideia de morte"
        + "|\\b(tirar|acabar com|dar fim (a|na|em)|por fim (a|na|em)) (a |com a )?(minha |propria |sua |da propria |a propria )?vida\\b"
        + "|\\bdar (um )?fim (nisso|em tudo|nisso tudo|a tudo|na minha vida|em mim|na vida)"
        + "|\\b(nao|n) (quero|quer|queria|aguento|aguenta|consigo|vejo motivo pra|vejo razao pra|tenho motivo pra|tenho vontade de|tem vontade de|sinto vontade de) (mais )?(viver|vive|acordar|acorda|existir|existi|continuar vivendo|continuar vivo|continuar viva|estar aqui|seguir vivendo|ficar nesse mundo|estar nesse mundo|estar mais aqui)\\b"
        + "|\\b(nao|n) (quero|quer|queria|aguento|aguenta) (mais )?ficar aqui\\b(?! (nesse|neste|nessa|no|na|em|com|sem) )"
        + "|\\b(nao|n) (vejo|tenho|encontro|acho|ve|tem) (mais )?(nenhum |nenhuma )?(motivo|razao|sentido)( nenhum| nenhuma)? (pra|para|em) (viver|continuar|seguir|nada|mais nada|existir|acordar|estar aqui|levantar|tudo isso)\\b"
        + "|\\bvida (nao tem|perdeu|sem) (mais )?(o |a )?(sentido|graca)\\b|\\bvida sem sentido\\b"
        + "|\\b(carta|cartas|bilhete|bilhetes|mensagem|video) (\\w+ ){0,2}de despedida\\b|\\bdespedi\\w* de todo mundo|\\bme despedindo\\b(?! (do|da|dos|das) (trabalho|empresa|escola|turma|pessoal|colegas?|ferias|cidade))"
        + "|\\bdespedida\\b(?! (de solteir\\w*|do trabalho|da empresa|da escola|da faculdade|da turma|do time|dos amigos|das amigas|do colega|da colega|de ferias|da cidade|do emprego))(?<!(?:festa|jantar|almoco|churrasco|bolo|presente) de despedida)"
        + "|\\b(me|se) (desped\\w*) (de )?(todos|todo mundo|geral|voces|da familia|de todos|de todo mundo)\\b|\\bse despedindo\\b(?! (do|da|dos|das) (trabalho|empresa|escola|turma|pessoal|colegas?|ferias|cidade))"
        + "|\\badeus (a|pra|para) (todos|todo mundo|voces|geral|minha familia)\\b|\\badeus mundo\\b|\\badeus a vida\\b"
        + "|\\b(e|seria|sera|vai ser|ser|hoje e) (o )?(meu|seu|dele|dela) ultimo dia\\b(?! (de|do|da|no|na|em|com) )|\\b(e|seria|sera|vai ser|hoje e) o ultimo dia (dele|dela|meu|seu)\\b(?! (de|do|da|no|na|em|com) )|\\bultimo dia (da minha vida|de vida|vivo|viva|nesse mundo|neste mundo)\\b"
        + "|\\b(vou|vai|medo de|pensando em|com vontade de|acho que vou) fazer (uma )?besteira\\b|\\bfazer (uma )?besteira comigo"
        + "|\\b(melhor|melhores|mais feliz|mais felizes|seria melhor|mundo seria melhor|todos ficariam melhor|todo mundo fica melhor|ficariam melhor) (sem mim|sem eu|se eu (nao existisse|morresse|sumisse))"
        + "|\\b(me|se) (corto|cortei|cortando|cortar|corta|cortou|machuco|machucando|machuquei|arranho|arranhando|arranhei|queimo|queimei|queimando|bato|batendo|belisco|beliscando|furo|furando)\\b.{0,40}\\b(proposito|ate sangrar|aliviar|sentir|raiva|escondido|castigo|punir)"
        + "|\\bse cortando\\b|\\bme corto\\b|\\bse corta\\b|\\bse cortou de proposito|\\bse machuca de proposito|\\bme machuco de proposito"
        + "|\\b(me|se) (cort|machuc|arranh|queim|belisc|mord|fur)\\w*\\b.{0,40}\\b(proposito|ate sangrar|aliviar|sentir alguma coisa|castigo|punir|ansios\\w*|nervos\\w*)"
        + "|\\b(vontade de|quero|queria|penso em|pensando em|pensei em|ideia de|desejo de|preciso|pensamento de|pensamentos de) me (machuc\\w*|ferir|feri|punir|puni|mutil\\w*|cortar|corta|queimar|arranhar|bater)\\b(?! (o|a|os|as|um|uma) )"
        + "|\\b(me|se) (ferindo|machucando|mutilando|cortando|punindo) (de novo|escondid\\w*|de proposito|toda|todo|sempre)"
        + "|\\b(tenho|ando|venho|vivo|voltei a|voltou a|vive|anda) (me|se) (cort|machuc|fer|mutil|queim|arranh|pun)\\w*"
        + "|\\b(faco|fiz|fazendo|faz|fez) (uns |umas |varios |alguns )?(cortes?|riscos?|machucados?) (n[oa]s? )?(braco|bracos|pulso|pulsos|coxa|coxas|perna|pernas|barriga|corpo)\\b"
        + "|\\b(ir|partir|morrer) junto (com )?(ele|ela|dele|dela|meu \\w+|minha \\w+)\\b(?=.*\\b(falec\\w*|morreu|perdi|enterr\\w*|luto|velorio)\\b)|\\b(falec\\w*|morreu|perdi|enterr\\w*|luto|velorio)\\b.{0,80}\\b(ir|partir|morrer) junto (com )?(ele|ela|dele|dela|meu \\w+|minha \\w+)\\b"
        + "|\\b(sumir|desaparecer) (e nunca mais|pra sempre|do mundo|do mapa|de vez|e nao voltar)|\\bnunca mais acordar?\\b"
        + "|\\b(penso|pensando|pensei|pensa|vontade|quero|queria|so queria|desejo|sonho) (em |de )?(sumir|desaparecer)\\b(?! (de vergonha|um pouco|uns dias|por uns|um tempo|umas|pra praia|de ferias|daqui|do trabalho|da festa|de casa|com|no|na|debaixo|embaixo|dentro))"
        + "|\\b(sem forcas?|sem vontade|sem motivo|sem razao|sem coragem) (pra|para|de) (continuar )?viv\\w*|\\bnao vale (mais )?a pena (continuar|viver|seguir|estar aqui)"
        + "|\\b(sem|nao tenho( mais)?|nao tem( mais)?|perdi a|perdeu a|nao sinto( mais)?) (nenhuma )?vontade de viver\\b"
        + "|\\bse eu (morresse|nao acordasse|sumisse|nao existisse|nao (estivesse|tivesse) mais (vivo|viva|aqui))|\\bqueria (estar|ta|tar) mort[oa]|\\bqueria nao ter nascido|\\bmelhor mort[oa]\\b"
        + "|\\b(nao|nunca|nem) (devia|deveria|era pra|tinha que) ter nascido\\b|\\b(queria|preferia|melhor|era melhor) (nao|nunca) ter nascido\\b"
        + "|\\b(partir|ir embora|sair|ir) (logo |de vez |embora )?(dessa|desta|desse|deste|do) (vida|mundo|plano|terra)\\b|\\bdeixar esse mundo|\\bacabar (logo )?comigo|\\bme livrar dessa vida"
        + "|\\b(pensando em|penso em|quero|queria|vontade de|melhor|vou) (ir|partir|sumir|apagar) de vez\\b"
        + "|\\bunica saida (e|seria|que eu vejo e) (eu )?(morrer|sumir|ir embora|partir|acabar|a morte|me matar)"
        + "|\\b(quero|queria|vou) morre\\b|\\bdesistir da vida|\\bdesisto da vida|\\b(pra|para) tudo acabar( logo)?\\b|\\bcansad[oa] de existir"
        + "|\\b(queria|quero|so queria|desejo|rezo pra|torco pra) que tudo (acabe|acabasse|termine|terminasse|acabar)\\b"
        + "|\\b(quero|queria|vou|penso em|pensando em|vontade de) acabar com tudo\\b(?! (de|da|do|isso de|que|o que|na|no))"
        + "|\\b(pensando em|penso em|vontade de|quero|vou) desistir de tudo\\b(?! (isso|da|do|de))|\\bnao aguento mais (essa|esta|a minha|minha) vida\\b"
        + "|\\bsou um peso (pra|para|na vida)|\\b(me|se) fazer mal\\b|\\bseria melhor eu (partir|sumir|morrer|ir embora de vez)"
        + "|\\b(melhor|mais facil|era melhor|e melhor) eu (morrer|sumir|nao existir|nao estar aqui|desaparecer|partir|estar mort[oa]|nao acordar)\\b"
        + "|\\b(porque|por que|pra que|para que) (eu )?(ainda )?(to|estou|tou|sigo|continuo|estar) viv[oa]\\b"
        + "|\\b(dormir?|deitar|fechar os olhos) (pra sempre )?e (nao|nunca mais) (acordar?|acorda)( mais)?\\b|\\b(dormir|apagar) (pra|para) sempre\\b|\\b(queria|quero|so queria|vontade de) (nao|nunca mais) acordar?\\b"
        + "|\\bvida\\b.{0,50}\\b(tirar|acabar com|dar fim (a|n)?)\\s?ela\\b"
        + "|\\bse eu (sumir|morrer|desaparecer|partir|nao voltar|nao acordar)\\b.{0,40}\\b(ja sabem|saibam|voces sabem|sabem (o )?porque|foi por|me perdoem|perdao|desculpa|a culpa|cuidem)"
        + "|\\bse eu (sumir|desaparecer|morrer|partir) (hoje|amanha|essa noite|hoje a noite)\\b"
        + "|\\bja tentei\\b.{0,40}\\btentar (de novo|outra vez|novamente)\\b"
        // doação de pertences e "não vou precisar mais" (sinal de despedida)
        + "|\\b(dando|doando|dei|doei|distribuindo|deixando|dividindo) (todas |tudo |as |os )*(minhas|meus) (coisas|pertences|bens|roupas|objetos|livros|jogos)\\b.{0,60}\\b(nao (vou|vai) (mais )?precisar|pros outros|pra todo mundo|de lembranca|antes de (ir|partir)|ultim\\w*)"
        + "|\\bnao (vou|vai) (mais )?precisar (mais )?(delas|deles|disso|dessas coisas|desses|de nada)\\b(?! (por|ate|pelo|pela|na|no|hoje))"
        + "|\\b(penso|pensando|pensar|pensei|pensa) (em|na) morte\\b|\\bpensar em (morrer|me matar|suicidio)\\b"
        + "|\\bpensamentos? (ruim|ruins|sombrios?|negativos?|estranhos?)( \\w+){0,2} (de )?(morte|morrer|me matar|sumir|se acabar)\\b"
        + "|\\bviver (doi|machuca|pesa|e um peso|e um fardo|e um sofrimento|nao vale)\\b|\\b(viver|vida|existir)\\b.{0,25}\\bnao (quero|aguento) mais\\s*$"
        + "|\\b(esperando|espera|esperar) (a morte|morrer|deus me levar|deus levar)\\b|\\bdeus (podia|pode|devia|bem que podia) me levar\\b"
        // em inglês (mistura de idioma)
        + "|\\b(want|wanna|wish|going|gonna|trying) (to )?(die|kill myself|end it all|end my life|disappear)\\b|\\bkill myself\\b|\\bsuicidal\\b|\\bend my life\\b"
        + "|\\b(don ?t|do not|dont) want to (live|be alive|wake up|exist)\\b|\\bkms\\b");

    /** Marcas escondidas no corpo (o outro diz que foi o gato): autolesão até prova em contrário. */
    private static final Pattern MARCA_ESCONDIDA = Pattern.compile(
        "\\b(braco|bracos|pulso|pulsos|coxa|coxas|antebraco|antebracos)\\b.{0,25}\\b(marcad\\w*|cortad\\w*|riscad\\w*|cheio de (cortes?|marcas?|riscos?)|cheia de (cortes?|marcas?|riscos?)|com (cortes?|marcas?|riscos?))\\b"
        + ".{0,60}\\b(diz que|fala que|falou que|disse que|esconde|escondendo|manga comprida|de proposito|nao foi (o gato|acidente|o cachorro))");

    private static final Pattern NEGACAO_ANTES = Pattern.compile("\\b(nao|nunca|jamais|nem)\\s+(\\w+\\s+){0,1}$");

    // =====================================================================================================
    // Intoxicação
    // =====================================================================================================
    private static final String VERBO_INGERIR = "(dei|demos|dava|bebi|bebeu|bebendo|beber|tomei|tomou|tomaram|engoli|engoliu|engolir|ingeri|ingeriu|ingerir|comi|comeu|comendo|mastigou|mastiguei|mastigando|chupou|lambeu|lambendo|lamber|cheirei|cheirou|cheirar|cheirando|inalei|inalou|inalar|inalando|respirei|respirou|respirar|respirando|aspirou|puxou|sugou|mordeu|mordi|comemos|bebemos|tomamos|provou|experimentou|botou na boca|colocou na boca|pos na boca|caiu no olho|espirrou no olho|passou na pele)";
    private static final String TOXICO = "(produto de limpeza|produtos de limpeza|agua sanitaria|candida|cloro|soda caustica|soda|desinfetante|detergente|veneno|raticida|chumbinho|inseticida|agrotoxico|pesticida|herbicida|mata rato|mata barata|mata mato|mata formiga|formicida|carrapaticida|mata carrapato|cupinicida|cogumelo|metanol|cachaca clandestina|cachaca de procedencia|alcool de limpeza|alcool 70|alcool gel|alcool combustivel|querosene|gasolina|diesel|oleo diesel|benzina|aguarras|varsol|fluido de isqueiro|thinner|tiner|acetona|removedor|solvente|diabo verde|desentupidor|limpa forno|alvejante|tira manchas?|multiuso|naftalina|creolina|pilha|bateria de relogio|bateria pequena|ima|amaciante|sabao em po|sabao liquido|sabao de lava roupa|sabao de lavar roupa|lava roupa|capsula de (lava roupa|sabao)|enxaguante( bucal)?|antisseptico bucal|lustra moveis|oleo de peroba|tinta|cola|amonia|acido muriatico|fertilizante|adubo|remedio a mais|remedios a mais|remedio demais|remedio errado|remedio dobrado|dose dobrada|cartela|comprimidos|comprimido da|remedio da vo|remedios da vo|remedio do vo|remedio de adulto|baiacu)";
    /** Remédio (classe ou nome comum) para a regra de quantidade. Nenhuma dose é mostrada: só se reconhece o excesso. */
    private static final String REMEDIO = "(remedio\\w*|comprimido\\w*|pilula\\w*|capsula\\w*|xarope\\w*|gotas|gotinhas|vitamina\\w*|sulfato ferroso|ferro|paracetamol|tylenol|dipirona|novalgina|ibuprofeno|aspirina|aas|rivotril|clonazepam|clonazepan|diazepam|zolpidem|fluoxetina|sertralina|amitriptilina|insulina|metformina|glibenclamida|captopril|losartana|levotiroxina|puran|anticoncepciona\\w*|calmante\\w*|antidepressivo\\w*|tarja preta|sonifero\\w*|antialergico\\w*|dramin|melatonina|carbamazepina|fenobarbital|gardenal|depakene|haldol|litio)";
    private static final String QUANTIDADE = "(inteir[oa]|tod[oa](?! (dia|noite|hora|manha|tarde|mes|semana|santo|santa|vez|vida|mundo|certinho))|todinh[oa]|o vidro|o frasco|a caixa|a cartela|uma cartela|um monte|monte de|demais|a mais|em dobro|dobrad[oa]s?|de uma vez|nem sei quantos|nem sei quantas|(duas|2|tres|3) vez(es)?\\b(?! (por|ao|na|no) (dia|semana|mes))|de novo sem querer|repetid[oa]|um punhado|um bocado)";
    private static final String CRIANCA = "\\b(bebe|nenem|crianca|criancas|menino|menina|meu filho|minha filha|o pequeno|a pequena|net[oa]|sobrinh[oa]|([1-9]|1[0-2]) anos|([1-9]|1[0-9]) mes(es)?)\\b";
    /** Droga pelo nome: qualquer verbo de uso serve. */
    private static final String DROGA = "(droga|drogas|cocaina|crack|ecstasy|ecstase|mdma|lsd|lanca perfume|lolo|k9|k2|k4|spice|haxixe|cetamina|ketamina|heroina|metanfetamina|comprimido estranho)";
    /** Gíria de droga ou "um negócio": só com fumar, cheirar, injetar, usar ou misturar. */
    private static final String DROGA_GIRIA = "(po|pedra|bala|balinha|md|doce|lanca|maconha|baseado|beck|cristal|rebite|keta|negocio|coisa|parada|bagulho|treco|trem)";
    private static final String SINTOMA_AGUDO = "(coracao (a mil|disparado|acelerado|batendo forte|disparou)|vendo coisa|vendo coisas|ouvindo coisa\\w*|alucin\\w*|tremend\\w*|tremedeira|nao acorda|sem acordar|apagad\\w*|apagou|desmai\\w*|convuls\\w*|dor no peito|suando (frio|muito)|suor frio|falando (nada com nada|coisa com coisa|enrolado)|confus\\w*|vomit\\w*|passando mal|sem ar|falta de ar|respirando (estranho|devagar|mal|pouco)|roxo|roxa|babando|tont\\w*|grogue|paranoi\\w*|surtando|surto|desorientad\\w*|nao responde|agitad\\w*|caiu duro|caid[oa]|desacordad\\w*|olho virado|espumando)";

    private static final Pattern INTOX = Pattern.compile(
        "\\bintoxic\\w*|\\benvenen\\w*|\\boverdose\\b|\\bciatox\\b"
        + "|\\b" + VERBO_INGERIR + "\\b(?! (um |uma )?banho)(\\s+\\w+){0,9}?\\s+" + TOXICO + "\\b"
        + "|\\b(bebeu|bebi|engoliu|engoli|tomou um gole|tomou o|tomou a) (\\w+ ){0,2}(perfume|colonia|desodorante|esmalte|alcool|enxaguante)\\b"
        + "|\\b" + TOXICO + "\\b(\\s+\\w+){0,6}?\\s+" + VERBO_INGERIR + "\\b"
        + "|\\b(respirou|respirei|inalou|inalei|cheirou|cheirei) (um |uns |algum )?produto|\\b" + TOXICO + "\\b.{0,30}\\b(na|a|pela) boca\\b"
        + "|\\b(vazou|vazamento de|vazando|cheiro (forte )?de|cheirando|respirou|respirei|escapando) gas\\b|\\bgas( de cozinha| do fogao| do botijao| do aquecedor)? (vazando|vazou|escapando)\\b|\\bgas( de cozinha| do fogao| do botijao| do aquecedor)? (ligado|aberto)\\b.{0,80}\\b" + SINTOMA_AGUDO + "|\\bmonoxido\\b"
        + "|\\bcheiro (forte |muito forte )?(de )?(solvente|tinta|thinner|tiner|cloro|agua sanitaria|gasolina|querosene|cola|veneno|inseticida|agrotoxico|produto|acetona|amonia)\\b.{0,80}\\b" + SINTOMA_AGUDO
        + "|\\b" + SINTOMA_AGUDO + ".{0,80}\\bcheiro (forte |muito forte )?(de )?(solvente|tinta|thinner|tiner|cloro|agua sanitaria|gasolina|querosene|cola|veneno|inseticida|agrotoxico|produto|acetona|amonia)\\b"
        + "|\\b(cachaca|bebida|pinga|vodka|whisky|alcool|cerveja) (\\w+ )?(falsificad|adulterad|clandestin|batizad)\\w*"
        + "|\\b(churrasqueira|braseiro|aquecedor|gerador|fogareiro|lareira|fogao a lenha|motor ligado|carro ligado)\\b.{0,100}\\b(dor de cabeca|tont\\w*|desmai\\w*|sonolent\\w*|sono|confus\\w*|enjo\\w*|nao acorda|nao responde)\\b"
        + "|\\bremedio\\w*\\b.{0,30}\\b(duas vez(es)?\\b|em dobro|repetido|dobrado|a mais|de outra pessoa|do meu avo|da minha avo|do marido|da mulher)(?! (por|ao|na) (dia|semana))|\\btroc\\w* (os|o|de) remedio|\\bconfund\\w* (o |os |a |as )?(remedio|comprimido|pilula|capsula|frasco|vidro)"
        + "|\\b(comeu|engoliu|mastigou|mordeu|chupou|lambeu|ingeriu|pegou e comeu)\\b(\\s+\\w+){0,4}?\\s+(remedio\\w*|pilula\\w*|comprimido\\w*|capsula\\w*|anticoncepciona\\w*)"
        + "|\\b(comeu|mordeu|mastigou|chupou|lambeu|bebeu|beber|tomou)\\b(\\s+\\w+){0,4}?\\s+(folha|planta|cha de (uma )?planta|comigo ninguem pode|mamona|espirradeira|copo de leite|tinhorao|cogumelo|frutinha do quintal)"
        + "|\\b(passou|passei|aplicou|apliquei|pulverizou|pulverizei|borrifou|borrifei|passando) (veneno|agrotoxico|inseticida|defensivo|herbicida|mata mato)"
        + "|\\b(colocaram|botaram|puseram|batizaram) (alguma coisa|algo|droga|remedio)? ?na (minha )?bebida|\\bme dopa\\w*|\\bfui dopad\\w*|\\bboa noite cinderela"
        + "|\\bmistur\\w* (de |uns |umas |os |as |dois |duas |varios )?(produto|produtos|agua sanitaria|cloro|candida|desinfetante|amonia|alvejante)|\\b(agua sanitaria|cloro|candida) com (vinagre|amonia|desinfetante|outro produto)"
        // sprint 3: bebida alcoólica misturada com droga ou remédio
        + "|\\bmistur\\w* (a )?(bebida|alcool|cachaca|cerveja|pinga|vodka|energetico)\\w* com (bala|droga|remedio|comprimido|calmante|po|doce|lsd|md|ecstasy|maconha|cocaina)"
        // fumaça e queima em lugar fechado (monóxido de carbono)
        + "|\\b(fumaca|fumaceira|incendio|pegou fogo)\\b(?! (de|do) (cigarro|narguile|vape|cachimbo|charuto)).{0,60}\\b(tont\\w*|desmai\\w*|confus\\w*|tossindo|tosse|sem ar|falta de ar|dor de cabeca|sonolent\\w*|apagad\\w*|nao responde|nao acorda|passando mal|vomit\\w*|enjo\\w*)"
        + "|\\b(churrasqueira|braseiro|carvao(?! ativado)|aquecedor|gerador|fogareiro|lareira|fogao a lenha|motor ligado|carro ligado|moto ligada)\\b.{0,60}\\b(dentro de casa|dentro do (quarto|apartamento|banheiro|carro)|fechad[oa]s?|tudo fechado|sem janela|sem ventilacao|garagem|na sala|no quarto|no banheiro)\\b"
        + "|\\b(dentro de casa|dentro do (quarto|apartamento|banheiro)|tudo fechado|garagem fechada|sem ventilacao)\\b.{0,60}\\b(churrasqueira|braseiro|carvao(?! ativado)|aquecedor|gerador|fogareiro|motor ligado|carro ligado)\\b");

    /** Remédio em quantidade ou repetido, perto de um verbo de tomar. */
    private static final Pattern REMEDIO_QTD = Pattern.compile(
        "\\b(tomou|tomei|tomaram|bebeu|bebi|engoliu|engoli|comeu|comi|mastigou|pegou|tomando|bebendo)\\b(?:\\s+\\w+){0,7}?\\s+" + REMEDIO + "\\b.{0,40}\\b" + QUANTIDADE + "\\b"
        + "|\\b(tomou|tomei|tomaram|bebeu|bebi|engoliu|engoli|comeu|comi|mastigou)\\b(?:\\s+\\w+){0,3}?\\s+" + QUANTIDADE + "\\b(?:\\s+\\w+){0,4}?\\s+" + REMEDIO + "\\b");
    /** Remédio de outra pessoa (criança, ou troca por engano). */
    private static final Pattern REMEDIO_OUTRO = Pattern.compile(
        "\\b(tomou|engoliu|bebeu|comeu|pegou|tomaram|tomei|engoli)\\b.{0,25}\\b(remedio|comprimido|xarope|pilula|capsula|gotas)\\w*\\b.{0,35}\\b(d[oa]s? (minha |meu |sua |seu )?(irma|irmao|mae|pai|avo|vo|vizinh\\w*|tio|tia|marido|mulher|esposa|esposo|outr\\w*|adulto|cachorro|gato|bisavo)|de adulto|de outra pessoa|de cachorro|veterinari\\w*)\\b");
    private static final Pattern ENGANO = Pattern.compile("\\b(confundiu|confundi|trocou|troquei|por engano|sem querer|escondid[oa]|achando que era|pensando que era)\\b");
    private static final Pattern DROGA_USO = Pattern.compile(
        "\\b(usou|usei|usando|usaram|cheirou|cheirei|cheirando|cheiraram|fumou|fumei|fumando|fumaram|tomou|tomei|tomaram|bebeu|bebi|misturou|misturei|injetou|injetei|me deram|deram pra (ele|ela|mim))\\b(?:\\s+\\w+){0,3}?\\s+" + DROGA + "\\b"
        + "|\\b(usou|usei|usando|usaram|cheirou|cheirei|cheirando|cheiraram|fumou|fumei|fumando|fumaram|misturou|misturei|injetou|injetei|me deram|deram pra (ele|ela|mim))\\b(?:\\s+\\w+){0,3}?\\s+" + DROGA_GIRIA + "\\b");
    private static final Pattern DROGA_DADA = Pattern.compile("\\b(que|q) (me |lhe |te )?(deram|ofereceram|passaram|venderam)\\b|\\bna festa\\b|\\bna balada\\b");
    private static final Pattern SINTOMA = Pattern.compile("\\b" + SINTOMA_AGUDO);
    private static final Pattern ALCOOL_GRAVE = Pattern.compile(
        "\\b((bebeu|bebi|tomou|tomei) (todas|demais|muit[oa]|um porre|uma garrafa|cachaca|pinga|vodka|whisky|cerveja\\w*|vinho|alcool|catuaba|corote)|encheu a cara|enchi a cara|bebad\\w*|embriagad\\w*|alcoolizad\\w*|coma alcoolico)\\b"
        + ".{0,80}\\b(nao acorda|sem acordar|apagad\\w*|desacordad\\w*|nao responde|confus\\w*|vomitando sem parar|respirando (estranho|devagar|mal|pouco)|boca roxa|labios? roxos?|vomitando sangue|vomitou sangue|convuls\\w*|desmai\\w*|engasgad\\w*|vomitando dormindo)");

    // =====================================================================================================
    // Bicho peçonhento
    // =====================================================================================================
    private static final String ACAO_BICHO = "(picou|picada|picado|picaram|mordeu|mordida|mordido|ferroou|ferroada|ferrou|queimou|queimada|encostei|encostou|pisei|pisou|atacou|ataque de)";
    private static final String BICHO = "(cobra|jararaca|cascavel|coral|surucucu|serpente|escorpiao|escorpioes|aranha|armadeira|aranha marrom|viuva negra|taturana|lagarta|lonomia|agua viva|caravela|arraia|bagre|enxame|abelhas|vespas|marimbondos|marimbondo)";
    private static final Pattern PECONHENTO = Pattern.compile(
        "\\b" + ACAO_BICHO + "\\b(\\s+\\w+){0,5}?\\s+" + BICHO + "\\b|\\b" + BICHO + "\\b(\\s+\\w+){0,5}?\\s+" + ACAO_BICHO + "\\b"
        + "|\\b(monte de|um monte de|varias|muitas|dezenas de|enxame de) (picadas?|ferroadas?|mordidas?) de (abelha|vespa|marimbondo|formiga)s?\\b");

    // =====================================================================================================
    // Violência
    // =====================================================================================================
    private static final String PESSOA = "(filh\\w*|mae|pai|irm\\w*|avo|vo|esposa|esposo|mulher|marido|namorad\\w*|companheir\\w*|sobrinh\\w*|net[oa]|entead\\w*|tia|tio|prima|primo|amig\\w*|bebe|crianca|madrasta|padrasto|sogr\\w*|cunhad\\w*|vizinh\\w*|aluno|aluna|menino|menina)";
    private static final String CONTEXTO_ABUSO = "(nao contar|nao conta|nao fala|nao falar|segredo|ninguem (saber|sabe)|escondid\\w*|quando (a mae|o pai|ninguem|a gente|eu) (nao )?(ta|esta|estou|to|sai)|sozinh\\w*|\\b([1-9]|1[0-7]) anos|padrasto|madrasta|tio|vizinho|primo|avo|entead\\w*|sobrinh\\w*|professor\\w*|treinador|baba|pastor|padre)";

    private static final Pattern SEXUAL = Pattern.compile(
        "\\bviolencia sexual|\\babuso sexual|\\babusad[oa] sexualmente|\\babus(ou|aram|ando|ava|avam) (de mim|dela|dele|da minha|do meu|da filha|do filho|da crianca|da menina|do menino|da sobrinha|da enteada|da neta|do neto)"
        + "|\\b(foi|fui|ser|sendo|era|tinha sido|foram) abusad[oa]s?\\b|\\babusad[oa]s? (pel[oa]|por|sexualmente|na|no|quando)\\b"
        + "|\\bestupr\\w*|\\bme (forcou|forcaram|obrigou|obrigaram|obriga|forca) a (transar|fazer sexo|fazer coisas|ter relac\\w*|fazer aquilo|ficar com ele|ficar com ela)"
        + "|\\b(ficou|transou|transaram|ficaram|abusou|fez|fizeram)\\b.{0,25}\\b(enquanto|quando) (eu |ela |ele )?(tava|estava|tavam|estavam) (apagad|desacordad|dormindo|bebad|dopad|inconscien|desmaiad)\\w*"
        + "|\\b(mexe|mexeu|mexendo|mexia|toca|tocou|tocando|tocava|passou a mao|passa a mao|passando a mao|passava a mao|pega|pegou|pegando|pegava|alisa|alisou|alisando|esfrega|esfregou|esfregando|encosta|encostando) (em mim|nela|nele|comigo|na minha filha|no meu filho|na minha|nas partes|nas partes intimas|no meu corpo|no corpo dela|no corpo dele|la embaixo)"
        + "|\\bacordei (sem roupa|pelad[oa]|nu|nua|sem calcinha|sem cueca)\\b|\\bpartes intimas\\b|\\bmexeu nas partes|\\bbolin(ou|aram|ando|ad[oa]|ar)\\b|\\bme apalp\\w*|\\bapalpad[oa]\\b|\\bencox\\w*"
        + "|\\bfui (tocad[oa]|apalpad[oa]|encoxad[oa]|bolinad[oa]) |\\b(me|nela|nele) (tocaram|tocou) sem\\b|\\btocad[oa] sem (eu |meu |o meu )?(querer|consentimento|permissao)"
        + "|\\b(obriga|obrigou|obrigava|obrigam|forca|forcou|forcava|forcam)\\b( me)? a (fazer|transar|ter relacao|ter relacoes|fazer sexo|fazer coisas?|fazer aquilo)\\b.{0,40}\\b(cama|sexo|transar|relacao|intim\\w*|partes|tirar a roupa|pelad[oa]|fotos?|video)"
        + "|\\b(pegou|pegaram|transou|transaram|ficou) (comigo |com ela |com ele |eu )?a forca\\b|\\bsem (meu|o meu) consentimento\\b"
        + "|\\b(mostrou|mostra|mostrando) (as |a )?partes? (pra|para|a|intimas?|dele|de baixo)\\b|\\bmostrou o (pinto|penis|pau|negocio|orgao|piu piu)\\b|\\bse masturb\\w* (na frente|pra|para)|\\bse exib\\w* (pra|para)"
        + "|\\b(prima|filha|sobrinha|irma|menina|neta|enteada|aluna|amiga) (de )?(1[0-3]|[5-9]) anos\\b.{0,30}\\bgravid\\w*|\\btenho (1[0-3]|[5-9]) anos e (to|estou|tou|fiquei) gravid\\w*");
    /** "mexe com ela" só é abuso com contexto (adulto da família, segredo, criança, ninguém em casa). */
    private static final Pattern TOQUE_COM = Pattern.compile(
        "\\b(fica |vive )?(mexe|mexeu|mexendo|mexia|pega|pegando|toca|tocando|passa a mao|passando a mao|alisa|alisando|se esfrega) (com|em) (ela|ele|a menina|o menino|a crianca|minha \\w+|meu \\w+)\\b");
    private static final Pattern CONTEXTO_ABUSO_P = Pattern.compile("\\b" + CONTEXTO_ABUSO);

    private static final Pattern FISICA = Pattern.compile(
        "\\bespanc\\w*|\\bagrid\\w* (eu|mim|me)\\b|\\bme agrid\\w*|\\bme agrediu|\\bagrediu (eu|ela|ele|minha|meu|a minha|o meu)|\\bfui agredid\\w*|\\bfoi agredid\\w*"
        + "|\\bme ameac\\w*|\\bameac\\w* (de morte|com faca|com arma|me matar)|\\bcoronhad\\w*"
        + "|\\b(me|nos|nela|nele|na minha mae|no meu filho|na minha filha) (bate|bateu|batem|bateram|socou|chutou|empurrou|enforcou|esganou|estrangulou|sufocou|espancou)\\b"
        + "|(?<!\\b(?:gato|cachorro|cao|cavalo|bicho|galo|boi|vaca|sol|oleo)) (me|nos) (machucou|machucaram|machuca|machucava|feriu|feriram)\\b"
        + "|\\bbate em mim\\b|\\bapanh\\w* (d[eoa]|do meu|da minha|em casa|na escola|na rua)"
        + "|\\bmarcas? de (cinto|cinta|dedo|dedos|mao|chinelo|mordida humana|fio|vara|corda|chicote|fivela)\\b|\\bmaus tratos\\b|\\bmaltrat\\w*"
        + "|\\b(sofro|sofri|sofre|sofreu|sofrendo|vitima de) (de )?(violencia|agressao|agressoes|abuso|maus tratos)\\b|\\bviolencia (domestica|familiar|em casa)\\b"
        + "|\\bapanh\\w* (na|no) (abordagem|delegacia|briga|festa|balada|saida|assalto|bar|onibus|trem|metro|trabalho)\\b|\\b(vive|fica|deixam|deixa|mantem) trancad[oa]\\b|\\b(deixa|deixam|deixou|deixaram|tranca|trancou|trancaram) (o |a )?(\\w+ ){0,2}trancad[oa]\\b"
        + "|\\b(tapa|tapas|tapao|soco|socos|chute|chutes|murro|murros|pontape|cotovelada|joelhada) (na|nas|no|nos|em) (cara|rosto|costelas|costela|barriga|cabeca|olho|boca|nariz|peito|minha|meu|nela|nele)\\b(?! (de|da|do) (luta|treino|judo|jiu|boxe|futebol|bola|jogo|academia))"
        + "|\\bamarra\\w* (o |a )?(meu |minha )?(avo|vo|pai|mae|filho|filha|crianca|idoso|idosa|na cama)|\\bcuidador\\w* (nao explica|bate|bateu|maltrata|amarra|grita|deixa sem comer)"
        + "|\\broxos? (e|que) (a|o) (cuidador\\w*|baba|creche|escola|padrasto|madrasta) nao explica|\\bmarcas? de mordida|\\b(apertou|aperta|apertaram) (o )?meu pescoco|\\btentou me enforcar|\\bsacud\\w* (o|meu|a|minha)? ?bebe\\b|\\bchacoalh\\w* (o|meu)? ?bebe\\b"
        // sprint 3: estrangulamento, agressão a outra pessoa, tapa "de leve", socorro, maus-tratos de idoso e criança
        + "|\\b(estrangul|esgan)\\w*|\\b(tentou|tentaram) (me |a |o )?(sufocar|enforcar)\\b|\\b(me|ela|ele|o bebe|a crianca) sufocou\\b"
        + "|\\b(bate|bateu|batem|bateram|batendo|bater|batia|soca|socou|socando|chuta|chutou|chutando|chutaram|espanca|surra|surrou|agride|agrediu|agredindo|empurra|empurrou) (em|n)(a|o|as|os)? ?(mim|ele|ela|eles|elas|gente|todo mundo|todos|nos)\\b"
        + "|\\b(bate|bateu|batem|bateram|batendo|bater|batia|soca|socou|chuta|chutou|espanca|agride|agrediu|empurra|empurrou) (em |n)(a|o|as|os) (minha |meu |minhas |meus |sua |seu )?" + PESSOA + "\\b"
        + "|\\b(levei|tomei|apanhei|levo|leva|levou|tomou) (uns?|umas?|um monte de|varios|varias|dois|duas|tres|alguns|algumas|mais uma?)? ?(tapa|tapas|tapinha|tapinhas|tapao|tabefe|tabefes|bofetada|bofetadas|porrada|porradas|murro|murros|socos?|chutes?|pancadas?|surra|sova|cintadas?|chineladas?|pauladas?|cascudos?|safanao|empurrao|empurroes)\\b"
        + "(?!.{0,40}\\b(treino|luta|jogo|jiu|muay|judo|boxe|karate|capoeira|futebol|bola|brincadeira|brincando|personagem|videogame|aula de)\\b)"
        + "(?=.{0,60}\\b((d[oa]|de) (meu |minha |seu |sua |um |uma |uns |umas )?(namorad\\w*|marido|esposa|esposo|mulher|companheir\\w*|ex|pai|mae|padrasto|madrasta|irmao|irma|filho|filha|tio|tia|cuidador\\w*|patrao|chefe|vizinh\\w*|cara|caras|homem|policia\\w*|seguranca|assaltante\\w*|bandid\\w*)|em casa|na rua|no assalto|na briga)\\b|.{0,3}$)"
        + "|\\b(me deu|me deram|me dava|me da) (uns?|umas?|um monte de|varios|varias|dois|duas|tres|mais uma?)? ?(tapa|tapas|tapinha|tapinhas|tapao|tabefe|tabefes|bofetada|bofetadas|porrada|porradas|murro|murros|socos?|chutes?|pancadas?|surra|sova|cintadas?|chineladas?|pauladas?|cascudos?|safanao|empurrao)\\b"
        + "(?!.{0,40}\\b(treino|luta|jogo|jiu|muay|judo|boxe|karate|capoeira|futebol|bola|brincadeira|brincando|personagem|videogame|aula de)\\b)"
        + "|\\b(socorro|help|me ajud\\w*|chama a policia|liga (pra|para) (a )?policia|190)\\b.{0,80}\\b(quebrando|batendo|gritando|arma|faca|invadi\\w*|arromb\\w*|trancad[oa]|me trancou|ameac\\w*|chutando|esmurrando|forcando a porta|escondid[oa])"
        + "|\\b(quebrando|batendo|gritando|arma|faca|invadi\\w*|arromb\\w*|trancad[oa]|me trancou|ameac\\w*|chutando|esmurrando|forcando a porta|escondid[oa])\\b.{0,80}\\b(socorro|help|me ajud\\w*|chama a policia|liga (pra|para) (a )?policia|190)\\b"
        + "|\\bgritando (socorro|por ajuda|pra parar)\\b|\\b(ele|ela) (ta|esta|tava) quebrando tudo\\b"
        + "|\\btrancad[oa] (pel[oa]|por|no quarto pel[oa])\\b|\\btranca (ele|ela|o idoso|a idosa|o vo|a vo|meu \\w+|minha \\w+) (no|na|em|dentro)\\b"
        + "|\\b(deixad[oa]|deixa|deixam|deixou|largad[oa]|abandonad[oa])\\b(\\s+\\w+){0,2}?\\s+sem (comida|comer|agua|banho|remedio|cuidado|limpar|trocar a fralda|trocar fralda)\\b"
        + "|\\b(pega|pegou|toma|tomou|fica com|ficou com|rouba|roubou|gasta) (a )?aposentadoria\\b"
        + "|\\b(roxo|roxos|roxa|roxas|hematoma\\w*|marcad[oa]|machucad[oa]|queimadura\\w*)\\b.{0,60}\\b(nao quer (falar|contar|dizer)|nao conta|nao explica|fica com medo|com medo (d[oa]|quando)|tem medo d[oa])"
        + "|\\b(tentou|tentaram) (levar|sequestrar|pegar) (ela|ele|minha \\w+|meu \\w+|a crianca|a menina|o menino)\\b|\\b(tentou|tentaram) me (levar|sequestrar)\\b|\\bsequestr\\w*"
        + "|\\b(levou|levei|tomou|tomei|deram|deu|meteu|meteram) (uma |um |umas |uns |duas |dois )?(facadas?|tiros?|navalhadas?|garrafadas?|pedradas?|pauladas?)\\b|\\besfaque\\w*|\\bbalead[oa]\\b"
        + "|\\bqueimadura\\w* de cigarro|\\b(foi|e) (o|a) (meu |minha |seu |sua )?" + PESSOA + " (que )?(bateu|bate|batia|machucou|machuca|empurrou|fez isso)\\b"
        + "|\\b(roxo|roxos|roxa|roxas|hematoma\\w*|marcas?|queimadura\\w*|machucad[oa]|fratura\\w*|quebrad[oa])\\b.{0,80}\\b(diz|disse|fala|falou|alega|jura) (que |q )?(foi )?(acidente|caiu|tropecou|bateu (na|no))\\b.{0,30}\\bmas\\b"
        + "|\\bme (agarrou|agarraram|puxou|puxaram|arrastou|arrastaram)\\b.{0,30}\\b(carro|mato|beco|forca|rua|quarto|pra dentro|cabelo)\\b|\\bme seguiu e\\b");

    /** Ameaça de outra pessoa. Lida no texto sem o humor tirado, porque "ele vai me matar" não é figura de linguagem. */
    private static final Pattern AMEACA = Pattern.compile(
        "\\b(ele|ela|meu marido|meu ex|meu companheiro|meu namorado|minha mulher|minha esposa|minha ex|meu pai|meu padrasto)\\s+(disse que |falou que |jurou que )?(vai|ia|quer) me matar\\b|\\bameac\\w* me matar"
        + "|\\b(disse|falou|jurou|ameacou|ameaca|mandou (msg|mensagem) dizendo|fica (dizendo|falando|mandando msg dizendo)|vive dizendo) (que |q )?(se eu \\w+( \\w+)? )?(ele |ela )?(vai |ia |vem |vai vir )?(me matar|me mata|mata eu|matar eu|matar a gente|mata a gente|matar (meus|os) filhos|matar as criancas|tacar fogo|botar fogo|por fogo|me pegar|vir me pegar|me achar|me furar|me dar um tiro|me bater|acabar comigo|sumir comigo|me queimar)"
        + "|\\bse eu (contar|terminar|sair|for embora|largar|denunciar|separar|falar|chamar a policia)\\b.{0,40}\\b(mat\\w*|fogo|bater|bate|pegar|acaba\\w*|sumir|tiro|furar|queimar)\\b"
        + "|\\b(mata|matar|matou) (eu|a gente|nos|nois|meus filhos|as criancas|os meninos|minha familia)\\b(?! de )"
        + "|\\bsabe onde (eu moro|eu trabalho|meus filhos estudam|a gente mora|moro)\\b"
        + "|\\b(tacar|botar|por|atear|jogar alcool e tacar) fogo (em mim|na gente|nos meus filhos|em nos|nela|nele|na casa|na minha casa)\\b"
        + "|\\b(ameacando|ameacou|ameacaram|ameaca) (a|o|minha|meu|todo|a gente|nos|eu|ela|ele)\\b.{0,30}\\b(arma|faca|revolver|pistola|facao|morte|matar)\\b"
        + "|\\b(apontou|apontando|apontaram) (uma |a )?(arma|faca|revolver|pistola)\\b");

    // =====================================================================================================
    // Convulsão descrita sem a palavra (sprint 3, item 5)
    // =====================================================================================================
    private static final String TREMOR = "(trem(i|eu|endo|ia|ido|ida|e|em)|sacud(i|iu|indo|ia)|se debat\\w*|debatendo|chacoalh\\w*|ficou dur[oa]|fiquei dur[oa]|enrijec\\w*|espasmo\\w*|se contorc\\w*|contorcendo|se batendo)";
    private static final String CORPO_TODO = "(inteir[oa]|o corpo todo|o corpo inteiro|todo|toda|bracos e pernas|no chao|duro|dura)";
    private static final String AUSENCIA = "(fora do ar|apag(ou|uei|ada|ado|ando)|desmai\\w*|desacordad\\w*|nao respond\\w*|olhos? (revirad\\w*|virad\\w*|vidrad\\w*|parad\\w*)|revir(ou|ando) os olhos|virou os olhos|olhar parado|olhar fixo|espum\\w*|bab(a|ando|ou)|mordeu a lingua|enrolou a lingua|fez xixi|urinou|perdeu os sentidos|perdi os sentidos|nao lembr\\w*|ausente|desligad[oa])";
    private static final Pattern CONVULSAO = Pattern.compile(
        "\\bestrebuch\\w*|\\b(ataque|crise) (epilet\\w*|de epilepsia|de bicho)\\b|\\bcrise convulsiva\\b"
        + "|\\b" + TREMOR + "\\b(?! de (frio|medo|nervoso|raiva|ansiedade|emocao|febre))(\\s+\\w+){0,2}?\\s+" + CORPO_TODO + "\\b.{0,80}(?<!\\b(?:nao|nem|sem) )\\b" + AUSENCIA
        + "|(?<!\\b(?:nao|nem|sem) )\\b" + AUSENCIA + "\\b.{0,80}\\b" + TREMOR + "\\b(?! de (frio|medo|nervoso|raiva|ansiedade|emocao|febre))(\\s+\\w+){0,2}?\\s+" + CORPO_TODO + "\\b"
        + "|\\bconvulsao\\b(?<!(remedio|remedios|medicamento|historico|historia|tratamento) (de|da|pra|para|contra) convulsao)");
    /** Só a forma pelo som ("com vulsão") ou a descrição; a palavra escrita certa já é do léxico. */
    private static final Pattern CONVULSAO_PALAVRA_CERTA = Pattern.compile("\\bconvuls\\w*");

    // =====================================================================================================

    /** O texto sem as figuras de linguagem comuns ("morrer de rir", "o trânsito vai me matar"); o resto fica igual. */
    public static String semFigurado(String texto) {
        if (texto == null) return null;
        String n = Normalizador.normalizar(texto);
        String sem = tirarFigurado(texto, " " + n + " ").trim();
        return sem.equals(n) ? texto : sem;
    }

    /** Analisa o texto livre (com ou sem acento, com ou sem pontuação). */
    public static Resultado analisar(String texto) {
        if (texto == null || texto.isBlank()) return new Resultado(false, false, false, false, false, false, false);
        String bruto = " " + Normalizador.normalizar(texto) + " ";
        String comSom = som(bruto);
        String n = tirarFigurado(texto, comSom);

        boolean suic = false, negado = true;
        Matcher m = SUICIDIO.matcher(n);
        while (m.find()) {
            suic = true;
            String antes = n.substring(Math.max(0, m.start() - 20), m.start());
            boolean temNaoDentro = m.group().startsWith("nao ") || m.group().startsWith("n ");
            if (temNaoDentro || !NEGACAO_ANTES.matcher(antes).find()) negado = false;
        }
        if (MARCA_ESCONDIDA.matcher(n).find()) { suic = true; negado = false; }

        boolean intox = INTOX.matcher(n).find() || REMEDIO_QTD.matcher(n).find()
            || (REMEDIO_OUTRO.matcher(n).find() && (Pattern.compile(CRIANCA).matcher(n).find() || ENGANO.matcher(n).find()))
            || (DROGA_USO.matcher(n).find() && (SINTOMA.matcher(n).find() || DROGA_DADA.matcher(n).find()))
            || ALCOOL_GRAVE.matcher(n).find();

        // ameaça: lida sem a figura "vai me matar" tirada, mas sem o exagero com humor ("meu marido vai me matar kkk")
        boolean ameaca = AMEACA.matcher(tirarHumor(texto, comSom)).find();
        boolean sexual = SEXUAL.matcher(n).find() || (TOQUE_COM.matcher(n).find() && CONTEXTO_ABUSO_P.matcher(n).find());

        boolean conv = CONVULSAO.matcher(n).find()
            && !(CONVULSAO_PALAVRA_CERTA.matcher(bruto).find() && !descricaoConvulsao(n));
        return new Resultado(suic, suic && negado, intox, PECONHENTO.matcher(n).find(),
            ameaca || FISICA.matcher(n).find(), sexual, conv);
    }

    /** Há descrição (tremor + ausência ou "estrebuchou"), não só a palavra. */
    private static boolean descricaoConvulsao(String n) {
        return CONVULSAO.matcher(n.replaceAll("\\bconvulsao\\b", " ")).find();
    }
}
