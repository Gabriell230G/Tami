package br.com.fiap.tami.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas de análise (GROUP BY, JOIN, AVG, CASE) sobre o uso do app. Os cálculos estatísticos ficam no serviço. */
public class EstatisticaDAO extends BaseDAO {

    public EstatisticaDAO(ConexaoFactory fabrica) { super(fabrica); }

    /** Uma conversa encerrada, só com números (nada de texto). */
    public record LinhaConversa(String destino, int perguntas, Long segundos, int idadeMeses, String regiao,
                                Integer sinaisTotal, Integer sinaisRegiao, boolean crise, boolean porNaoSei, int hora) {}

    public List<LinhaConversa> conversas() {
        String sql = "SELECT DS_DESTINO, NR_PERGUNTAS, DT_INICIO, DT_FIM, NR_IDADE_MESES, CD_REGIAO, NR_SINAIS_TOTAL, NR_SINAIS_REGIAO, "
            + "ST_CRISE, ST_POR_NAO_SEI, EXTRACT(HOUR FROM DT_INICIO) AS HORA FROM T_TAMI_CONVERSA WHERE DS_DESTINO IS NOT NULL";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<LinhaConversa> out = new ArrayList<>();
            while (rs.next()) {
                var ini = data(rs, "DT_INICIO");
                var fim = data(rs, "DT_FIM");
                Long seg = ini == null || fim == null ? null : java.time.Duration.between(ini, fim).toSeconds();
                out.add(new LinhaConversa(rs.getString("DS_DESTINO"), rs.getInt("NR_PERGUNTAS"), seg, rs.getInt("NR_IDADE_MESES"),
                    rs.getString("CD_REGIAO"), intOuNulo(rs, "NR_SINAIS_TOTAL"), intOuNulo(rs, "NR_SINAIS_REGIAO"),
                    sn(rs.getString("ST_CRISE")), sn(rs.getString("ST_POR_NAO_SEI")), rs.getInt("HORA")));
            }
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler as conversas para a análise", e);
        }
    }

    /** Tabela de contingência: avaliação ("Isso te ajudou?") × destino. */
    public Map<String, Map<String, Integer>> avaliacaoPorDestino() {
        String sql = "SELECT C.DS_DESTINO, F.DS_AVALIACAO, COUNT(*) AS N FROM T_TAMI_FEEDBACK F "
            + "JOIN T_TAMI_CONVERSA C ON C.ID_CONVERSA = F.ID_CONVERSA GROUP BY C.DS_DESTINO, F.DS_AVALIACAO";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
            while (rs.next()) out.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(rs.getString(2), rs.getInt(3));
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao cruzar avaliação e destino", e);
        }
    }

    /** Dor média por grande região do corpo (JOIN com a árvore: a região marcada sobe até a mãe de nível 1). */
    public List<Object[]> dorPorRegiao() {
        String sql = "SELECT COALESCE(M.NM_REGIAO, R.NM_REGIAO) AS GRANDE, COUNT(*) AS N, AVG(D.NR_NIVEL) AS MEDIA, MIN(D.NR_NIVEL), MAX(D.NR_NIVEL) "
            + "FROM T_TAMI_DOR D JOIN T_TAMI_REGIAO R ON R.CD_REGIAO = D.CD_REGIAO "
            + "LEFT JOIN T_TAMI_REGIAO M ON M.CD_REGIAO = R.CD_REGIAO_PAI AND R.NR_NIVEL = 2 "
            + "GROUP BY COALESCE(M.NM_REGIAO, R.NM_REGIAO) ORDER BY N DESC";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Object[]> out = new ArrayList<>();
            while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getInt(4), rs.getInt(5)});
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao calcular a dor por região", e);
        }
    }

    /** Quantas vezes cada regra levou ao destino final (as 10 mais comuns). */
    public List<Object[]> regrasMaisAcionadas(int limite) {
        String sql = "SELECT G.CD_REGRA, G.DS_DESTINO, COUNT(*) AS N FROM T_TAMI_REGRA_ACIONADA G "
            + "JOIN T_TAMI_CONVERSA C ON C.ID_CONVERSA = G.ID_CONVERSA AND C.DS_DESTINO = G.DS_DESTINO "
            + "GROUP BY G.CD_REGRA, G.DS_DESTINO ORDER BY N DESC FETCH FIRST " + Math.max(1, Math.min(limite, 50)) + " ROWS ONLY";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Object[]> out = new ArrayList<>();
            while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getString(2), rs.getInt(3)});
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar as regras", e);
        }
    }

    public int contar(String tabela) {
        if (!tabela.matches("T_TAMI_[A-Z_]+")) throw new IllegalArgumentException("tabela inválida");
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + tabela); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DaoException("Erro ao contar " + tabela, e);
        }
    }
}
