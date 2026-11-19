# Tami · Roteiro do vídeo 2: técnico (Entrega 2)

Vídeo de **até 3 minutos**: **1 minuto** para contar o que é a Tami e **2 minutos** para mostrar como ela foi feita, sem complicar. Cobre tudo o que o enunciado pede: o sistema Java funcionando, o banco **gravando, lendo e atualizando**, **quais dados** são coletados e **como são tratados**, os **conceitos do curso** e **dois desafios técnicos**.

**Alvo:** cerca de 2min55s. A contagem está no fim.

**Como falar.** Como se estivesse explicando para um amigo que não é da área. Frases curtas. Fale **enquanto** clica. "[pausa]" é 1 segundo.

**Legenda:** 🎤 fala · 🖥️ o que aparece na tela · ⌨️ o que clicar. Todo trecho de código aparece com **arquivo e linha** no canto da tela.

**Tudo o que é citado aqui foi conferido no código em 19/11** (arquivos, linhas, rotas e números).

---

## Antes de gravar (5 minutos de preparo)

1. Abra os **slides do vídeo técnico** (Tami · Vídeo técnico, 13 slides, a fala de cada um está nas notas) e deixe só quatro abas no Chrome: o app (`https://tami-app.onrender.com`) e o **Supabase** com `t_tami_conversa`, `t_tami_resposta` e `t_tami_dor` (já filtradas em `id_conversa > 7`, para não aparecer relato de testador). Código, figuras e log do Render já estão nos slides: não precisa abrir VS Code nem GitHub.
2. Abra o app uma vez antes, para o Render "acordar" (a primeira resposta pode levar uns 30 segundos).
3. No app, crie uma conta **só para o vídeo**: apelido "Teste GS", contato `teste.gs@exemplo.com`. Nunca use dados reais.
4. No Supabase, as tabelas aparecem em minúsculas (`t_tami_conversa`): o PostgreSQL guarda assim os nomes sem aspas.
5. **Nunca** mostre a aba de variáveis do Render nem o `tami.properties`: a senha do banco não pode aparecer.
6. Grave os slides no modo **Apresentar** (tela cheia). Para trocar entre slide e app, use Alt + Tab.

---

## O roteiro

O vídeo tem **duas partes**: no primeiro minuto você conta **o que é a Tami** (para quem nunca viu entender o resto), e depois mostra **como ela foi feita**, em linguagem simples. Cada termo técnico vem com uma frase que explica o que ele faz.

### Parte 1 · O que é a Tami (0:00 a 0:58)

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:00 a 0:14**<br>O gancho | 🎥 Você falando para a câmera, meio rindo, de quimono se tiver, com o **slide 1 (capa)** ao fundo ou em corte. ➜ **Slide 2 (A dúvida)**: os quatro papéis UPA? POSTO? PRONTO-SOCORRO? OU CASA? | É só eu, ou toda vez que você se machuca fica com a mesma dúvida? [pausa] Vou pra UPA? Pro posto? Pro pronto-socorro? Ou fico em casa? Eu treino judô, então essa dúvida aparece toda semana. |
| **0:14 a 0:18**<br>Não é só você | 🖥️ Ainda no **slide 2**: o **8 EM 10** com a fonte (Prefeitura de BH, jan. a jul. de 2026, 76,2%). | E não é só a gente: em BH, quase oito em cada dez atendimentos na UPA não eram urgentes. |
| **0:18 a 0:26**<br>A Tami | 🖥️ **Slide 3 (A Tami)**. | Pra isso eu criei a Tami: uma agente de saúde no celular, de graça. [pausa] Bora ver o que ela faz e como foi feita. |
| **0:26 a 0:47**<br>Como ela funciona | ⌨️ Gravação do app: digitar "torci o tornozelo no treino" ➜ as perguntas ➜ a escala de dor ➜ o resultado "cuidar em casa" com os cuidados ➜ "Usar minha localização" (num lugar público). | Você conta o que sente, do seu jeito. Ela confere primeiro se é emergência. Depois pergunta pouco e diz o próximo passo: SAMU, UPA hoje, posto em breve ou cuidar em casa. |
| **0:47 a 0:58**<br>O limite | 🖥️ Volta ao **slide 3**: o papel verde "Não diagnostica e não indica remédio". | Ela não diagnostica e não indica remédio: segue regras com fonte. [pausa] Agora, como isso foi feito. |

### Parte 2 · Como foi feita (0:58 a 2:58)

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:58 a 1:15**<br>O desenho | 🖥️ **Slide 4 (Arquitetura)** ➜ **slide 5 (No ar)**, com as linhas reais do log do Render. | Por trás, é um programa em Java num servidor na internet, que guarda tudo num banco PostgreSQL. [pausa] O app conversa com o Java, e o Java conversa com o banco. |
| **1:15 a 1:35**<br>Gravar | 🖥️ **Slide 6 (O banco)** por 2 segundos. ⌨️ App, logado como "Teste GS", com o painel **"Por dentro"** (botão `<>`) aberto ➜ mandar o relato. 🖥️ O painel mostra `POST /api/conversas` e `INSERT INTO T_TAMI_CONVERSA`. ⌨️ Responder até o resultado. ⌨️ Supabase ➜ `t_tami_conversa` e `t_tami_resposta`: as linhas novas. | Esse painel mostra tudo por dentro. Quando eu mando o relato, o Java grava uma conversa nova no banco: é uma inserção. [pausa] No fim, ele grava o destino e, como eu dei permissão, cada resposta, tudo de uma vez. Se der erro no meio, nada fica pela metade. |
| **1:35 a 1:50**<br>Atualizar e ler | ⌨️ App ➜ "Onde dói, e quanto?" ➜ 6 ➜ Guardar ➜ 3 ➜ Guardar. ⌨️ Supabase ➜ `t_tami_dor`: o nível foi de 6 para 3. ⌨️ App ➜ "Ver resumo para o atendimento". | Se eu marco dor seis e depois mudo pra três, ele atualiza a mesma linha. [pausa] E o resumo pro atendimento é uma leitura que junta tudo. |
| **1:50 a 2:08**<br>Os dados | 🖥️ **Slide 7 (Os dados)**, com `ConversaService.java` linhas 120 e 121 (a permissão de saúde). ⌨️ App ➜ Meus dados: os botões "Baixar meus dados" e "Apagar tudo". | Que dados ficam? Sem conta, só idade, parte do corpo e destino, pra estatística. O que a pessoa escreve só é guardado se ela tiver conta e der permissão. [pausa] Ela pode baixar os dados ou apagar tudo. E a localização nunca é gravada. |
| **2:08 a 2:25**<br>O que veio do curso | 🖥️ **Slide 8 (Do curso)**, com `DorDAO.java` linhas 14 a 21 (`extends BaseDAO`, o `inserir` e o `PreparedStatement`) ➜ **slide 9 (11 tabelas)** por 3 segundos, sem fala. | Do curso, usei orientação a objetos: cada parte do banco tem uma classe que cuida dela, o DAO, e todas herdam de uma classe base. Os comandos usam PreparedStatement, que impede que o texto da pessoa vire comando no banco. |
| **2:25 a 2:40**<br>Desafio 1 | 🖥️ **Slide 10 (Desafio 1)**, com `LocalService.java` linhas 36 a 38. Se quiser, mostre no app a lista com "Como chegar". | Primeiro desafio: achar a UPA real mais perto, na base do Ministério da Saúde. Ela devolve só vinte por vez. Então busco várias páginas ao mesmo tempo e guardo por doze horas, pra ficar rápido. |
| **2:40 a 2:58**<br>Desafio 2 e fim | 🖥️ **Slide 11 (Desafio 2)** ➜ **slide 12 (Resultado, 99,5%)** ➜ **slide 13**: o avatar com **"Falou em saúde? Tamo aqui."** | Segundo desafio: melhorar sem perder segurança. Trabalhei em ciclos curtos, com um quadro de tarefas, e cada mudança roda dez mil casos de teste. Se um urgente vai pra casa, eu desfaço. [pausa] Resultado: noventa e nove e meio por cento dos urgentes no lugar certo. Falou em saúde? Tamo aqui. |

## Contagem

| Bloco | Palavras faladas |
|---|---|
| 0:00 a 0:14  O gancho | 36 |
| 0:14 a 0:18  Não é só você | 19 |
| 0:18 a 0:26  A Tami | 24 |
| 0:26 a 0:47  Como ela funciona | 32 |
| 0:47 a 0:58  O limite | 16 |
| 0:58 a 1:15  O desenho | 30 |
| 1:15 a 1:35  Gravar | 48 |
| 1:35 a 1:50  Atualizar e ler | 26 |
| 1:50 a 2:08  Os dados | 43 |
| 2:08 a 2:25  O que veio do curso | 40 |
| 2:25 a 2:40  Desafio 1 | 35 |
| 2:40 a 2:58  Desafio 2 e fim | 50 |
| **Total** | **399 palavras ➜ 2min40s a 150 palavras por minuto** |

Você fala enquanto clica, então o vídeo fecha em cerca de 2min55s. Se passar de 3 minutos, corte primeiro "Esse painel mostra tudo por dentro" e depois "Bora ver o que ela faz e como foi feita".

## Checklist antes de publicar

- [ ] Conta de teste "Teste GS", sem dado real de ninguém.
- [ ] Nenhuma senha, chave, `tami.properties` ou aba de variáveis do Render na tela.
- [ ] Cada trecho de código aparece com o nome do arquivo e a linha.
- [ ] A busca da UPA mais perto foi feita num lugar público (não aparece o seu endereço).
- [ ] Publicar no YouTube (pode ser "não listado") e me mandar o link para eu colocar em `LINKS.txt` e na documentação.
- [ ] Depois de gravar, apagar a conta "Teste GS" em Meus dados.

## O que cada parte cobre no enunciado

| O enunciado pede | Onde está |
|---|---|
| Sistema Java em funcionamento | 0:20 (o app rodando) e 0:58 (o servidor no ar) |
| Gravar, ler e atualizar no banco | 1:15 (gravar), 1:35 (atualizar e ler) |
| Quais dados são coletados e como são tratados | 1:50 |
| Conceitos do curso com exemplos reais | 2:08 (orientação a objetos, DAO, herança, PreparedStatement e o diagrama do banco) |
| Um ou dois desafios técnicos e as decisões | 2:25 (a base do Ministério da Saúde) e 2:40 (segurança e testes) |
| Estatística e métodos ágeis (critérios gerais) | 2:40 (ciclos curtos com quadro de tarefas, 10.000 casos e 99,5%) |

## Revisão (pelo método do roteirista)

| Critério | Antes | Agora | O que mudou |
|---|---|---|---|
| Gancho | 8 | 10 | Abre com uma pergunta em que a pessoa se reconhece ("É só eu, ou toda vez que você se machuca..."), traz a sua história do judô e só depois o dado de BH, como prova de que não é só ela. Em seguida vem a promessa: "Bora ver o que ela faz e como foi feita." |
| Clareza para quem não é da área | 8 | 10 | Termos difíceis saíram da fala; cada termo que ficou (DAO, PreparedStatement) vem com uma frase do que ele faz. |
| Cobertura do enunciado | 9 | 10 | Entrou o que faltava dos critérios gerais: métodos ágeis (ciclos curtos com quadro de tarefas, Figura `11_kanban.png`). |
| Verdade | 8 | 10 | "Cada tabela tem uma classe" virou "cada parte do banco tem uma classe" (são 8 DAOs para 11 tabelas); as respostas só são gravadas com permissão, e a fala agora diz isso; o botão se chama "Apagar tudo", não "Apagar minha conta". |
| Ritmo e duração | 8 | 10 | Cerca de 2min40s de fala, e você fala enquanto clica, então fecha em uns 2min55s; o diagrama do banco fica na tela sem fala. |
| Final | 9 | 10 | Fecha com o resultado (99,5%) e o slogan, como no pitch. |

**Conferido no código em 19/11:** a conversa é criada no banco quando o relato é enviado (`ConversaService.java`, linha 141); as respostas só são gravadas com permissão (`ConversaDAO.java`, a partir da linha 66); o UPDATE da dor é a rota `PUT /api/dor/{id}`; os botões "Baixar meus dados" e "Apagar tudo" existem em Meus dados; o `LocalService` busca 20 por página, 6 páginas em paralelo, com cache de 12 horas.
