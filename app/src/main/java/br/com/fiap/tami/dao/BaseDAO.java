package br.com.fiap.tami.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/** O que todo DAO usa: a fábrica de conexões e conversões entre Java e as colunas do Oracle. */
public abstract class BaseDAO {

    protected final ConexaoFactory fabrica;

    protected BaseDAO(ConexaoFactory fabrica) { this.fabrica = fabrica; }

    protected static String sn(boolean b) { return b ? "S" : "N"; }

    protected static boolean sn(String s) { return "S".equals(s); }

    protected static Timestamp ts(LocalDateTime d) { return d == null ? null : Timestamp.valueOf(d); }

    protected static LocalDateTime data(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static Long longOuNulo(ResultSet rs, String coluna) throws SQLException {
        long v = rs.getLong(coluna);
        return rs.wasNull() ? null : v;
    }

    protected static Integer intOuNulo(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    protected static void longOuNulo(PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) ps.setNull(i, Types.NUMERIC); else ps.setLong(i, v);
    }

    protected static void intOuNulo(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.NUMERIC); else ps.setInt(i, v);
    }

    /**
     * Prepara um INSERT que devolve o id gerado. O Oracle precisa do nome da coluna; o PostgreSQL guarda os nomes
     * em minúsculas, então o nome vai em minúsculas para ele.
     */
    protected java.sql.PreparedStatement prepararInsert(java.sql.Connection c, String sql, String colunaId) throws SQLException {
        return c.prepareStatement(sql, new String[]{fabrica.postgres() ? colunaId.toLowerCase() : colunaId});
    }

    /** Lê o id gerado pela coluna IDENTITY depois de um INSERT. */
    protected static long idGerado(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("O banco não devolveu o id gerado.");
            return rs.getLong(1);
        }
    }

    /** Corta texto grande antes de gravar num VARCHAR2. */
    protected static String corta(String s, int max) {
        return s == null ? null : s.length() <= max ? s : s.substring(0, max);
    }
}
