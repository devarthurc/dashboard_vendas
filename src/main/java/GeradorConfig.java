import javax.swing.*;
import java.awt.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
public class GeradorConfig extends JFrame {

    private static final Color BG_DARK       = new Color(10, 14, 23);
    private static final Color BG_CARD       = new Color(22, 30, 48);
    private static final Color BORDER_COLOR  = new Color(35, 48, 75);
    private static final Color TEXT_PRIMARY  = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 135, 165);
    private static final Color ACCENT_GREEN  = new Color(0, 230, 150);

    private static final String ARQUIVO = "config.properties";

    private final JTextField     campoHost             = new JTextField(20);
    private final JTextField     campoPorta            = new JTextField(20);
    private final JTextField     campoUser             = new JTextField(20);
    private final JPasswordField campoSenha            = new JPasswordField(20);
    private final JTextField     campoBancoVendedores  = new JTextField(20);
    private final JTextField     campoBancoIshop       = new JTextField(20);
    private final JTextField     campoLoja             = new JTextField(20);
    private final JLabel         lblStatus             = new JLabel(" ");

    public GeradorConfig() {
        setTitle("Gerador de Configuração — Dashboard Vendas");
        Icone.aplicar(this);
        setSize(480, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("🔒  Configuração do banco (cifrada)");
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        titulo.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_DARK);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = addLinha(form, gbc, y, "Host do banco:",        campoHost);
        y = addLinha(form, gbc, y, "Porta:",                campoPorta);
        y = addLinha(form, gbc, y, "Usuário:",               campoUser);
        y = addLinha(form, gbc, y, "Senha:",                 campoSenha);
        y = addLinha(form, gbc, y, "Banco (vendedores):",    campoBancoVendedores);
        y = addLinha(form, gbc, y, "Banco (ishop):",         campoBancoIshop);
        y = addLinha(form, gbc, y, "Loja (opcional):",       campoLoja);

        if (campoPorta.getText().isBlank()) campoPorta.setText("5432");

        lblStatus.setForeground(TEXT_SECONDARY);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JButton btnSalvar = new JButton("💾  Salvar configuração cifrada");
        estilizarBotao(btnSalvar);
        btnSalvar.addActionListener(e -> salvar());

        JPanel painelBotao = new JPanel();
        painelBotao.setBackground(BG_DARK);
        painelBotao.setBorder(BorderFactory.createEmptyBorder(4, 0, 16, 0));
        painelBotao.add(btnSalvar);

        JPanel sul = new JPanel(new BorderLayout());
        sul.setBackground(BG_DARK);
        sul.add(lblStatus,    BorderLayout.NORTH);
        sul.add(painelBotao,  BorderLayout.SOUTH);

        add(titulo, BorderLayout.NORTH);
        add(form,   BorderLayout.CENTER);
        add(sul,    BorderLayout.SOUTH);

        carregarConfigExistente();
    }

    private int addLinha(JPanel form, GridBagConstraints gbc, int y, String rotulo, javax.swing.text.JTextComponent campo) {
        JLabel lbl = new JLabel(rotulo);
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0;
        form.add(lbl, gbc);

        campo.setBackground(BG_CARD);
        campo.setForeground(TEXT_PRIMARY);
        campo.setCaretColor(TEXT_PRIMARY);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        gbc.gridx = 1; gbc.gridy = y; gbc.weightx = 1;
        form.add(campo, gbc);
        return y + 1;
    }

    private void estilizarBotao(JButton btn) {
        btn.setBackground(ACCENT_GREEN);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void carregarConfigExistente() {
        try {
            String cifrado = Files.readString(Path.of(ARQUIVO), StandardCharsets.UTF_8);
            String claro   = CryptoUtil.decrypt(cifrado);

            Properties props = new Properties();
            props.load(new StringReader(claro));

            campoHost.setText(props.getProperty("db.host", ""));
            campoPorta.setText(props.getProperty("db.port", "5432"));
            campoUser.setText(props.getProperty("db.user", ""));
            campoSenha.setText(props.getProperty("db.password", ""));
            campoBancoVendedores.setText(props.getProperty("db.vendedores.database", ""));
            campoBancoIshop.setText(props.getProperty("db.ishop.database", ""));
            campoLoja.setText(props.getProperty("app.loja", ""));

            lblStatus.setText("Configuração existente carregada de \"" + ARQUIVO + "\". Altere e salve para atualizar.");
        } catch (Exception e) {
            lblStatus.setText("Nenhuma configuração encontrada ainda. Preencha os campos e salve.");
        }
    }

    private void salvar() {
        String host            = campoHost.getText().trim();
        String porta           = campoPorta.getText().trim();
        String user             = campoUser.getText().trim();
        String senha            = new String(campoSenha.getPassword());
        String bancoVendedores  = campoBancoVendedores.getText().trim();
        String bancoIshop       = campoBancoIshop.getText().trim();
        String loja             = campoLoja.getText().trim();

        if (host.isEmpty() || porta.isEmpty() || user.isEmpty() || senha.isEmpty()
                || bancoVendedores.isEmpty() || bancoIshop.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Preencha todos os campos antes de salvar.",
                    "Campos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", porta);
        props.setProperty("db.user", user);
        props.setProperty("db.password", senha);
        props.setProperty("db.vendedores.database", bancoVendedores);
        props.setProperty("db.ishop.database", bancoIshop);
        if (!loja.isEmpty()) props.setProperty("app.loja", loja);

        try {
            StringWriter sw = new StringWriter();
            props.store(sw, "Configuração gerada pelo GeradorConfig — não editar manualmente");
            String cifrado = CryptoUtil.encrypt(sw.toString());
            Files.writeString(Path.of(ARQUIVO), cifrado, StandardCharsets.UTF_8);

            lblStatus.setText("Salvo com sucesso em \"" + Path.of(ARQUIVO).toAbsolutePath() + "\".");
            JOptionPane.showMessageDialog(this,
                    "Configuração cifrada e salva com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar configuração: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GeradorConfig().setVisible(true));
    }
}
