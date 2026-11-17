package br.com.fiap.tami.dao;

import br.com.fiap.tami.app.Config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Fábrica de conexões JDBC (padrão Factory) com um pool simples: abrir conexão com um banco na nuvem
 * (Supabase, Oracle da FIAP) custa centenas de milissegundos, então as conexões são reaproveitadas.
 * Quem usa continua escrevendo try-with-resources: o close() devolve a conexão para o pool em vez de fechar.
 */
public final class ConexaoFactory {

    private static final int TAMANHO_POOL = 6;

    private final String url;
    private final String usuario;
    private final String senha;
    private final BlockingQueue<Connection> livres = new ArrayBlockingQueue<>(TAMANHO_POOL);

    public ConexaoFactory(Config config) {
        this(config.urlBanco(), config.usuarioBanco(), config.senhaBanco());
    }

    public ConexaoFactory(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public Connection abrir() throws SQLException {
        Connection real;
        while ((real = livres.poll()) != null) {
            if (valida(real)) break;
            fecharSemErro(real);
        }
        if (real == null) real = DriverManager.getConnection(url, usuario, senha);
        real.setAutoCommit(true);
        return emprestada(real);
    }

    private static boolean valida(Connection c) {
        try { return !c.isClosed() && c.isValid(2); } catch (SQLException e) { return false; }
    }

    /** Embrulha a conexão (Proxy): close() devolve ao pool; o resto vai direto para a conexão real. */
    private Connection emprestada(Connection real) {
        InvocationHandler h = new InvocationHandler() {
            private boolean devolvida;
            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] args) throws Throwable {
                if (m.getName().equals("close")) {
                    if (devolvida) return null;
                    devolvida = true;
                    try {
                        if (!real.getAutoCommit()) real.rollback();   // transação esquecida aberta não passa adiante
                    } catch (SQLException e) { fecharSemErro(real); return null; }
                    if (!livres.offer(real)) fecharSemErro(real);
                    return null;
                }
                if (m.getName().equals("isClosed")) return devolvida || real.isClosed();
                if (devolvida) throw new SQLException("Conexão já devolvida ao pool");
                try { return m.invoke(real, args); } catch (InvocationTargetException e) { throw e.getCause(); }
            }
        };
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, h);
    }

    private static void fecharSemErro(Connection c) {
        try { c.close(); } catch (SQLException ignorado) { }
    }

    public void fecharTudo() {
        Connection c;
        while ((c = livres.poll()) != null) fecharSemErro(c);
    }

    public String url() { return url; }

    public boolean oracle() { return url.startsWith("jdbc:oracle:"); }

    public boolean postgres() { return url.startsWith("jdbc:postgresql:"); }

    /** Pasta dos scripts: o H2 imita o Oracle, então usa os scripts do Oracle. */
    public String dialeto() { return postgres() ? "postgres" : "oracle"; }

    /** Nome curto para mostrar na tela. */
    public String nome() {
        if (postgres()) return url.contains("supabase") ? "PostgreSQL (Supabase)" : "PostgreSQL";
        if (oracle()) return "Oracle" + (url.contains("fiap") ? " (FIAP)" : "");
        return "H2 em modo Oracle";
    }
}
