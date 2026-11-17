package br.com.fiap.tami.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estatística usada nas análises da Tami, escrita do zero e comentada:
 * proporção com intervalo de confiança de 95% (Wilson), medidas descritivas, kappa de Cohen,
 * teste qui-quadrado de independência (com valor-p) e correlação de Pearson.
 */
public final class Estatistica {
    private Estatistica() {}

    /** z para 95% de confiança. */
    public static final double Z95 = 1.959963984540054;

    /**
     * Proporção x/n com intervalo de Wilson. Ele funciona bem mesmo com proporções perto de 0 ou de 1
     * (o intervalo "normal" p ± 1,96·√(p(1−p)/n) erra justamente nesses casos, que são os que mais importam aqui).
     */
    public static Map<String, Object> proporcao(long x, long n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("n", n);
        if (n == 0) { m.put("p", null); m.put("ic95", null); return m; }
        double p = (double) x / n, z2 = Z95 * Z95;
        double centro = (p + z2 / (2 * n)) / (1 + z2 / n);
        double margem = Z95 * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / (1 + z2 / n);
        m.put("p", arred(p, 5));
        m.put("ic95", List.of(arred(Math.max(0, centro - margem), 5), arred(Math.min(1, centro + margem), 5)));
        return m;
    }

    /** Média, mediana, desvio padrão (amostral), quartis, mínimo e máximo. */
    public static Map<String, Object> descrever(List<Double> valores) {
        Map<String, Object> m = new LinkedHashMap<>();
        int n = valores.size();
        m.put("n", n);
        if (n == 0) return m;
        List<Double> v = new ArrayList<>(valores);
        Collections.sort(v);
        double soma = 0;
        for (double x : v) soma += x;
        double media = soma / n, sq = 0;
        for (double x : v) sq += (x - media) * (x - media);
        double dp = n > 1 ? Math.sqrt(sq / (n - 1)) : 0;
        m.put("media", arred(media, 2));
        m.put("mediana", arred(quantil(v, 0.5), 2));
        m.put("desvioPadrao", arred(dp, 2));
        m.put("q1", arred(quantil(v, 0.25), 2));
        m.put("q3", arred(quantil(v, 0.75), 2));
        m.put("min", arred(v.get(0), 2));
        m.put("max", arred(v.get(n - 1), 2));
        m.put("ic95Media", n > 1 ? List.of(arred(media - Z95 * dp / Math.sqrt(n), 2), arred(media + Z95 * dp / Math.sqrt(n), 2)) : null);
        return m;
    }

    /** Quantil com interpolação linear (lista já ordenada). */
    public static double quantil(List<Double> ordenada, double q) {
        if (ordenada.isEmpty()) return Double.NaN;
        double pos = q * (ordenada.size() - 1);
        int i = (int) Math.floor(pos);
        double f = pos - i;
        return i + 1 < ordenada.size() ? ordenada.get(i) * (1 - f) + ordenada.get(i + 1) * f : ordenada.get(i);
    }

    /** Kappa de Cohen: concordância entre o esperado e o obtido, descontando o acaso (1 = perfeita, 0 = igual ao acaso). */
    public static double kappa(long[][] m) {
        int k = m.length;
        double n = 0, acordo = 0;
        double[] lin = new double[k], col = new double[k];
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++) { n += m[i][j]; lin[i] += m[i][j]; col[j] += m[i][j]; if (i == j) acordo += m[i][j]; }
        if (n == 0) return Double.NaN;
        double po = acordo / n, pe = 0;
        for (int i = 0; i < k; i++) pe += (lin[i] / n) * (col[i] / n);
        return arred((po - pe) / (1 - pe), 4);
    }

    /** Kappa com pesos lineares: errar por um nível (UPA no lugar de SAMU) pesa menos que errar por três. */
    public static double kappaPonderado(long[][] m) {
        int k = m.length;
        double n = 0;
        double[] lin = new double[k], col = new double[k];
        for (int i = 0; i < k; i++) for (int j = 0; j < k; j++) { n += m[i][j]; lin[i] += m[i][j]; col[j] += m[i][j]; }
        if (n == 0) return Double.NaN;
        double obs = 0, esp = 0;
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++) {
                double w = Math.abs(i - j) / (double) (k - 1);
                obs += w * m[i][j] / n;
                esp += w * (lin[i] / n) * (col[j] / n);
            }
        return arred(1 - obs / esp, 4);
    }

    /**
     * Teste qui-quadrado de independência numa tabela de contingência (linhas × colunas).
     * H0: as duas variáveis são independentes. Devolve χ², graus de liberdade, valor-p e V de Cramér.
     */
    public static Map<String, Object> quiQuadrado(long[][] t) {
        int r = t.length, c = r == 0 ? 0 : t[0].length;
        double n = 0;
        double[] lin = new double[r], col = new double[c];
        for (int i = 0; i < r; i++) for (int j = 0; j < c; j++) { n += t[i][j]; lin[i] += t[i][j]; col[j] += t[i][j]; }
        Map<String, Object> m = new LinkedHashMap<>();
        int linhasUsadas = 0, colunasUsadas = 0;
        for (double x : lin) if (x > 0) linhasUsadas++;
        for (double x : col) if (x > 0) colunasUsadas++;
        if (n == 0 || linhasUsadas < 2 || colunasUsadas < 2) { m.put("valido", false); return m; }
        double chi = 0;
        int poucos = 0;
        for (int i = 0; i < r; i++) {
            if (lin[i] == 0) continue;
            for (int j = 0; j < c; j++) {
                if (col[j] == 0) continue;
                double e = lin[i] * col[j] / n;
                if (e < 5) poucos++;
                chi += (t[i][j] - e) * (t[i][j] - e) / e;
            }
        }
        int gl = (linhasUsadas - 1) * (colunasUsadas - 1);
        m.put("valido", true);
        m.put("quiQuadrado", arred(chi, 3));
        m.put("grausLiberdade", gl);
        m.put("valorP", arred(gamaQ(gl / 2.0, chi / 2.0), 5));
        m.put("vCramer", arred(Math.sqrt(chi / (n * Math.max(1, Math.min(linhasUsadas, colunasUsadas) - 1))), 4));
        m.put("celulasComEsperadoMenorQue5", poucos);
        m.put("n", (long) n);
        return m;
    }

    /** Correlação de Pearson entre duas listas do mesmo tamanho. */
    public static double pearson(List<Double> x, List<Double> y) {
        int n = Math.min(x.size(), y.size());
        if (n < 3) return Double.NaN;
        double mx = 0, my = 0;
        for (int i = 0; i < n; i++) { mx += x.get(i); my += y.get(i); }
        mx /= n; my /= n;
        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - mx, dy = y.get(i) - my;
            sxy += dx * dy; sxx += dx * dx; syy += dy * dy;
        }
        return sxx == 0 || syy == 0 ? Double.NaN : arred(sxy / Math.sqrt(sxx * syy), 4);
    }

    // Função gama incompleta regularizada superior Q(a, x) = P(χ² > 2x) para a = gl/2 (Numerical Recipes, 6.2).
    static double gamaQ(double a, double x) {
        if (x <= 0) return 1;
        if (x < a + 1) return 1 - serieGama(a, x);
        return fracaoGama(a, x);
    }

    private static double serieGama(double a, double x) {
        double ap = a, soma = 1 / a, del = soma;
        for (int n = 0; n < 500; n++) {
            ap++;
            del *= x / ap;
            soma += del;
            if (Math.abs(del) < Math.abs(soma) * 1e-14) break;
        }
        return soma * Math.exp(-x + a * Math.log(x) - lnGama(a));
    }

    private static double fracaoGama(double a, double x) {
        double b = x + 1 - a, c = 1 / 1e-300, d = 1 / b, h = d;
        for (int i = 1; i < 500; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b;
            if (Math.abs(d) < 1e-300) d = 1e-300;
            c = b + an / c;
            if (Math.abs(c) < 1e-300) c = 1e-300;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < 1e-14) break;
        }
        return Math.exp(-x + a * Math.log(x) - lnGama(a)) * h;
    }

    private static double lnGama(double xx) {
        double[] cof = {76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5};
        double x = xx, y = xx, tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        double ser = 1.000000000190015;
        for (double c : cof) ser += c / ++y;
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }

    public static double arred(double v, int casas) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return v;
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }
}
