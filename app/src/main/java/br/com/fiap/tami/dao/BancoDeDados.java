package br.com.fiap.tami.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Cria as tabelas na primeira vez (roda os scripts da pasta sql/) e diz se o banco já está pronto. */
public final class BancoDeDados {

    private final ConexaoFactory fabrica;

    public BancoDeDados(ConexaoFactory fabrica) { this.fabrica = fabrica; }

    public boolean tabelasExistem() {
        try (Connection c = fabrica.abrir()) {
            DatabaseMetaData md = c.getMetaData();
            for (String nome : new String[]{"T_TAMI_REGIAO", "t_tami_regiao"})   // o PostgreSQL guarda em minúsculas
                try (ResultSet rs = md.getTables(null, null, nome, new String[]{"TABLE"})) {
                    if (rs.next()) return true;
                }
            return false;
        } catch (SQLException e) {
            throw new DaoException("Não consegui conectar ao banco " + fabrica.url(), e);
        }
    }

    /** Cria as tabelas e a árvore de regiões, se ainda não existirem. Devolve true se criou agora. */
    public boolean preparar() {
        if (tabelasExistem()) return false;
        rodarScript(pasta() + "01_criar_tabelas.sql");
        rodarScript(pasta() + "02_regioes.sql");
        return true;
    }

    /** Apaga tudo (para recomeçar a demonstração). */
    public void apagarTudo() {
        rodarScript(pasta() + "99_apagar_tabelas.sql");
    }

    private String pasta() { return "/sql/" + fabrica.dialeto() + "/"; }

    public void rodarScript(String caminho) {
        List<String> comandos = comandos(ler(caminho));
        try (Connection c = fabrica.abrir(); Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            for (String sql : comandos) {
                try {
                    st.execute(sql);
                } catch (SQLException e) {
                    if (caminho.contains("99_")) continue;   // apagar: tabela que não existe não é problema
                    throw new DaoException("Erro no script " + caminho + " no comando: " + sql.lines().findFirst().orElse(sql), e);
                }
            }
            c.commit();
        } catch (SQLException e) {
            throw new DaoException("Erro ao rodar " + caminho, e);
        }
    }

    private static String ler(String caminho) {
        try (InputStream in = BancoDeDados.class.getResourceAsStream(caminho)) {
            if (in == null) throw new IllegalStateException("Script não encontrado: " + caminho);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não consegui ler " + caminho, e);
        }
    }

    /** Separa o script em comandos: tira comentários "--", corta no ";" do fim da linha e ignora COMMIT. */
    static List<String> comandos(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String linha : script.split("\\R")) {
            String t = linha.strip();
            if (t.isEmpty() || t.startsWith("--")) continue;
            atual.append(linha).append('\n');
            if (t.endsWith(";")) {
                String cmd = atual.toString().strip();
                cmd = cmd.substring(0, cmd.length() - 1).strip();
                if (!cmd.equalsIgnoreCase("COMMIT")) out.add(cmd);
                atual.setLength(0);
            }
        }
        if (!atual.toString().isBlank()) out.add(atual.toString().strip());
        return out;
    }
}
