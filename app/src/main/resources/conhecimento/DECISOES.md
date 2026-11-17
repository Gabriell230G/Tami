# Decisões tomadas no caminho (sempre a opção mais cautelosa)

- D-001 (14/11/2026): sinais_alerta.csv ganhou as colunas `area` (A a J do mapa) e `revisar_medico`, para o painel de cobertura e para o validador marcar fonte única.
- D-002: remedios_classes.csv ganhou `fonte2_url`, `data_acesso` e `revisar_medico` (efeito grave também pede duas fontes).
- D-003: vinhetas_10k.csv usa `idade_meses` (igual ao motor) em vez de `idade`, e `sexo` = F/M.
- D-004: `vale_para` aceita vários grupos separados por `|` (bebe, crianca, adolescente, adulto, gestante, idoso, todos).
- D-005: `palavras_lexico` separa expressões por `|`; elas viram termos do léxico do motor na Etapa 2.

## D-006 · Como uma vinheta pode terminar em CUIDADO_EM_CASA (2026-11-14)
O caminho genérico (GEN_FIM) nunca termina em casa, e isso continua assim. CASA só sai de uma regra nova da base:
condição estudada com `cuidado_casa_seguro=SIM` reconhecida no texto + nenhum sinal de alerta + nenhum grupo de risco
(perfil ou fatores) + respostas leves no GEN (incômodo 0 a 3, sem piora rápida, sem febre). A regra se chama
`CO:<id>` e passa pela trava M12: sem aprovação médica vira UBS. O avaliador mede dos dois jeitos:
"com aprovação simulada" (o que a regra faria) e "produção hoje" (tudo aprovado = falso). Escolha cautelosa:
nada vai para casa sem um médico aprovar a regra.

## D-007 · Coluna palavras_lexico em condicoes.csv (2026-11-14)
Para o motor reconhecer a condição no texto, condicoes.csv ganha `palavras_lexico` (3 a 8 jeitos populares,
separados por `|`) logo depois de `sintomas_tipicos`. Mesmas regras de busca dos sinais (negação, erro de digitação).

## D-008 · Medida honesta das vinhetas (2026-11-14)
Cada lote de 500 é rodado no motor ANTES de qualquer ajuste de léxico ("primeira passada") e depois do ajuste.
O RELATORIO.md mostra os dois números. Ajustar o léxico até zerar não pode esconder que o texto livre falhou na
primeira vez; por isso também se separa um lote de 500 vinhetas de "teste cego" que nunca é usado para ajustar.

## D-009 · Vinhetas: escritas à mão + variações geradas (2026-11-14)
Escrever 10.000 relatos únicos à mão gastaria o estudo inteiro em texto repetitivo. Então: pelo menos 45% das
vinhetas são escritas à mão por pesquisadores (ids V-Lxx-, relatos naturais, armadilhas pensadas) e até 55% são
variações geradas por ferramentas/gerar_variacoes.py (ids V-G..-): digitação de celular, outro perfil (idoso,
criança, gestante) e fator de risco escondido. O destino das variações é RECALCULADO pelas regras da base, nunca
copiado; o que não dá para recalcular com segurança é descartado. O lote cego de teste (D-008) é só de vinhetas à mão.

## D-010 · sobe_um_nivel_se aceita grupo OU sinal no relato (2026-11-14)
Os pesquisadores escreveram em sobe_um_nivel_se tanto grupos (idoso, anticoagulado) quanto sinais ("bateu a cabeça",
"febre", "desmaio"). Os dois fazem sentido. O motor sobe um nível se a pessoa está num dos grupos/fatores OU se um
dos termos aparece no relato sem negação (mesma busca tolerante a erro de digitação).

## D-011 · Limite de buscas web (2026-11-14)
A cota de WebSearch da rodada (200 buscas, dividida entre todos os pesquisadores) acabou na Etapa 1. Linhas que
ficaram com uma fonte estão com revisar_medico=SIM. Daqui em diante: WebFetch direto em páginas índice conhecidas
das fontes permitidas (gov.br/saude "saúde de A a Z", nhs.uk/conditions, medlineplus.gov/ency) e o Chrome, que o
prompt liberou, só na sessão principal (abas novas, só leitura, fechadas ao fim de cada lote).

## D-012 · Sinal de risco à própria vida não é negável (2026-11-14)
Igual ao grupo SUICIDIO do léxico de emergência: "não quero mais viver", "não sei se vou me matar" disparam.
Ajuste: menção AFIRMATIVA ("vou me matar") dispara o sinal (SAMU_192 + CVV 188). Menção com "não" ("não vou me matar, só tô triste") não
dispara, mas manda direto para o roteiro CRI (FIL_08=S), que pergunta e decide, como o motor já fazia com o grupo SUICIDIO.
UPA_HOJE + CVV 188) ignoram a janela de negação. O roteiro CRI do motor continua sendo quem conduz a conversa.
Nenhuma linha da base traz método, só a ideia.

## D-013 · Sintoma solto fica com o roteiro (2026-11-14)
"dor no peito", "falta de ar", "febre", "dor de cabeça" sozinhos já abrem os roteiros do motor (FIL, FAR, FEB), que
fazem as perguntas certas. Na base de conhecimento, um sinal só dispara pela forma QUALIFICADA ("dor no peito que vai
pro braço", "dor no peito e suando"). Lista em ferramentas/sintomas_soltos.txt; o validador barra. Motivo: "dor no
peito ontem, hoje passou" ia direto para o SAMU sem pergunta nenhuma (sobretriagem), e o roteiro decide melhor.

## D-014 · Condições: uma coluna de fonte, segunda fonte conferida (2026-11-14)
condicoes.csv tem uma coluna de fonte (o formato do prompt). Os pesquisadores só marcaram revisar_medico=NAO quando
abriram uma segunda fonte de outra organização que concordava; as segundas fontes estão nos relatórios (FONTES.md).
Nomes de vacina: "tomou vacina da febre amarela" não pode virar piso de UPA; por isso as palavras_lexico dessas
condições usam "suspeita de / acho que é / tô com" em vez do nome sozinho.

## D-015 · Lista de confirmação de sinais (2026-11-14)
Texto livre sozinho não basta (1ª passada: 284 críticas em 900; com palavras-chave e raiz: 157). Nenhum app sério de
triagem depende só do texto. Depois do relato, o motor mostra até 8 sinais de alerta da base que mais se parecem
com o que a pessoa escreveu (busca aproximada por palavras com peso de raridade, normalizada pela raiz do tamanho do
sinal), só os que valem para o grupo dela, em linguagem simples: "Algum destes está acontecendo?". Se ela marca
"Nenhum", aparecem mais 8. Cada sinal marcado vira disparo SA:, com a subida de nível. A busca só PROPÕE: falso
positivo não vira sobretriagem, porque a pessoa não marca o que não tem. Na avaliação, a pessoa simulada é sincera:
marca o sinal que decide a vinheta se ele estiver na lista. O relatório mostra sempre as duas medidas
(só texto livre e com a lista). O expoente 0,5 foi escolhido por ser o padrão, não por ajuste no lote cego.

## D-016 · Condição reconhecida usa o destino_padrao como piso (2026-11-14)
Antes era o destino_minimo. As instruções das vinhetas (e a regra "na dúvida, o mais urgente") usam o destino_padrao
para o caso típico sem sinal de alerta. Exceção: condição de casa (padrão CUIDADO_EM_CASA) segue pela D-006.

## D-017 · O "eu" da subida de nível (2026-11-14)
A subida usa: grupos da idade, gestação, fatores do perfil (incluindo sexo), agravantes citados no relato (D-010),
idade calculada ("menor de 3 meses", "menor de 1 ano", "criança pequena" < 5 anos), semanas de gestação citadas
(< 37 ➜ "menos de 37 semanas") e sinônimos (tabagismo = fumante = "fumo"; arritmia = doença do coração = cardiopata;
transplante/HIV/quimio = imunossuprimido...). Assim "tabagismo" no perfil sobe um sinal que diz "fumante".

## D-018 · Ordem da lista de confirmação: região do corpo 3D e grupo (2026-11-14)
Sinais da grande região que a pessoa tocou (cabeça, peito, barriga, costas, braços, pernas) ganham peso 1,6; sinais
que valem só para um grupo (gestante, bebê, idoso...) ganham 1,5 quando a pessoa é desse grupo. Os pesos são valores
redondos escolhidos antes de medir, não ajustados no lote cego.

## D-019 · Relato de recuperação: condição citada é história (2026-11-14)
Se o relato fala de procedimento ou tratamento já feito (operei, cirurgia, tirei o apêndice, gesso, tipoia, tala,
prótese, pino, placa), o piso da condição citada (ex.: apendicite ➜ UPA) não é aplicado: a pessoa conta o que
já foi tratado. Os sinais de alerta (pós-operatórios e todos os outros) continuam valendo normalmente.
Também: a tolerância de 2 letras de erro de digitação só vale para palavras de tamanho parecido
("amarelando" deixou de casar com "amarelão").

## D-020 · Condições graves na lista de confirmação, pelos sintomas (2026-11-14)
Condições com destino padrão UPA ou SAMU (ex.: fratura do boxeador, fratura do punho, criança mancando sem trauma
claro) entram na lista de confirmação mostrando SÓ os sintomas típicos ("mão inchada depois de soco, nó do dedo
afundado"), nunca o nome da doença: a Tami não diagnostica. Marcada, vale o destino padrão da condição.

## D-021 · Palavra conhecida não é erro de digitação de outra (2026-11-14)
O vocabulário da base (todas as palavras das expressões, descrições e condições) é registrado no detector. Se a
palavra do texto existe escrita assim na base, ela não casa por tolerância de digitação com outro termo
("descendo a escada" deixou de virar "escara"; "de tanto" deixou de virar "tonto"). Flexões continuam casando
pelo radical (D-015).

## D-022 · Limite da pessoa simulada na lista de confirmação (2026-11-14)
Na avaliação, a pessoa simulada marca na lista só o id exato anotado em sinal_que_decide. Uma pessoa real também
marcaria itens equivalentes (ex.: relato de virose anotado com a condição CO-D-001; a lista mostra SA-D2-020
"diarreia leve, bebendo bem", que descreve o mesmo caso). Por isso parte da sobretriagem medida (sobretudo
casa ➜ UBS) é artefato da simulação. Escolha: NÃO afrouxar a simulação (inflaria o resultado); registrar o limite
no relatório. A subtriagem não é afetada por isso (a pessoa sempre marca o sinal grave que tem).

## D-023 · Agravantes escritos de forma abstrata (2026-11-14)
31 termos de sobe_um_nivel_se estavam com sublinhado ("esta_sozinho", "usou_alcool_ou_drogas") e nunca casavam.
Agora: sublinhado vira espaço; "a ou b" vale como alternativas (com singular e plural); e sinônimos de relato
ligam o agravante abstrato ao que a pessoa escreve (bebi, cerveja, maconha ➜ "usou álcool ou drogas";
"tô sozinha" ➜ "está sozinho"; "cachorro de rua" ➜ "animal de rua"). Achado na 1ª passada da Etapa 4.

## D-024 · Números de sinais vitais no texto livre (2026-11-14)
O ataque mostrou que "termômetro deu 38,4", "oxigênio 87", "182 batimentos", "glicemia 38" e "pressão 22 por 13"
passavam sem ser entendidos. Agora o motor (lexico/LeitorNumeros) põe, logo depois do número, as palavras que a base
já conhece, no mesmo trecho e sem tirar nada. Limiares cautelosos: febre a partir de 37,8 (alta a partir de 39,5);
saturação até 94; batimento a partir de 130 (ou até 40); glicemia até 70 ou a partir de 250 ou "HI"; pressão a partir
de 14 por 9 (alta), de 18 por 11 (muito alta), ou máxima até 9 (baixa). Cada pedaço do texto é lido por uma regra só,
e nenhuma palavra que o texto já tem ali perto é repetida. "39sem" e "32 sem," viram "semanas", porque "sem" solto
era lido como negação ("sem bolsa estourou verde").

## D-025 · Agravantes ditos com outras palavras (2026-11-14)
agravantes_sinonimos.csv (dado, não código): "batendo queixo de frio" é calafrio, "saiu sangue" é sangramento,
"cabeça latejando" é dor de cabeça, "bolsa estourou verde" é líquido verde, e assim por diante (55 linhas). Só serve
para subir um nível de um sinal que já foi achado, então o risco de exagero fica contido. O validador confere que cada
agravante citado existe em algum sobe_um_nivel_se. Também entram no normalizador: diminutivo que minimiza
("dorzinha" ➜ dor, "bucho" ➜ barriga) e "estorou" ➜ "estourou".

## D-026 · Febre em quem tem imunidade baixa (2026-11-14)
A trilha FEB do adulto não pergunta sobre transplante, quimioterapia ou imunossupressor, então febre nessas pessoas
terminava na UBS. Agora, com febre no texto e imunidade baixa (pelo perfil ou pelo texto, com os sinônimos da D-017),
o sinal SA-I-016 entra direto (UPA). Também: os sinais que só valem para imunossuprimido aceitam a expressão sem a
palavra do grupo ("transplantado sem fazer xixi hoje" ➜ "sem fazer xixi hoje"), como já era para gestante e idoso;
SA-G-044 passou a valer só para imunossuprimido.

## D-027 · Lista de confirmação: o mais urgente pesa mais na disputa por lugar (2026-11-14)
Cada sinal novo disputa lugar na lista (8 itens por página, 2 páginas). Com os 29 sinais da Etapa 5, sinais graves
parecidos com o relato foram empurrados para fora (+6 críticas). Escolha cautelosa: a nota do candidato é
multiplicada por 1,3 (SAMU), 1,15 (UPA), 1,0 (UBS) ou 0,85 (casa). Nada muda sozinho: a pessoa ainda confirma.
Efeito: críticas 225 ➜ 202, lote cego 52 ➜ 47. Custo: sobretriagem 17,7% ➜ 18,3%, toda ela casa ➜ UBS (item de
casa que saiu da lista); em produção a sobretriagem não mudou (3.067), porque lá casa já vira UBS (trava M12).

## D-028 · Membro frio e sangramento depois de cirurgia (2026-11-14)
O revisor E5 achou contradição: membro frio ou roxo depois de lesão ou cateterismo era SAMU (SA-C-020, SA-G-029),
mas depois de cirurgia no joelho ou no ombro era UPA (SA-G-169, 170, 172, 174, 176); sangramento que não para
apertando 10 min era SAMU (SA-A-004), mas no corte da cirurgia, 15 min, era UPA (SA-G-008). Na dúvida, o mais
urgente: as 6 linhas subiram para SAMU, com revisar_medico=SIM. 10 vinhetas que dependiam delas subiram junto.

## D-029 · Sprint de robustez (14/11/2026, 22:41 ➜ 23:41)
Achados das frentes de teste (PROMPT_SPRINT_1H.md) e o que mudou, sempre pelo lado mais cauteloso:
- Risco de suicídio achado pela base agora exige o CVV 188 e abre o roteiro de crise (FIL_08); intoxicação achada
  pela base mostra o CIATox. Léxico de ideação, intoxicação e violência sexual ampliado; SA-A-072 vale para
  adolescente; SA-A-079 (criança comeu planta) vale para todos, porque quem escreve muitas vezes é o adulto.
- Novo sinal SA-A-090: agressão física (UPA, sobe para SAMU com pancada na cabeça, pescoço apertado, sangramento,
  desmaio, gestante, criança, bebê ou idoso). Fontes NHS e Ligue 180; revisar_medico=SIM.
- 45 sinais de SAMU ou UPA que valiam para adulto e não para idoso passaram a valer para idoso (o idoso de 70 anos
  caía para a UBS com o mesmo relato grave). Sinais de gravidez, parto e criança ficaram de fora.
- Grupo de risco das condições compara com o "eu" completo (transplante, quimio, anticoagulante, doença crônica),
  não só com idade, gestação e diabetes. "quimio" virou sinônimo de imunidade baixa.
- Sinal de casa com sinal de alarme dito no relato vai para a UPA (antes só subia para a UBS).
- Auditoria dos sinais de casa: 115 expressões perigosas tiradas do léxico, 390 agravantes acrescentados, 42 grupos de
  risco acrescentados, 8 vale_para restringidos (sem bebê), CO-D-025 (inchaço na gravidez) deixou de ser casa.
  SA-D2-023 continua valendo para criança de 2+ (tirado só o bebê). 6 vinhetas subiram para ficar coerentes.
- "37,8" não quebra mais a frase na vírgula; "grávida de 30 sem" vira semanas; expressão que já fala do passado
  ("desmaiei no treino mês passado") não é anulada pelo "mês passado".
- Léxico: +767 expressões em 60 sinais e +1.122 sinônimos de agravantes, escritos sem ver casos de teste.

## D-030 · Lista de confirmação ordenada por BM25 (sprint, 14/11/2026)
A nota de cada candidato passou a ser BM25 (o sinal como documento) × (1 + cobertura da melhor expressão do sinal)
+ 0,5 × pares de palavras vizinhas do relato que aparecem juntas numa expressão. Os bônus de região, grupo e urgência
(D-018, D-020, D-027) continuam. Escolhida entre 17 variações pelo lote cego antigo (ExperimentoRanking.java,
_revisao/RESULTADO_RANKING.md): o sinal certo das vinhetas graves aparece entre as 8 primeiras opções em 91,6% (era
86,4%) e entre as 16 em 95,4% (era 92,7%), sem aumentar a sobretriagem. Por ter sido escolhida com o lote cego antigo,
ele deixou de ser cego para esta decisão: a medida honesta é o lote V-Z (300, nunca usado para nada).

## D-031 · Detector de risco por raiz e contexto (sprint 2, 14/11/2026)
A crise medida com frases CEGAS (escritas por um agente que não viu o léxico) mostrou que o 100% do sprint 1 era
ajuste: só 37,5% das ideias de suicídio chegavam ao CVV, 45,7% das intoxicações ao CIATox e 60% das violências à UPA.
O léxico só reconhece a frase exata. Agora lexico/DetectorRisco olha a raiz e o contexto: qualquer "suicid...",
desejo + morrer/matar, "tirar a própria vida", "dar um fim nisso", "não aguento mais viver", carta de despedida,
autolesão em qualquer tempo de verbo; verbo de ingerir perto de produto tóxico, remédio trocado ou dobrado, planta,
veneno aplicado na lavoura, "colocaram algo na minha bebida", gás; bicho peçonhento perto de picar ou morder; e
violência física e sexual dita de forma indireta, incluindo maus-tratos a idoso e criança. Antes de tudo, o sentido
figurado comum sai do texto ("morrer de rir", "o trânsito vai me matar", "a prova vai acabar comigo"). Suicídio
afirmado ➜ UPA com CVV 188 e roteiro de crise; negado ➜ só o roteiro de crise, que pergunta. Intoxicação ou bicho ➜ UPA
com CIATox. O detector lê o texto INTEIRO, mesmo quando a base lê só o começo e o fim.

## D-032 · Revisão de código do sprint 1 (sprint 2)
CVV 188 e CIATox nunca se perdem no resultado, mesmo quando outro disparo dá SAMU. Uma frase afirmada depois vence a
mesma negada antes ("não quero me matar. quero sim"). Indexação da lista segura entre threads. Fatores nulos não
derrubam o motor. "Faz anos" não nega o trecho quando o relato diz "agora", "hoje" ou "ontem". Leitor de números sem
falsos positivos: "mesmo" não é "mês"; "minha mãe de 40", "tenho 40, 3 filhos", "fazendo 35 graus", "30 dias",
"satisfeita com 90", "23:40", "paguei 20 x 12" e "de 8 em 8 horas" não viram sinal vital. Temperatura baixa lida
pelo termômetro ("termômetro deu 34"). "doença crônica" deixou de misturar os grupos de sinônimos. O regex que liga
o CIATox pelo termo médico não pega mais "ingestão".

## D-033 · Auditorias aplicadas (sprint 2)
Auditoria de casa (MÉDIA e BAIXA) e auditoria dos sinais de UBS (ALTA e MÉDIA) aplicadas por
ferramentas/aplicar_auditoria.py, que só SOBE destino e nunca tira idoso, gestante ou adulto do vale_para. Toda linha
mudada ficou revisar_medico=SIM. 40 vinhetas subiram de destino para ficar coerentes com a base mais cautelosa:
por isso os números de antes e depois do sprint 2 não são exatamente comparáveis; os lotes cegos V-Z e V-Y são.
16 sinais novos com fonte: temperatura baixa (bebê, idoso, todos), "ferroada" sem ver o bicho, maus-tratos em
criança (roxos, bebê chacoalhado, queimadura com marca, criança que conta), psicose depois do parto, dengue no
grupo B do Ministério da Saúde.

## D-034 · Sprint 3: lote cego escrito antes das correções
O cego 3 (378 vinhetas V-N e 400 frases de crise CN) foi escrito antes de qualquer correção e revisado contra a base
congelada da v7 (/tmp/v7-src), para o gabarito não sair das próprias correções. Ninguém que corrigiu abriu o lote.
A partir do sprint 4 ele vira material de ajuste; um cego 4 precisa ser escrito antes.

## D-035 · Detector de crise por categoria (F1)
Léxico novo por categoria (raiz + contexto), sem copiar frases. Humor tira só o exagero; com sinal de que é sério,
continua risco; "vou me matar kkk" sozinho continua risco. Gírias que soavam emergência saem antes da base ler.
Detalhes F1-1 a F1-5 em _revisao/sprint3_decisoes_F1.md.

## D-036 · Lista já marcada, glicose e C2/C3 (F2)
Só o item do topo da lista vem marcado, e só se for SAMU/UPA, com expressão inteira e afirmada no relato; desmarcar
SAMU abre CONF_DESMARCOU ("Não sei" vale sim). Glicose ≥ 300 com sintoma ➜ SA-D1-102; ≥ 300 sozinha ➜ UBS;
≤ 60 com confusão ➜ SAMU. C2 estreita ("achei que era") e C3 com proteção de "agora". Convulsão do detector ligada.
Detalhes em _revisao/sprint3_decisoes_F2.md.

## D-037 · Texto e voz (F3)
Número falado vira dígito; muletas saem; negação para no próximo sintoma, em 4 palavras ou em " / "; verbos de
intenção antes de "me/se" deixam a negação passar ("não vou me matar" fica negado); "quase" não nega. Grafia pelo
som por tabela e por chave de som, sem trocar palavras que existem (caiu/saiu fica para a tela de conferência).
Detalhes F3-01 a F3-15 em _revisao/sprint3_decisoes_F3.md.

## D-038 · Base (F4) e telas (F5)
SA-D1-102 (glicose muito alta) com NHS e CDC; 23 sinais novos para as lacunas do V-H, todos revisar_medico=SIM;
catapora UPA nas duas linhas; SA-D3-057 só com o próprio sujeito. 7 vinhetas das 10.000 subiram de UBS para UPA
pela catapora. Telas: só texto e rótulo (opcoes.csv, coluna rotulo); códigos e efeitos iguais. Detalhes em
_revisao/sprint3_decisoes_F4.md e sprint3_decisoes_F5.md.

## D-039 · F3 não foi revertida
No cego 3 a F3 resolveu V-N-0025 e criou V-N-0265 (saldo zero em críticas, +1 sobretriagem), e melhorou o só texto
(91 ➜ 83 com F3; 90 sem) e a voz (pesado 7 ➜ 2). Pela regra do sprint, só se reverte frente que piora um cego;
saldo zero não é piora. V-N-0265 fica como regressão conhecida para o sprint 4.

## D-040 · Crise não pode depender só do texto
Três rodadas de frases cegas mostram que o detector por palavras aprende as frases que vê (100% no ajuste, 61 a 76%
no cego 3). Proposta para o sprint 4: pergunta de segurança fixa no começo, com botões, para todo mundo; o detector
vira segunda camada.
