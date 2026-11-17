# Prompt: levar a nota das conversas da Tami de 7,7 para 9 ou mais

Cole este texto numa IA com acesso ao repositório Gabriell230G/tami (pasta `app/`). Ele continua o trabalho de `PROMPT_TESTES_SIMULADOS.md`: o teste com 12.000 conversas simuladas já existe e já roda. Agora o objetivo é a nota.

---

Você vai melhorar a conversa da Tami até a **nota média de uma pessoa real chegar a 9 ou mais** na prova final, sem perder segurança. A Tami é um app de orientação em saúde em Java. Ela não diagnostica e não indica remédio, dose nem exame. Ela só diz para onde ir: SAMU 192, UPA hoje, UBS em breve ou cuidar em casa.

## Onde estamos (17/11/2026)

O teste fica em `app/ferramentas/simulados/`: `gerar_casos.py`, `simular.py` e `relatorio.py`. Os resultados ficam em `app/dados/simulados/`.

| Medida (prova final, 2.000 casos) | Hoje |
| --- | --- |
| Nota de uma pessoa real (0 a 10) | 7,70 |
| Conversas com pergunta ou item sem relação | 56,7% |
| Perguntas por conversa (média) | 7,0 |
| Urgentes que foram para UPA ou SAMU | 99,8% |
| Urgentes mandados para UBS ou casa | 2 de 1.197 |

**De onde vêm os pontos perdidos** (média por conversa, nos 10.000 casos de treino):

| Motivo | Pontos perdidos |
| --- | --- |
| Item sem relação na **lista de confirmação** (CONF_SINAIS) | **1,69** |
| Pergunta de emergência sem relação (veneno, alergia, peito, se machucar, AVC) | 0,28 |
| Perguntas demais | 0,27 |
| Pergunta importante que faltou | 0,07 |
| Sem cuidado no fim | 0 |

Ou seja: **3 de cada 4 pontos perdidos vêm da lista de confirmação.** Quase tudo o que sobrou nela são itens **graves** que o motor mantém de propósito ("na dúvida, o item grave fica"). Exemplo: "lábio inchando de repente" aparece para quem tem dor de estômago.

**Famílias com nota mais baixa:** Pele (6,80; 10,8 perguntas), Barriga (6,81), Urina e região íntima (6,91; 10,8 perguntas), Gravidez e pós-parto (7,13), Cirurgia e crônicos (7,22), Febre e dengue (7,33). Quase todas passam pelo caminho genérico (GEN), que tem 8 perguntas de emergência e 6 gerais.

**Distribuição das notas:** 3.832 conversas com 10, mas 1.357 com 4 ou menos.

## Regras que não mudam

1. Não mexa nos textos de 192, 188 (CVV), 190 e CIATox. O CVV 188 aparece sempre que houver risco.
2. Não invente números, estudos nem fontes. Toda regra ou sinal novo leva fonte oficial (NHS, MedlinePlus, Ministério da Saúde) e `revisar_medico=SIM`.
3. Nunca indique remédio, dose ou exame.
4. **Segurança vem antes da nota.** Depois de cada mudança, rode os dois testes:
   - teste antigo (`vinhetas_10k.csv`): urgentes para UPA ou SAMU **≥ 99,46%** e urgentes para UBS ou casa **≤ 26**;
   - simulado de treino: urgentes para UBS ou casa **≤ 10**.
   Se qualquer um piorar, desfaça a mudança, mesmo que a nota suba.
5. **Não suba a nota mexendo na régua.** O avaliador (`avaliar()` em `simular.py`) só pode mudar no Passo 1, com a auditoria descrita lá. Depois disso ele fica congelado. Toda tabela final mostra a nota com o avaliador antigo e com o novo.
6. Não olhe a prova (2.000 casos) até o fim. Corrija só com os casos de treino.
7. Linguagem simples, sem travessão, com ➜ para indicar caminho.

## Passo 1: conferir a régua (auditoria do avaliador)

Antes de mexer na Tami, confira se o avaliador está certo. Hoje ele decide por palavras se um item é "de outra parte do corpo" ou "de outro grupo", e às vezes erra. Exemplo: marcou "item de gravidez" para uma mulher que acabou de ter bebê.

1. Sorteie 300 conversas de treino com nota ≤ 8 e leia cada item marcado como sem relação.
2. Para cada um, decida: **sem relação de verdade**, **tem relação** (o avaliador errou) ou **grave e plausível** (um profissional perguntaria mesmo sem relação direta).
3. Calcule a precisão do avaliador. Se ficar abaixo de 90%, corrija só as regras que erraram (ex.: pós-parto conta como grupo da gravidez; região tirada do relato e não da descrição do sinal) e mostre 10 exemplos de antes e depois.
4. Grave a auditoria em `dados/simulados/auditoria_avaliador.md` e **congele o avaliador**.

## Passo 2: uma tela de emergência no lugar de 8 perguntas

Troque as perguntas FIL_01 a FIL_05 (falta de ar forte, dor no peito, desmaio ou convulsão, sinais de AVC, sangramento forte ou acidente grave) por **uma pergunta só, de marcar**:

> Agora, algum destes está acontecendo? ☐ Falta de ar forte ☐ Dor ou aperto forte no peito ☐ Desmaiou, convulsionou ou está muito confuso ☐ Boca torta, fala enrolada ou fraqueza de um lado ☐ Sangramento que não para ☐ Nenhum destes

- Cada item mantém o mesmo efeito e a mesma regra de hoje, inclusive a herança do FAR e o "Não sei" que vale como o item mais leve.
- FIL_06 (alergia), FIL_07 (veneno) e FIL_08 (crise) continuam com as regras atuais de quando aparecem.
- Ajuste `Validacao.responder` e o simulador para a nova pergunta. Os dois testes precisam continuar passando.

## Passo 3: a lista de confirmação vira curta e certeira

A lista é a maior fonte de pontos perdidos. Mude o jeito de montar:

1. **No máximo 5 itens**, todos ligados ao relato: da mesma parte do corpo, do mesmo tipo de queixa (ex.: barriga, pele, urina) ou com boa parte das palavras do relato.
2. **Os itens graves "sem relação" saem da lista**, porque agora a tela de emergência do Passo 2 cobre os sinais graves gerais para todo mundo. Antes de tirar um item grave, confira se o sinal dele está coberto pela tela de emergência ou por uma trilha. Se não estiver, ele fica.
3. A segunda página só aparece se a primeira tinha itens graves e a pessoa marcou "Nenhum".
4. Meça o efeito por família e por destino. Se algum sinal grave deixar de ser achado no teste antigo, descubra o porquê antes de seguir.

## Passo 4: trilhas próprias para as famílias com nota baixa

Hoje só o machucado (LES) e a febre do adulto (FEB) têm trilha própria. Crie trilhas curtas, no mesmo formato da LES, com 4 a 6 perguntas cada e cuidados no fim. Cada pergunta e cada item levam fonte.

| Trilha | Perguntas que uma boa conversa faria | Fonte para começar |
| --- | --- | --- |
| Pele (PEL) | onde é e se está espalhando; febre; bolha ou ferida com pus; mancha que não some ao apertar; coceira forte; há quanto tempo | NHS: Rashes in babies and children, Cellulitis, Hives |
| Barriga (BAR) | onde dói; vômito e diarreia; consegue beber e fazer xixi; sangue no cocô ou no vômito; barriga dura; febre; há quanto tempo | NHS: Diarrhoea and vomiting, Stomach ache |
| Urina e região íntima (URI) | ardor ou dor ao fazer xixi; febre ou calafrio; dor nas costas do lado; sangue na urina; corrimento; gravidez | NHS: Urinary tract infections, Kidney infection |
| Gravidez (GES) | semanas; sangramento ou líquido; bebê mexendo; dor de cabeça forte, vista embaçada ou inchaço; contrações; febre | NHS: Pregnancy, Ministério da Saúde: caderneta da gestante |
| Febre na criança (FEC) | idade em meses; temperatura; está mamando ou bebendo; molinho ou difícil de acordar; manchas; respiração | NHS: Fever in children |

A rota escolhe a trilha pelo relato e pela parte do corpo, como já acontece com a LES. Se o texto tiver algo urgente, o caminho completo continua.

## Passo 5: perguntas que já foram respondidas não voltam

- Se o relato já diz há quanto tempo, a dor, a febre ou como aconteceu, a pergunta não aparece. O motor já faz isso em parte (`doTexto`). Amplie para tempo ("desde ontem", "faz 3 dias") e para a dor ("dor 8", "dor fortíssima").
- Nenhuma conversa com destino UBS ou casa pode passar de 8 perguntas.

## Passo 6: rodar, medir e repetir

Depois de cada passo:
1. rode `simular.py treino` e os dois testes de segurança;
2. anote no `CHANGELOG_SIMULADOS.md` o que mudou e os números antes e depois: nota, pontos perdidos por motivo, perguntas, segurança;
3. olhe as 20 conversas de treino com nota mais baixa e escreva o motivo de cada uma antes do próximo passo.

Pare quando a nota de treino passar de 9,2 (a prova costuma ficar um pouco abaixo). Se travar abaixo de 9 sem perder segurança, pare e escreva o que impede (ex.: "itens graves que precisam continuar na lista") e quanto cada coisa custa na nota.

## Passo 7: prova final

Rode os 2.000 casos guardados **uma única vez** e mostre, lado a lado (hoje x depois):
- a nota com o avaliador antigo e com o novo;
- a % de conversas com item ou pergunta sem relação;
- as perguntas por conversa, com média e máximo;
- a segurança nos dois testes;
- a nota por família.

## O que entregar

1. O código e as planilhas (`nos.csv`, `opcoes.csv`, `sinais_alerta.csv`, `textos_tami.csv`) com as trilhas novas e a tela de emergência.
2. `auditoria_avaliador.md`, `relatorio_testes.md` atualizado e o `CHANGELOG_SIMULADOS.md` com cada passo.
3. A aba "Conversas simuladas" do painel com a nota nova e a comparação.
4. As telas do app funcionando com a pergunta de marcar da emergência (testar no celular).

## Testes que precisam continuar passando

| Relato | Esperado |
| --- | --- |
| "estou com dor na perna, acho que luxei" | no máximo 7 perguntas, pergunta como aconteceu, fim com cuidados |
| "torci o pé no judô e não consigo pisar" | UPA hoje |
| "caí do telhado e o osso tá aparecendo" | SAMU 192 |
| "dor na perna, inchada e vermelha na batata, sem ter batido" | não entra na trilha de machucado |
| "to pensando em acabar com tudo" | CVV 188 aparece sempre |
| "meu filho de 2 anos tomou remédio da avó" | CIATox aparece sempre |
| "to vomitando tudo desde ontem e quase não fiz xixi" | pergunta de beber e fazer xixi, UPA |
| "ardendo pra fazer xixi, sem febre" | trilha de urina, no máximo 6 perguntas |
