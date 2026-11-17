# Gera sql/02_regioes.sql: a árvore das regiões do corpo (código, mãe, nome, nível, palavras sem acento separadas por |)
R = [
 ("CORPO", None, "Corpo todo", 0, "corpo todo|corpo inteiro|o corpo todo|no corpo todo"),
 ("CABECA_PESCOCO", "CORPO", "Cabeça e pescoço", 1, ""),
 ("CABECA", "CABECA_PESCOCO", "Cabeça", 2, "cabeca|testa|cranio|moleira"),
 ("ROSTO", "CABECA_PESCOCO", "Rosto", 2, "rosto|cara|face|bochecha|queixo|mandibula|maxilar"),
 ("OLHOS", "CABECA_PESCOCO", "Olhos", 2, "olho|olhos|vista|visao|palpebra|enxergar|enxergando|enxergo"),
 ("OUVIDOS", "CABECA_PESCOCO", "Ouvidos", 2, "ouvido|ouvidos|orelha|orelhas|zumbido|escutar"),
 ("NARIZ", "CABECA_PESCOCO", "Nariz", 2, "nariz|narina|narinas"),
 ("BOCA", "CABECA_PESCOCO", "Boca e dentes", 2, "boca|dente|dentes|lingua|gengiva|labio|labios"),
 ("GARGANTA", "CABECA_PESCOCO", "Garganta", 2, "garganta|goela|engolir|amigdala|amigdalas|rouco|rouquidao"),
 ("PESCOCO", "CABECA_PESCOCO", "Pescoço", 2, "pescoco|nuca"),
 ("TRONCO_PEITO", "CORPO", "Peito e respiração", 1, ""),
 ("PEITO", "TRONCO_PEITO", "Peito", 2, "peito|coracao|torax"),
 ("COSTELAS", "TRONCO_PEITO", "Costelas", 2, "costela|costelas"),
 ("MAMAS", "TRONCO_PEITO", "Mamas", 2, "mama|mamas|seio|seios|mamilo"),
 ("RESPIRACAO", "TRONCO_PEITO", "Respiração", 2, "respirar|respiracao|falta de ar|pulmao|pulmoes|tosse|tossindo|catarro|folego|chiado"),
 ("BARRIGA_GRUPO", "CORPO", "Barriga", 1, ""),
 ("ESTOMAGO", "BARRIGA_GRUPO", "Estômago", 2, "estomago|boca do estomago|azia|queimacao"),
 ("BARRIGA", "BARRIGA_GRUPO", "Barriga", 2, "barriga|abdome|abdomen|umbigo|intestino|gases|vomito|vomitando|diarreia|fezes|coco"),
 ("PE_BARRIGA", "BARRIGA_GRUPO", "Pé da barriga", 2, "pe da barriga|baixo ventre|bexiga|xixi|urina|urinar|mijar"),
 ("VIRILHA", "BARRIGA_GRUPO", "Virilha", 2, "virilha|virilhas"),
 ("INTIMA", "BARRIGA_GRUPO", "Região íntima", 2, "vagina|penis|testiculo|testiculos|saco|menstruacao|corrimento|parte intima|partes intimas"),
 ("COSTAS_GRUPO", "CORPO", "Costas", 1, ""),
 ("COSTAS", "COSTAS_GRUPO", "Costas", 2, "costas|coluna|omoplata"),
 ("LOMBAR", "COSTAS_GRUPO", "Lombar", 2, "lombar|lombo|rim|rins|pe das costas"),
 ("QUADRIL", "COSTAS_GRUPO", "Quadril e bumbum", 2, "quadril|bumbum|nadega|nadegas|coccix|bacia"),
 ("BRACOS", "CORPO", "Braços e mãos", 1, ""),
 ("OMBRO", "BRACOS", "Ombro", 2, "ombro|ombros|clavicula"),
 ("BRACO", "BRACOS", "Braço", 2, "braco|bracos|biceps|triceps|antebraco|antebracos"),
 ("COTOVELO", "BRACOS", "Cotovelo", 2, "cotovelo|cotovelos"),
 ("PUNHO", "BRACOS", "Punho", 2, "punho|punhos|pulso|pulsos|munheca"),
 ("MAO", "BRACOS", "Mão e dedos", 2, "mao|maos|palma da mao|dedo da mao|dedos da mao|polegar|unha|unhas"),
 ("PERNAS", "CORPO", "Pernas e pés", 1, ""),
 ("COXA", "PERNAS", "Coxa", 2, "coxa|coxas"),
 ("JOELHO", "PERNAS", "Joelho", 2, "joelho|joelhos|patela"),
 ("PERNA", "PERNAS", "Perna", 2, "perna|pernas|canela|canelas|panturrilha|batata da perna"),
 ("TORNOZELO", "PERNAS", "Tornozelo", 2, "tornozelo|tornozelos"),
 ("PE", "PERNAS", "Pé", 2, "pe|pes|calcanhar|dedo do pe|dedos do pe|dedao|sola do pe|planta do pe"),
 ("PELE", "CORPO", "Pele", 1, "pele|mancha|manchas|coceira|cocando|ferida|feridas|bolha|bolhas|picada|queimadura|queimei|corte|cortei|hematoma|caroco|inchaco na pele"),
]
with open("/home/claude/tami-gs/sql/02_regioes.sql", "w", encoding="utf-8") as f:
    f.write("-- Árvore das regiões do corpo (dados iniciais). Gerado por gen/regioes.py.\n")
    f.write("-- Nível 0: corpo todo · nível 1: grandes regiões · nível 2: partes. As palavras ficam sem acento.\n\n")
    for c, p, n, nv, pal in R:
        pai = f"'{p}'" if p else "NULL"
        f.write(f"INSERT INTO T_TAMI_REGIAO (CD_REGIAO, CD_REGIAO_PAI, NM_REGIAO, NR_NIVEL, DS_PALAVRAS) VALUES ('{c}', {pai}, '{n}', {nv}, '{pal}');\n")
    f.write("COMMIT;\n")
print(len(R))
