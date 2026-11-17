# Proposta · Tela "Para quem é a ajuda?" (sprint 3, Frente 5)

Status: **proposta**. Nada disto está no código ainda. Entra num sprint seguinte.

## 1. Por que essa tela existe

Hoje ninguém pergunta para quem é a ajuda. O perfil (idade, gestante, diabetes, fatores) é o da pessoa que usa o app.
Isso quebra em dois pontos:

- **A pergunta erra o sujeito.** A mãe lê "você está pensando em se machucar?" e responde por ela, não pelo filho
  adolescente. O mesmo acontece em FAR_01 (asma da criança) e FIL_02 (dor no peito do pai).
- **O perfil erra a pessoa.** A filha de 30 anos escreve pela mãe de 75. O motor usa 30 anos: não é idoso, não
  mostra FIL_I01, não sobe o nível por idade. A avó escreve pelo neto de 1 mês: o motor não mostra FIL_C02 nem FIL_C03.

No sprint 3 o texto usa, por enquanto, "você (ou a pessoa que precisa de ajuda)". É longo de propósito: é um marcador
fácil de achar e trocar por `{quem}` quando esta tela entrar.

## 2. Fluxo

```
ABERTURA (com "Se for uma emergência, ligue 192 agora")
  ➜ relato livre
  ➜ [se o texto já tem crise ou sinal de SAMU: mostra a ação primeiro (192, 188 ou CIATox), sem esperar esta tela]
  ➜ PARA_QUEM  "Para quem é a ajuda?"
        [Para mim]                ➜ usa o perfil salvo da pessoa ➜ POS_RELATO
        [Para outra pessoa]       ➜ PQ_IDADE ➜ PQ_GESTANTE (se couber) ➜ PQ_DIABETES ➜ POS_RELATO
        [Para um bebê ou criança] ➜ PQ_IDADE_CRIANCA ➜ PQ_GESTANTE (só se 10 anos ou mais) ➜ POS_RELATO
  ➜ perguntas do motor, com {quem} trocado
```

Regras do fluxo:

1. **A ação nunca espera esta tela.** Se o detector de risco ou o léxico de emergência acharem algo grave no texto, a
   mensagem de 192, 188 ou CIATox aparece antes. A tela vem depois, para afinar o resto.
2. **O texto pode sugerir, nunca decidir sozinho.** Se o relato tem "meu filho", "minha mãe", "o bebê", "minha esposa
   grávida", o botão certo já vem destacado. A pessoa confirma com um toque. Motivo: "minha mãe teve AVC" pode ser só
   história (proposta C3 do sprint 2).
3. **No máximo 3 telas extras** e todas com botão grande. Quem está em pânico não pode ficar preso aqui.
4. **Mudou de ideia?** Um link pequeno "A ajuda é para outra pessoa" fica no topo de todas as perguntas e volta para
   PARA_QUEM sem perder o relato.

## 3. Textos das telas

| Código | Texto | Botões |
|---|---|---|
| PARA_QUEM | Para quem é a ajuda? | [Para mim] [Para outra pessoa] [Para um bebê ou criança] |
| PARA_QUEM_SUGERIDO | Pelo que você escreveu, parece que a ajuda é para outra pessoa. Está certo? | [Sim, é para outra pessoa] [Não, é para mim] [É para um bebê ou criança] |
| PQ_IDADE | Quantos anos a pessoa tem? | campo de número + [Não sei] |
| PQ_IDADE_60 | Não tem problema. A pessoa tem 60 anos ou mais? | [Sim] [Não] [Não sei] |
| PQ_IDADE_CRIANCA | Qual a idade do bebê ou da criança? | [Menos de 2 meses] [2 meses, ainda não fez 3] [De 3 a 11 meses] [De 1 a 12 anos] [13 a 15 anos] [Não sei] |
| PQ_GESTANTE | A pessoa está grávida? | [Sim] [Não] [Não sei] |
| PQ_DIABETES | A pessoa tem diabetes? | [Sim] [Não] [Não sei] |
| PQ_OBRIGADA | Obrigada. Agora as perguntas vão ser sobre {quem}. | (some sozinha em 2 segundos) |

Todos os textos seguem o guia de voz: frase curta, uma pergunta por tela, sem diagnóstico, sem remédio.

## 4. Efeito nos dados (Perfil)

O `Perfil` passa a ser o **da pessoa que precisa de ajuda**, não o de quem usa o app.

| Resposta | idadeMeses | gestante | diabetes | fatores |
|---|---|---|---|---|
| Para mim | do perfil salvo | do perfil salvo | do perfil salvo | do perfil salvo |
| Outra pessoa, idade digitada N anos | N × 12 | PQ_GESTANTE | PQ_DIABETES | vazio (as perguntas GEN_05 e FEB_A10 perguntam de novo) |
| Outra pessoa, idade "Não sei" + 60 ou mais "Sim" ou "Não sei" | 720 (idoso) | PQ_GESTANTE | PQ_DIABETES | vazio |
| Outra pessoa, idade "Não sei" + 60 ou mais "Não" | 216 (18 anos, adulto) | PQ_GESTANTE | PQ_DIABETES | vazio |
| Menos de 2 meses | 1 | false | false | vazio |
| 2 meses, ainda não fez 3 | 2 | false | false | vazio |
| De 3 a 11 meses | 3 | false | false | vazio |
| De 1 a 12 anos | 12 | false | false | vazio |
| 13 a 15 anos | 156 | PQ_GESTANTE | false | vazio |
| Bebê ou criança, idade "Não sei" | 1 | false | false | vazio |

Regras de cautela (registradas como decisão, todas a conferir com o médico):

- **Faixa vira o menor valor da faixa.** Bebê mais novo abre mais perguntas de alarme (FIL_C02, FIL_C03). Idoso abre
  FIL_I01. Na dúvida, o mais urgente.
- **"Não sei" na gestação vale grávida** (já é a regra RT-14 do `Perfil`).
- **"Não sei" no diabetes vale diabetes.** Mostra FIL_I01, que é uma pergunta a mais de alarme. Decisão médica.
- **PQ_GESTANTE só aparece de 10 anos para cima** e para "outra pessoa". Para "Para mim" o dado já vem do perfil. O
  limite de 10 anos é decisão médica.
- **Os fatores (anticoagulante, quimioterapia, transplante...) do perfil salvo não passam para outra pessoa.** Eles são
  de quem usa o app.

## 5. Como `{quem}` troca o sujeito

### 5.1 Variáveis

| Variável | Para mim | Outra pessoa | Bebê (até 11 meses) | Criança (1 a 15 anos) |
|---|---|---|---|---|
| `{quem}` | você | a pessoa | o bebê | a criança |
| `{Quem}` | Você | A pessoa | O bebê | A criança |
| `{para_quem}` | para você | para a pessoa | para o bebê | para a criança |
| `{o}` (final de adjetivo) | do perfil (o/a); sem dado, "o" | a | o | o |

O verbo em português é o mesmo para "você" e para "a pessoa" ("tem", "está", "teve"). Por isso a troca é só do sujeito.

### 5.2 Troca mecânica do texto de hoje

O texto do sprint 3 já está pronto para a troca. No dia em que a tela entrar, basta substituir em `nos.csv` e
`textos_tami.csv`:

| Texto de hoje | Vira |
|---|---|
| `você (ou a pessoa que precisa de ajuda)` | `{quem}` |
| `Você (ou a pessoa que precisa de ajuda)` | `{Quem}` |
| `para você (ou para a pessoa que precisa de ajuda)` | `{para_quem}` |

### 5.3 Pergunta por pergunta

| Nó | Para mim | Para outra pessoa | Observação |
|---|---|---|---|
| FIL_01, FAR_01, GEN_01 | Agora, você tem algum destes sinais? | Agora, a pessoa tem algum destes sinais? | lista igual |
| FIL_02 | Você tem dor, aperto ou peso no peito? | A pessoa tem dor, aperto ou peso no peito? | |
| FIL_03, FIL_04 | ..., você teve algum destes sinais? | ..., a pessoa teve algum destes sinais? | "Ficou confus{o} de repente" |
| FIL_05, DCV_02, INT_01, FEB_A03B | Você tem / teve algum destes sinais? | A pessoa tem / teve... | |
| FIL_07 | Alguém engoliu... | igual | já é neutro |
| FIL_08 | Você está pensando em se machucar ou em tirar a própria vida? | A pessoa está pensando em se machucar ou em tirar a própria vida? Responda o que você sabe. | ver 5.4 |
| CRI_01 | Algum destes vale para você agora? | Algum destes vale para a pessoa agora? | itens sem sujeito |
| FIL_C01, FIL_C02, FIL_C03 | (não aparece: só criança) | O bebê / a criança tem algum destes sinais? | já usa "o bebê" |
| FIL_G01, FIL_G02, FIL_G03 | Você está perdendo sangue...? | A gestante está perdendo sangue...? | `{quem}` = "a gestante" quando gestante = true e não é para mim |
| FEB_A02, FEB_A05, FEB_A09 | Agora, você tem algum destes? | Agora, a pessoa tem algum destes? | |
| FEB_A08, FEB_A11 | Algum destes vale para você? | Algum destes vale para a pessoa? | FEB_A11: "Mora sozinh{o}" |
| FEB_A10, GEN_05 | Você tem...? | A pessoa tem...? | |
| DPE_*, DCV_03, DCV_04B, GEN_02, GEN_03, GEN_06, FEB_A03, FEB_A04, FEB_A06, FEB_A07, RES_07, FAR_02, FAR_03, PEL_03 | sem sujeito | sem sujeito | não muda |

### 5.4 Textos que mudam mais que o sujeito

| Código | Para mim | Para outra pessoa |
|---|---|---|
| FIL_08 (Tami) | Vou te fazer uma pergunta importante. Pode responder com sinceridade. Você está pensando em se machucar ou em tirar a própria vida? | Vou te fazer uma pergunta importante. A pessoa está pensando em se machucar ou em tirar a própria vida? Responda o que você sabe. |
| CVV188 | Se você está em perigo agora, ligue 192. Para conversar, ligue 188 (CVV): é de graça e funciona 24 horas. Você não precisa passar por isso sem ajuda. | Se a pessoa está em perigo agora, ligue 192. Fique com ela, se for seguro para você. O 188 (CVV) também conversa com quem está ajudando: é de graça, 24 horas. (revisar com médico) |
| BLK-EI-RESPIRACAO | Fique sentad{o}, com o corpo um pouco para frente. Afrouxe a roupa. | Deixe a pessoa sentada, com o corpo um pouco para frente. Afrouxe a roupa dela. |
| BLK-EI-PEITO | Enquanto a ajuda chega: fique sentad{o}, em repouso e com alguém junto. Não dirija. | Enquanto a ajuda chega: deixe a pessoa sentada, em repouso, e fique com ela. Ela não deve dirigir. |
| BLK-EI-INTOX | Não tente vomitar. Separe a embalagem do que foi engolido ou respirado. | Não faça a pessoa vomitar. Separe a embalagem do que foi engolido ou respirado. |
| LISTA_CONFIRMACAO | Marque as que você tem agora. | Marque as que a pessoa tem agora. |
| FIL_04 (ajuda) | Faça na frente do espelho: sorria, levante os dois braços e repita uma frase. | Peça para a pessoa sorrir, levantar os dois braços e repetir uma frase. |

Ponto aberto para o médico: FIL_08 e CRI_01 para bebê de menos de 2 anos não fazem sentido. Proposta: esconder FIL_08
quando a idade for menor que 10 anos **só se o médico aprovar**. Até lá, a pergunta continua aparecendo para todos.

## 6. O que muda no código (sprint futuro)

1. **Tela antes do motor.** O app monta o `Perfil` a partir de PARA_QUEM e chama `MotorTriagem.iniciar(perfil, ...)`
   com o perfil de quem precisa de ajuda. O motor não precisa saber da tela.
2. **Novo campo `ParaQuem`** (`MIM`, `OUTRA_PESSOA`, `BEBE`, `CRIANCA`) guardado na `Sessao` e no histórico
   (`RespostaDada`), para o relatório mostrar "respondido por outra pessoa".
3. **Renderizador de texto** (`TextoTela.montar(texto, paraQuem, perfil)`): troca `{quem}`, `{Quem}`, `{para_quem}`,
   `{o}` e quebra a lista em linhas no separador `" • "`. `CatalogoRoteiros.validar` passa a recusar variável
   desconhecida entre chaves.
4. **Fatores não passam** do perfil salvo para outra pessoa (`fatores = Set.of()`).
5. **Ordem com a crise:** o detector de risco e o léxico de emergência rodam antes da tela PARA_QUEM. Eles já não
   dependem do perfil para dar 192, 188 ou CIATox.
6. **Sugestão pelo texto:** um grupo pequeno de expressões ("meu filho", "minha filha", "minha mãe", "meu pai", "o
   bebê", "meu neto", "minha esposa", "meu marido", "minha avó") só destaca o botão. Não decide.
7. **Testes novos:**
   - todas as vinhetas atuais com `MIM` dão exatamente o mesmo destino (regressão);
   - perfil novo de pessoa imperfeita "RESPONDE_POR_OUTRO", que antes respondia por si e agora pelo outro;
   - vinhetas com "meu filho" + crise: FIL_08 com o texto de outra pessoa e o CVV188 de outra pessoa;
   - idade "Não sei" sempre cai no lado mais cuidadoso (tabela da seção 4).
8. **Planilha do médico:** os pontos marcados "decisão médica" nesta proposta vão para `revisao_medica.xlsx`.

Lembrete: isto é uma proposta feita por IA lendo como gente. Precisa de teste com pessoas de verdade e da revisão médica.
