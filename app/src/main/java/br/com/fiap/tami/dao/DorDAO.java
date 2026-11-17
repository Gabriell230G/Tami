package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.RegistroDor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** CRUD da tabela T_TAMI_DOR ("Onde dói?" e a escala de 0 a 10). */
public class DorDAO extends BaseDAO {

    public DorDAO(ConexaoFactory fabrica) { super(fabrica); }

    public RegistroDor inserir(RegistroDor d) {
        String sql = "INSERT INTO T_TAMI_DOR (ID_CONVERSA, CD_REGIAO, DS_LADO, NR_NIVEL, DS_TIPO, DT_REGISTRO) VALUES (?,?,?,?,?,?)";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = prepararInsert(c, sql, "ID_DOR")) {
            LocalDateTime agora = LocalDateTime.now();
            ps.setLong(1, d.idConversa());
            ps.setString(2, d.codigoRegiao());
            ps.setString(3, d.lado());
            ps.setInt(4, d.nivel());
            ps.setString(5, corta(d.tipo(), 30));
            ps.setTimestamp(6, ts(agora));
            ps.executeUpdate();
            return new RegistroDor(idGerado(ps), d.idConversa(), d.codigoRegiao(), d.lado(), d.nivel(), d.tipo(), agora);
        } catch (SQLException e) {
            throw new DaoException("Erro ao registrar a dor", e);
        }
    }

    /** UPDATE: a pessoa mudou o nível da dor. */
    public boolean atualizarNivel(long idDor, int nivel) {
        if (nivel < 0 || nivel > 10) throw new IllegalArgumentException("O nível da dor vai de 0 a 10.");
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("UPDATE T_TAMI_DOR SET NR_NIVEL = ? WHERE ID_DOR = ?")) {
            ps.setInt(1, nivel);
            ps.setLong(2, idDor);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao atualizar a dor", e);
        }
    }

    public List<RegistroDor> listarPorConversa(long idConversa) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("SELECT * FROM T_TAMI_DOR WHERE ID_CONVERSA = ? ORDER BY ID_DOR")) {
            ps.setLong(1, idConversa);
            List<RegistroDor> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new RegistroDor(rs.getLong("ID_DOR"), idConversa, rs.getString("CD_REGIAO"), rs.getString("DS_LADO"),
                        rs.getInt("NR_NIVEL"), rs.getString("DS_TIPO"), data(rs, "DT_REGISTRO")));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler a dor", e);
        }
    }
}
