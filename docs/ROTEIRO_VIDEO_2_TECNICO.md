# Tami · Roteiro do vídeo 2: técnico (Entrega 2)

Vídeo de **até 3 minutos** que cobre, nesta ordem, tudo o que o enunciado pede: o sistema Java funcionando, o banco recebendo **inserção, leitura e atualização**, **quais dados** são coletados e **como são tratados**, os **conceitos do curso** no código real e **dois desafios técnicos** com a solução.

**Alvo:** cerca de 2min55s. A contagem está no fim.

**Como falar.** Frases curtas, ritmo de quem está mostrando, não de quem está lendo. Fale **enquanto** clica. "[pausa]" é 1 segundo.

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

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:00 a 0:08**<br>Gancho | ⌨️ App aberto com o painel **"Por dentro"** (botão `<>`) ligado. Toque em qualquer botão: a chamada e o SQL aparecem no painel. | Cada toque na Tami vira uma chamada pra API e uma linha no banco. [pausa] Hoje eu te mostro isso por dentro. |
| **0:08 a 0:22**<br>Arquitetura e Java no ar | 🖥️ Figura `04_arquitetura.png`. ⌨️ Aba **Logs** do Render, na linha "Tami no ar". | É um servidor em Java 17, sem framework: o HttpServer do próprio Java, JDBC e PostgreSQL no Supabase, em São Paulo. Tudo publicado no Render: aqui, o servidor no ar. |
| **0:22 a 0:45**<br>INSERT | ⌨️ App, logado como "Teste GS" ➜ Meus dados ➜ ligue "Guardar meus dados de saúde" ➜ Início ➜ Começar ➜ Para mim ➜ relato "torci o tornozelo no treino" ➜ Continuar. 🖥️ No "Por dentro": `POST /api/conversas` e `INSERT INTO T_TAMI_CONVERSA`. | Quando eu mando o relato, o app faz um POST na API, e o servidor cria a conversa: um INSERT na tabela de conversas. O painel mostra a chamada e o comando que ela dispara. |
| **0:45 a 1:05**<br>Transação | ⌨️ Responda até o resultado. ⌨️ VS Code: `dao/ConversaDAO.java`, linhas 54 (`setAutoCommit(false)`) e 81 (`commit()`). ⌨️ Supabase ➜ `t_tami_conversa` (ordene por `id_conversa`, a linha nova) e `t_tami_resposta` filtrando por ela. | No fim, a conversa recebe o destino e as respostas são gravadas numa transação só: ou grava tudo, ou nada. [pausa] No Supabase, a linha nova e cada resposta, na ordem. |
| **1:05 a 1:20**<br>UPDATE e SELECT | ⌨️ App ➜ "Onde dói, e quanto?" ➜ toque no 6 ➜ Guardar. Depois no 3 ➜ Guardar. 🖥️ "Por dentro": `POST .../dor` (INSERT) e depois `PUT /api/dor/{id}` (UPDATE). ⌨️ Supabase ➜ `t_tami_dor`: `nr_nivel` foi de 6 para 3. ⌨️ App ➜ "Ver resumo para o atendimento". | Dor nível seis: INSERT. Mudei pra três: UPDATE na mesma linha. [pausa] E o resumo pro atendimento é um SELECT que junta conversa, respostas e dor. |
| **1:20 a 1:42**<br>Dados e tratamento | ⌨️ VS Code: `servico/ConversaService.java`, linhas 120 e 121 (só grava com consentimento). `dao/ConversaDAO.java`, linha 30 (relato só se `isGuardar()`). `api/Servidor.java`, linha 136 (a pessoa baixa os próprios dados). `sql/postgres/01_criar_tabelas.sql`, linha 110 (`ON DELETE CASCADE`). | Que dados ficam? Sem conta, só idade, parte do corpo e destino, pra estatística. O relato só é gravado com conta e consentimento de saúde: está no código, não só na tela. [pausa] E a pessoa baixa tudo o que existe sobre ela, ou apaga a conta, e o cascade leva junto o resto. |
| **1:42 a 2:02**<br>Conceitos do curso | ⌨️ `dao/BaseDAO.java` linha 11 (`abstract class`) e `dao/DorDAO.java` linha 14 (`extends BaseDAO`), linhas 18, 36 e 47 (`inserir`, `atualizarNivel`, `listarPorConversa`). `modelo/RegistroDor.java` linha 6 (`record`). `motor/Destino.java` linha 4 (`enum`). 🖥️ Figura `06_der.png`. | Do curso: padrão DAO, com herança de uma classe abstrata. PreparedStatement em todo comando, então o texto da pessoa nunca vira SQL. Records e enums no modelo, e o motor não conhece o banco. [pausa] O modelo tem onze tabelas, com chave primária, estrangeira e check. |
| **2:02 a 2:28**<br>Desafio 1: integração | ⌨️ `servico/LocalService.java`, linhas 36 a 38 (20 por página, 6 em paralelo, cache de 12 horas), 131 a 133 (`sendAsync`) e 163 (`semCoordenadaGenerica`). 🖥️ No app, "Usar minha localização" num resultado de UPA (num lugar público). | Primeiro desafio: achar a UPA real mais perto, na API do Ministério da Saúde. Ela devolve só vinte unidades por vez. Então busco seis páginas em paralelo, com o HttpClient assíncrono, e guardo em cache por doze horas. [pausa] E algumas unidades vinham com coordenada falsa: filtrei. A localização da pessoa nunca é gravada. |
| **2:28 a 2:55**<br>Desafio 2: segurança | 🖥️ Figura `22_rodadas.png` (as rodadas de teste) e, no fim, Figura `20_matriz.png` (a matriz de 10.000 casos). | Segundo desafio: melhorar a conversa sem perder segurança. Cada mudança no motor roda dez mil casos e doze mil conversas simuladas, em ciclos curtos. Se algum urgente vai pra casa, a mudança é desfeita. [pausa] Assim, as perguntas sem relação caíram de cinquenta e cinco pra vinte e dois por cento, e noventa e nove e meio por cento dos urgentes foram pro lugar certo. |

---

## Contagem

| Bloco | Palavras faladas |
|---|---|
| 0:00 a 0:08  Gancho | 21 |
| 0:08 a 0:22  Arquitetura e Java no ar | 30 |
| 0:22 a 0:45  INSERT | 35 |
| 0:45 a 1:05  Transação | 30 |
| 1:05 a 1:20  UPDATE e SELECT | 25 |
| 1:20 a 1:42  Dados e tratamento | 53 |
| 1:42 a 2:02  Conceitos do curso | 45 |
| 2:02 a 2:28  Desafio 1: integração | 53 |
| 2:28 a 2:55  Desafio 2: segurança | 64 |
| **Total** | **356 palavras ➜ 2min22s a 150 palavras por minuto (2min09s a 165, um ritmo bom para vídeo técnico)** |

Os cliques ocupam o resto do tempo, porque você fala enquanto clica. Se passar de 3 minutos, corte primeiro a frase "O modelo tem onze tabelas, com chave primária, estrangeira e check" (a Figura `06_der.png` fica na tela) e depois "Tudo publicado no Render: aqui, o servidor no ar".

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
| Sistema Java em funcionamento | 0:08 (servidor no ar) e o app em todos os blocos |
| Inserção, leitura e atualização no banco | 0:22 (INSERT), 0:45 (transação), 1:05 (UPDATE e SELECT) |
| Quais dados são coletados e como são tratados | 1:20 |
| Conceitos do curso com exemplos reais | 1:42 (DAO, herança, PreparedStatement, records, enum, camadas, DER) |
| Um ou dois desafios técnicos e as decisões | 2:02 (integração com o Ministério) e 2:28 (segurança e testes) |
| Estatística e métodos ágeis (critérios gerais) | 2:28 ("dez mil casos", "ciclos curtos", os percentuais) |
