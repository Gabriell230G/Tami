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
