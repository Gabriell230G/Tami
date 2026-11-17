# Prompt: corrigir a qualidade da conversa da Tami

Cole este texto numa IA (ou entregue a quem for mexer no motor). Ele descreve o problema que apareceu no teste real e o que precisa mudar. Não muda nenhuma regra médica: só a ordem e a escolha das perguntas, a escala de dor e os cuidados no fim.

---

Você vai melhorar a conversa da Tami, um app de orientação em saúde em Java (motor de regras em `br.com.fiap.tami.motor`). A Tami não diagnostica, não indica remédio, dose nem exame. Ela só diz para onde ir: SAMU 192, UPA hoje, UBS em breve ou cuidar em casa. Os textos de 192, 188 (CVV), 190 e CIATox não podem ser alterados.

## O que deu errado no teste

A pessoa escreveu "estou com dor na perna, acho que luxei". Ela tinha tomado um golpe mal aplicado no treino de judô. A Tami:

1. Nunca perguntou como a dor começou (pancada, queda, torção, esporte ou sozinha).
2. Mostrou uma lista com sinais sem relação: remédio para colesterol, urina escura, câncer, gravidez.
3. Fez as 8 perguntas do filtro geral (falta de ar, peito, AVC, alergia, veneno, suicídio) e depois as 6 do caminho genérico (febre, doença crônica). Foram 16 perguntas para uma torção.
4. Pediu a dor em 3 faixas (fraco, médio, forte) em vez de 0 a 10.
5. No fim não deu nenhum cuidado: gelo, repouso, perna elevada, dormir bem, beber água.

Nenhum desses erros é médico. São perguntas ruins para o caso.

## O que fazer

### 1. Trilha de lesão (LES)
- Detecte no texto sinais de machucado: torci, luxei, caí, tombo, tropecei, escorreguei, pancada, bati, golpe, chute, treino, jogo, judô, futebol, luta, estalo, entorse, fratura, quebrei, machuquei, "pisei errado", "virei o pé".
- Se a pessoa tocou num braço, perna, ombro, joelho, tornozelo, pé, mão ou costas e o texto não diz se foi machucado, a primeira pergunta é "Como isso começou?" (machucou ou começou sozinho).
- Se foi machucado, a conversa segue a trilha LES e pula o filtro geral e o caminho genérico. O detector de risco do texto continua ligado: se o texto falar em falta de ar, peito, suicídio, veneno etc., as perguntas certas voltam.
- Perguntas da trilha, nesta ordem:
  1. Como aconteceu? (treino ou esporte, torci andando, caí da minha altura, levei uma pancada, acidente de trânsito ou queda de mais de 1 metro, fui agredido). Pule se o texto já disser.
  2. Sinais graves (SAMU): osso aparecendo, mão ou pé gelado, pálido ou roxo, dor forte no pescoço ou costas com formigamento, bateu a cabeça e está vomitando, confuso ou muito sonolento.
  3. Sinais de UPA hoje: ficou torto ou fora do lugar, ouviu um estalo, não consegue apoiar o peso ou dar 4 passos, ficou dormente, inchaço ou roxo grande que aumenta rápido, bateu a cabeça e apagou ou ficou tonto.
  4. Dor de 0 a 10.
  5. Há quanto tempo.
  6. Se faz mais de um dia: está melhorando, igual ou piorando?
- Cada item das perguntas 2 e 3 aponta para um sinal da base de conhecimento (fonte: NHS, Sprains and strains), para o destino respeitar idade e grupo da pessoa.
- Decisão final: sem sinal de alerta, dor até 6, adulto, menos de 8 dias ou melhorando ➜ cuidar em casa. Criança, idoso ou gestante ➜ UBS com os mesmos cuidados. Sem melhora depois de dias ➜ UBS. Piorando ➜ UPA.

### 2. Lista de confirmação mais esperta
- Em caso de lesão, mostre só sinais de trauma, esporte e queda, sem repetir os itens das perguntas 2 e 3.
- Fora da lesão, esconda sinais de trauma.
- Esconda sinais que só valem para um grupo ("em quem tem câncer", "em quem toma remédio para colesterol", "na gravidez") se a pessoa não for desse grupo nem falar disso no texto.

### 3. Dor de 0 a 10
- Troque as 3 faixas por 11 botões (0 a 10) com carinhas. As regras continuam as mesmas: 7 ou mais conta como dor forte.

### 4. Cuidados no resultado
- Lesão: proteger, repouso, gelo envolto num pano por até 20 minutos a cada 2 ou 3 horas, faixa leve de dia, parte elevada num travesseiro. Nos primeiros 2 ou 3 dias, evitar calor, álcool e massagem. Quando conseguir mexer sem a dor travar, mexer devagar para não endurecer. Dormir bem e beber água. Voltar ao treino aos poucos, sem dor, avisando o professor.
- Indo para a UPA: não forçar a parte machucada, imobilizar com o que tiver, gelo no caminho, não dirigir.
- Outros casos de casa ou UBS: descansar, beber água, dormir bem, observar se piora.
- Nunca citar remédio.

### 5. Testes que precisam passar
- "estou com dor na perna, acho que luxei" ➜ no máximo 7 perguntas, pergunta a origem, termina em casa com os cuidados se tudo for leve.
- "torci o pé no judô e não consigo pisar" ➜ UPA hoje.
- "caí do telhado e o osso tá aparecendo" ➜ SAMU.
- "dor na perna, inchada e vermelha na batata, sem ter batido" ➜ não entra na trilha de lesão.
- A validação de 10 mil casos não pode perder sensibilidade para urgência (hoje 99,3%) nem aumentar a subtriagem crítica (hoje 0,75%).

---

## Situação (17/11/2026): feito

| Item do prompt | Situação |
| --- | --- |
| Trilha de lesão (LES) com as 6 perguntas | Feito. A pergunta "Como aconteceu?" some quando o texto já diz (ex.: "no treino") |
| "Como isso começou?" para braço, perna e costas | Feito. Não aparece se o texto já tem algo urgente |
| Itens das perguntas 2 e 3 apontam para sinais da base | Feito. Sinais novos SA-C-120 a SA-C-129 (NHS: Sprains and strains, Broken leg, Broken arm or wrist) |
| Item extra na pergunta 3 | "Dor forte bem em cima do osso, com inchaço logo depois": sem ele, criança mancando depois de cair e punho quebrado iam para a UBS |
| Lista de confirmação | Na lesão: uma página só, sem itens de casos crônicos, sem os itens das perguntas 2 e 3, e só da região tocada. Sinais de grupo (câncer, colesterol, gravidez, anticoagulante...) só para quem é do grupo ou fala disso |
| Dor de 0 a 10 com carinhas | Feito. 7 ou mais continua contando como dor forte |
| Cuidados no resultado | Feito: lesão, ida à UPA, volta ao treino e casos gerais. Sem remédio |
| Pancada no peito ou na barriga | Fica no caminho completo, que pergunta sobre peito e ar |

### Duas regras do prompt que foram ajustadas, com o teste dos 10.000 casos

1. **"Na lesão, mostre só sinais de trauma".** Testado ao pé da letra, perdia 12 casos urgentes a mais (alguns de SAMU indo para casa). Ficou assim: sinais de trauma, mais os que o relato descreve com clareza.
2. **"Fora da lesão, esconda sinais de trauma".** Escondia casos como "idosa com osteoporose sentiu dor forte nas costas ao se abaixar". Ficou assim: só esconde depois que a pessoa responde "Começou sozinho".

### Os 4 testes

| Relato | Resultado |
| --- | --- |
| "estou com dor na perna, acho que luxei" | 7 perguntas, pergunta a origem, cuidar em casa com os cuidados |
| "torci o pé no judô e não consigo pisar" | UPA hoje |
| "caí do telhado e o osso tá aparecendo" | SAMU 192 |
| "dor na perna, inchada e vermelha na batata, sem ter batido" | Não entra na trilha de lesão |

### Validação (10.000 casos)

| | Antes | Agora |
| --- | --- | --- |
| Urgentes que foram para UPA ou SAMU | 99,29% | 99,38% |
| Grave mandado para mais leve | 36 (0,75%) | 32 (0,66%) |
| Destino exato | 83,4% | 83,9% |
| Perguntas por conversa (média) | 10,8 | 10,1 |
