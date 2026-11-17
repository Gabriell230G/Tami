package br.com.fiap.tami.dao;

import br.com.fiap.tami.regiao.ArvoreRegioes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lê a árvore do corpo da tabela T_TAMI_REGIAO. */
public class RegiaoDAO extends BaseDAO {

    public RegiaoDAO(ConexaoFactory fabrica) { super(fabrica); }

    public List<ArvoreRegioes.Linha> listar() {
        String sql = "SELECT CD_REGIAO, CD_REGIAO_PAI, NM_REGIAO, NR_NIVEL, DS_PALAVRAS FROM T_TAMI_REGIAO ORDER BY NR_NIVEL, CD_REGIAO";
        try (Connection c = fabrica.abrir(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<ArvoreRegioes.Linha> out = new ArrayList<>();
            while (rs.next())
                out.add(new ArvoreRegioes.Linha(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5)));
            return out;
        } catch (SQLException e) {
            throw new DaoException("Erro ao ler as regiões do corpo", e);
        }
    }

    public ArvoreRegioes carregarArvore() { return new ArvoreRegioes(listar()); }
}
