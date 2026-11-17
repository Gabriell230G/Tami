package br.com.fiap.tami.teste;

import br.com.fiap.tami.api.Json;
import br.com.fiap.tami.motor.*;
import br.com.fiap.tami.motor.conhecimento.BaseConhecimento;
import br.com.fiap.tami.servico.Textos;
import br.com.fiap.tami.util.Mapa;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ponte para os testes simulados: liga o motor de regras a um simulador de fora (Python) por linhas de JSON,
 * sem banco e sem HTTP. Cada linha que entra é um pedido; cada linha que sai é a resposta.
 *
 * <pre>
 *   {"op":"iniciar","idadeMeses":228,"gestante":false,"diabetes":false,"fatores":["homem"],"texto":"...","regiao":"Joelho direito"}
 *   {"op":"responder","sessao":1,"no":"LES_01","codigos":["esporte"]}
 * </pre>
 * Resposta: a próxima pergunta (id, texto, tipo, opções com os efeitos) ou o resultado (destino, blocos, motivos,
 * histórico com a origem de cada resposta).
 */
public final class Ponte {

    public static void main(String[] args) throws IOException {
        MotorTriagem motor = new MotorTriagem(CatalogoRoteiros.carregarDoClasspath(), r -> true, BaseConhecimento.carregarDoClasspath());
        Textos textos = new Textos();
        Map<Long, Sessao> sessoes = new HashMap<>();
        long seq = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        LocalDateTime agora = LocalDateTime.of(2026, 11, 17, 10, 0);   // terça de manhã: UBS aberta
        String linha;
        while ((linha = in.readLine()) != null) {
            if (linha.isBlank()) continue;
            Map<String, Object> resp;
            try {
                Map<String, Object> p = Json.objeto(linha);
                String op = String.valueOf(p.get("op"));
                Sessao s;
                long id;
                if (op.equals("iniciar")) {
                    Perfil perfil = new Perfil(((Number) p.get("idadeMeses")).intValue(), Boolean.TRUE.equals(p.get("gestante")), Boolean.TRUE.equals(p.get("diabetes")));
                    Set<String> fatores = new HashSet<>();
                    if (p.get("fatores") instanceof List<?> l) for (Object o : l) fatores.add(String.valueOf(o));
                    Object reg = p.get("regiao");
                    s = motor.iniciar(perfil, String.valueOf(p.get("texto")), null, agora, fatores, reg == null ? null : String.valueOf(reg));
                    id = ++seq;
                    sessoes.put(id, s);
                } else if (op.equals("responder")) {
                    id = ((Number) p.get("sessao")).longValue();
                    s = sessoes.get(id);
                    List<String> cods = new ArrayList<>();
                    for (Object o : (List<?>) p.get("codigos")) cods.add(String.valueOf(o));
                    motor.responder(s, String.valueOf(p.get("no")), cods);
                } else throw new IllegalArgumentException("op desconhecida: " + op);
                Optional<No> prox = motor.proxima(s);
                if (prox.isPresent()) {
                    No n = prox.get();
                    List<Map<String, Object>> ops = new ArrayList<>();
                    for (Opcao o : n.opcoes()) {
                        List<String> ef = new ArrayList<>();
                        for (Efeito e : o.efeitos()) ef.add(e.acao() + ":" + e.valor());
                        ops.add(Mapa.de("codigo", o.codigo(), "rotulo", o.rotulo(), "gravidade", o.gravidade(), "efeitos", ef));
                    }
                    resp = Mapa.de("sessao", id, "fim", false, "queixa", s.queixa(), "pergunta", Mapa.de("id", n.id(), "texto", textos.pergunta(n.id(), n.pergunta()),
                        "multipla", n.tipo() == No.Tipo.MULTI, "alarme", n.alarme(), "opcoes", ops, "preMarcados", motor.preMarcados(s, n.id())));
                } else {
                    Resultado r = motor.resultado(s);
                    sessoes.remove(id);
                    List<Map<String, Object>> hist = new ArrayList<>();
                    for (RespostaDada d : r.respostas())
                        hist.add(Mapa.de("no", d.noId(), "codigos", d.codigos(), "origem", d.origem().name()));
                    List<Map<String, Object>> blocos = new ArrayList<>();
                    for (String x : r.exige()) { String t = textos.texto(x); if (t != null) blocos.add(Mapa.de("codigo", x, "texto", t)); }
                    resp = Mapa.de("sessao", id, "fim", true, "queixa", s.queixa(), "destino", r.destino().name(),
                        "prioridade", r.prioridadeUbs() == null ? null : r.prioridadeUbs().name(), "motivos", r.motivos(),
                        "blocos", blocos, "crise", r.telaCrise(), "historico", hist);
                }
            } catch (Exception e) {
                resp = Mapa.de("erro", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            out.println(Json.escrever(resp));
        }
    }
}
