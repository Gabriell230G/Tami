# Prompt: 10.000+ conversas simuladas com feedback, para melhorar a Tami

Cole este texto numa IA com acesso ao código da Tami (pasta `app/` do repositório Gabriell230G/tami). Ele pede um teste em massa parecido com o que o Gabriel fez à mão no caso do joelho: conversar como uma pessoa real e dizer o que ficou ruim na conversa, e não só se o destino final acertou.

---

Você vai testar e melhorar a **conversa** da Tami, um app de orientação em saúde em Java. A Tami não diagnostica e não indica remédio, dose nem exame. Ela só diz para onde ir: SAMU 192, UPA hoje, UBS em breve ou cuidar em casa.

Já existe um teste de 10.000 casos (`vinhetas_10k.csv`) que mede **só o destino**. Ele não pegou o problema real que apareceu: uma pessoa com a perna machucada no judô recebeu 16 perguntas, várias sem relação (colesterol, urina, gravidez), nenhuma sobre como se machucou, a dor em 3 faixas e nenhum cuidado no fim. O destino estava "certo", mas a conversa era ruim.

Seu trabalho é criar um teste que pegue esse tipo de problema, rodar, dar feedback de cada conversa e corrigir.

## Regras que não mudam

1. Não mexa nos textos de 192, 188 (CVV), 190 e CIATox.
2. Não invente números, estudos, depoimentos nem fontes. O destino esperado de cada caso vem de fonte pública (NHS, MedlinePlus, Ministério da Saúde) com o link, e fica marcado como "precisa de revisão médica".
3. Nunca indique remédio, dose ou exame, nem nos cuidados.
4. Segurança vem antes de conforto. Nenhuma mudança pode baixar a sensibilidade para urgência (hoje 99,38%) nem aumentar os graves mandados para mais leve (hoje 32 de 4.821) no teste de 10.000 casos que já existe.
5. Linguagem simples, sem travessão, com ➜ para indicar caminho.

## Passo 1: criar os casos (12.000, com 2.000 guardados)

Gere **12.000 casos** em 15 famílias. Para cada caso, escreva duas coisas:

**A. O que a pessoa digita** (o relato livre), do jeito que gente de verdade escreve:
- erros de digitação, abreviação ("vc", "q", "tbm", "hj"), gíria, sem pontuação;
- relato curto ("dor no joelho") e longo (história inteira);
- quem escreve: a própria pessoa, mãe ou pai de criança, filho de idoso, amigo;
- às vezes minimiza ("deve ser bobeira"), às vezes exagera, às vezes mistura duas queixas.

**B. A ficha escondida do caso**, que só o simulador vê e usa para responder as perguntas com sinceridade:
- idade, gestante, doenças, remédios de uso contínuo (sem dose);
- o que aconteceu de verdade, como e há quanto tempo;
- a dor de 0 a 10;
- quais sinais de alerta a pessoa tem e quais não tem;
- o destino esperado, com a fonte;
- **as perguntas que uma boa conversa faria** para esse caso (ex.: torção ➜ como aconteceu, consegue apoiar, ouviu estalo, ficou torto, inchaço);
- **as perguntas que não fazem sentido** para esse caso (ex.: torção ➜ colesterol, urina, gravidez num homem).

Famílias (mínimo de 600 casos cada):
1. Machucado no esporte (judô, futebol, corrida, academia, luta)
2. Queda em casa ou na rua (adulto, criança, idoso)
3. Pancada, corte e queimadura
4. Dor de cabeça e tontura
5. Febre (adulto, criança, bebê) e dengue
6. Barriga: dor, vômito, diarreia
7. Peito e falta de ar
8. Pele: mancha, coceira, ferida, picada
9. Urina e região íntima
10. Gravidez e pós-parto
11. Bebês e crianças pequenas
12. Idosos e quem toma remédio para afinar o sangue
13. Saúde mental e crise (sempre com o CVV 188 quando houver risco)
14. Intoxicação, bicho peçonhento e animais
15. Depois de cirurgia, doenças crônicas e "não é nada"

Separe **2.000 casos para a prova final** e não olhe para eles enquanto estiver corrigindo. Corrigir olhando para a prova faz a Tami decorar o teste.

## Passo 2: simular a pessoa respondendo

Rode cada caso direto no motor (`MotorTriagem`, como faz o `Validacao.java`), sem passar pela internet. A pessoa simulada:
- responde com base na ficha escondida, com sinceridade;
- em 8% das perguntas responde "Não sei" quando a ficha não diz;
- em 3% das perguntas entende errado e escolhe uma opção vizinha (para testar se a Tami aguenta erro humano);
- na escala de dor escolhe o número da ficha, com variação de ±1;
- se a pergunta for sobre algo que não tem nada a ver com ela, responde "Não" e marca a pergunta como irrelevante.

Guarde cada conversa inteira: as perguntas na ordem, as opções mostradas, as respostas, o destino e os blocos de cuidado.

## Passo 3: dar feedback de cada conversa

Para cada conversa, preencha esta ficha (um JSON por linha, arquivo `feedback.jsonl`):

```json
{
  "caso": "ESP-0412",
  "relato": "torci o joelho no futsal ontem, ta inchado",
  "destino_esperado": "UBS_EM_BREVE",
  "destino_tami": "CUIDADO_EM_CASA",
  "seguranca": "subtriagem_leve",
  "perguntas_feitas": 7,
  "perguntas_ideal": 5,
  "irrelevantes": ["CONF_SINAIS: Dor nas costas com fraqueza nas duas pernas"],
  "faltaram": ["Consegue esticar o joelho todo?"],
  "repetidas": [],
  "estranhas_para_o_perfil": [],
  "cuidados_no_fim": "ok",
  "cuidados_que_faltaram": [],
  "linguagem": "ok",
  "nota_pessoa_real": 7,
  "comentario": "Perguntou de costas e das duas pernas pra quem torceu um joelho. Faltou perguntar se o joelho trava."
}
```

Como dar cada nota:
- **seguranca**: `ok`, `sobretriagem` (mais cuidadoso que o necessário), `subtriagem_leve` (UBS ➜ casa) ou `subtriagem_critica` (urgente ➜ UBS ou casa).
- **irrelevantes**: pergunta ou item de lista que a ficha marca como sem sentido, ou que fala de um grupo de que a pessoa não faz parte.
- **faltaram**: pergunta da lista de "boa conversa" da ficha que não apareceu.
- **cuidados_no_fim**: `ok`, `faltou` (casa ou UBS sem nenhum cuidado) ou `inadequado` (cuidado que não combina, ou que cita remédio).
- **nota_pessoa_real** de 0 a 10: como uma pessoa real se sentiria. Desconte 1 ponto por pergunta irrelevante, 1 por pergunta que faltou, 2 se o fim veio sem cuidado, e zere se houve subtriagem crítica.
- **comentario**: uma ou duas frases, no tom de quem testou de verdade. Exemplo: "ficou perguntando de colesterol e não perguntou como eu me machuquei".

## Passo 4: relatório e correções

Junte tudo num relatório curto:
1. **Números gerais**, com intervalo de confiança de 95% (Wilson): destino exato, sensibilidade para urgência, subtriagem crítica, média e mediana de perguntas, % de conversas com pergunta irrelevante, % com pergunta que faltou, % sem cuidado no fim e nota média.
2. **Os 20 problemas mais comuns**, cada um com quantos casos, a família, 3 exemplos reais (relato + pergunta ruim) e a causa no código ou na planilha.
3. **Por família**: tabela com nota média, perguntas por conversa e os problemas principais.

Depois corrija, um problema por vez, do mais comum para o menos comum:
- prefira mudar a planilha (`nos.csv`, `opcoes.csv`, `sinais_alerta.csv`, `textos_tami.csv`) a mudar o código;
- todo sinal novo leva fonte oficial e `revisar_medico=SIM`;
- depois de cada correção, rode **os dois testes**: o novo (10.000 casos de treino) e o antigo (`vinhetas_10k.csv`);
- se a sensibilidade cair ou a subtriagem crítica subir, desfaça a correção, por melhor que ela seja para a conversa;
- anote cada correção num CHANGELOG: o problema, o que mudou e os números antes e depois.

## Passo 5: prova final

Só no fim, rode os 2.000 casos guardados uma única vez e mostre a mesma tabela do Passo 4, lado a lado: antes das correções e depois.

## O que entregar

1. `casos_simulados.csv`: os 12.000 casos, com relato, ficha e fonte.
2. `feedback.jsonl`: uma linha por conversa.
3. `relatorio_testes.md`: números, os 20 problemas, a tabela por família e a prova final.
4. As correções no código e nas planilhas, com o CHANGELOG.
5. O painel do app com uma aba "Conversas simuladas": nota média, % de perguntas irrelevantes e os 5 problemas mais comuns.

## Testes que precisam continuar passando

| Relato | Esperado |
| --- | --- |
| "estou com dor na perna, acho que luxei" | no máximo 7 perguntas, pergunta como aconteceu, fim com cuidados |
| "torci o pé no judô e não consigo pisar" | UPA hoje |
| "caí do telhado e o osso tá aparecendo" | SAMU 192 |
| "dor na perna, inchada e vermelha na batata, sem ter batido" | não entra na trilha de machucado |
| "to pensando em acabar com tudo" | CVV 188 aparece sempre |
| "meu filho de 2 anos tomou remédio da avó" | CIATox aparece sempre |
