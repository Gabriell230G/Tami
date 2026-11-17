# VOZ_TAMI · desenho da função "falar com a Tami"

Versão 1 · 2026-11-15 · Frente 4 do sprint "gente de verdade" (PROMPT_TESTE_HUMANO.md).
Números do teste: `_revisao/RESULTADO_VOZ.md`. Simulador: `ferramentas/simular_transcricao.py`. Medição: `ferramentas/avaliar_voz.py`.

Regras de ouro continuam valendo na voz: nunca diagnosticar, nunca indicar remédio, na dúvida o mais urgente,
CVV 188, SAMU 192, CIATox 0800 722 6001. **Caso grave nunca pode ir para um lugar mais leve por causa da transcrição.**

## 1. Resumo em 6 linhas

- A pessoa fala, o áudio vira texto, **a pessoa confere e corrige o texto**, e só então o motor recebe o texto, igual a hoje.
- O áudio é apagado logo depois da transcrição. Nada de guardar gravação.
- Transcrição recomendada: **faster-whisper num servidor nosso no Brasil**. O reconhecimento do navegador só no modo
  "no aparelho" (on-device); o modo comum do Chrome manda o áudio para o Google e fica fora do padrão.
- No teste com 300 vinhetas, a transcrição com erros fez casos graves caírem de lugar: com a lista de confirmação,
  0 ➜ 3 (leve), 4 (médio) e 7 (pesado) críticas em 200 graves. Só com o texto, 84 ➜ 84, 96 e 114.
- Por isso a voz entra com 6 proteções (seção 8). A mais importante é a conferência com destaque nos números e nas
  palavras que costumam ser trocadas.
- A Tami pode falar de volta (síntese de voz). Na urgência, a frase de ação vem primeiro, falada e escrita.

## 2. Fluxo

```
[Botão grande "Falar"]
   ➜ 1ª vez: tela de consentimento do microfone (seção 5) ➜ "Permitir" / "Prefiro digitar"
   ➜ pedido de permissão do navegador (getUserMedia)
   ➜ gravando: onda na tela, contador de tempo, botão "Parar". Limite de 90 s por fala.
   ➜ transcrição (servidor no Brasil ou no aparelho)
   ➜ ÁUDIO APAGADO (memória e disco)
   ➜ TELA DE CONFERÊNCIA (obrigatória)
        - o texto em letra grande, editável
        - números destacados em amarelo: "38 e 5 ➜ você quis dizer 38,5 graus?"
        - palavras suspeitas sublinhadas, com a troca provável: "saiu ➜ caiu?", "ouço ➜ osso?", "sem anos ➜ semanas?"
        - botões: [Está certo] [Corrigir digitando] [Falar de novo] [Ouvir o que entendi]
   ➜ motor recebe o texto conferido (igual ao texto digitado de hoje)
   ➜ perguntas e lista de confirmação ("Algum destes está acontecendo?"), que podem ser lidas em voz alta
   ➜ resultado: escrito e falado, com a frase de ação primeiro
```

**A conferência nunca atrasa um alerta.** Enquanto a pessoa confere, o motor já roda no texto bruto. Se o texto bruto
der SAMU, a Tami mostra e fala na hora: "Ligue agora para o SAMU, 192". Depois da conferência:

- se o texto corrigido também der SAMU, segue normal;
- se o texto corrigido der um lugar mais leve, a Tami **pergunta o sinal que tinha disparado**, com "sim / não /
  não sei" ("Você disse que ele não acorda. Isso está acontecendo?"). "Não sei" conta como sim. Assim a correção da
  pessoa pode tirar um falso alarme, mas um erro de digitação na correção não apaga um alarme verdadeiro.

**Se a pessoa não conferir** (fechou, demorou mais de 60 s, apertou "Está certo" sem olhar): o motor usa o texto bruto
**mais** a expansão por som (seção 8, proteção 5), e a lista de confirmação é obrigatória. Nunca o caminho só texto.

## 3. Quem transcreve: as 3 opções

Legenda: **[V]** conferido na documentação oficial em 2026-11-15 · **[NV]** não consegui verificar, é estimativa ou
conhecimento geral; conferir antes de decidir.

| | A. Web Speech API (navegador) | B. Whisper / faster-whisper em servidor próprio no Brasil | C. Nuvem com região Brasil |
|---|---|---|---|
| Como é | `SpeechRecognition` no JavaScript, sem servidor nosso | Modelo aberto rodando numa VM nossa em São Paulo | API paga (AWS Transcribe, Azure Speech, Google Speech-to-Text) |
| Custo | Grátis para nós | Custo fixo da VM (CPU ou GPU), não por minuto. Valor da VM em São Paulo **[NV]** | AWS: US$ 0,01/min em streaming e US$ 0,006/min em lote, preço do exemplo em US East; preço em sa-east-1 **[NV]**. Google V2: US$ 0,016/min até 500 mil min/mês **[V]**. Azure: lote padrão US$ 0,36/h desde 2023 **[V]**; tempo real padrão perto de US$ 1/h **[NV]** (a página oficial não mostrou o valor) |
| Qualidade pt-BR | Depende do navegador e do sistema. Sem número oficial **[NV]**. Na prática o Chrome devolve pouca ou nenhuma pontuação **[NV]** | Multilíngue; o README diz que a qualidade "varia muito por idioma" e remete a um gráfico de erro por idioma; não extraí o número do português **[V/NV]**. Devolve pontuação **[NV]** | Google: pt-BR com pontuação automática nos modelos chirp_2 e chirp_3 **[V]**. AWS e Azure: suportam pt-BR **[NV]** (não abri a lista de idiomas) |
| Privacidade e LGPD | **Modo padrão: no Chrome, o áudio vai para um serviço web** **[V]**, ou seja, para o Google, fora do nosso controle e provavelmente fora do Brasil (transferência internacional) **[NV]**. **Modo no aparelho** (`processLocally = true`): o áudio não sai do celular **[V]** | Melhor opção: o áudio não sai da nossa infraestrutura no Brasil, e nós garantimos o "transcreveu, apagou" | Contrato com operador (art. 39 da LGPD). AWS tem endpoint em São Paulo (sa-east-1) para lote e streaming **[V]**. Azure lista a região Brazil South **[V]**, mas não confirmei a tabela por recurso **[NV]**. Google: a lista de idiomas do V2 **não** mostra southamerica-east1 para pt-BR (só asia-southeast1, europe-west4, eu e global) **[V]**, então o Google hoje não atende "região Brasil" para pt-BR. Google V1 não guarda áudio por padrão; com "data logging" ligado, guarda e usa para melhorar o serviço **[V]**: nunca ligar |
| Offline | Modo padrão: não funciona sem internet **[V]**. Modo no aparelho: funciona offline depois de baixar o pacote do idioma (`available()`, `install()`) **[V]**. Quais navegadores já têm o pacote pt-BR **[NV]** | Não (precisa falar com o servidor). Dá para rodar um modelo pequeno no aparelho no futuro **[NV]** | Não |
| Suporte | API marcada pela MDN como "Limited availability", não funciona em alguns navegadores muito usados **[V]**; o Firefox não tem reconhecimento **[NV]** | Funciona em qualquer navegador que grave áudio (MediaRecorder) | Qualquer navegador |
| Velocidade | Quase em tempo real | Licença MIT **[V]**. faster-whisper: até 4x mais rápido que o Whisper original com a mesma precisão **[V]**; modelo small em CPU int8 transcreveu 13 min em 1min42s (i7-12700K, 8 threads) **[V]**, então uma fala de 30 s deve levar poucos segundos em CPU **[NV, conta nossa]**. Modelo turbo: perto de 6 GB de VRAM, ~8x mais rápido que o large **[V]** | Streaming, quase em tempo real |
| Risco principal | Áudio de saúde indo para o Google sem contrato | Nós operamos o servidor (atualização, segurança, custo fixo) | Dependência de fornecedor, custo por minuto, contrato de operador |

**Decisão proposta (D-VOZ-1):**

1. **Padrão: B (faster-whisper em servidor no Brasil)**, modelo `small` ou `turbo` em int8, sem gravar nada em disco.
2. **A no modo "no aparelho"** quando `SpeechRecognition.available({langs: ["pt-BR"], processLocally: true})` disser que
   está disponível. É o melhor para a privacidade e funciona sem internet.
3. **A no modo padrão (servidor do Google) fica desligado.** Se um dia for usado, só com consentimento que diga com todas
   as letras "seu áudio será enviado ao Google".
4. **C só como reserva**, e só AWS sa-east-1 ou Azure brazilsouth depois de confirmar pt-BR e preço na região, com
   contrato de operador e sem guardar áudio.

Conta de ordem de grandeza (só para comparar, **[NV]**): 1.000 relatos por dia de 30 s = 15.000 min/mês ➜ Google V2
perto de US$ 240/mês, AWS streaming perto de US$ 150/mês (preço US East), Azure tempo real perto de US$ 250/mês. A opção
B custa o mesmo com 10 ou 10.000 relatos, até a VM encher.

## 4. O que muda no motor

Nada no núcleo. A voz é uma camada antes do motor:

```
áudio ➜ transcrição ➜ limpeza da fala (proteções 2, 3 e 4) ➜ tela de conferência ➜ motor (igual a hoje)
```

O motor recebe também uma marca `origem = VOZ`, para (a) ligar a lista de confirmação sempre, (b) usar o alcance curto
da negação quando não há pontuação (proteção 2) e (c) medir separado nos relatórios.

## 5. LGPD

- **Relato de saúde é dado pessoal sensível** (art. 5º, II). A voz também pode virar dado biométrico se for usada
  para identificar a pessoa; a Tami **nunca** usa a voz para identificar ninguém.
- **Base legal: consentimento específico e destacado** (art. 11, I). A Tami não é profissional nem serviço de saúde,
  então não se apoia na "tutela da saúde" (art. 11, II, f).
- **Consentimento próprio para o microfone**, separado do consentimento geral do app, na primeira vez que a pessoa
  toca em "Falar". Texto curto, em linguagem simples:
  > "Para ouvir você, a Tami usa o microfone. O áudio vira texto e **é apagado na hora**. Ninguém escuta a gravação.
  > Você vai ver o texto antes de enviar. Se preferir, pode digitar."
  > [Permitir] [Prefiro digitar]
  Dá para tirar o consentimento a qualquer hora nas configurações (art. 8º, §5º).
- **Não guardar o áudio.** O áudio fica só na memória do servidor durante a transcrição e é descartado logo depois.
  Não vai para disco, log, fila, backup nem ferramenta de erro. Teste automático que procura arquivo de áudio no
  servidor depois de cada transcrição.
- **O texto segue a mesma regra do texto digitado** (o que a Tami já faz com o relato hoje). A transcrição bruta só fica
  enquanto a sessão está aberta.
- **Logs sem conteúdo**: guardar só duração, tempo de resposta e se a pessoa corrigiu (sim/não), nunca o texto.
- **Fornecedor de nuvem** (opção C) = operador: contrato com cláusula de não guardar e não treinar com os dados, e
  região Brasil. Se o dado sair do país, vale o art. 33 (transferência internacional): evitar.
- **Crianças e adolescentes** (art. 14): quando o relato é sobre o filho, quem consente é o responsável que está usando o app.
- Registro desta decisão no relatório de impacto (RIPD) quando ele existir.

## 6. A Tami falando de volta (síntese de voz)

- **Opcional e fácil de ligar**: botão "Ouvir" em cada pergunta, e opção "Ler tudo em voz alta" no começo.
- **Motor de voz**: `speechSynthesis` do navegador com voz pt-BR do próprio aparelho (não manda nada para fora na maioria
  dos sistemas **[NV]**), ou voz neural gerada no nosso servidor. A Tami só fala textos fixos dela (perguntas, botões,
  destinos), então dá para **gerar os áudios uma vez e guardar como arquivos estáticos**: sem custo por uso e sem dado
  da pessoa envolvido.
- **Urgência primeiro.** No SAMU, a primeira frase falada e escrita é a ação:
  > "Ligue agora para o SAMU, 192." (repete uma vez) ➜ depois o porquê, em uma frase ➜ depois o que fazer enquanto espera.
  Botão grande "Ligar 192" junto. Nada de introdução antes. O mesmo para CVV 188 e CIATox 0800 722 6001, com os números
  falados devagar e em blocos ("zero oitocentos, sete dois dois, seis mil e um").
- **Velocidade um pouco mais lenta que o normal** (rate 0,9), frases curtas, uma ideia por frase.
- **Ler as opções com número**: "Opção 1: sim. Opção 2: não. Opção 3: não sei."
- **Nunca só voz**: tudo que é falado também está escrito na tela.

## 7. Acessibilidade (idosos e baixa escolaridade)

- Botão de microfone grande, com desenho e a palavra "Falar". Na tela inicial, as duas portas: "Falar" e "Escrever".
- Fala por **tocar para começar, tocar para parar** (não "segurar para falar", que é difícil com mão trêmula).
- Mensagens de ajuda simples: "Pode falar do seu jeito. Conte o que está sentindo e desde quando."
- Tela de conferência com letra grande, contraste alto e a opção **"Ouvir o que entendi"**: quem lê com dificuldade
  confere ouvindo.
- Pergunta de dúvida em forma de escolha, nunca de digitação: "Você disse **caiu** ou **saiu**?" com dois botões.
- Aceitar fala longa e confusa: a pessoa pode contar a história do jeito dela; a limpeza tira muletas, não conteúdo.
- Se a transcrição vier vazia ou muito curta: "Não consegui ouvir. Quer tentar de novo mais perto do celular, ou
  escrever?"
- Funciona com leitor de tela (rótulos nos botões, foco no texto transcrito).
- Testar com pessoas reais: 70+ anos, pouca escolaridade, sotaques de várias regiões (nordeste, sul, interior),
  ambiente com barulho (ônibus, cozinha, criança chorando). A simulação não substitui isso.

## 8. Proteções que o teste mostrou serem necessárias

Base: Tarefa B, 300 vinhetas (200 graves), 3 níveis de erro, ablação de cada operação. Números em `_revisao/RESULTADO_VOZ.md`.

| # | Proteção | O que o teste mostrou |
|---|---|---|
| 1 | **Conferência obrigatória com destaque** de números e de palavras que costumam ser trocadas, e pergunta de escolha nos pares perigosos ("caiu/saiu", "osso/ouço", "semanas/sem anos", "febre/feb", "bebê/beber", "picou/ficou", "dor/dó", "braço/baço", "barriga/briga", "cobra/obra", "inchaço/em chaço", "bateu/bate o") | A **troca de palavra** é a causa nº 1: está em 11 das 14 quebras com lista e em 35 das 64 só texto (quebra = caso × nível × modo). "caiu ➜ saiu" aparece em 10 quebras; "34 semanas ➜ 34 sem anos" tirou uma bolsa rota pré-termo do SAMU mesmo com a lista; "bebê ➜ beber" derrubou um caso de pensamento de machucar o bebê no pós-parto |
| 2 | **Pontuação e negação**: preferir transcrição que devolve pontuação; usar as pausas do áudio como quebra de frase; no modo voz, a negação ("não", "nenhum", "sem") só vale para as 3 ou 4 palavras seguintes | Tirar a pontuação, sozinho, causou 11 quebras (3 delas com a lista). Ex.: "não para de jeito nenhum, já encharquei duas toalhas" sem a vírgula fez o "nenhum" apagar o sangramento (SAMU ➜ UPA **mesmo com a lista**) |
| 3 | **Tirar muletas e repetições** antes do motor ("né", "tipo assim", "ééé", "então", "hum", "meu meu") | Hesitação ou repetição no meio de uma expressão causou 15 quebras só texto: "falando **é** coisa sem sentido", "jogando água no **éé né** corpo" |
| 4 | **Normalizar números falados**: extenso ➜ dígito ("trinta e oito e meio" ➜ 38,5); "38 e 4", "38 5" com contexto de febre ➜ 38,4; número absurdo (800 anos, 320 semanas, 7 anos para quem disse 74) ➜ perguntar | "termômetro deu trinta e oito e quatro" não foi lido como febre (gestante, SAMU ➜ UBS). Hoje o LeitorNumeros só entende dígitos e "39 e meio". Número por extenso ou com erro causou 7 quebras (2 com a lista) |
| 5 | **Lista de confirmação sempre ligada no modo voz** e **expansão por som**: quando aparece uma palavra que costuma ser confusão de um termo clínico, o termo clínico entra como candidato na lista ("saiu" ➜ oferece "queda") | Sem a lista, a transcrição pesada levou as críticas de 84 para 114 (42% ➜ 57% dos graves); com a lista, de 0 para 7. A expansão (protótipo, com a tabela do simulador, por isso é um teto) baixou o pesado com lista de 7 para 4, ao custo de +13 casos de sobretriagem |
| 6 | **Corte no começo e no fim**: mostrar o texto e oferecer "Falar de novo"; avisar quando a última palavra parece cortada (palavra que não existe no fim) | O corte da última palavra ("estômago ➜ estoma", "pus" sumindo) causou 6 quebras só texto; o corte da primeira ("bati" sumindo), 2 |

O protótipo das proteções 3 e 4 (sem usar a tabela do simulador) ajudou pouco sozinho: pesado com lista 7 ➜ 6, só texto
114 ➜ 108. Isso confirma que **a defesa principal é a pessoa conferir** e a lista de confirmação; a limpeza automática
é complemento.

## 9. Critérios para liberar a voz no app

1. Rodar `python3 ferramentas/avaliar_voz.py` com as proteções implementadas: **zero crítica nova com a lista** nos
   níveis leve e médio, e no pesado no máximo o que o original já tem.
2. Repetir com um lote novo de vinhetas que ninguém viu (regra do sprint: medir com lote cego depois de corrigir).
3. Teste com áudio de verdade: pelo menos 30 pessoas lendo relatos em voz alta (idosos, sotaques, barulho), medindo a
   taxa de erro de palavra do transcritor escolhido e quantas vezes a conferência pegou o erro.
4. Teste automático de "áudio apagado" passando.
5. Revisão médica dos pares perigosos da proteção 1.

## 10. O que eu não consegui verificar

- Qualidade (taxa de erro) do Whisper, do Chrome, da AWS e da Azure em pt-BR com números oficiais.
- Preço da AWS e da Azure especificamente na região São Paulo / Brazil South; preço de tempo real padrão da Azure.
- Se a AWS e a Azure têm pt-BR em todos os modos (streaming e lote) na região Brasil.
- Quais navegadores já oferecem o reconhecimento "no aparelho" para pt-BR, e se o Chrome no Android oferece.
- Se o Chrome devolve pontuação em pt-BR.
- Custo de uma VM com GPU em São Paulo.
- Se as vozes pt-BR do `speechSynthesis` rodam sempre no aparelho.

Fontes consultadas (2026-11-15): MDN "Using the Web Speech API" e "SpeechRecognition"; github.com/openai/whisper;
github.com/SYSTRAN/faster-whisper; cloud.google.com/speech-to-text/pricing; docs.cloud.google.com/speech-to-text/docs/languages
e /docs/v1/data-logging; docs.aws.amazon.com/general/latest/gr/transcribe.html; aws.amazon.com/transcribe/pricing;
learn.microsoft.com/azure/ai-services/speech-service/regions; techcommunity.microsoft.com (atualização de preços do
Azure Speech, 15/09/2023).

## 11. Limites do teste

O texto de entrada já era escrito (com erros de digitação). O simulador imita o que o reconhecedor faria com a fala da
pessoa, mas os erros de digitação originais continuam no texto, e a tabela de confusões foi escrita por IA, não medida
num reconhecedor de verdade. Algumas trocas (como "caiu ➜ saiu") podem ser mais ou menos frequentes na vida real. Isto
ainda é IA simulando gente: não substitui teste com áudio real nem a revisão médica.
