package br.com.fiap.tami.servico;

import br.com.fiap.tami.api.Json;
import br.com.fiap.tami.util.Mapa;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Onde fica a mais perto?": acha UPAs (ou pronto atendimentos) e UBS reais perto da pessoa.
 *
 * Fonte: API de Dados Abertos do Ministério da Saúde (CNES), sem chave:
 * https://apidadosabertos.saude.gov.br/cnes/estabelecimentos
 *
 * Privacidade: a latitude e a longitude da pessoa só servem para escolher o município e calcular
 * a distância. Não são gravadas no banco nem enviadas ao Ministério (ele só recebe o código do município).
 */
public final class LocalService {

    private static final String API = System.getProperty("tami.cnes.url", "https://apidadosabertos.saude.gov.br/cnes/estabelecimentos");
    private static final int POR_PAGINA = 20;           // a API não aceita mais de 20 por página
    private static final int PAGINAS_EM_PARALELO = 6;
    private static final long VALIDADE_CACHE_MS = 12L * 60 * 60 * 1000;
    private static final double RAIO_MUNICIPIOS_KM = 25; // também olha os municípios vizinhos cuja sede está perto

    /** Tipos de unidade do CNES. UPA: 73 pronto atendimento, 20 pronto-socorro geral. UBS: 2 centro de saúde/UBS, 1 posto. */
    private static final Map<String, int[]> TIPOS = Map.of("UPA", new int[]{73, 20}, "UBS", new int[]{2, 1});

    private record Municipio(String codigo, String nome, double lat, double lon) {}
    private record Unidade(String nome, String rotulo, String endereco, String telefone, String turno, double lat, double lon) {}
    private record Cache(long quando, List<Unidade> unidades) {}

    private final List<Municipio> municipios = new ArrayList<>();
    private final Map<String, Cache> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public LocalService() {
        try (InputStream in = LocalService.class.getResourceAsStream("/conhecimento/municipios.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.startsWith("#") || linha.startsWith("codigo")) continue;
                String[] c = linha.split(";");
                municipios.add(new Municipio(c[0], c[1], Double.parseDouble(c[2]), Double.parseDouble(c[3])));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não consegui ler conhecimento/municipios.csv", e);
        }
    }

    public int municipiosCarregados() { return municipios.size(); }

    /** As 3 unidades mais perto, do tipo pedido ("UPA" ou "UBS"). */
    public Map<String, Object> perto(double lat, double lon, String tipo) {
        if (!TIPOS.containsKey(tipo)) throw new IllegalArgumentException("Tipo deve ser UPA ou UBS.");
        if (lat < -34 || lat > 6 || lon < -74.5 || lon > -32) throw new IllegalArgumentException("A busca funciona só no Brasil.");

        List<Municipio> perto = municipios.stream().sorted(Comparator.comparingDouble(m -> km(lat, lon, m.lat, m.lon))).limit(3).toList();
        List<Municipio> consultar = new ArrayList<>();
        consultar.add(perto.get(0));
        for (int i = 1; i < perto.size(); i++) if (km(lat, lon, perto.get(i).lat, perto.get(i).lon) <= RAIO_MUNICIPIOS_KM) consultar.add(perto.get(i));

        List<Unidade> todas = new ArrayList<>();
        boolean falhou = false;
        for (Municipio m : consultar) {
            try { todas.addAll(unidades(m.codigo, tipo)); } catch (RuntimeException e) { falhou = true; }
        }
        List<Map<String, Object>> lista = todas.stream()
            .sorted(Comparator.comparingDouble(u -> km(lat, lon, u.lat, u.lon)))
            .limit(3)
            .map(u -> Mapa.de("nome", u.nome, "rotulo", u.rotulo, "endereco", u.endereco, "telefone", u.telefone, "turno", u.turno,
                "distanciaKm", Math.round(km(lat, lon, u.lat, u.lon) * 10) / 10.0,
                "comoChegar", String.format(Locale.ROOT, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", u.lat, u.lon)))
            .toList();
        return Mapa.de("tipo", tipo, "municipio", perto.get(0).nome, "unidades", lista, "fonte", "CNES, Ministério da Saúde",
            "aviso", falhou && lista.isEmpty() ? "Não consegui falar com o cadastro do Ministério da Saúde agora." : null,
            "buscaNoMapa", "https://www.google.com/maps/search/?api=1&query=" + ("UPA".equals(tipo) ? "UPA" : "UBS+posto+de+saude"));
    }

    /** Todas as unidades de um tipo num município, com cache de 12 horas. */
    private List<Unidade> unidades(String municipio, String tipo) {
        String chave = municipio + "|" + tipo;
        Cache c = cache.get(chave);
        if (c != null && System.currentTimeMillis() - c.quando < VALIDADE_CACHE_MS) return c.unidades;
        List<Unidade> out = new ArrayList<>();
        for (int t : TIPOS.get(tipo)) for (Map<String, Object> e : todasAsPaginas(municipio, t)) {
            Unidade u = converter(e, tipo);
            if (u != null) out.add(u);
        }
        cache.put(chave, new Cache(System.currentTimeMillis(), out));
        return out;
    }

    /** A API devolve no máximo 20 por página e "offset" é o número da página. Busca de 6 em 6 até vir uma página incompleta. */
    private List<Map<String, Object>> todasAsPaginas(String municipio, int tipo) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int inicio = 0; inicio < 60; inicio += PAGINAS_EM_PARALELO) {
            List<CompletableFuture<List<Map<String, Object>>>> lote = new ArrayList<>();
            for (int p = inicio; p < inicio + PAGINAS_EM_PARALELO; p++) lote.add(pagina(municipio, tipo, p));
            boolean acabou = false;
            for (CompletableFuture<List<Map<String, Object>>> f : lote) {
                List<Map<String, Object>> l = f.join();
                out.addAll(l);
                if (l.size() < POR_PAGINA) acabou = true;
            }
            if (acabou) break;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<Map<String, Object>>> pagina(String municipio, int tipo, int pagina) {
        URI uri = URI.create(API + "?status=1&limit=" + POR_PAGINA + "&offset=" + pagina + "&codigo_municipio=" + municipio + "&codigo_tipo_unidade=" + tipo);
        HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("Accept", "application/json").GET().build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> {
            if (r.statusCode() != 200) throw new IllegalStateException("CNES respondeu " + r.statusCode());
            Object corpo = Json.ler(r.body());
            Object lista = corpo instanceof Map<?, ?> m ? m.get("estabelecimentos") : corpo;
            return lista instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
        });
    }

    private static Unidade converter(Map<String, Object> e, String tipo) {
        Object la = e.get("latitude_estabelecimento_decimo_grau"), lo = e.get("longitude_estabelecimento_decimo_grau");
        if (!(la instanceof Number) || !(lo instanceof Number)) return null;
        String nome = texto(e.get("nome_fantasia"));
        if (nome.isBlank()) nome = texto(e.get("nome_razao_social"));
        boolean sus = "SIM".equals(e.get("estabelecimento_faz_atendimento_ambulatorial_sus"));
        boolean upaNoNome = nome.matches("(?i).*\\bUPA\\b.*");
        if (!sus && !upaNoNome) return null;   // fica só o que atende pelo SUS (algumas UPAs vêm sem a marcação)
        String rotulo = "UBS".equals(tipo) ? "UBS" : upaNoNome ? "UPA" : nome.matches("(?i).*\\bAMA\\b.*") ? "AMA" : "Pronto atendimento";
        String endereco = titulo(texto(e.get("endereco_estabelecimento")))
            + (texto(e.get("numero_estabelecimento")).isBlank() ? "" : ", " + texto(e.get("numero_estabelecimento")))
            + (texto(e.get("bairro_estabelecimento")).isBlank() ? "" : " · " + titulo(texto(e.get("bairro_estabelecimento"))));
        String turno = texto(e.get("descricao_turno_atendimento")).toUpperCase(Locale.ROOT).contains("24 HORAS") ? "Aberto 24 horas" : "Confirme o horário";
        return new Unidade(titulo(nome), rotulo, endereco, texto(e.get("numero_telefone_estabelecimento")), turno,
            ((Number) la).doubleValue(), ((Number) lo).doubleValue());
    }

    private static String texto(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private static final Map<String, String> SIGLAS = new HashMap<>(Map.of("upa", "UPA", "ubs", "UBS", "ama", "AMA", "ps", "PS", "pa", "PA", "usf", "USF", "esf", "ESF", "caps", "CAPS"));

    /** "UPA MOOCA III DOM PAULO" ➜ "UPA Mooca III Dom Paulo". */
    static String titulo(String s) {
        StringBuilder b = new StringBuilder();
        for (String p : s.toLowerCase(new Locale("pt", "BR")).split("\\s+")) {
            if (p.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            if (SIGLAS.containsKey(p)) b.append(SIGLAS.get(p));
            else if (p.matches("[ivx]+")) b.append(p.toUpperCase(Locale.ROOT));
            else if (p.matches("d[aeo]s?|e")) b.append(p);
            else b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return b.toString();
    }

    /** Distância em linha reta (fórmula de Haversine). */
    static double km(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
