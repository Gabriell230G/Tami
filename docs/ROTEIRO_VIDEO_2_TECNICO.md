# Tami · Roteiro do vídeo 2: técnico (Entrega 2)

Vídeo de **até 3 minutos** mostrando o sistema Java funcionando, o banco recebendo inserção, leitura e atualização, os dados coletados e como são tratados, os conceitos do curso no código e dois desafios técnicos.

**Alvo:** 2min55s no total: 329 palavras faladas (2min11s a 150 palavras por minuto) mais o tempo dos cliques. A contagem está no fim.

**Como falar.** Frases curtas. "[pausa]" é um silêncio de 1 segundo. Fale enquanto clica: cada bloco diz exatamente o que abrir.

**Legenda:** 🎤 fala · 🖥️ o que aparece na tela · ⌨️ o que clicar.

---

## Antes de gravar (5 minutos de preparo)

1. Abra três abas no navegador: o app (`https://tami-app.onrender.com`), o painel do Supabase no projeto da Tami (Table Editor) e o repositório no GitHub (ou o VS Code com o projeto aberto).
2. Abra o app uma vez antes de gravar, para o Render "acordar" (a primeira resposta pode demorar uns 30 segundos).
3. No app, crie uma conta **só para o vídeo**: apelido "Teste GS", contato `teste.gs@exemplo.com`. Nunca use dados reais.
4. No Supabase, as tabelas aparecem em minúsculas (`t_tami_conversa`), porque o PostgreSQL guarda assim os nomes sem aspas.
5. Não mostre a aba de configuração do Render nem o `tami.properties`: a senha do banco não pode aparecer.

---

| Tempo | 🖥️ ⌨️ Na tela e o que clicar | 🎤 O que falar |
|---|---|---|
| **0:00 a 0:15**<br>Abertura | Slide ou tela com a Figura `04_arquitetura.png`. | Esta é a parte técnica da Tami. [pausa] É um servidor em Java 17, sem framework, com JDBC e banco PostgreSQL no Supabase, em São Paulo. Está publicado no Render. |
| **0:15 a 0:50**<br>Java rodando e INSERT | ⌨️ Aba do app, logado como "Teste GS" ➜ Meus dados ➜ ligue "Guardar meus dados de saúde" ➜ Início ➜ Começar ➜ Para mim, 22 anos ➜ relato "torci o tornozelo no treino" ➜ Continuar. ⌨️ Toque no botão `<>` (Por dentro) e mostre o `POST /api/conversas` com o `INSERT INTO T_TAMI_CONVERSA`. | O app conversa com o servidor por uma API JSON. [pausa] Quando eu toco em continuar, o servidor cria a conversa. Este painel, o "Por dentro", mostra cada chamada e o SQL que ela fez. Aqui está o INSERT na tabela de conversas. |
| **0:50 a 1:15**<br>Banco recebendo | ⌨️ Responda as perguntas até o resultado. ⌨️ Aba do Supabase ➜ Table Editor ➜ `t_tami_conversa` ➜ ordene por `id_conversa` decrescente: a linha nova com `ds_destino`, `nr_perguntas` e o relato. ⌨️ Abra `t_tami_resposta` filtrando pelo `id_conversa`. | No fim, a conversa é atualizada com o destino, e as respostas são gravadas. [pausa] Tudo numa transação só: ou grava tudo, ou nada. No Supabase dá para ver a linha nova e cada resposta, na ordem. |
| **1:15 a 1:35**<br>UPDATE e SELECT | ⌨️ No app, no cartão "Onde dói, e quanto?", toque no 6 e em "Guardar a dor". Depois toque no 3 e salve de novo. ⌨️ Supabase ➜ `t_tami_dor` ➜ recarregue: `nr_nivel` mudou de 6 para 3. ⌨️ No app, "Ver resumo para o atendimento". | Agora a dor. Gravo nível seis, é um INSERT. Mudo para três, é um UPDATE na mesma linha. [pausa] E o resumo para o atendimento é um SELECT que junta a conversa, as respostas e a dor. |
| **1:35 a 1:55**<br>Dados e tratamento | ⌨️ VS Code ➜ `servico/ConversaService.java`, linhas 120 e 121. Depois `dao/ConversaDAO.java`, linha 30 (`cv.isGuardar() ? ... : null`). ⌨️ `servico/LocalService.java`: o comentário do topo (a localização não é gravada) e o método `perto`. | Quais dados ficam? Sem conta, só idade, parte do corpo e destino, para estatística. O relato só é guardado com conta e com a permissão de saúde ligada. Está aqui no código, não só na tela. [pausa] A localização, usada para achar a UPA mais perto, nunca é gravada: o Ministério recebe só o código do município. |
| **1:55 a 2:20**<br>Conceitos do curso | ⌨️ `dao/BaseDAO.java` linha 11 (`public abstract class BaseDAO`). ⌨️ `dao/DorDAO.java` linhas 18, 36 e 47 (inserir, atualizarNivel, listarPorConversa). ⌨️ `modelo/RegistroDor.java` linha 6 (record) e `motor/Destino.java` linha 4 (enum). ⌨️ Figura `06_der.png`. | Os conceitos do curso estão no código. Padrão DAO, com herança de uma classe abstrata. PreparedStatement em todo comando, então o texto da pessoa nunca vira SQL. Records e enums no modelo. Camadas separadas: o motor não conhece o banco. E o modelo tem onze tabelas, com chave primária, estrangeira e check. |
| **2:20 a 2:55**<br>Dois desafios | ⌨️ Figura `22_rodadas.png`. ⌨️ Abra `dados/simulados/CHANGELOG_SIMULADOS.md` na tabela "Testado e desfeito". ⌨️ Feche com a Figura `20_matriz.png`. | Primeiro desafio: perguntas sem relação. No meu teste de joelho luxado no judô, a Tami perguntava de gravidez e cirurgia. Criei a trilha do machucado e a lista curta. As perguntas sem relação caíram de cinquenta e cinco para vinte e dois por cento. [pausa] Segundo desafio: melhorar sem perder segurança. Cada mudança roda dez mil casos. Se algum urgente vai para casa, ela é desfeita. Resultado: noventa e nove e meio por cento dos urgentes no lugar certo. |

---

## Contagem

| Bloco | Palavras faladas |
|---|---|
| 0:00 a 0:15 · Abertura | 29 |
| 0:15 a 0:50 · Java rodando e INSERT | 42 |
| 0:50 a 1:15 · Banco recebendo | 36 |
| 1:15 a 1:35 · UPDATE e SELECT | 36 |
| 1:35 a 1:55 · Dados e tratamento | 56 |
| 1:55 a 2:20 · Conceitos do curso | 52 |
| 2:20 a 2:55 · Dois desafios | 78 |
| **Total** | **329 palavras ➜ 2min11s a 150 palavras por minuto** |

Os cliques ocupam os segundos que sobram. Se passar de 3 minutos, encurte o bloco de 1:55 (fale só DAO, PreparedStatement e camadas).

## Checklist antes de publicar

- [ ] Conta de teste "Teste GS", sem dado real de ninguém.
- [ ] Nenhuma senha, chave ou `tami.properties` na tela. A aba de variáveis do Render fica fechada.
- [ ] Cada trecho de código aparece com o nome do arquivo e a linha.
- [ ] Publicar no YouTube (pode ser "não listado") e colar o link em `LINKS.txt`.
- [ ] Depois de gravar, apagar a conta "Teste GS" em Meus dados (o `ON DELETE CASCADE` apaga o resto).
