# Testes simulados da Tami: 12.000 conversas com feedback

Cada caso é uma pessoa simulada com uma ficha escondida (idade, o que aconteceu, a dor, os sinais que tem). Ela conversa com o motor de verdade e responde com sinceridade, às vezes "Não sei" e às vezes se confunde. Cada conversa recebe um feedback: perguntas sem relação, perguntas que faltaram, cuidados no fim, uma nota de 0 a 10 e um comentário.

> O destino esperado de cada caso vem de um sinal da base de conhecimento, escrito a partir de fontes públicas (NHS, MedlinePlus, Ministério da Saúde), e **ainda precisa de revisão médica**. Os números medem se a conversa faz sentido e se o motor segue as regras, não se a orientação é clinicamente correta.

## 1. Prova final (2.000 casos que não foram olhados durante as correções)

| Medida | Antes | Depois |
| --- | --- | --- |
| Destino exato | 65,7% (63,6% a 67,8%) | 70,8% (68,7% a 72,7%) |
| Urgentes que foram para UPA ou SAMU | 99,9% (99,5% a 100,0%) | 100,0% (99,7% a 100,0%) |
| Urgente mandado para UBS ou casa | 0,1% (0,0% a 0,5%) | 0,0% (0,0% a 0,3%) |
| Conversas com pergunta ou item sem relação | 54,7% (52,5% a 56,9%) | 35,7% (33,6% a 37,8%) |
| Conversas em que faltou uma pergunta importante | 3,6% (2,9% a 4,5%) | 8,1% (6,9% a 9,3%) |
| UBS ou casa sem nenhum cuidado no fim | 0,0% (0,0% a 0,8%) | 0,0% (0,0% a 0,7%) |
| Perguntas por conversa (média) | 6,89 | 4,71 |
| Nota de uma pessoa real (0 a 10) | 7,97 | 9,08 |

Intervalos de confiança de 95% pelo método de Wilson.

## 2. Casos de treino (10.000)

| Medida | Antes | Depois |
| --- | --- | --- |
| Destino exato | 67,9% (67,0% a 68,8%) | 73,2% (72,3% a 74,1%) |
| Urgentes que foram para UPA ou SAMU | 99,9% (99,8% a 99,9%) | 99,9% (99,7% a 99,9%) |
| Urgente mandado para UBS ou casa | 0,1% (0,1% a 0,2%) | 0,1% (0,1% a 0,3%) |
| Conversas com pergunta ou item sem relação | 54,7% (53,7% a 55,6%) | 35,8% (34,9% a 36,8%) |
| Conversas em que faltou uma pergunta importante | 3,9% (3,5% a 4,3%) | 8,4% (7,8% a 8,9%) |
| UBS ou casa sem nenhum cuidado no fim | 0,0% (0,0% a 0,2%) | 0,0% (0,0% a 0,1%) |
| Perguntas por conversa (média) | 6,88 | 4,67 |
| Nota de uma pessoa real (0 a 10) | 7,98 | 9,08 |

## 3. Os 20 problemas mais comuns antes das correções

### 1. Mais cuidadosa que o necessário (destino mais urgente que o esperado)

**3185 conversas antes ➜ 2651 depois.** Famílias mais atingidas: Febre e dengue (287), Urina e região íntima (276), Pele (256).

- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ esperado UBS_EM_BREVE, a Tami deu UPA_HOJE
- "bati com forca na prota febre" ➜ esperado CUIDADO_EM_CASA, a Tami deu UBS_EM_BREVE
- "escape no meio do mes desde seugnda" ➜ esperado UBS_EM_BREVE, a Tami deu SAMU_192

### 2. Lista com item de outra parte do corpo

**2827 conversas antes ➜ 1008 depois.** Famílias mais atingidas: Barriga (300), Pele (298), Cirurgia, crônicos e 'não é nada' (258).

Causa: A lista não sabia onde era o problema quando a pessoa não tocava no corpo.

- "cancer na lingua to fazendo quimio" ➜ CONF_SINAIS: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "siso arrancado latejando 3 dias depois e buraco do siso vazio e doendo desde ontem" ➜ CONF_SINAIS: Dor forte na barriga que só piora, com barriga dura ou mal-estar, depois de lipoaspiração (Lipoaspiração)
- "tomei um chute crise de sinusite, dor ou pressão no rosto e na testa agora pouco" ➜ CONF_SINAIS: Atraso menstrual com dor leve de um lado da barriga e sangramento escuro

### 3. Perguntas demais para o caso

**2676 conversas antes ➜ 479 depois.** Famílias mais atingidas: Urina e região íntima (360), Intoxicação e animais (346), Pele (334).

Causa: O caminho genérico tem 8 perguntas de emergência e 6 gerais, além da lista.

- "cancer na lingua to fazendo quimio" ➜ 16 perguntas
- "meu vo de 68 anos reclama de pele queimada de produto" ➜ 12 perguntas
- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ 11 perguntas

### 4. Item de "depois de queda ou pancada" para quem não se machucou

**1366 conversas antes ➜ 583 depois.** Famílias mais atingidas: Urina e região íntima (140), Saúde mental e crise (140), Intoxicação e animais (135).

Causa: A lista mostrava sinais de "depois de queda" para quem não caiu.

- "escape no meio do mes desde seugnda" ➜ CONF_SINAIS_2: Pancada na cabeça durante o jogo e ficou zonzo, com dor de cabeça ou enjoo
- "meu vo de 63 anos ta fzees escuras c anticoagulante agora uso anticoagulante" ➜ CONF_SINAIS: Toma remédio para afinar o sangue e bateu a cabeça, mesmo sem sintomas
- "nao sei se e serio  meu menino de 2 anos ta  urticaria agora" ➜ CONF_SINAIS: Criança que não consegue fazer nada de xixi depois da cirurgia de fimose (Cirurgia de fimose em criança)

### 5. Item de cirurgia para quem não operou

**1316 conversas antes ➜ 462 depois.** Famílias mais atingidas: Febre e dengue (158), Barriga (136), Gravidez e pós-parto (133).

Causa: A base tem 215 sinais de pós-operatório e a busca por palavras parecidas trazia todos para quem tinha febre ou dor.

- "bati com forca na prota febre" ➜ CONF_SINAIS_2: Febre ou calafrios nos dias depois de uma cirurgia
- "bolha de agua quente hj de mahna" ➜ CONF_SINAIS: Corte da cirurgia ficando mais vermelho, quente, inchado ou dolorido
- "meu vo de 81 anos ta  dedos inchados no gesso essa semana alem disso costas travadas" ➜ CONF_SINAIS: Mão difícil de abrir e fechar depois de tirar a tala ou o gesso (cirurgia com fios no osso)

### 6. Pergunta de veneno ou remédio demais sem motivo no relato

**1009 conversas antes ➜ 1000 depois.** Famílias mais atingidas: Idosos e anticoagulante (184), Pele (149), Cirurgia, crônicos e 'não é nada' (145).

Causa: O filtro geral fazia as 8 perguntas de emergência para todo mundo.

- "cancer na lingua to fazendo quimio" ➜ FIL_07: veneno ou remédio demais
- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ FIL_07: veneno ou remédio demais
- "meu vo de 81 anos ta  dedos inchados no gesso essa semana alem disso costas travadas" ➜ FIL_07: veneno ou remédio demais

### 7. Item de gravidez ou parto para quem não está grávida

**709 conversas antes ➜ 358 depois.** Famílias mais atingidas: Bebês e crianças (96), Urina e região íntima (90), Febre e dengue (82).

- "meu bebe de 23 meses gripe que n melhora tbm caatrro faz mais de dez dias hj" ➜ CONF_SINAIS: Gripe ou covid com febre em idoso, grávida, criança pequena ou pessoa com doença crônica
- "febre e sou transplantado essa semana sou transplantado" ➜ CONF_SINAIS: Febre nos dias ou semanas depois do parto ou da cesárea
- "mniha avo de 83 anos ta minha avo esta esquecendo tudo hoje de manha" ➜ CONF_SINAIS_2: Gripe ou covid com febre em idoso, grávida, criança pequena ou pessoa com doença crônica

### 8. Pergunta de alergia com inchaço sem motivo no relato

**612 conversas antes ➜ 638 depois.** Famílias mais atingidas: Cabeça e tontura (124), Barriga (68), Pancada, corte e queimadura (60).

Causa: O filtro geral fazia as 8 perguntas de emergência para todo mundo.

- "cancer na lingua to fazendo quimio" ➜ FIL_06: alergia com inchaço
- "to gravida de 10 semanas vendo pontinhos brilhando e gravida c dor de cabeca forte desde cedo alem disso fique" ➜ FIL_06: alergia com inchaço
- "sinusite de novo esse mes e nariz entupido ha semanas faz 3 dias" ➜ FIL_06: alergia com inchaço

### 9. Machucado que não entrou na trilha de machucado

**569 conversas antes ➜ 493 depois.** Famílias mais atingidas: Pancada, corte e queimadura (232), Machucado no esporte (148), Queda (147).

- "cancer na lingua to fazendo quimio"
- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana"
- "bati com forca na prota febre"

### 10. Item de câncer para quem não tem

**543 conversas antes ➜ 88 depois.** Famílias mais atingidas: Barriga (82), Febre e dengue (70), Cabeça e tontura (58).

- "to gravida de 10 semanas vendo pontinhos brilhando e gravida c dor de cabeca forte desde cedo alem disso fique" ➜ CONF_SINAIS_2: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "vive sangrando a boca tbm gengiva sangrando toda hora" ➜ CONF_SINAIS_2: Sangramento que não para ou manchas roxas grandes em quem faz tratamento de câncer
- "nao sei se e serio  tremedeira e diarreia c antidepressivo novo agora pouco" ➜ CONF_SINAIS_2: Diarreia aquosa muitas vezes ao dia durante a quimioterapia

### 11. Item de transplante ou HIV para quem não tem

**332 conversas antes ➜ 181 depois.** Famílias mais atingidas: Urina e região íntima (113), Barriga (54), Cirurgia, crônicos e 'não é nada' (44).

- "cancer na lingua to fazendo quimio" ➜ CONF_SINAIS: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "deve ser bobeira mas perdendo xixi depois de tirar a prostata faz uns 2 dias to fazendo quimio" ➜ CONF_SINAIS_2: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer
- "ca de esofago faco tratamento de cnacer" ➜ CONF_SINAIS_2: Dor de cabeça nova em quem tem imunidade baixa ou já teve câncer

### 12. Pergunta de dor no peito num machucado de braço ou perna

**318 conversas antes ➜ 0 depois.** Famílias mais atingidas: Pancada, corte e queimadura (167), Machucado no esporte (107), Queda (44).

- "cancer na lingua to fazendo quimio" ➜ FIL_02: dor no peito
- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ FIL_02: dor no peito
- "bati com forca na prota febre" ➜ FIL_02: dor no peito

### 13. Item de criança ou bebê para adulto

**310 conversas antes ➜ 267 depois.** Famílias mais atingidas: Urina e região íntima (40), Machucado no esporte (39), Febre e dengue (38).

- "meu vo de 68 anos reclama de pele queimada de produto" ➜ CONF_SINAIS_2: Criança, bebê ou idoso com queimadura que fez bolha
- "corrimento com odor que nao melhora agora pouco" ➜ CONF_SINAIS: Criança ativa com dor atrás do calcanhar que piora correndo ou pulando e melhora no descanso, andando normal
- "to de 15 semanas barriga contraindo e depois relaxa desde ontem" ➜ CONF_SINAIS_2: Bebê mexendo menos que o normal ou parou de mexer na barriga

### 14. Pergunta de se machucar de propósito num machucado comum

**284 conversas antes ➜ 247 depois.** Famílias mais atingidas: Pancada, corte e queimadura (152), Machucado no esporte (95), Queda (37).

- "cancer na lingua to fazendo quimio" ➜ FIL_08: pensar em se machucar
- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ FIL_08: pensar em se machucar
- "bati com forca na prota febre" ➜ FIL_08: pensar em se machucar

### 15. Item de diabetes para quem não tem

**280 conversas antes ➜ 62 depois.** Famílias mais atingidas: Febre e dengue (43), Idosos e anticoagulante (41), Cabeça e tontura (35).

- "fiz canal e a dor voltou e fiz canal e segue doendo faz uns 2 dias e tbm hipoglicemia" ➜ CONF_SINAIS_2: Pessoa com diabetes que ficou confusa de repente
- "meu vo de 69 anos tá: levou uma pancada ulcera de pressao, machucado de ficar acamado hoje de manha" ➜ CONF_SINAIS: Diabético com ferida ou bolha no pé que não cicatriza
- "freida que n sara na pele desde segunda" ➜ CONF_SINAIS: Diabético com ferida ou bolha no pé que não cicatriza

### 16. Pergunta de AVC num machucado de braço ou perna

**225 conversas antes ➜ 0 depois.** Famílias mais atingidas: Pancada, corte e queimadura (106), Machucado no esporte (94), Queda (25).

- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ FIL_04: AVC
- "bati com forca na prota febre" ➜ FIL_04: AVC
- "ca de esofago faco tratamento de cnacer" ➜ FIL_04: AVC

### 17. Pergunta de confusão e sonolência num machucado de braço ou perna

**197 conversas antes ➜ 157 depois.** Famílias mais atingidas: Pancada, corte e queimadura (92), Machucado no esporte (86), Queda (19).

- "meu vo de 84 aons, bateu na quina da mesa dedo ainda n estica sozinho, ponta do dedo ainda caida essa semana" ➜ GEN_01: confusão e sonolência
- "ca de esofago faco tratamento de cnacer" ➜ GEN_01: confusão e sonolência
- "minha mae de 85 anos reclama de bateu de moto fratura no esterno agora e tbm claridade incomodando apos exame " ➜ GEN_01: confusão e sonolência

### 18. Item de diálise para quem não faz

**158 conversas antes ➜ 164 depois.** Famílias mais atingidas: Urina e região íntima (41), Queda (17), Cirurgia, crônicos e 'não é nada' (17).

- "to de 8 semanas de barriga e sangrando agora" ➜ CONF_SINAIS_2: Fístula da hemodiálise sangrando muito e sem parar mesmo apertando
- "acho q nao e nada mas anida nao escuta depois do tubinho faz 2 semnaas" ➜ CONF_SINAIS: Fístula da hemodiálise parou de vibrar ou de fazer o barulhinho
- "sangue depois da trsana hj" ➜ CONF_SINAIS_2: Fístula da hemodiálise sangrando muito e sem parar mesmo apertando

### 19. Vômito ou diarreia sem perguntar se bebe líquido e faz xixi

**146 conversas antes ➜ 152 depois.** Famílias mais atingidas: Barriga (146).

Causa: Não existia pergunta sobre beber líquido e fazer xixi no caminho genérico.

- "intestino travado faz 2 semanas" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi
- "acho q nao e nada mas dor de menstruacao" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi
- "xixi de noite toda hora" ➜ faltou perguntar se está conseguindo beber líquido e fazer xixi

### 20. Item de remédio para afinar o sangue para quem não toma

**123 conversas antes ➜ 67 depois.** Famílias mais atingidas: Pancada, corte e queimadura (50), Urina e região íntima (23), Pele (13).

- "tomei um chute crise de sinusite, dor ou pressão no rosto e na testa agora pouco" ➜ CONF_SINAIS: Adulto bateu a cabeça de leve, sem desmaio, sem vômito e sem remédio para afinar o sangue
- "bati com forca na prota febre" ➜ CONF_SINAIS_2: Adulto bateu a cabeça de leve, sem desmaio, sem vômito e sem remédio para afinar o sangue
- "bati c forca na porta dente saiu inteiro c a raiz depois de pancada tbm dente arrancado" ➜ CONF_SINAIS: Adulto bateu a cabeça de leve, sem desmaio, sem vômito e sem remédio para afinar o sangue

## 4. Por família (treino, depois das correções)

| Família | Casos | Nota antes | Nota depois | Perguntas depois | Destino exato depois | Problema mais comum depois |
| --- | --- | --- | --- | --- | --- | --- |
| Barriga | 656 | 6,95 | 9,04 | 5,5 | 77,1% | Vômito ou diarreia sem perguntar se bebe líquido e faz xixi |
| Bebês e crianças | 656 | 8,79 | 9,48 | 4,4 | 76,1% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Cabeça e tontura | 669 | 8,57 | 9,17 | 3,3 | 82,5% | faltou:sinais_avc |
| Cirurgia, crônicos e 'não é nada' | 674 | 7,42 | 8,92 | 5,4 | 77,7% | Pergunta de veneno ou remédio demais sem motivo no relato |
| Febre e dengue | 669 | 7,48 | 8,8 | 5,8 | 63,7% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Gravidez e pós-parto | 659 | 8,04 | 9,01 | 5,0 | 68,3% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Idosos e anticoagulante | 661 | 8,31 | 9,18 | 3,7 | 70,5% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Intoxicação e animais | 669 | 8,36 | 9,12 | 4,3 | 77,1% | faltou:sinais_graves_intox |
| Machucado no esporte | 668 | 7,73 | 9,18 | 4,2 | 69,3% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Pancada, corte e queimadura | 686 | 7,53 | 8,71 | 3,7 | 71,9% | Machucado que não entrou na trilha de machucado |
| Peito e falta de ar | 673 | 8,24 | 9,02 | 4,3 | 71,8% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Pele | 663 | 7,35 | 9,05 | 7,0 | 67,7% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |
| Queda | 657 | 9,27 | 9,65 | 1,6 | 84,5% | Machucado que não entrou na trilha de machucado |
| Saúde mental e crise | 660 | 8,77 | 9,27 | 5,4 | 70,3% | Perguntas demais para o caso |
| Urina e região íntima | 680 | 6,98 | 8,66 | 6,6 | 69,9% | Mais cuidadosa que o necessário (destino mais urgente que o esperado) |

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
