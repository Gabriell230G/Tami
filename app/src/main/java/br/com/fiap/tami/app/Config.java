package br.com.fiap.tami.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Lê o tami.properties (ou as variáveis de ambiente TAMI_DB_URL, TAMI_DB_USUARIO, TAMI_DB_SENHA, TAMI_PORTA). */
public final class Config {

    private final Properties props = new Properties();

    public Config(Path arquivo) {
        if (arquivo != null && Files.exists(arquivo)) {
            try (InputStream in = Files.newInputStream(arquivo)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Não consegui ler " + arquivo, e);
            }
        }
    }

    private String valor(String chave, String ambiente, String padrao) {
        String env = System.getenv(ambiente);
        if (env != null && !env.isBlank()) return env;
        return props.getProperty(chave, padrao).trim();
    }

    public String urlBanco() { return valor("db.url", "TAMI_DB_URL", "jdbc:h2:./dados/tami;MODE=Oracle;DEFAULT_NULL_ORDERING=HIGH"); }
    public String usuarioBanco() { return valor("db.usuario", "TAMI_DB_USUARIO", "tami"); }
    public String senhaBanco() { return valor("db.senha", "TAMI_DB_SENHA", "tami"); }
    /** TAMI_PORTA, ou PORT (a variável que Render, Railway e parecidos usam), ou o tami.properties. */
    public int porta() {
        String port = System.getenv("PORT");
        if (System.getenv("TAMI_PORTA") == null && port != null && !port.isBlank()) return Integer.parseInt(port.strip());
        return Integer.parseInt(valor("porta", "TAMI_PORTA", "8080"));
    }

    /** "demonstracao" (padrão) ou "producao". */
    public String modo() { return valor("modo", "TAMI_MODO", "demonstracao"); }

    public boolean oracle() { return urlBanco().startsWith("jdbc:oracle:"); }
}
