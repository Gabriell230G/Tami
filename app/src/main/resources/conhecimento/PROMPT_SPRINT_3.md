# Prompt · Sprint 3 da Tami: corrigir o que a "gente de verdade" quebrou (15/11/2026)

**Objetivo.** Aplicar a lista priorizada de `_revisao/RESULTADO_TESTE_HUMANO.md` (seção 7) e medir de forma honesta se a Tami melhorou de verdade.

**O critério não muda:**
- caso grave nunca pode ir para um lugar mais leve;
- a sobretriagem só pode baixar se a subtriagem não subir.

Valem as regras de ouro:
- nunca diagnosticar;
- nunca indicar remédio;
- na dúvida, o mais urgente;
- CVV 188, SAMU 192 e CIATox 0800 722 6001;
- nada de método em suicídio;
- nada de conteúdo pago nem com direitos autorais.

Trabalhe sem pedir confirmação. Se algo for ambíguo, escolha a opção mais cautelosa, registre em `DECISOES.md` (D-034 em diante) e siga.

## Regra de ouro da medição

**O que já foi visto pode ser usado para ajustar:**
- as 10.000 vinhetas;
- o lote V-H (`_revisao/vinhetas_VH.csv`);
- as 300 frases de crise humana (`_revisao/crise_humana.csv`);
- os conjuntos de crise antigos.

Por isso, o resultado nesses arquivos **não prova nada sozinho**.

**O que prova:**
- os lotes cegos antigos V-Z e V-Y, que ninguém abre e que só servem para medir;
- o **lote cego novo do sprint 3**, escrito ANTES das correções, em `_rascunho/cego3/`.

Quem corrige **não abre** `_rascunho/cego3/`, `V-Z` nem `V-Y`. Quem escreve o cego **não abre** a base, o léxico, o código, `RESULTADO_TESTE_HUMANO.md`, `RESULTADO_CRISE_HUMANA.md` nem as frases de crise antigas.

## Frente 0 ➜ Lote cego novo (5 autores e depois 1 revisor)

**Autores das vinhetas: 4 agentes, 100 casos cada, ids V-N-0001 a V-N-0400.**
- Seguem `_rascunho/humano/INSTRUCOES_AUTOR.md`.
- Usam personas novas e misturadas: avó do interior que manda áudio, motoboy, mãe de primeira viagem, pessoa que escreve em inglês e português, cuidadora de idoso, quem se envergonha e esconde, quem cola o texto de outra pessoa, e quem já tentou outro app e veio desconfiado.

**Autor da crise: 1 agente, 300 frases, ids CN-R-xxx e CN-C-xxx.**
- 120 de suicídio (só ideia, nunca método), 90 de intoxicação, 90 de violência e 100 controles com exagero, humor e gíria.
- Mesmo formato de `crise_risco.csv`.

**Revisor: 1 agente.**
- Segue `INSTRUCOES_REVISOR.md` e gera `vin_N.csv`, `meta_N.csv` e `lacunas_N.csv`.

Ninguém olha esse lote até a medição final.

## Frente 1 ➜ Detector de crise (dono: `DetectorRisco.java` e `DetectorRiscoTest.java`)

Itens 1, 2, 3, 5 (parte texto) e 8 da lista:
- **violência:** ameaça, socorro, abuso de criança dito por eufemismo, estrangulamento, "estrupo", maus-tratos de idoso;
- **suicídio:** grafia pelo som e de áudio, automutilação, despedida, doação de pertences, frase em inglês;
- **intoxicação:** produtos novos, gás e fumaça, droga com gíria, remédio tomado duas vezes;
- **convulsão descrita sem a palavra**, como "tremi inteira" e "fiquei fora do ar";
- **exagero com humor não liga crise:** "me matar de estudar", "quero morrer kkk", "vou matar meu filho kkk".

O léxico é escrito **pelas categorias**, sem copiar as frases do `crise_humana.csv`.

**Meta no `crise_humana.csv`:**
- risco ≥ 90% em cada grupo;
- controles ≥ 95%.

## Frente 2 ➜ Motor (dono: `MotorTriagem.java` e `BaseConhecimento.java`)

- **Item 7 (minimização):** um sinal grave achado no texto aparece **já marcado** na lista de confirmação (CONF_SINAIS e CONF_SINAIS_2). A pessoa precisa desmarcar de propósito. Desmarcar um sinal SAMU pede uma confirmação curta. Os testes de `PessoaImperfeita` precisam refletir isso.
- **Item 6 (glicose):**
  - glicemia ≥ 300 com sede, xixi toda hora, fraqueza, vômito ou confusão ➜ sinal **SA-D1-102** (criado pela Frente 4);
  - glicemia ≥ 300 sozinha ➜ UBS no mínimo;
  - glicemia ≤ 60 com sintoma ➜ o sinal de hipoglicemia que já existe.
- **Item 11 (propostas prontas):** aplicar C2 (hipótese não é queixa) e C3 (história não dispara SAMU pelo léxico de emergência), que estão em `_revisao/RESULTADO_SOBRETRIAGEM.md` e `RESULTADO_PROPRIEDADES_2.md`.

## Frente 3 ➜ Texto e voz (dono: `Normalizador.java`, `LeitorNumeros.java` e `BuscaExpressoes.java`)

- **Item 9:**
  - número por extenso vira dígito ("trinta e oito e quatro" ➜ 38,4);
  - "38 e 5" e "38 5" com contexto de febre ➜ 38,5;
  - tirar as muletas ("né", "tipo assim", "ééé") e as repetições.
- **Alcance da negação:** o "não", o "nenhum" e o "sem" param no próximo sintoma ou no máximo em 4 palavras quando não há pontuação.
- Grafias pelo som mais comuns viram a forma certa, com tabela própria:
  - isquerdo, formigano, vista dobrada;
  - estrupo, convulção, com vulsão, sui sídio.

**Medidas:**
- `python3 ferramentas/avaliar_voz.py`: críticas com lista no nível pesado ≤ 2.
- Sem piorar o V-H.

## Frente 4 ➜ Base (dona: `sinais_alerta.csv`, `condicoes.csv`, `lexico_extra.csv` e `agravantes_sinonimos.csv`)

- **Item 4:** léxico do AVC escrito pelo som e em fala popular (SA-D1-031/033/034 e vizinhos).
- **Item 5:** léxico de convulsão sem a palavra (SA-A ou SA-D de convulsão), mais o sinal da gestante com convulsão ➜ SAMU.
- **Item 6:** criar **SA-D1-102**, glicose muito alta com sintomas ➜ UPA_HOJE.
  - Duas fontes oficiais com URL: Ministério da Saúde, SBD, NHS ou MedlinePlus (este só nas partes de domínio público).
  - `revisar_medico=SIM`.
- **Item 12:** as 49 lacunas de `_revisao/lacunas_VH.csv`.
  - Entram como condições de casa ou de UBS, com fonte e com os grupos de risco que mudam o destino.
  - Ficam de fora as que a regra D-006 não permite.
- **Seção 8, contradição da catapora:** fica UPA nas duas linhas, pela regra do mais urgente.
- As subidas que parecem exagero **não mudam** neste sprint. Vão para a planilha do médico.
- Rodar `validar_conhecimento.py`: 0 erros.

## Frente 5 ➜ Telas (dono: `nos.csv`, só texto e rótulo, e `src/main/resources/roteiros/textos_tami.csv`)

- **Item 10:**
  - um sinal por linha nas 18 perguntas de alarme, com os botões [Sim, pelo menos um] [Não, nenhum] [Não sei];
  - corrigir os 3 textos novos que mudaram de sentido e os outros casos ALTA de `LEITURA_DIFICIL.csv`.
- **Não muda código de nó, código de resposta nem efeito.** O teste precisa continuar passando com as mesmas respostas.
- A tela "Para quem é a ajuda?" fica como proposta, com fluxo e textos, em `src/main/resources/roteiros/PARA_QUEM.md`. Entra no código num sprint seguinte.

## Frente 6 ➜ Integração e medição (coordenador)

- Junta as frentes, roda os 90+ testes e roda as 10.000.
- Mede:
  - V-H com a pessoa sincera e com os 7 perfis;
  - crise humana;
  - voz;
  - **V-Z, V-Y e o cego 3 (V-N e crise nova)**.
- Comparação **v7 ➜ v8** em todos.
- Se uma frente piorar um cego, ela é revertida e o motivo vai para `DECISOES.md`.
- Escreve `_revisao/RESULTADO_SPRINT_3.md`, atualiza CHANGELOG (v8) e `RELATORIO.md`, regera `revisao_medica.xlsx` e manda uma mensagem curta.

**Regra de entrega:** primeiro o número honesto (cegos), depois o número de ajuste. Sempre lembrar que ainda é IA simulando gente.
