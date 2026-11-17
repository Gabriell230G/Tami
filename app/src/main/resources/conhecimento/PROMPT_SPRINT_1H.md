# Prompt · Sprint de robustez da Tami (1 hora: 22:41 ➜ 23:41 de 14/11/2026)

Objetivo: deixar o motor da Tami mais difícil de quebrar e medir isso de forma honesta. O critério é sempre o mesmo:
**caso grave nunca pode ir para um lugar mais leve**. Valem todas as regras de ouro do estudo: nunca diagnosticar,
nunca indicar remédio, na dúvida o mais urgente vence, CVV 188, SAMU 192 e CIATox 0800 722 6001.

Trabalhe sem pedir confirmação. Se algo for ambíguo, escolha a opção mais cautelosa, registre em DECISOES.md e siga.
A hora é fixa. Às 23:30 pare de mexer no motor e só meça, documente e entregue.

## Frente 1 ➜ Lote cego final (subagente, começa já)
- 300 vinhetas novas, escritas à mão, com cara de mensagem real de WhatsApp: áudio transcrito, erro, gíria, mãe
  escrevendo pelo filho, idoso que minimiza.
- **Ninguém olha essas vinhetas antes da medição final.** Elas servem só para dar o número honesto do fim do sprint.
- Ids V-Z-0001 a V-Z-0300. Ficam fora do vinhetas_10k.csv até a medição.

## Frente 2 ➜ Léxico dos sinais que mais falham (subagente, começa já)
- Recebe só a lista de ids dos sinais que mais aparecem nas falhas críticas, **sem os relatos**.
- Escreve do zero de 10 a 15 jeitos populares novos de dizer cada sinal (regional, gíria, paráfrase, sem a palavra
  óbvia), sem ver nenhuma vinheta.

## Frente 3 ➜ Testes de propriedade, ou metamórficos (eu)
Regras que o motor tem de cumprir sempre. Cada uma é testada em todas as 10.000 vinhetas:
1. **Grave nunca some:** acrescentar ao relato uma frase claramente de SAMU ("e agora ele desmaiou e não acorda")
   leva o destino para SAMU.
2. **Risco nunca baixa:** pôr no perfil um fator de risco (anticoagulante, imunidade baixa, idade acima de 60) nunca
   deixa o destino mais leve.
3. **Escrita não muda o destino grave:** letras maiúsculas, texto sem acento, sem pontuação ou com letras repetidas
   não deixam um caso grave mais leve.
4. **Ruído não muda o destino grave:** cumprimento antes ("boa noite, tudo bem?") ou queixa leve depois não deixa um
   caso grave mais leve.
5. **Entrada estranha não derruba o motor:** texto vazio, gigante, emoji, só números, só pontuação ou código.

Cada violação vira bug, e cada bug vira correção mais teste.

## Frente 4 ➜ Testes unitários do que nasceu na Etapa 5 (eu)
- Leitor de números (D-024), sinônimos de agravante (D-025), febre em imunossuprimido (D-026), "39sem".
- Todos entram em BaseConhecimentoTest e LeitorNumerosTest, para não quebrarem depois sem ninguém ver.

## Frentes extras (pedido do Gabriel às 22:42: muitos agentes ao mesmo tempo)
- **Frente 6 ➜ Crise:** mais de 150 frases de ideia de suicídio, intoxicação e violência, e frases de controle
  ("morrendo de fome"). A meta é que nenhuma frase de risco escape do CVV, do SAMU ou do CIATox.
- **Frente 7 ➜ Auto-teste do léxico:** cada expressão de cada sinal de SAMU ou UPA, sozinha, tem de dar pelo menos o
  destino do próprio sinal.
- **Frente 8 ➜ Sinônimos de agravantes:** passar de 55 para cerca de 175 agravantes cobertos.
- **Frente 9 ➜ Auditoria dos sinais de casa:** procurar o que pode mandar para casa algo grave.
- **Frente 10 ➜ Experimento de ranking da lista:** BM25, pares de palavras e melhor expressão, escolhido pelo lote cego.
- Coordenação: só o coordenador muda src/main/. Cada agente compila na própria pasta (ferramentas/compilar_em.sh)
  e entrega os achados em _revisao/ até 23:12.

## Frente 5 ➜ Juntar, medir e entregar (23:30 ➜ 23:41)
- Juntar o léxico, rodar o validador, os testes e as 10.000 vinhetas.
- Só no final, rodar o lote cego V-Z.
- Gravar v6 no CHANGELOG, atualizar o RELATORIO.md e mandar uma mensagem curta com os números de antes e depois.
