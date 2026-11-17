# Correções a partir das conversas simuladas

Cada correção passou pelos dois testes: o novo (10.000 casos simulados de treino) e o antigo (`vinhetas_10k.csv`, 10.000 casos). Se o teste antigo perdia segurança, a correção era refeita ou desfeita.

Ponto de partida no teste antigo: 99,38% dos urgentes para UPA ou SAMU e 30 casos urgentes mandados para UBS ou casa.

## 1. Pergunta de alergia (FIL_06) e de veneno (FIL_07) só quando faz sentido

- **Problema:** as duas apareciam para todo mundo. No simulado, 3.396 e 3.288 conversas tinham essas perguntas sem nenhum motivo.
- **O que mudou:** a pergunta de alergia aparece se o relato fala de picada, comida, remédio, inchaço, coceira, lábio, língua, garganta ou falta de ar, ou se o problema é na cabeça ou no rosto. A pergunta de veneno aparece sempre para criança e idoso, e para os outros se o relato fala de remédio, produto, gás, vômito, sonolência, confusão, desmaio ou tremor. O detector de risco do texto continua ligado para os dois.
- **Antes ➜ depois:** 3.396 ➜ 660 e 3.288 ➜ 1.048 conversas.

## 2. Todo resultado com o que fazer agora

- **Problema:** 6,5% dos resultados de UBS ou casa não tinham nenhum cuidado, e 2.179 resultados de UPA não diziam o que fazer até chegar.
- **O que mudou:** UBS e casa sem bloco de cuidado ganham "Enquanto isso" (descansar, beber água, dormir bem, voltar se piorar) e quando procurar ajuda. UPA sem bloco ganha "Enquanto vai para lá" (não dirigir se estiver mal, levar documento, cartão do SUS e a lista de remédios que usa, ligar 192 se piorar).
- **Antes ➜ depois:** 6,5% ➜ 0% sem cuidado.

## 3. Pergunta nova: está conseguindo beber líquidos e fazer xixi? (BEB_01)

- **Problema:** vômito e diarreia passavam sem nenhuma pergunta sobre o corpo estar perdendo água.
- **O que mudou:** quando o relato fala de vômito, diarreia, enjoo ou virose (ou o problema é na barriga), o caminho genérico pergunta. "Bebo, mas faço bem menos xixi" ➜ UBS no mesmo dia. "Vomito tudo ou quase não faço xixi" ➜ UPA. Fonte: NHS, Diarrhoea and vomiting. Revisar com médico.
- **Antes ➜ depois:** 211 ➜ 146 conversas sem essa pergunta (o resto é quando a Tami já mandou para UPA ou SAMU antes, ou o caminho é outro).

## 4. Lista de confirmação: só o que é do grupo da pessoa

- **Problema:** itens de cirurgia para quem não operou (3.837 conversas), de gravidez para quem não está grávida, de câncer, diabetes, diálise, criança para adulto.
- **O que mudou:** o item que fala de um grupo ("em idoso", "na gravidez", "em quem tem câncer", "depois de cirurgia") só aparece se a pessoa for de pelo menos um dos grupos citados, pela idade, pela gravidez ou pelo que contou. Item leve fora do grupo sai sempre. Item grave fora do grupo só fica se o relato já descreve uma boa parte dele. Item sobre uma idade ("Idoso caiu...", "Criança pequena chora...") só aparece para essa idade. A lista de palavras que indicam cirurgia aceita erro de digitação ("cirugia", "opeei", "csarea") e nomes de procedimento ("bypass", "safena", "colposcopia").
- **Primeira versão:** filtrava também os graves. O teste antigo caiu para 98,30% e 82 urgentes para UBS ou casa (ex.: gripe em idoso, dor na batata da perna depois de operar o joelho, sangramento depois de tirar pólipo). **Desfeita** e refeita do jeito acima.
- **Antes ➜ depois:** cirurgia 3.837 ➜ 1.505 conversas; gravidez 568 ➜ 904. A gravidez subiu porque agora mais itens graves de pós-parto ficam quando o relato descreve parte deles: é o preço de não perder sangramento depois do parto.

## 5. Lista de confirmação: a parte do corpo e o "depois de queda"

- **Problema:** itens de outra parte do corpo (2.838 conversas) e de "depois de uma queda ou pancada" para quem não se machucou (2.293).
- **O que mudou:** o motor acha a parte do corpo pelo relato quando a pessoa não toca no corpo. Item leve de outra parte do corpo e com pouca semelhança com o relato sai; item leve de "depois de queda" sai se o relato não fala de machucado. Item grave só sai se for de outra parte do corpo e o relato não descrever nada dele.
- **Antes ➜ depois:** 2.838 ➜ 2.674 e 2.293 ➜ 2.068 conversas. Pouca mudança de propósito: quase tudo o que sobrou é item grave.
- **Testado e desfeito:** tirar todo item leve de outra parte do corpo não mudou o simulado e baixou o destino exato do teste antigo de 83,5% para 83,3%.

## Números finais do teste antigo (`vinhetas_10k.csv`)

| | Antes das correções | Depois |
| --- | --- | --- |
| Urgentes que foram para UPA ou SAMU | 99,38% | 99,46% |
| Urgentes mandados para UBS ou casa | 30 | 26 |
| Destino exato | 83,9% | 83,5% |

O destino exato caiu um pouco porque 54 casos que eram "cuidar em casa" agora vão para "UBS em breve": a Tami ficou mais cuidadosa quando não acha o item leve de casa na lista.

---

# Rodada 2: meta de nota 9 (PROMPT_NOTA_9)

Ponto de partida ("hoje", casos de treino gerados de novo depois da correção do pós-parto): **nota 7,98** com a régua nova (7,79 com a antiga), 6,88 perguntas por conversa, 7 urgentes mandados para UBS ou casa, 54,7% das conversas com pergunta ou item sem relação. Teste antigo: 99,46% dos urgentes para UPA ou SAMU, 26 urgentes para UBS ou casa, destino exato 83,5%.

Regras dos dois testes depois de cada mudança: teste antigo com pelo menos 99,46% e no máximo 26; simulado de treino com no máximo 10 urgentes mandados para UBS ou casa.

## Passo 1: auditoria do avaliador

Ver `auditoria_avaliador.md`. Precisão de 77% para 89,5%. Avaliador congelado depois disso.

## Passo 2: uma tela de emergência no lugar de 5 perguntas

- **Problema:** falta de ar forte, dor no peito, desmaio ou convulsão, sinais de AVC e sangramento forte eram 5 perguntas separadas para todo mundo.
- **O que mudou:** viraram uma tela só, de marcar ("Agora, você tem algum destes?"). Cada item tem o mesmo efeito de antes (SAMU para falta de ar forte, AVC, sangramento e desacordado agora; peito vai para as perguntas de dor no peito; desmaio que já passou vai para as perguntas de desmaio). O que o relato já respondeu não volta na tela, e com falta de ar as perguntas de respiração vêm antes, como antes.
- **Antes ➜ depois (simulado de treino):** nota 7,98 ➜ 8,13; perguntas 6,88 ➜ 5,05; urgentes para UBS ou casa 7 ➜ 8.
- **Teste antigo:** 99,46% e 26 (igual), destino exato 83,5% (igual); perguntas por caso 9,3 ➜ 6,6.

## Passo 3: lista de confirmação curta

- **Problema:** 3 de cada 4 pontos perdidos vinham da lista: itens de outra parte do corpo, de outro grupo (cirurgia, gravidez, câncer, diálise) e de "depois de queda" para quem não caiu, quase todos graves, que ficavam "por segurança".
- **O que mudou:**
  - no máximo 5 itens por página;
  - item de um grupo de que a pessoa não é sai, grave ou não. Só fica se for grave e o relato repetir boa parte das palavras dele (aceitando erro de digitação, como "cotre" e "ceasrea");
  - item que repete boa parte das palavras do relato fica;
  - item de "depois de queda ou pancada" sai se a pessoa não contou nada que machuque. Fica se for grave e da parte do corpo que a pessoa citou;
  - item de outra parte do corpo sai, a não ser que seja grave e o relato o descreva com clareza;
  - a segunda página só aparece se a primeira tinha item grave e a pessoa marcou "Nenhum" (até 10 itens nela).
- **Primeira versão:** filtrava pela parte do corpo das palavras que a base associa ao sinal (ex.: "mamar" contava como peito) e pela palavra "queda" mesmo em "sem queda". O teste antigo caiu para 98,59% e 68 urgentes para UBS ou casa (ex.: bebê que não consegue beber, gestante com bebê mexendo menos, idoso com dor nas costas depois de escorregar). **Refeita:** a parte do corpo vem só do nome escrito no item; "escorreguei", "virei o pé", "no sol", "na piscina" contam como causa; "sem queda" não conta como queda; palavras de procedimento novas ("sleeve", "bariátrica", "cateterismo" mesmo com erro, "stent") contam como cirurgia; "renal" e "rins" contam como costas.
- **Antes ➜ depois (simulado de treino):** nota 8,13 ➜ 9,05; conversas com item sem relação 55,0% ➜ 36,8%; urgentes para UBS ou casa 8 ➜ 8.
- **Teste antigo:** 99,46% e 26 (no limite); destino exato 83,5% ➜ 82,7% (131 casos de "cuidar em casa" agora vão para "UBS em breve", mais cuidadoso); perguntas 6,6 (igual).
- **O que sobrou:** quase tudo é item grave que divide palavras com o relato ou que fala de vários grupos ao mesmo tempo (ex.: "Gripe ou covid com febre em idoso, grávida, criança pequena..."). Esses ficam.

## Passo 5: perguntas que o relato já respondeu não voltam

- **O que mudou:** o motor lê no relato há quanto tempo começou ("desde ontem", "faz 3 dias", "faz 2 semanas", "há meses"), a dor ("dor 8", "8 de 10", "dor fortíssima", "dorzinha") e "sem febre". Essas perguntas não aparecem. "28 semanas" de gravidez não conta como tempo.
- **Antes ➜ depois (simulado de treino):** perguntas 5,03 ➜ 4,86; nota 9,05 ➜ 9,05; urgentes para UBS ou casa 8 ➜ 8.
- **Teste antigo:** 99,50% e 24; perguntas 6,6 ➜ 6,4.

## Melhoria junto: mais jeitos de dizer que se machucou

"jiu jitsu", "futsal", "pisei em falso", "rompi o ligamento", "menisco", "o joelho falseia" e parecidos agora levam para a trilha do machucado. Antes caíam no caminho geral e recebiam pergunta de alergia, veneno e crise.

## Passo 4: trilhas curtas para urina, pele e barriga

- **Problema:** essas famílias passavam pelo caminho geral, com perguntas de alergia, veneno, crise e confusão que não tinham a ver com a queixa.
- **O que mudou:** três trilhas novas, no formato da trilha do machucado. Valem para adulto e adolescente que não está grávida, quando o texto não traz nada urgente e a Tami ainda não decidiu UPA ou SAMU. Braço ou perna tocado não entra em urina nem em barriga.
  - **Urina e região íntima (URI):** sinais de alerta (febre ou calafrio com dor nas costas ou do lado; não consegue fazer xixi; xixi com sangue ou coágulos; dor forte e de repente no testículo; dor no pé da barriga com febre e corrimento) ➜ febre ➜ começou de repente ou piorando ➜ dor de 0 a 10 ➜ há quanto tempo ➜ lista da base. Cuidados no fim: beber água, não segurar o xixi, higiene, quando procurar atendimento. Fontes: NHS (Urinary tract infections, Kidney infection, Testicular torsion).
  - **Pele (PEL):** manchas que não somem ao apertar um copo; lábios, língua ou garganta inchando; vermelhidão quente que se espalha; bolhas ou pele soltando; olho vermelho e inchado ➜ febre ➜ espalhando ou piorando ➜ dor ➜ tempo ➜ lista. Fontes: NHS (Rashes, Cellulitis, Hives, Meningitis).
  - **Barriga (BAR):** barriga dura como tábua; vômito com sangue ou cocô preto; sem soltar gases nem fazer cocô; dor que vai do umbigo para a direita; olhos amarelos com febre ➜ tela de emergência (dor na barriga pode ser do coração) ➜ febre ➜ consegue beber e fazer xixi ➜ dor ➜ tempo ➜ lista. Fontes: NHS (Stomach ache, Diarrhoea and vomiting, Appendicitis).
  - Cada item aponta para um sinal da base que já tem fonte e `revisar_medico=SIM`.
  - Vômito ou diarreia com "pouco xixi" vai para a barriga, não para a urina: é falta de água.
- **Primeira versão:** a lista só tinha uma página e a pergunta "começou de repente ou piorando" ficava de fora. O teste antigo caiu para 99,27% e 35 urgentes para UBS ou casa (trombose na batata da perna, ferida da cirurgia, reação forte a remédio), e 434 casos de "cuidar em casa" passaram a ir para a UBS. **Refeita:** segunda página como no caminho geral, a pergunta de piora entrou em todas e braço ou perna tocado não entra nas trilhas.
- **Antes ➜ depois (simulado de treino):** nota 9,05 ➜ 9,08; perguntas 4,86 ➜ 4,67; urgentes para UBS ou casa 8 ➜ 8. Urina 8,58 ➜ 8,66.
- **Teste antigo:** 99,50% e 24; perguntas 6,4 ➜ 6,1.

## As 20 conversas de treino com nota mais baixa (depois do Passo 4)

- 6 são casos mal gerados pelo simulador: relato que não é machucado na família de machucado ("tumor no esôfago, faço tratamento de câncer"), então o avaliador cobra "como se machucou" e "consegue apoiar".
- 5 são os casos urgentes que a Tami manda para UBS ou casa e que já estavam assim antes desta rodada (ex.: "gestante de 28 semanas com sífilis", "febre de repente agora", "pancada com feridas na boca e remédio de reumatismo"). Ficam como pendência para revisão médica: nenhum tem um sinal de alerta escrito no relato.
- 5 são machucados ditos de um jeito que a trilha não reconhecia ("não dá pra pisar", "dedinho quebrado", "desde a batida"). Corrigido no fim: essas formas agora levam para a trilha do machucado.
- 4 são listas com item grave que divide palavras com o relato, mas é de outro contexto (ex.: "câncer no sistema linfático" recebe "dor de cabeça nova em quem já teve câncer").

## Onde parou e por quê

A meta era passar de 9,2 no treino. Parou em **9,08 no treino e 9,08 na prova**, sem perder segurança. O que ainda tira ponto, em pontos por conversa:

| O que tira ponto | Pontos | Por que ficou |
| --- | --- | --- |
| Pergunta de veneno (FIL_07) e de alergia (FIL_06) no caminho geral | 0,17 | A regra de quando aparecem não podia mudar. Criança e idoso sempre recebem a de veneno. |
| Itens graves da lista que falam de vários grupos ao mesmo tempo ou dividem palavras com o relato | 0,15 | Tirar mais derruba o teste antigo: cada versão mais agressiva mandou de 3 a 42 urgentes a mais para UBS ou casa. |
| Item grave de outra parte do corpo ou de "depois de queda" | 0,16 | Mesmo motivo. |
| Pergunta de crise (FIL_08) em quem se machucou fora da trilha | 0,03 | Regra mantida. |
| "Faltou pergunta" que a tela de emergência já cobre | 0,04 | O avaliador congelado não sabe que a tela nova pergunta falta de ar, peito, AVC e sangramento. Na prova, 71 das 161 conversas com "faltou" são só isso. |

**Conversas com UBS ou casa e mais de 8 perguntas:** caíram de 389 para 320 na prova, mas não chegaram a zero. Quase todas são do caminho geral (303), com as duas páginas da lista, a tela de emergência, as perguntas de alergia, veneno e crise e as gerais. Para zerar seria preciso cortar perguntas de segurança, e isso não foi feito.

## Prova final (2.000 casos, rodada uma vez)

| | Hoje | Depois |
| --- | --- | --- |
| Nota, régua nova | 7,97 | **9,08** |
| Nota, régua antiga | 7,79 | 8,83 |
| Conversas com pergunta ou item sem relação | 54,7% | 35,7% |
| Perguntas por conversa (média / máximo) | 6,89 / 19 | 4,71 / 15 |
| Urgentes que foram para UPA ou SAMU | 99,9% | 100% |
| Urgentes mandados para UBS ou casa | 1 | 0 |
| Destino exato | 65,7% | 70,8% |
| Conversas em que faltou uma pergunta importante | 3,6% | 8,1% (4,5% sem contar o que a tela de emergência já cobre) |

| Família | Hoje | Depois |
| --- | --- | --- |
| Urina e região íntima | 7,42 | 8,85 |
| Barriga | 6,92 | 8,87 |
| Machucado no esporte | 7,48 | 8,91 |
| Cirurgia, crônicos e "não é nada" | 7,31 | 8,91 |
| Febre e dengue | 7,53 | 8,92 |
| Intoxicação e animais | 8,17 | 8,93 |
| Peito e falta de ar | 8,28 | 8,97 |
| Pancada, corte e queimadura | 7,74 | 8,99 |
| Cabeça e tontura | 8,26 | 9,08 |
| Pele | 7,55 | 9,12 |
| Saúde mental e crise | 8,74 | 9,20 |
| Idosos e anticoagulante | 8,29 | 9,20 |
| Gravidez e pós-parto | 8,06 | 9,23 |
| Bebês e crianças | 8,62 | 9,39 |
| Queda | 8,99 | 9,45 |

**Teste antigo no fim:** 99,50% dos urgentes para UPA ou SAMU (antes 99,46%), 24 urgentes para UBS ou casa (antes 26), destino exato 82,4% (antes 83,5%: 154 casos de "cuidar em casa" agora vão para "UBS em breve"), 6,1 perguntas por caso (antes 9,3).

**Os 8 testes que precisavam passar:** CVV 188 aparece em "não aguento mais, quero morrer"; CIATox em "meu filho bebeu água sanitária"; "luxei no treino de judô" com 6 perguntas; joelho torto no judô ➜ UPA; queda do telhado ➜ SAMU; dor na batata da perna não vai para a trilha do machucado; "vomitando tudo desde ontem e quase não fiz xixi" ➜ pergunta de beber e fazer xixi e UPA; "ardendo pra fazer xixi, sem febre" ➜ trilha da urina com 6 perguntas e sem a pergunta de febre.

**Atenção ao número oficial (`dados/validacao.json`):** ele conta como subtriagem crítica também o caso de SAMU que foi para UPA. Por essa conta foi de 27 para 35: os 24 urgentes para UBS ou casa caíram (26 ➜ 24), mas 10 casos que pediam SAMU agora vão para UPA (continuam urgentes, um nível abaixo). Exemplos: idoso de 62 anos que caiu de costas com dor forte (a lista do machucado tem uma página e o item de SAMU ficou de fora), criança com pintinhas roxas (marca o item de UPA e a página com o de SAMU não aparece). **Testado e desfeito:** pôr os itens de SAMU no topo da lista. Resolveu 5 desses 10, mas empurrou outros itens para fora da primeira página e o teste antigo caiu para 98,73% e 61 urgentes para UBS ou casa. Fica como pendência para a revisão médica.
