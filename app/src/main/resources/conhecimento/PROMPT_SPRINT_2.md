# Prompt · Sprint 2 de robustez da Tami (23:25 ➜ 00:50 de 14 para 15/11/2026)

Objetivo: continuar o sprint 1 (v6) e atacar o que ficou aberto: a **sobretriagem**, que está em 19,3% e a meta é até
15%; os **74 casos graves** que ainda vão para um lugar mais leve; e os **sinais que faltam na base**. O critério não
muda: **caso grave nunca pode ir para um lugar mais leve**, e a sobretriagem só pode baixar sem subir a subtriagem.
Valem as regras de ouro: nunca diagnosticar, nunca indicar remédio, na dúvida o mais urgente, CVV 188, SAMU 192 e
CIATox 0800 722 6001.

Trabalhe sem pedir confirmação. Se algo for ambíguo, escolha a opção mais cautelosa, registre em DECISOES.md e siga.
Os agentes entregam até 00:10. Às 00:30 o motor congela: daí em diante só se mede, documenta e entrega.

## Frente 1 ➜ Lote cego novo V-Y (subagente)
- 300 vinhetas à mão, com autores e estilos diferentes do V-Z: idoso que escreve devagar, nordeste, interior,
  adolescente, áudio longo transcrito, quem escreve por outra pessoa.
- **Ninguém olha essas vinhetas antes da medição final.** O V-Z (sprint 1) também continua sem ser aberto: só se
  mede.

## Frente 2 ➜ Sobretriagem: causas (subagente)
- Analisa os casos de sobretriagem do falhas.csv por regra que disparou e por sinal, e acha as causas que se repetem.
- Propõe correções que baixam o exagero **sem mexer em caso grave**, cada uma com a estimativa de casos afetados.

## Frente 3 ➜ Auditoria dos sinônimos de agravantes (subagente)
- São 1.180 expressões que sobem o nível de um sinal. A auditoria procura as ambíguas, que sobem sem motivo
  ("mole", "inchado" sozinhos), e entrega a lista do que tirar ou deixar mais preciso.

## Frente 4 ➜ Lacuna semântica nos 74 casos graves (subagente)
- Para cada caso grave perdido, diz por que o sinal certo não apareceu e escreve léxico novo para o sinal, **sem copiar
  o relato**. O que este ajuste ganhar só vale como número se aparecer nos lotes cegos.

## Frente 5 ➜ Sinais que faltam, com fonte (subagente com WebFetch)
- Temperatura baixa em bebê e idoso; maus-tratos em criança (roxos sem explicação, bebê chacoalhado); psicose depois
  do parto; picada com "ferroada" que pode ser escorpião; dengue com grupo de risco (grupo B do Ministério da Saúde).

## Frente 6 ➜ Auditoria dos sinais de UBS (subagente)
- Faz com os 208 sinais de UBS a mesma auditoria que o sprint 1 fez com os de casa: procura o que pode esconder um caso
  de UPA ou SAMU.

## Frente 7 ➜ Crise com frases cegas (subagente)
- Outro agente escreve 150 frases NOVAS de risco (suicídio só como ideia, intoxicação, violência) e de controle.
  Elas medem de forma honesta o que o sprint 1 ajustou.

## Frente 8 ➜ Revisão do código do sprint 1 (subagente)
- Lê LeitorNumeros, as mudanças em BuscaExpressoes, Normalizador e MotorTriagem, o BM25 e a D-029, procurando bug,
  caso de borda e regex perigosa. Entrega os achados com o texto que reproduz cada um.

## Frente 9 ➜ Testes metamórficos de novo (subagente)
- Roda o PropriedadesVinhetas no motor v6 e confere o que o sprint 1 resolveu e o que sobrou, com atenção à negação que
  "vaza" para outro sinal ("eu não tenho febre" apagando as petéquias).

## Frente 10 ➜ Coordenador
- Aplica os achados de média gravidade da auditoria de casa e junta tudo o que as frentes entregarem.
- Cada correção ganha um teste de regressão.

## Frente 11 ➜ Juntar, medir e entregar (00:30 ➜ 00:50)
- Validador, testes e as 10.000 vinhetas.
- Lotes cegos V-Z e V-Y, medidos no motor v6 e no motor final.
- Crise cega.
- CHANGELOG v7, RELATORIO, e uma mensagem curta com os números de antes e depois.
