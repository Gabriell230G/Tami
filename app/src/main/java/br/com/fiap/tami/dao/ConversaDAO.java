package br.com.fiap.tami.dao;

import br.com.fiap.tami.modelo.Conversa;
import br.com.fiap.tami.modelo.RegraAcionada;
import br.com.fiap.tami.modelo.RespostaRegistrada;
import br.com.fiap.tami.motor.Destino;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD das conversas, com as respostas e as regras acionadas (tabelas filhas). */
public class ConversaDAO extends BaseDAO {

    public ConversaDAO(ConexaoFactory fabrica) { super(fabrica); }

    /** INSERT: a conversa começa. Sem permissão (guardar = N), o relato não vai para o banco. */
    public Conversa inserir(Conversa cv) {
        String sql = "INSERT INTO T_TAMI_CONVERSA (ID_USUARIO, ID_PESSOA, NR_IDADE_MESES, ST_GUARDAR, DS_RELATO, CD_REGIAO, "
            + "NR_SINAIS_TOTAL, NR_SINAIS_REGIAO, DS_QUEIXA, DT_INICIO) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = prepararInsert(c, sql, "ID_CONVERSA")) {
            longOuNulo(ps, 1, cv.getIdUsuario());
            longOuNulo(ps, 2, cv.getIdPessoa());
            ps.setInt(3, cv.getIdadeMeses());
            ps.setString(4, sn(cv.isGuardar()));
            ps.setString(5, cv.isGuardar() ? corta(cv.getRelato(), 4000) : null);
            ps.setString(6, cv.getCodigoRegiao());
            intOuNulo(ps, 7, cv.getSinaisTotal());
            intOuNulo(ps, 8, cv.getSinaisRegiao());
            ps.setString(9, cv.getQueixa());
            ps.setTimestamp(10, ts(cv.getInicio()));
            ps.executeUpdate();
            cv.setId(idGerado(ps));
            return cv;
        } catch (SQLException e) {
            throw new DaoException("Erro ao iniciar a conversa", e);
        }
    }

    /**
     * UPDATE: a conversa termina. Grava o destino, as respostas e as regras numa transação só
     * (ou vai tudo, ou nada).
     */
    public void finalizar(Conversa cv, List<RespostaRegistrada> respostas, List<RegraAcionada> regras) {
        String up = "UPDATE T_TAMI_CONVERSA SET DT_FIM = ?, DS_DESTINO = ?, DS_PRIORIDADE_UBS = ?, CD_MENSAGEM = ?, ST_CRISE = ?, "
            + "ST_POR_NAO_SEI = ?, NR_PERGUNTAS = ? WHERE ID_CONVERSA = ?";
        String insR = "INSERT INTO T_TAMI_RESPOSTA (ID_CONVERSA, NR_ORDEM, CD_PERGUNTA, DS_PERGUNTA, DS_RESPOSTA, DS_ORIGEM, ST_NAO_SEI, DT_RESPOSTA) VALUES (?,?,?,?,?,?,?,?)";
        String insG = "INSERT INTO T_TAMI_REGRA_ACIONADA (ID_CONVERSA, CD_REGRA, DS_DESTINO, DS_MOTIVO) VALUES (?,?,?,?)";
        try (Connection c = fabrica.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement(up); PreparedStatement p2 = c.prepareStatement(insR);
                 PreparedStatement p3 = c.prepareStatement(insG)) {
                p1.setTimestamp(1, ts(cv.getFim()));
                p1.setString(2, cv.getDestino().name());
                p1.setString(3, cv.getPrioridadeUbs());
                p1.setString(4, cv.getCodigoMensagem());
                p1.setString(5, sn(cv.isCrise()));
                p1.setString(6, sn(cv.isPorNaoSei()));
                p1.setInt(7, cv.getPerguntas());
                p1.setLong(8, cv.getId());
                p1.executeUpdate();
                if (cv.isGuardar()) {
                    for (RespostaRegistrada r : respostas) {
                        p2.setLong(1, cv.getId()); p2.setInt(2, r.ordem()); p2.setString(3, r.codigoPergunta());
                        p2.setString(4, corta(r.pergunta(), 1000)); p2.setString(5, corta(r.resposta(), 1000));
                        p2.setString(6, r.origem()); p2.setString(7, sn(r.naoSei())); p2.setTimestamp(8, ts(r.data()));
                        p2.addBatch();
                    }
                    p2.executeBatch();
                }
                for (RegraAcionada g : regras) {   // as regras não têm o relato: ficam mesmo sem permissão (explicam o destino)
                    p3.setLong(1, cv.getId()); p3.setString(2, corta(g.codigoRegra(), 60));
                    p3.setString(3, g.destino()); p3.setString(4, corta(g.motivo(), 300));
                    p3.addBatch();
                }
                p3.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao finalizar a conversa", e);
        }
    }

    public Optional<Conversa> buscarPorId(long id) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("SELECT * FROM T_TAMI_CONVERSA WHERE ID_CONVERSA = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(ler(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Erro ao buscar a conversa", e);
        }
    }

    /** Histórico: as conversas encerradas do usuário, da mais nova para a mais antiga. */
    public List<Conversa> listarPorUsuario(long idUsuario, int limite) {
        String sql = "SELECT * FROM T_TAMI_CONVERSA WHERE ID_USUARIO = ? AND DS_DESTINO IS NOT NULL ORDER BY DT_INICIO DESC FETCH FIRST "
            + Math.max(1, Math.min(limite, 200)) + " ROWS ONLY";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            List<Conversa> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(ler(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao listar o histórico", e);
        }
    }

    public List<RespostaRegistrada> respostas(long idConversa) {
        String sql = "SELECT * FROM T_TAMI_RESPOSTA WHERE ID_CONVERSA = ? ORDER BY NR_ORDEM";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idConversa);
            List<RespostaRegistrada> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new RespostaRegistrada(rs.getLong("ID_RESPOSTA"), idConversa, rs.getInt("NR_ORDEM"), rs.getString("CD_PERGUNTA"),
                        rs.getString("DS_PERGUNTA"), rs.getString("DS_RESPOSTA"), rs.getString("DS_ORIGEM"), sn(rs.getString("ST_NAO_SEI")),
                        data(rs, "DT_RESPOSTA")));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler as respostas", e);
        }
    }

    public List<RegraAcionada> regras(long idConversa) {
        String sql = "SELECT * FROM T_TAMI_REGRA_ACIONADA WHERE ID_CONVERSA = ? ORDER BY ID_REGRA";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idConversa);
            List<RegraAcionada> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new RegraAcionada(rs.getLong("ID_REGRA"), idConversa, rs.getString("CD_REGRA"), rs.getString("DS_DESTINO"), rs.getString("DS_MOTIVO")));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler as regras", e);
        }
    }

    /** DELETE de uma conversa do histórico (as respostas, regras, dor e avaliação saem junto). */
    public boolean apagar(long id, long idUsuario) {
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM T_TAMI_CONVERSA WHERE ID_CONVERSA = ? AND ID_USUARIO = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, idUsuario);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Erro ao apagar a conversa", e);
        }
    }

    private static Conversa ler(ResultSet rs) throws SQLException {
        Conversa cv = new Conversa();
        cv.setId(rs.getLong("ID_CONVERSA"));
        cv.setIdUsuario(longOuNulo(rs, "ID_USUARIO"));
        cv.setIdPessoa(longOuNulo(rs, "ID_PESSOA"));
        cv.setIdadeMeses(rs.getInt("NR_IDADE_MESES"));
        cv.setGuardar(sn(rs.getString("ST_GUARDAR")));
        cv.setRelato(rs.getString("DS_RELATO"));
        cv.setCodigoRegiao(rs.getString("CD_REGIAO"));
        cv.setSinaisTotal(intOuNulo(rs, "NR_SINAIS_TOTAL"));
        cv.setSinaisRegiao(intOuNulo(rs, "NR_SINAIS_REGIAO"));
        cv.setQueixa(rs.getString("DS_QUEIXA"));
        cv.setInicio(data(rs, "DT_INICIO"));
        cv.setFim(data(rs, "DT_FIM"));
        String d = rs.getString("DS_DESTINO");
        cv.setDestino(d == null ? null : Destino.valueOf(d));
        cv.setPrioridadeUbs(rs.getString("DS_PRIORIDADE_UBS"));
        cv.setCodigoMensagem(rs.getString("CD_MENSAGEM"));
        cv.setCrise(sn(rs.getString("ST_CRISE")));
        cv.setPorNaoSei(sn(rs.getString("ST_POR_NAO_SEI")));
        cv.setPerguntas(rs.getInt("NR_PERGUNTAS"));
        return cv;
    }
}
