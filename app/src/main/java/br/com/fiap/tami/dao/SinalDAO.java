package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.SinalCatalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A base de conhecimento no banco: sinais, expressões populares e o vínculo com as regiões do corpo. */
public class SinalDAO extends BaseDAO {

    public SinalDAO(ConexaoFactory fabrica) { super(fabrica); }

    public int contar() { return contar("SELECT COUNT(*) FROM T_TAMI_SINAL"); }

    public int contarExpressoes() { return contar("SELECT COUNT(*) FROM T_TAMI_EXPRESSAO"); }

    private int contar(String sql) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar a base de conhecimento", e);
        }
    }

    /** Grava sinais, expressões e regiões numa transação só, em lotes (rápido mesmo no Oracle remoto da FIAP). */
    public void inserirTudo(Collection<SinalCatalogo> sinais, Map<String, Set<String>> regioesPorSinal) {
        String sSinal = "INSERT INTO T_TAMI_SINAL (CD_SINAL, DS_TIPO, DS_POPULAR, DS_TERMO, DS_DESTINO, DS_VALE_PARA, DS_FONTE, DS_FONTE_URL, ST_REVISAR) VALUES (?,?,?,?,?,?,?,?,?)";
        String sExpr = "INSERT INTO T_TAMI_EXPRESSAO (CD_SINAL, DS_EXPRESSAO) VALUES (?,?)";
        String sReg = "INSERT INTO T_TAMI_SINAL_REGIAO (CD_SINAL, CD_REGIAO) VALUES (?,?)";
        try (Connection c = fabrica.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement(sSinal); PreparedStatement p2 = c.prepareStatement(sExpr);
                 PreparedStatement p3 = c.prepareStatement(sReg)) {
                int n = 0;
                for (SinalCatalogo s : sinais) {
                    p1.setString(1, s.codigo()); p1.setString(2, s.tipo()); p1.setString(3, corta(s.popular(), 600));
                    p1.setString(4, corta(s.termo(), 300)); p1.setString(5, s.destino()); p1.setString(6, corta(s.valePara(), 300));
                    p1.setString(7, corta(s.fonte(), 200)); p1.setString(8, corta(s.fonteUrl(), 400)); p1.setString(9, sn(s.revisar()));
                    p1.addBatch();
                    for (String e : s.expressoes()) {
                        p2.setString(1, s.codigo()); p2.setString(2, corta(e, 300)); p2.addBatch();
                        if (++n % 1000 == 0) { p1.executeBatch(); p2.executeBatch(); }
                    }
                }
                p1.executeBatch();
                p2.executeBatch();
                for (var e : regioesPorSinal.entrySet())
                    for (String r : e.getValue()) { p3.setString(1, e.getKey()); p3.setString(2, r); p3.addBatch(); }
                p3.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao carregar a base de conhecimento no banco", e);
        }
    }

    public void apagarTudo() {
        try (Connection c = fabrica.abrir(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM T_TAMI_SINAL_REGIAO");
            st.executeUpdate("DELETE FROM T_TAMI_EXPRESSAO");
            st.executeUpdate("DELETE FROM T_TAMI_SINAL");
        } catch (SQLException e) {
            throw new DaoException("Erro ao limpar a base de conhecimento", e);
        }
    }

    /** O filtro da árvore: quantos sinais e expressões valem para estas regiões. */
    public record Filtro(int sinais, int expressoes, Map<String, Integer> porDestino) {}

    public Filtro filtrar(List<String> regioes) {
        if (regioes.isEmpty()) return new Filtro(contar(), contarExpressoes(), Map.of());
        String in = String.join(",", regioes.stream().map(r -> "?").toList());
        String sql = "SELECT S.DS_DESTINO, COUNT(DISTINCT S.CD_SINAL) AS SINAIS, COUNT(E.ID_EXPRESSAO) AS EXPRESSOES "
            + "FROM T_TAMI_SINAL S LEFT JOIN T_TAMI_EXPRESSAO E ON E.CD_SINAL = S.CD_SINAL "
            + "WHERE S.CD_SINAL IN (SELECT CD_SINAL FROM T_TAMI_SINAL_REGIAO WHERE CD_REGIAO IN (" + in + ")) "
            + "GROUP BY S.DS_DESTINO";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < regioes.size(); i++) ps.setString(i + 1, regioes.get(i));
            int sinais = 0, expr = 0;
            Map<String, Integer> porDestino = new java.util.LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porDestino.put(rs.getString(1), rs.getInt(2));
                    sinais += rs.getInt(2);
                    expr += rs.getInt(3);
                }
            }
            return new Filtro(sinais, expr, porDestino);
        } catch (SQLException e) {
            throw new DaoException("Erro ao filtrar os sinais pela região", e);
        }
    }

    /** Alguns sinais da região, do mais urgente para o mais leve (para mostrar o que entrou no filtro). */
    public List<String[]> exemplos(List<String> regioes, int limite) {
        if (regioes.isEmpty()) return List.of();
        String in = String.join(",", regioes.stream().map(r -> "?").toList());
        String sql = "SELECT S.CD_SINAL, S.DS_POPULAR, S.DS_DESTINO FROM T_TAMI_SINAL S "
            + "WHERE S.CD_SINAL IN (SELECT CD_SINAL FROM T_TAMI_SINAL_REGIAO WHERE CD_REGIAO IN (" + in + ")) "
            + "ORDER BY CASE S.DS_DESTINO WHEN 'SAMU_192' THEN 1 WHEN 'UPA_HOJE' THEN 2 WHEN 'UBS_EM_BREVE' THEN 3 ELSE 4 END, S.CD_SINAL "
            + "FETCH FIRST " + Math.max(1, Math.min(limite, 50)) + " ROWS ONLY";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < regioes.size(); i++) ps.setString(i + 1, regioes.get(i));
            List<String[]> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao listar os sinais da região", e);
        }
    }

    /** Quantos sinais cada região tem ligados a ela (para o painel). */
    public Map<String, Integer> sinaisPorRegiao() {
        String sql = "SELECT CD_REGIAO, COUNT(*) FROM T_TAMI_SINAL_REGIAO GROUP BY CD_REGIAO";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            Map<String, Integer> out = new java.util.HashMap<>();
            while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar sinais por região", e);
        }
    }
}
