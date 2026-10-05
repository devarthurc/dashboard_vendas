import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class Login extends JFrame {

    private static final Color BG_DARK       = new Color(10, 14, 23);
    private static final Color BG_CARD       = new Color(22, 30, 48);
    private static final Color BORDER_COLOR  = new Color(35, 48, 75);
    private static final Color TEXT_PRIMARY  = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 135, 165);
    private static final Color ACCENT_GREEN  = new Color(0, 230, 150);

    private final String URL  = Config.getVendedoresUrl();
    private final String USER = Config.getVendedoresUser();
    private final String PASS = Config.getVendedoresPassword();
    private final String LOJA = Config.getLoja();

    private JComboBox<String> comboUsuario = new JComboBox<>();
    private JPasswordField senha = new JPasswordField();

    public Login() {
        setTitle("Entrar");
        Icone.aplicar(this);
        setSize(420, 340);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 14, 0);

        Font fLbl   = new Font("SansSerif", Font.PLAIN, 11);
        Font fInput = new Font("SansSerif", Font.PLAIN, 13);

        JLabel titulo = new JLabel("📊  ANÁLISE DE VENDAS");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel sub = new JLabel("Faça login para continuar");
        sub.setFont(fLbl);
        sub.setForeground(TEXT_SECONDARY);
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridy = 0; g.insets = new Insets(0, 0, 4, 0);
        root.add(titulo, g);
        g.gridy = 1; g.insets = new Insets(0, 0, 24, 0);
        root.add(sub, g);

        JLabel lUser = new JLabel("👤  USUÁRIO");
        lUser.setFont(fLbl);
        lUser.setForeground(TEXT_SECONDARY);
        g.gridy = 2; g.insets = new Insets(0, 0, 4, 0);
        root.add(lUser, g);

        comboUsuario.setFont(fInput);
        comboUsuario.setForeground(TEXT_PRIMARY);
        comboUsuario.setBackground(BG_CARD);
        comboUsuario.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        comboUsuario.setPreferredSize(new Dimension(0, 36));
        g.gridy = 3; g.insets = new Insets(0, 0, 14, 0);
        root.add(comboUsuario, g);

        JLabel lPass = new JLabel("🔑  SENHA");
        lPass.setFont(fLbl);
        lPass.setForeground(TEXT_SECONDARY);
        g.gridy = 4; g.insets = new Insets(0, 0, 4, 0);
        root.add(lPass, g);

        senha.setFont(fInput);
        senha.setForeground(TEXT_PRIMARY);
        senha.setBackground(BG_CARD);
        senha.setCaretColor(ACCENT_GREEN);
        senha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 10, 6, 10)));
        g.gridy = 5; g.insets = new Insets(0, 0, 22, 0);
        root.add(senha, g);

        JButton entrar = new JButton("🚀  ENTRAR") {
            @Override protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g2.setColor(ACCENT_GREEN.darker());
                else if (getModel().isRollover()) g2.setColor(ACCENT_GREEN.brighter());
                else                              g2.setColor(ACCENT_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g2d);
            }
        };
        entrar.setFont(new Font("SansSerif", Font.BOLD, 13));
        entrar.setForeground(BG_DARK);
        entrar.setOpaque(false);
        entrar.setContentAreaFilled(false);
        entrar.setBorderPainted(false);
        entrar.setFocusPainted(false);
        entrar.setBorder(new EmptyBorder(10, 0, 10, 0));
        entrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        g.gridy = 6; g.insets = new Insets(0, 0, 0, 0);
        root.add(entrar, g);

        add(root);

        carregarUsuarios();
        entrar.addActionListener(e -> logar());
        senha.addActionListener(e -> logar());
    }

    private String filtroLoja() {
        return LOJA.isEmpty() ? "" : " AND (cdempresa = ? OR UPPER(tipo) = 'SUPERVISOR')";
    }

    private void carregarUsuarios() {
        comboUsuario.removeAllItems();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT username FROM usuarios WHERE ativo = true" + filtroLoja() + " ORDER BY username")) {
            if (!LOJA.isEmpty()) ps.setString(1, LOJA);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) comboUsuario.addItem(rs.getString("username"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar usuários:\n" + e.getMessage());
        }
    }

    private void logar() {
        String usuario      = (String) comboUsuario.getSelectedItem();
        String senhaDigitada = new String(senha.getPassword());
        if (usuario == null || senhaDigitada.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe a senha");
            return;
        }
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT cdempresa, idpessoa, tipo, username, categoria FROM usuarios " +
                             "WHERE username = ? AND senha = ? AND ativo = true" + filtroLoja())) {
            ps.setString(1, usuario);
            ps.setString(2, senhaDigitada);
            if (!LOJA.isEmpty()) ps.setString(3, LOJA);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String empresa    = rs.getString("cdempresa");
                String idpessoa   = rs.getString("idpessoa");
                String tipo       = rs.getString("tipo");
                String nome       = rs.getString("username");
                String categoria  = rs.getString("categoria");

                
                if ("SUPERVISOR".equalsIgnoreCase(tipo)) {
                    new CadastroMetas().setVisible(true);
                } else {
                    new Dashboard(empresa, idpessoa, tipo, nome, categoria).setVisible(true);
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Senha inválida");
                senha.setText("");
                senha.requestFocus();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
        UIManager.put("ComboBox.background",      new Color(22, 30, 48));
        UIManager.put("ComboBox.foreground",      new Color(230, 235, 245));
        UIManager.put("Spinner.background",       new Color(22, 30, 48));
        UIManager.put("Spinner.foreground",       new Color(230, 235, 245));
        UIManager.put("FormattedTextField.background", new Color(22, 30, 48));
        UIManager.put("FormattedTextField.foreground", new Color(230, 235, 245));
        UIManager.put("TextField.background",     new Color(22, 30, 48));
        UIManager.put("TextField.foreground",     new Color(230, 235, 245));
        UIManager.put("TextArea.background",      new Color(22, 30, 48));
        UIManager.put("TextArea.foreground",      new Color(230, 235, 245));
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}