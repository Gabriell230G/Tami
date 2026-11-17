package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.Pessoa;
import br.com.fiap.tami.modelo.Relacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD da tabela T_TAMI_PESSOA ("Pessoas que eu cuido"). */
public class PessoaDAO extends BaseDAO {

    public PessoaDAO(ConexaoFactory fabrica) { super(fabrica); }

    public Pessoa inserir(Pessoa p) {
        String sql = "INSERT INTO T_TAMI_PESSOA (ID_USUARIO, NM_APELIDO, DS_RELACAO, NR_IDADE_MESES, ST_GESTANTE, ST_DIABETES, DT_CADASTRO) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = prepararInsert(c, sql, "ID_PESSOA")) {
            LocalDateTime agora = LocalDateTime.now();
            ps.setLong(1, p.getIdUsuario());
            ps.setString(2, p.getApelido());
            ps.setString(3, p.getRelacao().name());
            ps.setInt(4, p.getIdadeMeses());
            ps.setString(5, sn(p.isGestante()));
            ps.setString(6, sn(p.isDiabetes()));
            ps.setTimestamp(7, ts(agora));
            ps.executeUpdate();
            p.setId(idGerado(ps));
            p.setDataCadastro(agora);
            return p;
        } catch (SQLException e) {
            throw new DaoException("Erro ao cadastrar a pessoa", e);
        }
    }

    public List<Pessoa> listarPorUsuario(long idUsuario) {
        String sql = "SELECT * FROM T_TAMI_PESSOA WHERE ID_USUARIO = ? ORDER BY CASE DS_RELACAO WHEN 'EU' THEN 0 ELSE 1 END, NM_APELIDO";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            List<Pessoa> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(ler(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao listar as pessoas", e);
        }
    }

    public Optional<Pessoa> buscarPorId(long id) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("SELECT * FROM T_TAMI_PESSOA WHERE ID_PESSOA = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(ler(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar a pessoa", e);
        }
    }

    /** UPDATE: idade, gravidez ou diabetes mudaram. */
    public boolean atualizar(Pessoa p) {
        String sql = "UPDATE T_TAMI_PESSOA SET NM_APELIDO = ?, DS_RELACAO = ?, NR_IDADE_MESES = ?, ST_GESTANTE = ?, ST_DIABETES = ?, DT_ATUALIZACAO = ? WHERE ID_PESSOA = ? AND ID_USUARIO = ?";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getApelido());
            ps.setString(2, p.getRelacao().name());
            ps.setInt(3, p.getIdadeMeses());
            ps.setString(4, sn(p.isGestante()));
            ps.setString(5, sn(p.isDiabetes()));
            ps.setTimestamp(6, ts(LocalDateTime.now()));
            ps.setLong(7, p.getId());
            ps.setLong(8, p.getIdUsuario());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao atualizar a pessoa", e);
        }
    }

    public boolean apagar(long id, long idUsuario) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM T_TAMI_PESSOA WHERE ID_PESSOA = ? AND ID_USUARIO = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, idUsuario);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao apagar a pessoa", e);
        }
    }

    private static Pessoa ler(ResultSet rs) throws SQLException {
        Pessoa p = new Pessoa();
        p.setId(rs.getLong("ID_PESSOA"));
        p.setIdUsuario(rs.getLong("ID_USUARIO"));
        p.setApelido(rs.getString("NM_APELIDO"));
        p.setRelacao(Relacao.valueOf(rs.getString("DS_RELACAO")));
        p.setIdadeMeses(rs.getInt("NR_IDADE_MESES"));
        p.setGestante(sn(rs.getString("ST_GESTANTE")));
        p.setDiabetes(sn(rs.getString("ST_DIABETES")));
        p.setDataCadastro(data(rs, "DT_CADASTRO"));
        p.setDataAtualizacao(data(rs, "DT_ATUALIZACAO"));
        return p;
    }
}
