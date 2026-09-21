import javax.swing.JOptionPane;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
public final class Config {

    private static final String ARQUIVO = "config.properties";
    private static final Properties props = new Properties();

    static {
        try {
            String conteudoCifrado = Files.readString(Path.of(ARQUIVO), StandardCharsets.UTF_8);
            String conteudoClaro   = CryptoUtil.decrypt(conteudoCifrado);
            props.load(new StringReader(conteudoClaro));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível encontrar/ler o arquivo \"" + ARQUIVO + "\".\n" +
                    "Ele precisa estar na mesma pasta do programa, com as credenciais do banco (arquivo cifrado, gerado pela ferramenta GeradorConfig).\n\n" +
                    "Detalhe: " + e.getMessage(),
                    "Erro de configuração", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private Config() {}

    private static String get(String chave) {
        String valor = props.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "A propriedade \"" + chave + "\" não foi encontrada em \"" + ARQUIVO + "\".",
                    "Erro de configuração", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return valor;
    }




    public static String getLoja() {
        String valor = props.getProperty("app.loja");
        return valor == null ? "" : valor.trim();
    }

    public static String getVendedoresUrl() {
        return "jdbc:postgresql://" + get("db.host") + ":" + get("db.port")
                + "/" + get("db.vendedores.database");
    }

    public static String getVendedoresUser() {
        return get("db.user");
    }

    public static String getVendedoresPassword() {
        return get("db.password");
    }




    public static String getIshopUrl() {
        return "jdbc:postgresql://" + get("db.host") + ":" + get("db.port")
                + "/" + get("db.ishop.database");
    }

    public static String getIshopUser() {
        return get("db.user");
    }

    public static String getIshopPassword() {
        return get("db.password");
    }
}
