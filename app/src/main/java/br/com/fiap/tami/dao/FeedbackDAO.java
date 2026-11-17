package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/** "Isso te ajudou?": uma avaliação por conversa. Salvar de novo atualiza (UPDATE); a primeira vez insere (INSERT). */
public class FeedbackDAO extends BaseDAO {

    public FeedbackDAO(ConexaoFactory fabrica) { super(fabrica); }

    public Feedback salvar(long idConversa, Feedback.Avaliacao avaliacao, String comentario) {
        LocalDateTime agora = LocalDateTime.now();
        try (Connection c = fabrica.abrir()) {
            c.setAutoCommit(false);
            try {
                int mudou;
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE T_TAMI_FEEDBACK SET DS_AVALIACAO = ?, DS_COMENTARIO = ?, DT_ATUALIZACAO = ? WHERE ID_CONVERSA = ?")) {
                    up.setString(1, avaliacao.name());
                    up.setString(2, corta(comentario, 1000));
                    up.setTimestamp(3, ts(agora));
                    up.setLong(4, idConversa);
                    mudou = up.executeUpdate();
                }
                if (mudou == 0)
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO T_TAMI_FEEDBACK (ID_CONVERSA, DS_AVALIACAO, DS_COMENTARIO, DT_FEEDBACK) VALUES (?,?,?,?)")) {
                        ins.setLong(1, idConversa);
                        ins.setString(2, avaliacao.name());
                        ins.setString(3, corta(comentario, 1000));
                        ins.setTimestamp(4, ts(agora));
                        ins.executeUpdate();
                    }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao salvar a avaliação", e);
        }
        return buscarPorConversa(idConversa).orElseThrow();
    }

    public Optional<Feedback> buscarPorConversa(long idConversa) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("SELECT * FROM T_TAMI_FEEDBACK WHERE ID_CONVERSA = ?")) {
            ps.setLong(1, idConversa);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Feedback(rs.getLong("ID_FEEDBACK"), idConversa, Feedback.Avaliacao.valueOf(rs.getString("DS_AVALIACAO")),
                    rs.getString("DS_COMENTARIO"), data(rs, "DT_FEEDBACK"), data(rs, "DT_ATUALIZACAO")));
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler a avaliação", e);
        }
    }
}
