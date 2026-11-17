# Progresso do estudo clínico da Tami

Retomar com: "continue o estudo da Tami pelo PROGRESSO.md". Pegar a primeira linha [ ] abaixo.

## Etapa 1 ➜ Preparação e segurança
- [x] Inventário do motor (roteiros FIL, FAR, DPE, DCV, FEB, GEN, INT, CRI, PEL; 153 termos de léxico; 44 vinhetas; 30 testes OK)
- [x] Pasta conhecimento/, arquivos com cabeçalho, controles
- [x] Validador (ferramentas/validar_conhecimento.py, validar_rascunho.py; metas de distribuição com --metas; coerência vinheta × sinal)
- [x] sinais_alerta.csv completo A a J (rascunhos por área em _rascunho/, juntar no principal)
- [x] remedios_classes.csv: classes de maior risco
- [x] Revisor independente (10%): 53 OK, 9 avisos, 2 erros corrigidos; revisão focada criança (54 linhas) ➜ v1.1
- [x] Fechar v1 no CHANGELOG + mensagem

## Etapa 2 ➜ Dia a dia do pronto-atendimento
- [x] condicoes.csv ~150 condições (5 frentes em _rascunho/co_R, co_D, co_P, co_M, co_C)
- [x] lexico_extra.csv
- [x] Ponte conhecimento ➜ motor: BaseConhecimento + BuscaExpressoes (sinais SA: e condições CO:, D-006 casa com trava M12); AvaliarVinhetas; gerar_variacoes.py (D-009); 39 testes OK
- [x] 2.000 vinhetas + motor (críticas 22 = 1,1%; cego 6/500; meta zero NÃO atingida, segue)
- [x] Revisor + v2

## Etapa 3 ➜ Osso, músculo, esporte e cirurgia
- [x] Pesquisa: co_B1, co_B2, pr/re_B1, pr/re_B2, le_C, pr/re_G1, pr/re_G2 (em _rascunho, validados)
- [x] Juntar: 210 condições, 84 procedimentos, 473 recuperação, 35 lesões; 166 sinais pós-procedimento (SA-G-046..211) + SA-G-212/213/214 + SA-D2-112
- [x] +3.000 vinhetas (total 5.000) + motor: críticas 74 (1,5%), cego 14/1250, sobretriagem 18,0%
- [x] Revisores E3: vinhetas 182 OK / 7 graves (corrigidos); fontes 47 OK / 16 avisos / 11 erros (todos "normal" sem apoio)
- [x] Conferência completa da recuperação (473 linhas): 225 OK, 153 reescritas, 95 normais apagados, 8 alertas subidos
- [x] Revisor + v3
## Etapa 4 ➜ Resto do mapa
- [x] Pesquisa e junção: 71 exames (H), +52 classes de remédio (total 69), 49 sinais de câncer + 15 condições (F), 18 sinais pós-exame (SA-H)
- [x] +4.500 vinhetas (total 9.500): críticas 246 (2,6%), cego 58/2.375, sobretriagem 17,7%
- [x] Revisor + v4
## Etapa 5 ➜ Ataque e buracos
- [x] 500 vinhetas de ataque (V-A1, V-A2; total 10.000), medidas ANTES de ajuste: só texto 181 críticas (36,2%), com lista 26 (5,2%)
- [x] Buracos do ataque: leitor de números (D-024), sinônimos de agravantes (D-025), febre em imunossuprimido (D-026), léxico de 8 sinais
- [x] Buracos de cobertura: +29 sinais (esporte, nariz, teste do pezinho e coraçãozinho, miosite; 14 SIM), lista com peso de urgência (D-027)
- [x] Revisor 15% (75): 69 OK, 6 corrigidas (3 subtriagens) ➜ D-028 (6 linhas pós-op subiram para SAMU)
- [x] v5: críticas 202 de 10.000 (2,0%), cego 47/2.375, só texto 2.133, sobretriagem 18,3%
## Etapa 6 ➜ Auditoria final
- [x] Rodar todos os testes (41 OK) e o validador com --metas (0 erros, 0 avisos)
- [x] revisao_medica.xlsx: 300 linhas (P1 39, P2 150, P3 105, P4 6), gerada por ferramentas/gerar_revisao_medica.py
- [x] RELATORIO.md
- [x] Mensagem final (Chrome não foi usado: nenhuma aba para fechar)

ESTUDO CONCLUÍDO. Próximo passo é humano: revisão médica.

## Sprint de robustez (22:41 ➜ 23:41) · PROMPT_SPRINT_1H.md
- [x] 10 frentes com agentes em paralelo (lote cego V-Z, léxico, metamórficos, unitários e fuzz, crise, auto-teste do léxico, agravantes, auditoria de casa, ranking)
- [x] Correções (D-029, D-030) + 8 testes de regressão (SprintRobustezTest): 79 testes OK
- [x] Medição final e v6

## Sprint 2 (23:25 ➜ 00:50) · PROMPT_SPRINT_2.md
- [x] 11 frentes, D-031 a D-033, 90 testes OK, medição v7 (críticas 37; V-Z 1/300; V-Y 5/300; crise cega 2: 75,6/74,3/77,1%)
- [ ] Próximo: frases de crise reais revisadas por especialista; propostas C2, C3 e negação (código pronto em _revisao/); revisão médica
