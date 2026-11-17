# Relatório final do estudo clínico da Tami

> **Atualização v7 (sprint 2, 14/11 23:25 ➜ 15/11 00:50).** Nas 10.000 vinhetas, os casos graves perdidos caíram de 74
> para 37 (0,4%), e a sobretriagem caiu de 19,3% para 17,1%. Nos dois lotes cegos novos, a melhora foi pequena ou
> nenhuma: V-Z ficou com 1 caso grave perdido em 300, e V-Y continuou com 5 em 300 e 24% de sobretriagem. A crise
> medida com frases cegas mostrou que o 100% do sprint 1 era ajuste (só 37,5% das ideias de suicídio chegavam ao CVV).
> Isso levou ao detector de risco por raiz e contexto (D-031). Seção "Sprint 2" no fim.
>
> **Atualização v6 (sprint de robustez, 14/11 22:41 ➜ 23:41).** Com 10.000 vinhetas, os casos graves mandados para um
> lugar mais leve caíram de 202 para 74 (0,7%). No lote cego novo V-Z (300 casos que ninguém viu), caíram de 2 para 1.
> A sobretriagem subiu de 18,3% para 19,3%. A crise ficou muito mais forte: ideia de suicídio sempre chega ao CVV 188,
> e agora existe sinal para agressão física. As metas de zero subtriagem e de sobretriagem até 15% continuam não
> atingidas. Detalhes na seção "Sprint de robustez", no fim, e em CHANGELOG v6, D-029 e D-030.

Data: 14/11/2026. Base v5. Este relatório fecha as 6 etapas do prompt de estudo.

## Resumo em uma linha

A base de conhecimento cresceu muito e o motor ficou bem mais difícil de enganar. Mesmo assim, **as duas metas
principais não foram atingidas**:
- **Subtriagem de casos SAMU ou UPA:** a meta era 0 e o resultado é 202 de 10.000 (2,0%).
- **Sobretriagem:** a meta era até 15% e o resultado é 18,3%.

**Nada aqui foi revisado por um médico**, e por isso a Tami **não deve ser usada com pessoas reais** antes da
revisão médica (revisao_medica.xlsx).

## O que existe agora

| Arquivo | Linhas | O que é |
|---|---|---|
| sinais_alerta.csv | 895 | Sinais de alerta (SAMU 258, UPA 361, UBS 208, casa 68). 425 marcados revisar_medico=SIM e 271 com uma fonte só. |
| condicoes.csv | 225 | Condições comuns, com nome popular, destino típico e destino mínimo. 75 marcadas SIM. |
| procedimentos.csv / recuperacao.csv | 84 / 473 | Cirurgias e procedimentos, e o que é esperado ou não na recuperação. Todas as linhas foram conferidas. |
| lesoes_esporte.csv | 35 | Lesões do esporte. |
| exames.csv | 71 | Para que serve cada exame, o preparo e o que se sente. Nunca diz "suspenda o remédio". |
| remedios_classes.csv | 69 | Classes de remédio, só para reconhecer reação grave. Sem nome comercial, dose, troca ou parada. |
| lexico_extra.csv / agravantes_sinonimos.csv | 107 / 55 | Linguagem popular de emergência, e agravantes ditos com outras palavras. |
| vinhetas_10k.csv | 10.000 | Casos de teste: 4.775 escritos à mão (500 deles de ataque) e 5.225 variações. |
| DECISOES.md | D-001 a D-028 | Cada escolha ambígua, sempre pelo lado mais cauteloso. |
| CONFLITOS.md | ~95 conflitos | Fontes que discordam. A opção mais urgente venceu e a linha ficou com revisar_medico=SIM. |
| revisao_medica.xlsx | 300 | O que um médico precisa revisar primeiro. |

Todas as linhas têm fonte_nome, fonte_url e data_acesso. As fontes usadas foram: Ministério da Saúde, ANVISA,
CIATox, SBP, SBC, FEBRASGO, SBD, OMS, CDC, NHS, MedlinePlus, NICE, DailyMed e NCBI Bookshelf/StatPearls/PMC.
Não foram usados fóruns, blogs, redes sociais nem conteúdo pago.

## Números por etapa (a primeira passada é a medida honesta)

A "primeira passada" é o motor rodando sobre vinhetas novas **antes** de qualquer ajuste. O "lote cego" são
vinhetas escritas à mão que **nunca** foram olhadas para ajustar o motor.

| Etapa | Vinhetas | 1ª passada (antes de ajuste) | Depois dos ajustes: críticas | Lote cego | Só texto livre | Sobretriagem |
|---|---|---|---|---|---|---|
| 2 | 2.000 | 284 de 900 (31,6%) | 22 (1,1%) | 6/500 (1,2%) | 354 (17,7%) | 15,5% |
| 3 | 5.000 | 36 de 1.350 (2,7%) | 76 (1,5%) | 15/1.250 (1,2%) | 1.069 (21,4%) | 18,0% |
| 4 | 9.500 | 82 de 2.025 (4,0%) | 246 (2,6%) | 58/2.375 (2,4%) | 2.032 (21,4%) | 17,7% |
| 5 (ataque) | 10.000 | ataque: 26 de 500 com a lista (5,2%); 181 de 500 só texto (36,2%) | **202 (2,0%)** | **47/2.375 (2,0%)** | 2.133 (21,3%) | 18,3% |

"Crítica" quer dizer que o esperado era SAMU ou UPA e a Tami mandou para um lugar menos urgente.

Em produção hoje, nenhuma regra de casa foi aprovada por médico (trava M12). Por isso todo "casa" vira UBS: as
críticas continuam 202 e a sobretriagem sobe para 30,7%. Isso foi feito de propósito, e só um médico pode liberar.

### Os 202 casos críticos que restam

- **127 (63%):** o motor não achou nenhum sinal da base. O sinal certo ficou fora das 16 opções da lista de
  confirmação (lacuna semântica: a pessoa descreve com palavras que o léxico não liga ao sinal).
- **38:** o motor achou outro sinal, mais leve.
- **37:** o motor achou o sinal certo, mas não subiu de nível. O agravante estava no relato, dito de um jeito que o motor
  não reconheceu.
- **Por destino:**
  - UPA virou UBS em 125.
  - SAMU virou UBS em 37.
  - SAMU virou UPA em 31.
  - Algo grave virou casa em 9.
- **Por lote:** a maioria está nas variações geradas (G4: 81) e nos lotes à mão das Etapas 3 e 4. O lote de
  ataque, depois dos ajustes, ficou com 2.

## O que a Etapa 5 (ataque) ensinou

Os 500 casos de ataque foram escritos para enganar a leitura de texto. Antes de qualquer ajuste, eles mostraram
três buracos:
1. **Números:** "38,4", "oxigênio 87", "182 batimentos", "glicemia 38" e "pressão 22 por 13" passavam sem ser
   entendidos. Agora existe o leitor de números (D-024).
2. **Agravante com outras palavras:** "batendo queixo de frio" no lugar de calafrio, "saiu sangue" no lugar de
   sangramento, "38,6" no lugar de febre. Agora existe o arquivo de sinônimos (D-025).
3. **Febre em imunossuprimido:** a trilha de febre não pergunta sobre transplante ou quimioterapia. Agora o sinal
   SA-I-016 entra direto (D-026).

Um problema pequeno com efeito grande: "39sem" virava "39 sem", e o "sem" era lido como negação.

## Limites que precisam ser ditos

- **Sem médico.** Todas as linhas foram escritas a partir de fontes oficiais por um assistente de IA e conferidas
  por revisores que também são IA (amostras de 10% a 15% em cada etapa). Esses revisores acharam erros reais em todas
  as etapas, inclusive casos graves classificados como leves. Isso indica que ainda há erros não achados.
- **Vinhetas escritas por quem conhece a base.** Os casos foram escritos por IA olhando os sinais da base. Gente de
  verdade escreve de formas que ninguém previu. O número que mais se aproxima da realidade é o "só texto livre"
  (21,3% de críticas), que mostra o quanto a Tami depende da lista de confirmação.
- **Pessoa simulada sincera.** Na avaliação, a pessoa marca na lista exatamente o sinal que tem. Uma pessoa real pode
  não marcar, marcar errado ou desistir. A subtriagem real tende a ser **maior** que a medida (D-022).
- **Lista de confirmação com limite.** São 8 itens por página e no máximo 2 páginas. Cada sinal novo disputa lugar.
  O peso de urgência (D-027) ajuda, mas não resolve a lacuna semântica.
- **Sobretriagem acima da meta.** A maior parte vem de "casa" que virou UBS, o tipo mais leve de exagero. Parte é
  artefato da simulação (D-022).
- **Destinos discutíveis.** Em ~95 conflitos a opção mais urgente venceu, e isso empurra a sobretriagem para cima de
  propósito.
- **Saúde mental e intoxicação.** O caminho é sempre CVV 188 e/ou SAMU, e CIATox 0800 722 6001. Nenhuma linha traz
  método. A regra foi conferida pelo validador, mas o fluxo de crise (CRI) não teve revisão de especialista.

## Metas do prompt

| Meta | Resultado | Situação |
|---|---|---|
| Subtriagem SAMU ou UPA = 0 | 202 de 10.000 (2,0%) | **Não atingida** |
| Sobretriagem até 15% | 18,3% (30,7% em produção) | **Não atingida** |
| Distribuição ~15/30/30/25 | dentro de ±5 (validador --metas) | Atingida |
| Grupos especiais com pelo menos 12% | criança, gestante e idoso com pelo menos 12% | Atingida |
| Pelo menos 25% de armadilhas | sim | Atingida |
| Toda linha com fonte e data | sim (validador) | Atingida |
| Conflitos no CONFLITOS.md com SIM | sim | Atingida |
| Nunca diagnosticar nem indicar remédio | conferido pelo validador e pelos revisores | Atingida |

## O que fazer antes de usar com pessoas

1. **Revisão médica**, começando pelas 300 linhas da revisao_medica.xlsx (P1 a P4). Depois disso, revisar as 129
   linhas marcadas SIM com destino UBS ou casa que ficaram de fora. Elas importam porque, se estiverem erradas, causam
   subtriagem.
2. **Liberar a trava M12** só para as regras de casa que o médico aprovar.
3. **Testar com relatos de pessoas reais**, anonimizados e com consentimento, e medir de novo a subtriagem "só texto"
   e "com lista".
4. **Atacar a lacuna semântica.** Hoje ela responde por 63% das críticas. O caminho é aumentar o léxico a partir de
   relatos reais, e não de relatos escritos por IA.

## Como verificar

- `python3 ferramentas/validar_conhecimento.py --metas` deve dar 0 erros e 0 avisos.
- `bash ferramentas/rodar_testes.sh` deve dar 41 testes OK.
- `bash ferramentas/rodar_vinhetas.sh` gera o RESULTADO_VINHETAS.md e o falhas.csv.
- As primeiras passadas de cada etapa estão guardadas em `_revisao/`.

O navegador (Chrome) não foi usado em nenhuma etapa. Toda a pesquisa foi feita com busca e leitura de páginas
públicas, então não há abas para fechar.

## Sprint de robustez (v6)

Roteiro em PROMPT_SPRINT_1H.md. Dez frentes rodaram em paralelo e cada achado virou correção e teste de regressão.

| Medida | Antes do sprint (v5) | Depois (v6) |
|---|---|---|
| Críticas em 10.000 | 202 (2,0%) | **74 (0,7%)** |
| Lote cego antigo (2.375) | 47 | 20 (ele ajudou a escolher o ranking, então não é mais cego) |
| **Lote cego novo V-Z (300, nunca usado)** | 2 críticas, sobretriagem 12,3% | **1 crítica, 13,0%** |
| Só texto livre (10.000) | 2.133 | 2.059 |
| Sobretriagem (10.000) | 18,3% | 19,3% |
| Testes | 41 | 79 |

O que mais pesou, nesta ordem:
1. **Ordem da lista de confirmação por BM25 (D-030).** O sinal certo passou a aparecer mais no alto da lista.
2. **Idoso fora de 45 sinais graves.** Esses sinais não valiam para idoso, e um idoso com relato grave caía para a UBS.
3. **Auditoria dos sinais de casa.** Saíram 115 expressões perigosas e entraram 390 agravantes.
4. **Febre em gestante e em bebê.** Agora é reconhecida mesmo sem a palavra "grávida" no relato.

Limites do sprint:
- As frases de crise foram usadas para ajustar o motor, então os 100% delas não são medida cega.
- A mudança de ranking foi escolhida olhando o lote cego antigo. A medida honesta é o V-Z.
- Ficaram para depois:
  - temperatura baixa em bebê e idoso, que não tem sinal na base;
  - 51 achados MÉDIA e 8 BAIXA da auditoria de casa;
  - "a prova vai me matar", que ainda entra em crise. É exagero, mas para o lado seguro.

## Sprint 2 (v7)

Roteiro em PROMPT_SPRINT_2.md. A lição principal: **número medido em material já usado para ajuste engana**. O sprint
1 mostrava a crise em 100%. Com frases que ninguém tinha visto, a crise caiu para 37,5% (suicídio), 45,7%
(intoxicação) e 60% (violência). Os lotes cegos novos também mostram o motor mais fraco do que as 10.000 vinhetas
sugerem.

| Medida | v5 (antes do sprint 1) | v6 | **v7** |
|---|---|---|---|
| Críticas nas 10.000 (já usadas para ajuste) | 202 | 74 | **37** |
| Sobretriagem nas 10.000 | 18,3% | 19,3% | **17,1%** |
| **V-Z, cego (300)**: críticas / sobretriagem | 2 / 12,3% | 1 / 13,0% | **1 / 12,0%** |
| **V-Y, cego (300, mais difícil)**: críticas / sobretriagem | 7 / 22,7% | 5 / 24,0% | **5 / 24,0%** |
| Crise, frases cegas: suicídio / intoxicação / violência | - | 37,5% / 45,7% / 60% (conjunto 1) | **75,6% / 74,3% / 77,1%** (conjunto 2, controles 95,6%) |
| Testes | 41 | 79 | **90** |

O que mudou de verdade:
- **D-031, detector de risco:** a crise deixou de depender da frase exata.
- **D-032, correções da revisão de código:** o CVV e o CIATox não se perdem mais, e o leitor de números parou de
  inventar sinal vital ("minha mãe de 40", "paguei 20 x 12").
- **D-033, auditorias de casa e de UBS:** saíram expressões perigosas e entraram centenas de sinais de alarme.
- **16 sinais novos, com fonte.**

O que ainda falta:
- **Crise:** uma em cada quatro frases de risco ainda escapa nas frases cegas, como "acho que era melhor eu não acordar
  mais amanhã" e "quer partir logo desse mundo". O caminho é aumentar as famílias de frases do DetectorRisco a partir de
  frases REAIS, revisadas por especialista, e medir sempre com um conjunto novo que ninguém viu.
- **Sobretriagem acima da meta.** O V-Y continua em 24%. A maior causa é o item de casa que não entra na lista,
  junto com o limite da simulação (D-022).
- **Propostas medidas e não aplicadas,** por mexerem em caminho de caso grave:
  - hipótese não é queixa ("achei que era infarto");
  - história não dispara SAMU pelo léxico de emergência ("tive AVC em 2018");
  - alcance da negação parar no próximo sintoma.

  Todas têm o código pronto em _revisao/RESULTADO_SOBRETRIAGEM.md e _revisao/RESULTADO_PROPRIEDADES_2.md.
- **Revisão médica.** A planilha revisao_medica.xlsx foi gerada de novo com a base v7, que mudou muito (centenas de
  linhas viraram revisar_medico=SIM nas auditorias).

## Sprint 3 (v8)
Correções do teste com "gente de verdade" em 5 frentes, medidas num lote cego escrito antes delas. Com a lista de
confirmação, a v8 empata com a v7 nos três lotes cegos (cego 3: 12 críticas em 378; V-Z: 1; V-Y: 5) e melhora o
texto livre (cego 3: 91 ➜ 83 críticas). A crise só por texto melhorou de 10 a 15 pontos no cego (suicídio 63%,
intoxicação 76%, violência 61%), bem longe dos 100% do material de ajuste. Conclusão: a detecção de crise precisa de
uma pergunta de segurança fixa, e o texto vira segunda camada. Detalhes em _revisao/RESULTADO_SPRINT_3.md.
