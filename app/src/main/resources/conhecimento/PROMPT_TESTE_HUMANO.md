# Prompt · Sprint "gente de verdade": testar a Tami com os erros que humanos cometem

Objetivo: medir como a Tami se sai quando a pessoa do outro lado escreve mal, se contradiz, responde errado, desiste
no meio, manda áudio transcrito ou está em crise. Hoje a simulação assume uma pessoa sincera e organizada. Este sprint
cobre, com IA, as lacunas 1, 3, 4, 5 e 6 do diagnóstico:
1. tudo escrito pela mesma "cabeça";
3. nenhum caso real;
4. a crise deixa escapar uma em cada quatro frases;
5. poucos casos cegos para ter segurança estatística;
6. ninguém usou de verdade.

A lacuna 2 (revisão médica) continua precisando de gente.

O critério não muda: **caso grave nunca pode ir para um lugar mais leve.** Valem as regras de ouro: nunca diagnosticar,
nunca indicar remédio, na dúvida o mais urgente, CVV 188, SAMU 192 e CIATox 0800 722 6001. Nas frases de suicídio,
só ideia, nunca método.

Trabalhe sem pedir confirmação. Se algo for ambíguo, escolha a opção mais cautelosa, registre em DECISOES.md e siga.

## Regras de honestidade (valem para todas as frentes)

- **Os geradores são cegos.** Os agentes que escrevem casos NÃO podem abrir o léxico, a base (sinais_alerta.csv,
  condicoes.csv, lexico*.csv, agravantes_sinonimos.csv), o código do motor nem os lotes anteriores. Eles leem só o
  catálogo de erros abaixo e as regras de formato das vinhetas. Assim a Tami é testada contra o jeito de falar, não
  contra o próprio vocabulário.
- **Autores diferentes.** Cada gerador recebe uma "persona de autor" diferente (abaixo), para não repetir o mesmo estilo.
- **Nada deste sprint é usado para ajustar o motor durante o sprint.** Primeiro mede, depois documenta. As correções
  vêm num sprint seguinte e são medidas com um lote novo, que ninguém viu.
- **O destino esperado é revisado por um segundo agente**, que não escreveu o caso.

## Catálogo de erros humanos (a base de todas as frentes)

**Escrita**
- Digitação: letra trocada, faltando ou sobrando ("dor no pieto", "febe", "vomitandoo").
- Escrita pelo som: "derrepente", "fébri", "dipirona" virando "dipirona", "estomago" sem acento, "mim dói", "nós vai".
- Corretor automático que troca a palavra: "dor no peixe", "minha mãe desmamou" (desmaiou), "febre de 39" virando
  "febre de 3".
- Sem pontuação nenhuma, ou tudo em MAIÚSCULA, ou só emoji ("😭😭 peito").
- Abreviação pesada: "q", "vc", "tbm", "mt", "hj", "pq", "sla", "tlg", "pfv".
- Mensagem quebrada em várias partes ("oi" / "então" / "minha mãe" / "tá roxa").
- Mistura de idioma ("tô com shortness of breath"), gíria regional, palavrão.

**Coerência**
- Conta a história antes do ponto ("ontem fui no mercado, aí encontrei a vizinha, aí...").
- Se contradiz ("não tenho febre... tá 38,5").
- Troca a pessoa no meio (começa falando de si e passa para o filho).
- Lugar errado do corpo ("dor no estômago" apontando para o peito; "rim" para as costas).
- Termo médico usado errado ("pressão alta" querendo dizer baixa; "AVC" para enxaqueca; "infecção" para qualquer coisa).
- Vago demais ("tô mal", "tô estranha", "não sei explicar").
- Tempo confuso ("faz uns dias", "desde sempre", "agora há pouco que foi ontem").

**Atitude**
- Minimiza para não ir ao hospital ("não é nada", "só queria saber", "não quero incomodar").
- Exagera um caso leve ("vou morrer de dor de dente").
- Mente para receber o resultado que quer.
- Pânico, frase cortada, repetição.
- Sarcasmo e humor ("tô ótima, só não sinto o braço kkk").
- Escreve por outra pessoa, sem saber os detalhes.

**Áudio transcrito** (o que um reconhecedor de voz costuma errar)
- Palavras parecidas trocadas ("falta de ar" ➜ "falta de lar"; "convulsão" ➜ "com vulsão"; "SAMU" ➜ "samba").
- Nenhuma pontuação, frases longas e emendadas.
- "né", "tipo", "então", "assim" e hesitações ("ééé").
- Números por extenso ou errados ("trinta e oito e meio", "38 5").
- Barulho virando palavra solta, e palavras cortadas no começo ou no fim.

## Personas de autor (uma para cada gerador)

- Idosa de 75 anos, que escreve devagar e formal, com erros de digitação.
- Adolescente de 16 anos, com gíria e abreviação.
- Mãe ou pai em pânico, de madrugada, escrevendo pelo filho.
- Pessoa com pouca escolaridade, que escreve pelo som.
- Trabalhador apressado no intervalo, que escreve em pedaços.
- Pessoa que minimiza tudo e não quer ir ao médico.
- Pessoa ansiosa, que exagera e conta tudo em detalhe.
- Pessoa que manda áudio (o gerador escreve a fala e simula a transcrição com erros).

## Frente 1 ➜ Lote cego "gente de verdade" (8 subagentes, um por persona)

- Cada um escreve 100 vinhetas à mão, total de 800, ids V-H-0001 em diante.
- Distribuição: ~15% SAMU, ~30% UPA, ~30% UBS e ~25% casa. Pelo menos 12% de crianças, 12% de gestantes e 12% de
  idosos.
- Cada vinheta traz pelo menos 2 erros do catálogo, marcados numa coluna nova `erros_humanos` (por exemplo
  DIGITACAO|CONTRADICAO).
- Por que 800: com pelo menos 120 casos graves cegos dá para estimar a subtriagem com intervalo de confiança. Com mais
  de 600 casos graves seguidos sem erro, dá para afirmar menos de 0,5% com 95% de confiança.

## Frente 2 ➜ Pessoa imperfeita na conversa (código)

- Criar `PessoaImperfeita` no AvaliarVinhetas: uma pessoa simulada que, durante as perguntas, erra como gente.
- Perfis com taxas configuráveis:

| Perfil | Comportamento |
|---|---|
| apressada | responde a primeira opção, pula a lista |
| confusa | marca a opção errada em 15% das vezes, "não sei" em 20% |
| minimizadora | responde "não" para os sinais graves que tem em 30% das vezes |
| ansiosa | marca sinais que não tem |
| desistente | abandona a conversa depois de N perguntas |
| idosa | não entende a lista e marca "nenhum" |

- Medir, para cada perfil, nas 10.000 e no lote V-H:
  - críticas;
  - sobretriagem;
  - perguntas até o fim;
  - em que pergunta mais gente desistiria.
- O resultado mostra quais perguntas são frágeis: quais mudam o destino grave quando a pessoa erra.

## Frente 3 ➜ Crise cega com erros humanos (2 subagentes)

- Um escreve 200 frases de risco (suicídio só como ideia, intoxicação, violência). O outro escreve 100 de controle,
  com as mesmas palavras em sentido figurado. Todas com os erros do catálogo, inclusive áudio transcrito.
- Medir a taxa por grupo e listar todas as falhas.

## Frente 4 ➜ Voz (código e teste)

- Criar um gerador de "transcrição com erros": pega 300 vinhetas existentes e aplica os erros de áudio do catálogo
  (sem pontuação, troca de palavras parecidas, números por extenso, hesitações).
- Medir a Tami com o texto original e com o transcrito, e listar o que quebra.
- Escrever `VOZ_TAMI.md` com o desenho da função "falar com a Tami", detalhado na seção "Voz" abaixo.

## Frente 5 ➜ Leitura difícil (1 subagente)

- O agente lê todas as perguntas, botões e mensagens finais como uma pessoa de 70 anos com pouca escolaridade, e como
  uma pessoa em pânico.
- Marca palavra difícil, frase longa, pergunta dupla ("A ou B?") e opção que confunde.
- Entrega a lista com a reescrita sugerida, sem mudar a informação clínica.

## Frente 6 ➜ Juntar, medir e entregar

- Rodar validador e testes.
- Rodar as 10.000 vinhetas, o lote V-H e a crise cega, com a pessoa sincera e com cada perfil de pessoa imperfeita.
- `RESULTADO_TESTE_HUMANO.md` com:
  - os números;
  - os erros humanos que mais derrubam a Tami (ranking por tipo de erro);
  - as perguntas mais frágeis;
  - o efeito da voz;
  - uma lista de correções em ordem de prioridade (só a lista; elas vêm no sprint seguinte).
- CHANGELOG e mensagem curta com os números.
- Lembrete no relatório: isto ainda é IA simulando gente. Não substitui testadores reais nem a revisão médica.

## Voz: o desenho da função "falar com a Tami"

- **Como funciona:** a pessoa toca no microfone e fala ➜ o áudio vira texto ➜ a Tami **mostra o texto para a pessoa
  conferir e corrigir** antes de usar ➜ o motor recebe o texto, igual a hoje. A conferência é obrigatória, porque um
  erro de transcrição ("falta de lar") pode esconder um sinal grave.
- **Quem transcreve:** três opções a comparar no documento.
  - Web Speech API do navegador: grátis, mas no Chrome o áudio vai para o Google.
  - Whisper rodando num servidor próprio no Brasil (versão aberta, por exemplo faster-whisper): melhor para a LGPD.
  - Serviço de nuvem com região no Brasil.
- **LGPD:** áudio com relato de saúde é dado sensível. Não guardar o áudio: transcrever e apagar. Consentimento
  específico para usar o microfone.
- **A Tami falando de volta (opcional):** síntese de voz das perguntas e do destino, para quem lê com dificuldade. Na
  urgência, a frase de ação vem primeiro, falada e escrita.
- **Teste:** a Frente 4 mede o motor com texto transcrito antes de a função ir para o app.
