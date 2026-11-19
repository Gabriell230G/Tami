# Tami · Roteiro do vídeo 2: técnico (Entrega 2)

Vídeo de **até 3 minutos**: **1 minuto** para contar o que é a Tami e **2 minutos** para mostrar como ela foi feita, sem complicar. Cobre tudo o que o enunciado pede: o sistema Java funcionando, o banco **gravando, lendo e atualizando**, **quais dados** são coletados e **como são tratados**, os **conceitos do curso** e **dois desafios técnicos**.

**Alvo:** cerca de 2min55s. A contagem está no fim.

**Como falar.** Como se estivesse explicando para um amigo que não é da área. Frases curtas. Fale **enquanto** clica. "[pausa]" é 1 segundo.

**Legenda:** 🎤 fala · 🖥️ o que aparece na tela · ⌨️ o que clicar. Todo trecho de código aparece com **arquivo e linha** no canto da tela.

**Tudo o que é citado aqui foi conferido no código em 19/11** (arquivos, linhas, rotas e números).

---

## Antes de gravar (5 minutos de preparo)

1. Abra quatro abas: o app (`https://tami-app.onrender.com`), o **Supabase** do projeto da Tami (Table Editor), os **Logs** do serviço no Render e o **VS Code** com o projeto.
2. Abra o app uma vez antes, para o Render "acordar" (a primeira resposta pode levar uns 30 segundos).
3. No app, crie uma conta **só para o vídeo**: apelido "Teste GS", contato `teste.gs@exemplo.com`. Nunca use dados reais.
4. No Supabase, as tabelas aparecem em minúsculas (`t_tami_conversa`): o PostgreSQL guarda assim os nomes sem aspas.
5. **Nunca** mostre a aba de variáveis do Render nem o `tami.properties`: a senha do banco não pode aparecer.
6. No VS Code, aumente a fonte (Ctrl + `+` três vezes) para o código ficar legível no vídeo.

---

## O roteiro

O vídeo tem **duas partes**: no primeiro minuto você conta **o que é a Tami** (para quem nunca viu entender o resto), e depois mostra **como ela foi feita**, em linguagem simples. Cada termo técnico vem com uma frase que explica o que ele faz.

### Parte 1 · O que é a Tami (0:00 a 0:58)

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:00 a 0:12**<br>O problema | 🎥 Você falando para a câmera. Na tela, uma palavra por vez: **UPA? POSTO? PRONTO-SOCORRO? CASA?** | Quando dói, a dúvida é sempre a mesma: eu vou na UPA? No posto? No pronto-socorro? Ou fico em casa? [pausa] Em BH, quase oito em cada dez atendimentos na UPA não eram urgentes. |
| **0:12 a 0:20**<br>A Tami | 🖥️ O avatar da Tami e o slogan. | Pra isso eu criei a Tami: uma agente de saúde no celular, de graça. |
| **0:20 a 0:45**<br>Como ela funciona | ⌨️ Gravação do app: digitar "torci o tornozelo no treino" ➜ as perguntas ➜ a escala de dor ➜ o resultado "cuidar em casa" com os cuidados ➜ "Usar minha localização" (num lugar público). | Você conta o que sente, do seu jeito. Ela confere primeiro se é emergência. Depois pergunta pouco e diz o próximo passo: SAMU, UPA hoje, posto em breve ou cuidar em casa. [pausa] Mostra os cuidados, o SUS mais perto e monta um resumo pro atendimento. |
| **0:45 a 0:58**<br>O limite | 🖥️ **NÃO DIAGNOSTICA · NÃO INDICA REMÉDIO · 945 SINAIS COM FONTE** | Ela não diagnostica e não indica remédio: segue regras com fonte. [pausa] Agora, como isso foi feito. |

### Parte 2 · Como foi feita (0:58 a 2:55)

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:58 a 1:15**<br>O desenho | 🖥️ Figura `04_arquitetura.png`. ⌨️ Aba **Logs** do Render, na linha "Tami no ar". | Por trás, é um programa em Java que roda num servidor na internet, o Render. Ele guarda tudo num banco PostgreSQL, no Supabase. [pausa] O app conversa com o Java, e o Java conversa com o banco. |
| **1:15 a 1:35**<br>Gravar | ⌨️ App, logado como "Teste GS", com o painel **"Por dentro"** (botão `<>`) aberto ➜ mandar o relato. 🖥️ O painel mostra `POST /api/conversas` e `INSERT INTO T_TAMI_CONVERSA`. ⌨️ Responder até o resultado. ⌨️ Supabase ➜ `t_tami_conversa` e `t_tami_resposta`: as linhas novas. | Esse painel mostra o que acontece por dentro. Quando eu mando o relato, o Java grava uma conversa nova no banco: é uma inserção. [pausa] No fim, ele grava o destino e cada resposta de uma vez só. Se der erro no meio, nada fica pela metade. |
| **1:35 a 1:50**<br>Atualizar e ler | ⌨️ App ➜ "Onde dói, e quanto?" ➜ 6 ➜ Guardar ➜ 3 ➜ Guardar. ⌨️ Supabase ➜ `t_tami_dor`: o nível foi de 6 para 3. ⌨️ App ➜ "Ver resumo para o atendimento". | Se eu marco dor seis e depois mudo pra três, ele atualiza a mesma linha. [pausa] E o resumo pro atendimento é uma leitura que junta tudo. |
| **1:50 a 2:08**<br>Os dados | ⌨️ VS Code: `servico/ConversaService.java`, linhas 120 e 121 (a permissão de saúde), por 3 segundos. ⌨️ App ➜ Meus dados: os botões "Baixar meus dados" e "Apagar minha conta". | Que dados ficam? Sem conta, só idade, parte do corpo e destino, pra estatística. O que a pessoa escreve só é guardado se ela tiver conta e der permissão: tá aqui no código. [pausa] Ela pode baixar os dados ou apagar tudo. E a localização nunca é gravada. |
| **2:08 a 2:25**<br>O que veio do curso | ⌨️ VS Code: `dao/DorDAO.java` linha 14 (`extends BaseDAO`) e linha 18 (o `inserir`). 🖥️ Figura `06_der.png`. | Do curso, usei orientação a objetos: cada tabela tem uma classe que cuida dela, e todas herdam de uma classe base. Os comandos usam PreparedStatement, que impede que o texto da pessoa vire comando no banco. [pausa] E o banco tem onze tabelas ligadas por chaves. |
| **2:25 a 2:40**<br>Desafio 1 | ⌨️ VS Code: `servico/LocalService.java`, linhas 36 a 38. 🖥️ No app, a lista de UPAs com "Como chegar". | Primeiro desafio: achar a UPA real mais perto, na base do Ministério da Saúde. Ela devolve só vinte unidades por vez. Então busco várias páginas ao mesmo tempo e guardo o resultado por doze horas, pra ficar rápido. |
| **2:40 a 2:55**<br>Desafio 2 e fim | 🖥️ Figura `20_matriz.png` e, no fim, o avatar com **"Falou em saúde? Tamo aqui."** | Segundo desafio: melhorar sem perder segurança. Cada mudança roda dez mil casos de teste. Se um urgente vai pra casa, eu desfaço. [pausa] Resultado: noventa e nove e meio por cento dos urgentes no lugar certo. Falou em saúde? Tamo aqui. |

## Contagem

| Bloco | Palavras faladas |
|---|---|
| 0:00 a 0:12  O problema | 34 |
| 0:12 a 0:20  A Tami | 14 |
| 0:20 a 0:45  Como ela funciona | 45 |
| 0:45 a 0:58  O limite | 16 |
| 0:58 a 1:15  O desenho | 36 |
| 1:15 a 1:35  Gravar | 46 |
| 1:35 a 1:50  Atualizar e ler | 26 |
| 1:50 a 2:08  Os dados | 47 |
| 2:08 a 2:25  O que veio do curso | 45 |
| 2:25 a 2:40  Desafio 1 | 38 |
| 2:40 a 2:55  Desafio 2 e fim | 40 |
| **Total** | **387 palavras ➜ 2min35s a 150 palavras por minuto** |

Você fala enquanto clica, então o vídeo fecha em cerca de 2min55s. Se passar de 3 minutos, corte primeiro "E o banco tem onze tabelas ligadas por chaves" (a figura fica na tela) e depois "Mostra os cuidados, o SUS mais perto e monta um resumo pro atendimento".

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
| Conceitos do curso com exemplos reais | 2:08 (orientação a objetos, herança, PreparedStatement, modelo do banco) |
| Um ou dois desafios técnicos e as decisões | 2:25 (a base do Ministério da Saúde) e 2:40 (segurança e testes) |
| Estatística (critério geral) | 2:40 (10.000 casos e 99,5%) |
