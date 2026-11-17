# Auditoria do avaliador (Passo 1 do PROMPT_NOTA_9)

O avaliador é a parte do simulador que dá a nota de cada conversa (`avaliar()` em `ferramentas/simulados/simular.py`). Antes de mexer na Tami, conferimos se ele estava julgando certo.

## Como foi feito

1. Sorteamos 300 conversas de treino com nota 8 ou menos (sorteio fixo, sempre o mesmo).
2. Nessas conversas, o avaliador marcou 1.120 itens como "sem relação com o relato".
3. Lemos 100 desses itens um por um, com o relato da pessoa ao lado, e demos um rótulo para cada:
   - **SR** sem relação de verdade (o avaliador acertou);
   - **TR** tem relação (o avaliador errou);
   - **GP** grave e plausível (um profissional perguntaria mesmo sem ligação direta);
   - **MAL** caso mal gerado pelo simulador (não conta para nenhum lado).

## Resultado

| Rótulo | Itens |
| --- | --- |
| Sem relação de verdade (SR) | 77 |
| Tem relação, o avaliador errou (TR) | 13 |
| Grave e plausível (GP) | 7 |
| Caso mal gerado (MAL) | 3 |

- **Precisão antes:** 77 de 100 = **77%** (abaixo dos 90% pedidos, então corrigimos).
- **Precisão depois:** 77 de 86 itens que ele ainda marca, sem contar os MAL = **89,5%**.
- **Itens sem relação que ele continua achando:** 68 de 77 = 88,3%.

## O que mudou no avaliador (só as regras que erraram)

| Regra | Antes | Depois |
| --- | --- | --- |
| Parte do corpo do caso | vinha da descrição do sinal sorteado | vem do que a pessoa escreveu e do lugar que tocou; a descrição só entra se não houver nada |
| Gravidez | "item de gravidez" para quem acabou de ter bebê | pós-parto (parto, cesárea, resguardo, amamentando, "tive o bebê") conta como do grupo da gravidez |
| Cirurgia | só via o campo da ficha | também vê no relato ("operei", "amígdalas", "prótese", "colonoscopia", "pino") |
| Mesmas palavras | ignorava | se o item repete boa parte das palavras do relato (raiz de 5 letras, 34% ou mais), tem relação |
| Grave e plausível | contava como erro | item grave da mesma parte do corpo e com palavra em comum com o relato não tira ponto (vai para `graves_plausiveis`) |
| Pergunta de veneno (FIL_07) | sempre irrelevante fora das famílias de intoxicação | faz sentido se o relato fala de vômito, sonolência, confusão, desmaio, tontura, tremor ou convulsão |
| Pergunta de alergia (FIL_06) | idem | faz sentido se o relato fala de alergia, picada, inchaço no rosto, lábio ou garganta |

## 10 exemplos, antes e depois

| Caso | Item | Antes | Depois | Por quê |
| --- | --- | --- | --- | --- |
| SIM-10-04486 | Febre nos dias ou semanas depois do parto | sem relação (grupo gravidez) | tem relação | "sensível depois do parto": pós-parto |
| SIM-10-11768 | Febre nos dias ou semanas depois do parto | sem relação (grupo gravidez) | tem relação | "tristinha depois que ganhei o bebê": pós-parto |
| SIM-05-03308 | Febre nos dias ou semanas depois do parto ou da cesárea | sem relação | grave e plausível | "calafrio depois da histeroscopia": febre depois de procedimento é sinal que um profissional confere |
| SIM-15-10650 | Febre ou calafrios nos dias depois de uma cirurgia | sem relação (grupo cirurgia) | tem relação | "ouvido doendo após operar a garganta": operou |
| SIM-15-11829 | Falta de ar ou dor no peito depois de uma cirurgia | sem relação | grave e plausível | "quadril operado": pós-operatório |
| SIM-07-06933 | Falta de ar que vai piorando depois de pancada no peito | outra parte do corpo | tem relação | "bati a costela na quina": a parte do corpo vinha da descrição do sinal e não do relato |
| SIM-04-06159 | Pressão muito alta com dor no peito ou falta de ar | outra parte do corpo | tem relação | "deu pressão alta na farmácia": mesmas palavras |
| SIM-11-09897 | Criança caiu e não consegue mexer ou apoiar um braço ou uma perna | outra parte do corpo | tem relação | "ela não usa mais o braço": mesma parte do corpo e mesmas palavras |
| SIM-09-03481 | Sangramento no pênis do menino que não para (cirurgia de fimose) | sem relação | tem relação | "meu menino com coágulo na urina": sangue no xixi do menino |
| SIM-15-02656 | Depois de torção, não consegue apoiar o peso nem dar 4 passos | outra parte do corpo | grave e plausível | "peso e dor nas pernas": mesma parte do corpo, palavra em comum |

## Avaliador congelado

Depois desta auditoria o avaliador não mudou mais. A impressão digital (sha1) do código da função `avaliar()` é `cec32be7d46bfe8844f0a4e0fd2d22ddb361b360`. A régua antiga ficou guardada em `ferramentas/simulados/avaliador_v1.py`, e toda tabela final mostra a nota com as duas réguas (`reavaliar.py`).
