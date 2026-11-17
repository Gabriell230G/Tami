# Testes simulados da Tami: 12.000 conversas com feedback

Cada caso é uma pessoa simulada com uma ficha escondida (idade, o que aconteceu, a dor, os sinais que tem). Ela conversa com o motor de verdade e responde com sinceridade, às vezes "Não sei" e às vezes se confunde. Cada conversa recebe um feedback: perguntas sem relação, perguntas que faltaram, cuidados no fim, uma nota de 0 a 10 e um comentário.

> O destino esperado de cada caso vem de um sinal da base de conhecimento, escrito a partir de fontes públicas (NHS, MedlinePlus, Ministério da Saúde), e **ainda precisa de revisão médica**. Os números medem se a conversa faz sentido e se o motor segue as regras, não se a orientação é clinicamente correta.

## 1. Prova final (2.000 casos que não foram olhados durante as correções)

| Medida | Antes | Depois |
| --- | --- | --- |
| Destino exato | 67,7% (65,6% a 69,7%) | 68,5% (66,4% a 70,5%) |
| Urgentes que foram para UPA ou SAMU | 99,8% (99,4% a 100,0%) | 99,8% (99,4% a 100,0%) |
| Urgente mandado para UBS ou casa | 0,2% (0,1% a 0,6%) | 0,2% (0,1% a 0,6%) |
| Conversas com pergunta ou item sem relação | 65,9% (63,8% a 68,0%) | 56,7% (54,5% a 58,9%) |
| Conversas em que faltou uma pergunta importante | 5,7% (4,8% a 6,8%) | 5,0% (4,1% a 6,0%) |
| UBS ou casa sem nenhum cuidado no fim | 5,0% (3,4% a 7,3%) | 0,0% (0,0% a 0,8%) |
| Perguntas por conversa (média) | 7,60 | 7,00 |
| Nota de uma pessoa real (0 a 10) | 6,58 | 7,70 |

Intervalos de confiança de 95% pelo método de Wilson.

## 2. Casos de treino (10.000)

| Medida | Antes | Depois |
| --- | --- | --- |
| Destino exato | 66,2% (65,3% a 67,1%) | 67,3% (66,3% a 68,2%) |
| Urgentes que foram para UPA ou SAMU | 99,8% (99,7% a 99,9%) | 99,8% (99,7% a 99,9%) |
| Urgente mandado para UBS ou casa | 0,2% (0,1% a 0,3%) | 0,2% (0,1% a 0,3%) |
| Conversas com pergunta ou item sem relação | 65,5% (64,6% a 66,5%) | 56,7% (55,8% a 57,7%) |
| Conversas em que faltou uma pergunta importante | 4,9% (4,5% a 5,3%) | 4,0% (3,7% a 4,4%) |
| UBS ou casa sem nenhum cuidado no fim | 6,5% (5,6% a 7,6%) | 0,0% (0,0% a 0,2%) |
| Perguntas por conversa (média) | 7,44 | 6,88 |
| Nota de uma pessoa real (0 a 10) | 6,64 | 7,75 |

## 3. Os 20 problemas mais comuns antes das correções

### 1. Item de cirurgia para quem não operou

**3837 conversas antes ➜ 1505 depois.** Famílias mais atingidas: Urina e região íntima (409), Pele (402), Febre e dengue (369).

Causa: A base tem 215 sinais de pós-operatório e a busca por palavras parecidas trazia todos para quem tinha febre ou dor.

- "minha avo de 74 anos, caroco na axila doi apos beber e ingua doi com alcool hj" ➜ CONF_SINAIS: Inchaço novo na mama com prótese meses ou anos depois, ou caroços pequenos e doloridos (Prótese de silicone na mama)
- "queria fechar os olhos e pronto faz uns 2 dias" ➜ CONF_SINAIS: Mão difícil de abrir e fechar depois de tirar a tala (cirurgia com placa e parafusos no braço, punho ou mão)
- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ CONF_SINAIS_2: Quadril operado voltando a doer, inchar, estalar ou ficar mais duro meses ou anos depois (Prótese de quadril)

### 2. Pergunta de alergia com inchaço sem motivo no relato

**3396 conversas antes ➜ 660 depois.** Famílias mais atingidas: Urina e região íntima (465), Febre e dengue (394), Barriga (375).

Causa: O filtro geral fazia as 8 perguntas de emergência para todo mundo.

- "minha avo de 74 anos, caroco na axila doi apos beber e ingua doi com alcool hj" ➜ FIL_06: alergia com inchaço
- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ FIL_06: alergia com inchaço
- "nao sei se e serio, to gravida de 31 semanas pontada forte na boca do estomago" ➜ FIL_06: alergia com inchaço

### 3. Mais cuidadosa que o necessário (destino mais urgente que o esperado)

**3338 conversas antes ➜ 3231 depois.** Famílias mais atingidas: Febre e dengue (306), Urina e região íntima (294), Pele (273).

- "queria fechar os olhos e pronto faz uns 2 dias" ➜ esperado UPA_HOJE, a Tami deu SAMU_192
- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ esperado UBS_EM_BREVE, a Tami deu UPA_HOJE
- "cabeca latejando depois da cirurgia na coluna ah e xixi rosinha depois da cistoscopia" ➜ esperado UPA_HOJE, a Tami deu SAMU_192

### 4. Pergunta de veneno ou remédio demais sem motivo no relato

**3288 conversas antes ➜ 1048 depois.** Famílias mais atingidas: Pele (497), Urina e região íntima (492), Febre e dengue (455).

Causa: O filtro geral fazia as 8 perguntas de emergência para todo mundo.

- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ FIL_07: veneno ou remédio demais
- "gravida de 9 semanas, to gravida e com manchas vermelhas e febre" ➜ FIL_07: veneno ou remédio demais
- "suspeita de zika, zika faz 2 semanas" ➜ FIL_07: veneno ou remédio demais

### 5. Perguntas demais para o caso

**2962 conversas antes ➜ 2688 depois.** Famílias mais atingidas: Urina e região íntima (362), Pele (346), Intoxicação e animais (326).

Causa: O caminho genérico tem 8 perguntas de emergência e 6 gerais, além da lista.

- "minha avo de 74 anos, caroco na axila doi apos beber e ingua doi com alcool hj" ➜ 16 perguntas
- "suspeita de zika, zika faz 2 semanas" ➜ 16 perguntas
- "rosto cheio de espinha e acne" ➜ 15 perguntas

### 6. Lista com item de outra parte do corpo

**2838 conversas antes ➜ 2674 depois.** Famílias mais atingidas: Pele (300), Barriga (297), Pancada, corte e queimadura (280).

Causa: A lista não sabia onde era o problema quando a pessoa não tocava no corpo.

- "minha avo de 74 anos, caroco na axila doi apos beber e ingua doi com alcool hj" ➜ CONF_SINAIS: Homem com caroço no peito ou na axila ou com líquido saindo do bico do peito
- "cabeca latejando depois da cirurgia na coluna ah e xixi rosinha depois da cistoscopia" ➜ CONF_SINAIS: Pancada na cabeça durante o jogo e ficou zonzo, com dor de cabeça ou enjoo
- "dor nas costas ou do lado embaixo das costelas e infeccao urinaria subiu pro rim" ➜ CONF_SINAIS: Dor na barriga ou no ombro depois da pancada nas costelas (cuidados em casa da costela quebrada)

### 7. Item de "depois de queda ou pancada" para quem não se machucou

**2293 conversas antes ➜ 2068 depois.** Famílias mais atingidas: Pele (286), Saúde mental e crise (220), Febre e dengue (201).

Causa: A lista mostrava sinais de "depois de queda" para quem não caiu.

- "queria fechar os olhos e pronto faz uns 2 dias" ➜ CONF_SINAIS: Mão difícil de abrir e fechar depois de tirar a tala (cirurgia com placa e parafusos no braço, punho ou mão)
- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ CONF_SINAIS: Febre junto com dor nas costas (cuidados da vértebra quebrada por osteoporose)
- "cabeca latejando depois da cirurgia na coluna ah e xixi rosinha depois da cistoscopia" ➜ CONF_SINAIS: Pancada na cabeça durante o jogo e ficou zonzo, com dor de cabeça ou enjoo

### 8. Resultado de UPA sem o que fazer até chegar lá

**2179 conversas antes ➜ 84 depois.** Famílias mais atingidas: Febre e dengue (327), Gravidez e pós-parto (245), Bebês e crianças (242).

Causa: Só alguns caminhos de UPA tinham o bloco do que fazer até chegar.

- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m"
- "nao sei se e serio, to gravida de 31 semanas pontada forte na boca do estomago"
- "gravida de 9 semanas, to gravida e com manchas vermelhas e febre"

### 9. Machucado que não entrou na trilha de machucado

**594 conversas antes ➜ 572 depois.** Famílias mais atingidas: Pancada, corte e queimadura (225), Machucado no esporte (161), Queda (157).

- "no futsal doi o ossinho do tornozleo e ossinho do lado do pe doi faz umas 2 horas"
- "to c muito medo meu pai de 60 anos, caiu da escada caiu e levantou sozinho hj de manha"
- "nunca senti nada assim minha avo de 63 anos  levanta e fica tonta e tonto ao ficar de pe"

### 10. Item de gravidez ou parto para quem não está grávida

**568 conversas antes ➜ 904 depois.** Famílias mais atingidas: Gravidez e pós-parto (119), Saúde mental e crise (87), Urina e região íntima (64).

- "suspeita de zika, zika faz 2 semanas" ➜ CONF_SINAIS: Febre nos dias ou semanas depois do parto
- "dor no fundo da boca faz 2 semanas" ➜ CONF_SINAIS_2: Febre nos dias ou semanas depois do parto ou da cesárea
- "ciclo n votlou depois da curetagem essa semana" ➜ CONF_SINAIS: Tristeza ou ansiedade forte que não passa depois da perda da gravidez (Curetagem ou aspiração do útero)

### 11. Item de criança ou bebê para adulto

**438 conversas antes ➜ 340 depois.** Famílias mais atingidas: Saúde mental e crise (63), Machucado no esporte (50), Pele (43).

- "correndo no parque dor no osso do pe depois de torcer agora pouco e tbm espinha da barba" ➜ CONF_SINAIS: Criança ou adolescente com dor no joelho ou no calcanhar junto com febre, pele vermelha e quente no lugar ou sem conseguir apoiar o pé no chão
- "febrinha todo dia sem motivo desde o comeco do ano" ➜ CONF_SINAIS: Criança que começou a andar torto, perder o equilíbrio ou mudar o jeito de ser sem motivo
- "n enxergo de longe dedse o comeco do ano" ➜ CONF_SINAIS_2: Criança pequena que começou a mancar ou parou de andar depois de tropeço ou queda pequena, chora ao apoiar o pé, pouco ou nenhum inchaço

### 12. Pergunta de dor no peito num machucado de braço ou perna

**341 conversas antes ➜ 305 depois.** Famílias mais atingidas: Pancada, corte e queimadura (169), Machucado no esporte (121), Queda (51).

- "no futsal doi o ossinho do tornozleo e ossinho do lado do pe doi faz umas 2 horas" ➜ FIL_02: dor no peito
- "nao sei se e serio, levei uma pancada roxo no corpo sem pancada essa semana" ➜ FIL_02: dor no peito
- "n sei se e serio dor forte no dedo faz umas 2 horas" ➜ FIL_02: dor no peito

### 13. Pergunta de se machucar de propósito num machucado comum

**299 conversas antes ➜ 271 depois.** Famílias mais atingidas: Pancada, corte e queimadura (152), Machucado no esporte (110), Queda (37).

- "no futsal doi o ossinho do tornozleo e ossinho do lado do pe doi faz umas 2 horas" ➜ FIL_08: pensar em se machucar
- "nao sei se e serio, levei uma pancada roxo no corpo sem pancada essa semana" ➜ FIL_08: pensar em se machucar
- "n sei se e serio dor forte no dedo faz umas 2 horas" ➜ FIL_08: pensar em se machucar

### 14. Item de osteoporose para quem não tem

**249 conversas antes ➜ 117 depois.** Famílias mais atingidas: Idosos e anticoagulante (51), Febre e dengue (46), Cirurgia, crônicos e 'não é nada' (21).

- "minha mae de 64 anos ta com dor nas juntas depois da chikungunya e dor nas juntas desde a chikungunya ja tem m" ➜ CONF_SINAIS: Febre junto com dor nas costas (cuidados da vértebra quebrada por osteoporose)
- "tontura ao deitar ou virar na cama faz 2 semanas" ➜ CONF_SINAIS: Dor que acorda à noite ou piora quando deita (cuidados da vértebra quebrada por osteoporose)
- "meu pai de 86 anos, tombo leve sem machucado, caiu sentado e ja levantou desde ontem" ➜ CONF_SINAIS: Dor forte nas costas depois de queda em idoso ou em quem tem osteoporose

### 15. Pergunta de AVC num machucado de braço ou perna

**249 conversas antes ➜ 214 depois.** Famílias mais atingidas: Machucado no esporte (112), Pancada, corte e queimadura (108), Queda (29).

- "no futsal doi o ossinho do tornozleo e ossinho do lado do pe doi faz umas 2 horas" ➜ FIL_04: AVC
- "nao sei se e serio, levei uma pancada roxo no corpo sem pancada essa semana" ➜ FIL_04: AVC
- "n sei se e serio dor forte no dedo faz umas 2 horas" ➜ FIL_04: AVC

### 16. Item de transplante ou HIV para quem não tem

**229 conversas antes ➜ 313 depois.** Famílias mais atingidas: Urina e região íntima (69), Barriga (49), Cirurgia, crônicos e 'não é nada' (36).

- "pele descascando na quimio agora pouco faco tratamento de cancer" ➜ CONF_SINAIS: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "minha avo de 78 anos tá: colica em onda com tumor intestinal faco tratamento de cancer" ➜ CONF_SINAIS: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "acho q n e nada mas corte q n estanca na quimio essa semana faco tratamento de cancer" ➜ CONF_SINAIS: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer

### 17. Pergunta de confusão e sonolência num machucado de braço ou perna

**220 conversas antes ➜ 193 depois.** Famílias mais atingidas: Machucado no esporte (104), Pancada, corte e queimadura (94), Queda (22).

- "no futsal doi o ossinho do tornozleo e ossinho do lado do pe doi faz umas 2 horas" ➜ GEN_01: confusão e sonolência
- "nao sei se e serio, levei uma pancada roxo no corpo sem pancada essa semana" ➜ GEN_01: confusão e sonolência
- "n sei se e serio dor forte no dedo faz umas 2 horas" ➜ GEN_01: confusão e sonolência

### 18. Vômito ou diarreia sem perguntar se bebe líquido e faz xixi

**211 conversas antes ➜ 146 depois.** Famílias mais atingidas: Barriga (211).

Causa: Não existia pergunta sobre beber líquido e fazer xixi no caminho genérico.

- "minha avo de 74 anos, caroco na axila doi apos beber e ingua doi com alcool hj" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi
- "dor de estomago q sempre volta e barriga inchada todo dia agora" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi
- "nao sei se e serio, orelha de cachorro na abdomino e queloide depois da plastica da barriga" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi

### 19. Item de câncer para quem não tem

**169 conversas antes ➜ 518 depois.** Famílias mais atingidas: Cabeça e tontura (31), Febre e dengue (30), Pele (17).

- "deve ser bobeira mas coceira forte, alergia na pele faz uns 2 dias alem disso pele descascando da agua sanitar" ➜ CONF_SINAIS: Manchas vermelhas espalhadas com bolhas ou pele soltando em quem faz quimioterapia ou imunoterapia
- "vomitando sem parar, nem agua para no estomago" ➜ CONF_SINAIS: Vômitos que não deixam beber nem segurar o remédio durante a quimioterapia
- "acho q nao e nada mas perna formigando e subindo desde ontem" ➜ CONF_SINAIS: Fraqueza, peso ou dormência nas pernas ou perda do controle do xixi ou cocô em quem tem câncer

### 20. Item de diálise para quem não faz

**163 conversas antes ➜ 163 depois.** Famílias mais atingidas: Urina e região íntima (45), Queda (18), Pele (15).

- "levei um tombo de bicicleta pulso c um degaru tbm caiu e o pulso ficou estranho faz uns 2 dias" ➜ CONF_SINAIS: Fístula da hemodiálise parou de vibrar ou de fazer o barulhinho
- "nunca senti nada assim dor no pulso do lado do dedao hj" ➜ CONF_SINAIS: Fístula da hemodiálise parou de vibrar ou de fazer o barulhinho
- "minha mae sangrou depois da menopausa e ja parei de menstruar e sangrei desde segunda" ➜ CONF_SINAIS_2: Fístula da hemodiálise sangrando muito e sem parar mesmo apertando

## 4. Por família (treino, depois das correções)

| Família | Casos | Nota antes | Nota depois | Perguntas depois | Destino exato depois | Problema mais comum depois |
| --- | --- | --- | --- | --- | --- | --- |
| Barriga | 673 | 5,67 | 6,81 | 8,2 | 70,6% | Lista com item de outra parte do corpo |
| Bebês e crianças | 674 | 8,02 | 8,57 | 6,5 | 69,9% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Cabeça e tontura | 667 | 7,37 | 8,25 | 4,7 | 75,4% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Cirurgia, crônicos e 'não é nada' | 668 | 6,03 | 7,22 | 7,8 | 71,9% | Lista com item de outra parte do corpo |
| Febre e dengue | 670 | 5,47 | 7,33 | 8,9 | 54,6% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Gravidez e pós-parto | 672 | 5,62 | 7,13 | 8,1 | 59,4% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Idosos e anticoagulante | 670 | 7,28 | 8,04 | 4,9 | 66,1% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Intoxicação e animais | 658 | 7,76 | 8,28 | 6,5 | 68,8% | Perguntas demais para o caso |
| Machucado no esporte | 660 | 6,4 | 7,58 | 6,3 | 66,2% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Pancada, corte e queimadura | 677 | 6,83 | 7,61 | 4,7 | 72,2% | Lista com item de outra parte do corpo |
| Peito e falta de ar | 642 | 7,17 | 8,07 | 6,3 | 67,9% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Pele | 666 | 5,23 | 6,8 | 10,8 | 60,2% | Perguntas demais para o caso |
| Queda | 660 | 8,66 | 9,27 | 1,8 | 83,0% | Machucado que não entrou na trilha de machucado |
| Saúde mental e crise | 674 | 7,23 | 8,47 | 7,1 | 64,8% | Perguntas demais para o caso |
| Urina e região íntima | 669 | 4,92 | 6,91 | 10,8 | 58,3% | Perguntas demais para o caso |

## 5. O que ainda falta (de propósito ou para a próxima rodada)

- **Itens graves continuam na lista mesmo quando podem não ter relação.** Exemplo: lábio inchando de repente aparece para quem tem dor de estômago. A regra é: item leve sem relação sai; item grave só sai se for de outra parte do corpo e o relato não descrever nada dele. Isso segura a segurança, mas mantém parte dos itens "estranhos".
- **O caminho genérico ainda é longo** (8 perguntas de emergência e 6 gerais). O próximo passo é juntar as perguntas de emergência numa tela só, com uma lista para marcar.
- **"Não sei" em pergunta de emergência vale como "sim"** e manda para o SAMU. É uma regra de segurança do motor (M02): fica, mas aumenta a sobretriagem.
- **Trilhas próprias** (como a do machucado) ainda faltam para barriga, febre em criança e falta de ar. Elas cortariam o caminho genérico para esses casos.
- O avaliador é automático: ele usa palavras para decidir se um item é de outra parte do corpo ou de outro grupo, e às vezes erra. Os números servem para comparar antes e depois.

## Arquivos

- `casos_simulados.csv` e `casos_simulados.jsonl`: os 12.000 casos, com relato, ficha e fonte
- `feedback_treino.jsonl` e `feedback_prova.jsonl`: uma linha por conversa (os `_base` são de antes das correções)
- `conversas_*.jsonl`: cada conversa inteira, pergunta por pergunta
- `CHANGELOG_SIMULADOS.md`: cada correção, com os números antes e depois
