package br.com.fiap.tami.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON sem biblioteca: escreve Map, List, String, número, booleano e null; e lê o corpo das requisições. */
public final class Json {
    private Json() {}

    // ---------------- escrever ----------------

    public static String escrever(Object v) {
        StringBuilder sb = new StringBuilder();
        escrever(sb, v);
        return sb.toString();
    }

    private static void escrever(StringBuilder sb, Object v) {
        if (v == null) sb.append("null");
        else if (v instanceof String s) texto(sb, s);
        else if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short) sb.append(v);
        else if (v instanceof Number n) {
            double d = n.doubleValue();
            sb.append(Double.isFinite(d) ? (d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : String.valueOf(d)) : "null");
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primeiro = true;
            for (var e : m.entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escrever(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primeiro = true;
            for (Object x : c) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(sb, x);
            }
            sb.append(']');
        } else if (v instanceof Object[] arr) escrever(sb, List.of(arr));
        else if (v instanceof Enum<?> en) texto(sb, en.name());
        else texto(sb, v.toString());
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");   // evita fechar um <script> por acidente
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------------- ler ----------------

    public static Object ler(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<String, Object>();
        Leitor l = new Leitor(json);
        Object v = l.valor();
        l.espacos();
        if (l.i < json.length()) throw new IllegalArgumentException("JSON com sobra no fim");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objeto(String json) {
        Object v = ler(json);
        if (!(v instanceof Map)) throw new IllegalArgumentException("Esperava um objeto JSON");
        return (Map<String, Object>) v;
    }

    private static final class Leitor {
        final String s;
        int i;

        Leitor(String s) { this.s = s; }

        void espacos() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        Object valor() {
            espacos();
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return texto();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numero();
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacos();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacos();
                String k = texto();
                espacos();
                if (s.charAt(i++) != ':') throw new IllegalArgumentException("Faltou ':' no JSON");
                m.put(k, valor());
                espacos();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("Faltou ',' no JSON");
            }
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<>();
            i++;
            espacos();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor());
                espacos();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("Faltou ',' na lista JSON");
            }
        }

        String texto() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("Esperava texto no JSON");
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            }
        }

        Number numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(ini, i);
            if (n.isEmpty()) throw new IllegalArgumentException("Valor JSON inválido perto da posição " + ini);
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
            return Long.parseLong(n);
        }
    }
}
