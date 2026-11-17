# Changelog do conhecimento da Tami

## v0 (14/11/2026)
- Estrutura criada.

## v1 · 2026-11-14 · Etapa 1 (preparação e segurança)
- sinais_alerta.csv: 622 sinais de alerta, áreas A a I (J é linguagem popular, entra no léxico na Etapa 2).
  SAMU 222 · UPA 271 · UBS 74 · CASA 55 (aprox.). 545 com 2 fontes de organizações diferentes; os de 1 fonte estão com revisar_medico=SIM.
- remedios_classes.csv: 17 classes de maior risco (anticoagulantes, antiagregantes, insulina e outros remédios de diabetes,
  opioides, corticoides, imunossupressores, quimio e imunoterapia), efeitos comuns e graves, o que a Tami pode e não pode dizer.
- Validadores: validar_conhecimento.py (formato, fontes, proibições, coerência vinheta × sinal, metas, D-013) e validar_rascunho.py.
- Ponte para o motor: BaseConhecimento + BuscaExpressoes (negação, erro de digitação, formas verbais), regras SA:/CO:,
  D-006 (casa só com regra aprovada), D-010 (agravantes no relato), D-012 (suicídio ➜ CRI), D-013 (sintoma solto fica no roteiro).
- Revisor independente 10% (64 linhas): 53 OK, 9 avisos, 2 erros. Erros e avisos corrigidos (SA-D3-051 e SA-D2-093 subiram
  para UPA_HOJE; SA-D1-045 subiu; SA-G-045 e SA-D3-071 criadas; fontes trocadas em SA-D3-059 e SA-E-050; RM-011 ➜ SIM).
  Os 2 erros tinham o mesmo padrão (linha leve valendo para criança) ➜ revisão focada das 54 linhas do padrão em andamento.
- Testes: 41 OK. Com a base ligada nenhuma das 44 vinhetas antigas baixou; 3 subiram com fonte (água sanitária em criança, dengue).

## v1.1 · revisão focada em criança (54 linhas)
- 43 OK, 7 restringir, 1 subir, 3 sem fonte. Linhas leves de adulto ficaram só para adolescente/adulto/idoso e
  ganharam gêmeas para criança/bebê com o destino da fonte (SA-D1-101, SA-D2-111, SA-D3-072/073/074, SA-A-081).
  SA-A-068 (engasgo com tosse forte) ➜ UBS; SA-G-012/013/014 só adultos; SA-D3-071 sem subida duplicada.

## v2 · 2026-11-14 · Etapa 2 (dia a dia do pronto-atendimento)
- condicoes.csv: 160 condições comuns (respiratório, pele, olhos, ORL, digestivo, urinário, mulher e gestação,
  criança, infecções brasileiras, osso/músculo, dor, nervos, mente, hormônios, coração, crônicas, idoso, boca).
  Nomes populares (área J) ligados às condições: cobreiro, zipra, pereba, vento virado, caganeira, dor nos quartos...
- lexico_extra.csv: 107 expressões populares de emergência ("teve um treco", "estrebuchando", "pelando de febre").
- Léxico dos sinais: +4.824 expressões populares (escritas sem ver as vinhetas).
- Motor: busca por palavras-chave e radical; formas verbais; negação que não confunde "não aguento"; passado forte;
  falsos amigos; lista de confirmação de sinais (D-015) com bônus da região do corpo 3D e do grupo (D-018);
  subida de nível com idade calculada, semanas de gestação e sinônimos de fator (D-017); condição usa o destino padrão (D-016).
- vinhetas_10k.csv: 2.000 (900 escritas à mão + 1.100 variações recalculadas). Lote cego: 500 das escritas à mão.
- Resultado no motor (2.000): subtriagem crítica 22 (1,1%) com a lista de confirmação; 354 (17,7%) só com texto livre.
  Lote cego (500, nunca usado para ajustar): 6 críticas (1,2%). Sobretriagem 15,5%.
  Primeira passada honesta (900, antes de qualquer ajuste): 284 críticas (31,6%).
- Revisores: condições 16/160 (0 erro, 7 avisos aplicados); vinhetas 200 (184 OK, 11 exageros, 5 erros graves,
  todos nas variações geradas ➜ gerador corrigido e lote refeito).
- META NÃO ATINGIDA: subtriagem zero. Continua nas próximas etapas (léxico, lista, região).

## v3 · 2026-11-14 · Etapa 3 (osso, esporte, procedimentos e recuperação)
- condicoes.csv: +50 fraturas e luxações osso por osso (CO-B1 membros superiores, tronco e face; CO-B2 pelve e
  membros inferiores, criança e idoso). Total 210.
- procedimentos.csv: 84 (tala, gesso, tipoia, redução, placa, haste, prótese; bariátrica, vesícula, apêndice, hérnia,
  cesárea, parto, útero, próstata, rim, tireoide; cateterismo, stent, marca-passo, ponte, válvula; artroscopia, LCA,
  coluna; catarata, retina; amígdala, adenoide, septo; siso, implante, canal; plástica).
- recuperacao.csv: 473 linhas (o que é normal e o que é alerta em cada fase). CONFERÊNCIA COMPLETA linha a linha
  contra a fonte: 225 OK, 153 reescritas, 95 "normais" apagados por falta de apoio, 8 alertas subidos.
- lesoes_esporte.csv: 35 lesões com graus, fases de retorno e prevenção.
- sinais_alerta.csv: +170 sinais (166 pós-procedimento detectáveis pelo motor, gesso normal/irritado, gás no olho e
  avião, queda na gestação > 20 semanas). 7 sinais subiram de nível pela fonte. Total 799.
- Motor: condições graves na lista de confirmação só pelos sintomas (D-020), relato de recuperação não aplica o piso
  da condição (D-019), palavra conhecida não vira erro de digitação (D-021), índices (avaliação 4x mais rápida).
- vinhetas: 5.000 (2.250 à mão + 2.750 variações). Lote cego: 1.250 à mão.
- Resultado: subtriagem crítica 76 (1,5%); lote cego 15/1.250 (1,2%); só texto livre 1.069 (21,4%).
  Sobretriagem 18,0% (meta 15%). Primeira passada da Etapa 3, antes de ajuste: 36 críticas nas 1.350 novas (2,7%).
- Revisores E3: vinhetas 182 OK / 7 graves (corrigidos: gestante que caiu, prótese luxada, hiponatremia...);
  fontes 47 OK / 16 avisos / 11 erros ("normal" sem apoio) ➜ motivou a conferência completa da recuperação.
- METAS AINDA NÃO ATINGIDAS: subtriagem zero e sobretriagem até 15%.

## v4 · 2026-11-14 · Etapa 4 (exames, remédios, câncer e o resto do mapa)
- exames.csv: 71 exames (sangue, urina, fezes, imagem, coração, endoscopia, colonoscopia, biópsias, colposcopia,
  cintilografia, PET-CT, EEG, espirometria, sono, audição, retina, testes do bebê): para que serve, preparo, como é,
  quanto dura e o que se sente. Nenhum "suspenda o remédio": sempre "avise quem pediu o exame".
- remedios_classes.csv: 69 classes (+52: analgésicos, AINE, antibióticos, antivirais, pressão, estatina, estômago,
  alergia, asma, anticoncepcional, tireoide, vitaminas; antidepressivos, benzodiazepínicos, antipsicóticos, lítio,
  anticonvulsivantes, TDAH, Parkinson, demência, digoxina, amiodarona, gota, isotretinoína, hormônios, anabolizantes,
  emagrecedores, fitoterápicos, disfunção erétil).
- sinais_alerta.csv: +49 câncer (SA-F2), +18 depois de exames (SA-H), lítio (SA-I-025). Total 866. Léxico popular
  2ª rodada: +2.050 expressões nos 257 sinais com léxico curto.
- condicoes.csv: +15 cânceres (CO-F). Total 225.
- Motor: agravantes abstratos com sinônimos de relato (D-023).
- vinhetas: 9.500 (4.275 à mão + 5.225 variações). Lote cego: 2.375 à mão.
- Resultado: subtriagem crítica 246 (2,6%); lote cego 58/2.375 (2,4%); só texto livre 2.032 (21,4%).
  1ª passada da Etapa 4 (2.025 novas, antes de ajuste): 82 críticas (4,0%). Sobretriagem 17,7%.
- Revisores E4: vinhetas 183 OK / 14 exageros / 1 grave (anafilaxia ao contraste, corrigido); fontes 10 OK /
  8 avisos / 4 erros (lítio "beba água" ➜ corrigido e sinal SA-I-025 criado; anticonvulsivante com bolhas ➜ 192;
  SA-H-030 sem apoio da fonte ➜ UPA e SIM; leucemia SA-F2-026 ➜ UPA).
- METAS AINDA NÃO ATINGIDAS: subtriagem zero e sobretriagem até 15%.

## v5 · 2026-11-14 · Etapa 5 (ataque e buracos)
- vinhetas: +500 de ataque escritas à mão (V-A1, V-A2: paráfrase, números, escondido, negação traiçoeira, passado x
  agora, dois sinais, gíria e regional, curtíssimo, terceiro real, parece grave). Total 10.000.
- Medição do ataque ANTES de qualquer ajuste: só texto livre 181 críticas de 500 (36,2%); com a lista 26 (5,2%);
  sobretriagem 10,8%. Depois dos ajustes (já não é medida cega): com a lista 2 (0,4%).
- Motor: leitor de números de sinais vitais (D-024); agravantes ditos com outras palavras, em agravantes_sinonimos.csv
  (D-025); febre em imunossuprimido vai para UPA (D-026); peso de urgência na lista de confirmação (D-027);
  "39sem" não vira mais negação; diminutivos ("dorzinha").
- sinais_alerta.csv: +29 (SA-C-101..119 esporte, SA-D2-301..305 triagem neonatal e chiado no exercício,
  SA-D3-301..305 nariz depois de pancada e músculo); léxico de 8 sinais ampliado; 6 linhas pós-op subiram para SAMU
  (D-028). Total 895. 14 conflitos novos no CONFLITOS.md.
- Revisor E5 (15%, 75 vinhetas de ataque): 69 OK, 6 corrigidas (3 subtriagens: membro frio pós-artroscopia, corte
  sangrando 20 min, espinha no queixo).
- Resultado com 10.000: subtriagem crítica 202 (2,0%); lote cego 47/2.375 (2,0%; era 58); só texto livre 2.133
  (21,3%); sobretriagem 18,3%; produção (trava M12) críticas 202, sobretriagem 30,7%.
- METAS AINDA NÃO ATINGIDAS: subtriagem zero e sobretriagem até 15%.

## v6 · 2026-11-14 · Sprint de robustez (22:41 ➜ 23:41, PROMPT_SPRINT_1H.md)
- 10 frentes com agentes em paralelo: lote cego novo V-Z (300, estilo WhatsApp), léxico dos 60 sinais que mais falhavam
  (+767), sinônimos de agravantes (+1.122, e mais 6 linhas do coordenador), testes metamórficos (2.000 vinhetas),
  testes unitários e fuzz (3.000 entradas estranhas: 0 exceções), crise (173 frases), auto-teste do léxico
  (8.550 expressões: 2 falhas), auditoria dos sinais de casa (140 linhas: 56 ALTA), ranking da lista (17 variações).
- Correções: D-029 (CVV 188 e crise para risco de suicídio da base; CIATox para intoxicação da base; violência física
  SA-A-090; 45 sinais graves passaram a valer para idoso; grupo de risco completo nas condições; casa com sinal de
  alarme vai para a UPA; auditoria de casa aplicada; vírgula decimal; "grávida de 30 sem"; febre em gestante e em bebê
  de menos de 3 meses sem precisar da palavra; "R$ 38,5" não é febre; texto gigante cortado no começo e no fim) e
  D-030 (lista ordenada por BM25 com cobertura e pares de palavras).
- Testes: 79 OK (eram 41), incluindo SprintRobustezTest (regressão de cada bug achado).
- Resultado com 10.000 (motor final): subtriagem crítica 74 (0,7%; era 202); lote cego antigo 20/2.375 (era 47,
  mas ele ajudou a escolher o ranking); só texto livre 2.059 (era 2.133); sobretriagem 19,3% (era 18,3%).
  Sem o ranking novo: 124 críticas, sobretriagem 19,7%.
- Lote cego novo V-Z (300, nunca usado para nada, medido antes e depois): críticas 2 ➜ 1; sobretriagem 12,3% ➜ 13,0%;
  só texto 44 ➜ 41.
- Crise (frases do teste, já usadas para ajuste): ideia de suicídio 120/120 chega ao CVV (era 106), só pelo texto
  38/60 (era 12/60); intoxicação 40/41 (era 19/41); violência 35/35 (era 5/35); controles sem risco 33/37 (era 35/37).
- METAS AINDA NÃO ATINGIDAS: subtriagem zero e sobretriagem até 15%.

## v7 · 2026-11-15 · Sprint 2 de robustez (23:25 ➜ 00:50, PROMPT_SPRINT_2.md)
- 11 frentes (10 agentes + coordenador): lote cego novo V-Y (300, autores diferentes), causas da sobretriagem,
  auditoria dos sinônimos de agravantes, lacuna semântica nos 74 graves, 16 sinais novos com fonte (temperatura baixa,
  "ferroada", maus-tratos em criança, psicose depois do parto, dengue grupo B), auditoria dos 294 sinais e condições
  de UBS, crise com frases cegas (2 conjuntos), revisão de código do sprint 1, testes metamórficos de novo.
- Motor: detector de risco por raiz e contexto, que lê o texto inteiro e tira antes o sentido figurado (D-031);
  correções da revisão de código: CVV/CIATox nunca se perdem, afirmado vence negado, thread, fatores nulos, história
  com "agora", falsos positivos do leitor de números, temperatura baixa (D-032); idoso vale como adulto em sinal
  grave; negação não pontua a lista; casa com agravante de tempo sobe só um nível; até 3 itens de casa a mais na lista;
  resfriado no texto responde a FEB_A09.
- Base: auditorias de casa (MÉDIA, BAIXA) e de UBS (ALTA, MÉDIA) aplicadas (D-033); 65 sinônimos de agravante
  ambíguos tirados; 13 expressões que disparavam sinal grave em relato leve trocadas; +224 expressões de léxico e +40 de
  agravante escritas sem copiar os relatos; 16 sinais novos. 40 vinhetas subiram de destino para ficar coerentes com a
  base mais cautelosa (por isso o "antes e depois" nas 10.000 não é exatamente comparável; os lotes cegos são).
- Testes: 90 OK (eram 79), com DetectorRiscoTest e RevisaoCodigoTest.
- Resultado nas 10.000: críticas 37 (0,4%; eram 74 na v6 e 202 na v5); sobretriagem 17,1% (era 19,3%); só texto
  livre 2.054; lote cego antigo 13/2.375 (não é mais cego).
- Lotes cegos (nunca usados para ajuste): V-Z críticas 1 ➜ 1, sobretriagem 13,0% ➜ 12,0%; V-Y críticas 5 ➜ 5,
  sobretriagem 24,0% ➜ 24,0% (no motor v5: 7 críticas, 22,7%).
- Crise com frases cegas, conjunto 1, medido no motor v6 ANTES do ajuste: suicídio 37,5%, intoxicação 45,7%,
  violência 60%, controles 95%. Depois do detector: 100% nas três (já é ajuste). Conjunto 2 (160 frases NOVAS, medida
  honesta no motor v7 congelado): suicídio 75,6%, intoxicação 74,3%, violência 77,1%, controles 95,6%.
- METAS AINDA NÃO ATINGIDAS: subtriagem zero e sobretriagem até 15%.

## v7-h · 2026-11-15 · Sprint "gente de verdade" (só medição, motor v7 congelado)
- Lote V-H: 8 personas escreveram 800 casos à mão, revisados contra a base: 751 aceitos e 49 lacunas
  (_revisao/vinhetas_VH.csv, meta_VH.csv, lacunas_VH.csv). Fica fora do vinhetas_10k.csv.
- V-H com a pessoa sincera: críticas v5 52 ➜ v6 38 ➜ v7 28 (3,7%); sobretriagem 19,3%. Quando autor e revisor concordaram
  sozinhos: 6/577 (1,0%), sendo 1 AVC escrito pelo som, 1 convulsão na gestante e 4 glicoses muito altas (lacuna da base).
- Pessoa imperfeita (PessoaImperfeita.java): MINIMIZADORA 8,6% de críticas nas 10.000; a pergunta frágil é a CONF_SINAIS.
- Crise com erros humanos (CriseHumanaTeste.java, 300 frases cegas, só texto): suicídio 66,7%, intoxicação 65,0%,
  violência 50,0%, controles 83,0%.
- Voz: simulador de transcrição (ferramentas/simular_transcricao.py, avaliar_voz.py); críticas com lista 0 ➜ 7 no nível pesado.
  Desenho em VOZ_TAMI.md.
- Leitura difícil: 109 problemas em 63 textos de tela (LEITURA_DIFICIL.csv); 3 textos novos mudaram o sentido clínico.
- Relatório e lista priorizada de 12 correções: _revisao/RESULTADO_TESTE_HUMANO.md. Nenhuma correção aplicada.

## v8 · 2026-11-15 · Sprint 3 (correções do teste com "gente de verdade")
- Cego 3 escrito antes das correções: 378 vinhetas (V-N) e 400 frases de crise (CN), revisadas pela base v7.
- F1 detector de crise por categoria; F2 lista já marcada, glicose pelo número, C2/C3, convulsão ligada; F3 número
  falado, muletas, negação curta, grafia pelo som; F4 SA-D1-102 e 23 sinais novos com fonte, AVC e convulsão em fala
  popular, catapora UPA; F5 um sinal por linha em 20 perguntas, 3 textos corrigidos, PARA_QUEM.md.
- CEGOS (número honesto): cego 3 críticas 12 ➜ 12, sobretriagem 13,8% ➜ 13,8%, só texto 91 ➜ 83; crise cega
  suicídio 53,3% ➜ 63,3%, intoxicação 61,1% ➜ 75,6%, violência 51,1% ➜ 61,1%, controles 85% ➜ 89%;
  V-Z 1 ➜ 1 (12,0% ➜ 12,3%); V-Y 5 ➜ 5 (24,0% ➜ 23,7%).
- AJUSTE: 10.000 críticas 37 ➜ 36, sobretriagem 17,1% ➜ 16,2%; V-H 28 ➜ 22; crise humana 100% nos 4 grupos;
  voz pesado 7 ➜ 2; MINIMIZADORA 52 ➜ 43. Testes 148 OK.
- Lição (D-040): crise só por texto chegou no limite; sprint 4 propõe pergunta de segurança fixa.
- Relatório: _revisao/RESULTADO_SPRINT_3.md.
