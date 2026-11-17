package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/** CRUD da tabela T_TAMI_USUARIO. */
public class UsuarioDAO extends BaseDAO {

    public UsuarioDAO(ConexaoFactory fabrica) { super(fabrica); }

    public Usuario inserir(Usuario u) {
        String sql = "INSERT INTO T_TAMI_USUARIO (NM_USUARIO, DS_CONTATO, ST_CONSENT_SAUDE, DT_CONSENTIMENTO, DT_CADASTRO) VALUES (?,?,?,?,?)";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = prepararInsert(c, sql, "ID_USUARIO")) {
            LocalDateTime agora = LocalDateTime.now();
            ps.setString(1, u.getNome());
            ps.setString(2, u.getContato());
            ps.setString(3, sn(u.isConsentimentoSaude()));
            ps.setTimestamp(4, u.isConsentimentoSaude() ? ts(agora) : null);
            ps.setTimestamp(5, ts(agora));
            ps.executeUpdate();
            u.setId(idGerado(ps));
            u.setDataCadastro(agora);
            if (u.isConsentimentoSaude()) u.setDataConsentimento(agora);
            return u;
        } catch (SQLException e) {
            throw new DaoException("Erro ao cadastrar o usuário", e);
        }
    }

    public Optional<Usuario> buscarPorId(long id) {
        return buscar("SELECT * FROM T_TAMI_USUARIO WHERE ID_USUARIO = ?", ps -> ps.setLong(1, id));
    }

    public Optional<Usuario> buscarPorContato(String contato) {
        return buscar("SELECT * FROM T_TAMI_USUARIO WHERE DS_CONTATO = ?", ps -> ps.setString(1, contato));
    }

    private interface Parametros { void aplicar(PreparedStatement ps) throws SQLException; }

    private Optional<Usuario> buscar(String sql, Parametros p) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            p.aplicar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Usuario u = new Usuario();
                u.setId(rs.getLong("ID_USUARIO"));
                u.setNome(rs.getString("NM_USUARIO"));
                u.setContato(rs.getString("DS_CONTATO"));
                u.setConsentimentoSaude(sn(rs.getString("ST_CONSENT_SAUDE")));
                u.setDataConsentimento(data(rs, "DT_CONSENTIMENTO"));
                u.setDataCadastro(data(rs, "DT_CADASTRO"));
                return Optional.of(u);
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar o usuário", e);
        }
    }

    /** UPDATE: liga ou desliga a permissão para guardar dados de saúde. */
    public boolean atualizarConsentimento(long id, boolean consentimento) {
        String sql = "UPDATE T_TAMI_USUARIO SET ST_CONSENT_SAUDE = ?, DT_CONSENTIMENTO = ? WHERE ID_USUARIO = ?";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sn(consentimento));
            ps.setTimestamp(2, consentimento ? ts(LocalDateTime.now()) : null);
            ps.setLong(3, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao atualizar a permissão", e);
        }
    }

    public boolean atualizarNome(long id, String nome) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("UPDATE T_TAMI_USUARIO SET NM_USUARIO = ? WHERE ID_USUARIO = ?")) {
            ps.setString(1, nome);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao atualizar o nome", e);
        }
    }

    /** DELETE: "Apagar tudo". Pessoas, conversas, respostas e avaliações saem junto (ON DELETE CASCADE). */
    public boolean apagar(long id) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM T_TAMI_USUARIO WHERE ID_USUARIO = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao apagar o usuário", e);
        }
    }
}
