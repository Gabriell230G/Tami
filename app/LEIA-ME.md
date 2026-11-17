# Tami · Global Solution 2026.2 (CitizenTech) · FIAP 2SIOA

App de orientação em saúde: a pessoa conta o que sente, a Tami acha a parte do corpo numa árvore,
filtra a base de 1.169 sinais e faz as perguntas certas até um destino: SAMU 192, UPA hoje, UBS em breve ou cuidar em casa.

## Rodar no Windows

1. Precisa do Java 17 ou mais novo (JDK). Teste no terminal: `java -version` e `javac -version`.
2. Copie `tami.properties.exemplo` para `tami.properties`. Sem mudar nada, ele usa um banco local (H2), que não precisa de instalação.
   Para o Supabase: no `tami.properties`, tire o `#` das 3 linhas do Supabase, ponha `#` nas do H2 e preencha a senha do banco.
   O `tami.properties` fica fora do Git: a senha nunca vai para o repositório.
3. Dê dois cliques em `scripts\rodar.bat`. Na primeira vez ele compila e cria as tabelas no banco.
4. Abra http://localhost:8080 no navegador. O botão "Por dentro" mostra cada chamada à API e o SQL que ela faz.

Outros comandos (no terminal, na pasta do projeto):
- `scripts\rodar.bat simular 300` ➜ grava 300 conversas de demonstração (pessoas simuladas) para o painel
- `scripts\rodar.bat validar` ➜ roda os 10.000 casos de teste (uns 7 minutos) e grava `dados\validacao.json`
- `scripts\rodar.bat zerar` ➜ apaga e recria as tabelas

## Publicar (link para outras pessoas testarem)

O `Dockerfile` desta pasta compila e roda o app. No Render, o `render.yaml` da raiz do repositório faz tudo:
New ➜ Blueprint ➜ escolha o repositório ➜ preencha só `TAMI_DB_SENHA`.

Segurança no link público:
- cada pessoa recebe um token ao entrar e só consegue ler ou mudar os próprios dados (HMAC com a chave `TAMI_SEGREDO`);
- o botão "Gerar conversas de teste" só aparece rodando no próprio computador;
- a senha do banco fica no Render, nunca no Git (`tami.properties` está no `.gitignore`).

## Pastas

- `src/main/java/br/com/fiap/tami/` ➜ código Java
  - `motor/` regras da conversa · `regiao/` árvore do corpo · `dao/` banco (JDBC) · `modelo/` classes do domínio
  - `servico/` regras de negócio, carga, estatística, validação · `api/` servidor HTTP e JSON · `app/` ponto de entrada
- `src/main/resources/` ➜ base de conhecimento (sinais com fonte) e roteiros
- `sql/postgres/` e `sql/oracle/` ➜ scripts do banco (o mesmo modelo nos dois)
- `web/` ➜ telas do app (HTML, CSS e JavaScript, sem framework)
- `lib/` ➜ drivers JDBC (PostgreSQL e H2). Para o Oracle da FIAP, coloque aqui o `ojdbc11.jar`.

A Tami não é médica, não faz diagnóstico e não indica remédio. As regras ainda não passaram por revisão médica.
Em emergência, ligue 192.
