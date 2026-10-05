import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class CadastroVendedor extends JFrame {

    private static final Color BG_DARK        = new Color(10, 14, 23);
    private static final Color BG_PANEL       = new Color(16, 22, 36);
    private static final Color BG_CARD        = new Color(22, 30, 48);
    private static final Color BORDER_COLOR   = new Color(35, 48, 75);
    private static final Color TEXT_PRIMARY   = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 135, 165);
    private static final Color ACCENT_GREEN   = new Color(0, 230, 150);
    private static final Color ACCENT_BLUE    = new Color(60, 160, 255);
    private static final Color ACCENT_ORANGE  = new Color(255, 140, 60);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_INPUT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BTN   = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_TABLE = new Font("SansSerif", Font.PLAIN, 11);

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String URL  = Config.getVendedoresUrl();
    private final String USER = Config.getVendedoresUser();
    private final String PASS = Config.getVendedoresPassword();
    private final String URL_ISHOP  = Config.getIshopUrl();
    private final String USER_ISHOP = Config.getIshopUser();
    private final String PASS_ISHOP = Config.getIshopPassword();

    private final String lojaFixa;

    private final JTextField campoBusca = new JTextField(24);
    private JButton btnBuscar;
    private final JLabel lblStatus = new JLabel(" ");
    private DefaultTableModel modelBusca;
    private JTable tabelaBusca;

    private final JTextField     campoUsuario   = new JTextField(20);
    private final JPasswordField campoSenha     = new JPasswordField(20);
    private final JComboBox<String> comboLoja      = new JComboBox<>(new String[]{"001", "002", "003"});
    private final JComboBox<String> comboCategoria = new JComboBox<>(new String[]{"ADULTO", "INFANTIL"});
    private final JLabel lblVinculo = new JLabel();

    private JTabbedPane abas;
    private DefaultTableModel modelCadastrados;
    private JTable tabelaCadastrados;
    private final JLabel lblTotalCadastrados = new JLabel(" ");

    private String idPessoaSelecionada = null;
    private String nomeAlterdataSelecionado = null;

    public CadastroVendedor(String lojaFixa) {
        this.lojaFixa = (lojaFixa == null || lojaFixa.isBlank()) ? null : lojaFixa.trim();
        setTitle("CADASTRO DE VENDEDOR");
        Icone.aplicar(this);
        setSize(900, 640);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) setExtendedState(JFrame.MAXIMIZED_BOTH);
        else setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        if (this.lojaFixa != null) {
            comboLoja.setSelectedItem(this.lojaFixa);
            comboLoja.setEnabled(false);
        }

        add(criarCabecalho(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setBackground(BG_PANEL);
        centro.setBorder(new EmptyBorder(14, 16, 14, 16));
        centro.add(criarPainelBusca(), BorderLayout.CENTER);
        centro.add(criarPainelCadastro(), BorderLayout.SOUTH);
        abas = new JTabbedPane();
        abas.setBackground(BG_PANEL);
        abas.setForeground(TEXT_PRIMARY);
        abas.setFont(FONT_BTN);
        abas.addTab("➕  Cadastrar", centro);
        abas.addTab("👥  Vendedores cadastrados", criarAbaCadastrados());
        abas.addChangeListener(e -> { if (abas.getSelectedIndex() == 1) carregarCadastrados(); });
        add(abas, BorderLayout.CENTER);

        atualizarVinculo();
    }

    private JPanel criarCabecalho() {
        JPanel cab = new JPanel(new BorderLayout());
        cab.setBackground(BG_DARK);
        cab.setBorder(new EmptyBorder(14, 16, 10, 16));
        JLabel t = new JLabel("👤  CADASTRO DE VENDEDOR");
        t.setFont(FONT_TITLE); t.setForeground(TEXT_PRIMARY);
        JLabel s = new JLabel("Clique em Buscar para listar os vendedores do Alterdata (ou digite parte do nome), selecione e informe o nome de acesso e a senha.");
        s.setFont(FONT_LABEL); s.setForeground(TEXT_SECONDARY);
        cab.add(t, BorderLayout.NORTH);
        cab.add(s, BorderLayout.SOUTH);
        return cab;
    }

    private JPanel criarPainelBusca() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_PANEL);

        estilizarCampo(campoBusca);
        btnBuscar = botao("🔍  BUSCAR NO ALTERDATA", ACCENT_BLUE);
        btnBuscar.addActionListener(e -> buscarAlterdata());
        campoBusca.addActionListener(e -> buscarAlterdata());

        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linha.setOpaque(false);
        linha.add(rotulo("Nome ou ID no Alterdata:"));
        linha.add(campoBusca);
        linha.add(btnBuscar);
        lblStatus.setFont(FONT_LABEL);
        lblStatus.setForeground(TEXT_SECONDARY);
        linha.add(lblStatus);

        modelBusca = new DefaultTableModel(new String[]{"ID Pessoa", "Nome no Alterdata", "Última loja", "Última venda", "Já cadastrado como"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaBusca = estilizarTabela(new JTable(modelBusca));
        tabelaBusca.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaBusca.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabelaBusca.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabelaBusca.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionar();
        });

        JScrollPane scroll = new JScrollPane(tabelaBusca);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(BG_CARD);

        p.add(linha, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarPainelCadastro() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        form.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(12, 14, 12, 14)));

        estilizarCampo(campoUsuario);
        estilizarCampo(campoSenha);
        estilizarCombo(comboLoja);
        estilizarCombo(comboCategoria);
        lblVinculo.setFont(new Font("SansSerif", Font.BOLD, 12));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; form.add(lblVinculo, g);
        g.gridwidth = 1;

        g.gridx = 0; g.gridy = 1; form.add(rotulo("Nome do vendedor (acesso):"), g);
        g.gridx = 1; form.add(campoUsuario, g);
        g.gridx = 2; form.add(rotulo("Senha:"), g);
        g.gridx = 3; form.add(campoSenha, g);

        g.gridx = 0; g.gridy = 2; form.add(rotulo("Loja:"), g);
        g.gridx = 1; form.add(comboLoja, g);
        g.gridx = 2; form.add(rotulo("Categoria:"), g);
        g.gridx = 3; form.add(comboCategoria, g);

        JButton btnSalvar = botao("💾  CADASTRAR VENDEDOR", ACCENT_GREEN);
        btnSalvar.addActionListener(e -> salvar());
        g.gridx = 0; g.gridy = 3; g.gridwidth = 4; g.anchor = GridBagConstraints.EAST;
        form.add(btnSalvar, g);

        return form;
    }

    private String sqlBusca(boolean comTermo) {
        return """
            WITH vend AS (
                SELECT p.idpessoa, p.nmpessoa
                FROM ishop.pessoas p
                WHERE p.sttipopessoa = 'U'
                """ + (comTermo ? " AND (p.nmpessoa ILIKE ? OR p.idpessoa = ?) " : "") + """
            ),
            ult AS (
                SELECT ci.idpessoa, MAX(d.dtreferencia) AS ultima
                FROM ishop.comitem ci
                JOIN vend v ON v.idpessoa = ci.idpessoa
                JOIN ishop.documen d ON d.iddocumento = ci.iddocumento
                WHERE COALESCE(d.stdocumentocancelado, '') <> '*'
                GROUP BY ci.idpessoa
            ),
            loja AS (
                SELECT DISTINCT ON (ci.idpessoa) ci.idpessoa, ci.cdempvend
                FROM ishop.comitem ci
                JOIN ult u ON u.idpessoa = ci.idpessoa
                JOIN ishop.documen d ON d.iddocumento = ci.iddocumento AND d.dtreferencia = u.ultima
                ORDER BY ci.idpessoa, ci.cdempvend
            )
            SELECT v.idpessoa, v.nmpessoa, l.cdempvend AS ultima_loja, CAST(u.ultima AS date) AS ultima_venda
            FROM vend v
            LEFT JOIN ult u  ON u.idpessoa = v.idpessoa
            LEFT JOIN loja l ON l.idpessoa = v.idpessoa
            ORDER BY v.nmpessoa
            """;
    }

    private void buscarAlterdata() {
        String termo = campoBusca.getText().trim();
        boolean comTermo = !termo.isEmpty();
        modelBusca.setRowCount(0);
        limparSelecao();
        btnBuscar.setEnabled(false);
        campoBusca.setEnabled(false);
        lblStatus.setText("Buscando no Alterdata...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<java.util.List<Object[]>, Void>() {
            @Override protected java.util.List<Object[]> doInBackground() throws Exception {
                java.util.Map<String, String> cadastrados = new java.util.HashMap<>();
                try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                     PreparedStatement ps = conn.prepareStatement("SELECT idpessoa, username FROM public.usuarios WHERE ativo = true")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) cadastrados.merge(rs.getString("idpessoa"), rs.getString("username"), (a, b) -> a + ", " + b);
                }
                java.util.List<Object[]> linhas = new java.util.ArrayList<>();
                try (Connection conn = DriverManager.getConnection(URL_ISHOP, USER_ISHOP, PASS_ISHOP);
                     PreparedStatement ps = conn.prepareStatement(sqlBusca(comTermo))) {
                    if (comTermo) {
                        ps.setString(1, "%" + termo + "%");
                        ps.setString(2, termo.toUpperCase());
                    }
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        Date dia = rs.getDate("ultima_venda");
                        String id = rs.getString("idpessoa");
                        linhas.add(new Object[]{
                                id,
                                rs.getString("nmpessoa"),
                                rs.getString("ultima_loja") == null ? "—" : rs.getString("ultima_loja"),
                                dia == null ? "sem venda" : dia.toLocalDate().format(BR),
                                cadastrados.getOrDefault(id, "")});
                    }
                }
                return linhas;
            }

            @Override protected void done() {
                btnBuscar.setEnabled(true);
                campoBusca.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    java.util.List<Object[]> linhas = get();
                    for (Object[] l : linhas) modelBusca.addRow(l);
                    lblStatus.setText(linhas.size() + " encontrado(s)");
                    if (linhas.isEmpty())
                        JOptionPane.showMessageDialog(CadastroVendedor.this, comTermo
                                ? "Nenhum vendedor encontrado no Alterdata com \"" + termo + "\"."
                                : "Nenhum vendedor encontrado no Alterdata.");
                } catch (Exception e) {
                    lblStatus.setText(" ");
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(CadastroVendedor.this, "Erro ao buscar no Alterdata:\n" + c.getMessage());
                }
            }
        }.execute();
    }

    private void selecionar() {
        int r = tabelaBusca.getSelectedRow();
        if (r < 0) return;
        idPessoaSelecionada = (String) modelBusca.getValueAt(r, 0);
        nomeAlterdataSelecionado = (String) modelBusca.getValueAt(r, 1);
        String loja = (String) modelBusca.getValueAt(r, 2);
        if (lojaFixa == null && loja != null && !"—".equals(loja)) comboLoja.setSelectedItem(loja);
        if (campoUsuario.getText().isBlank() && nomeAlterdataSelecionado != null)
            campoUsuario.setText(nomeAlterdataSelecionado.trim().split("\\s+")[0]);
        atualizarVinculo();
    }

    private void limparSelecao() {
        idPessoaSelecionada = null;
        nomeAlterdataSelecionado = null;
        atualizarVinculo();
    }

    private void atualizarVinculo() {
        if (idPessoaSelecionada == null) {
            lblVinculo.setText("Nenhum vendedor do Alterdata selecionado");
            lblVinculo.setForeground(ACCENT_ORANGE);
        } else {
            lblVinculo.setText("Vinculado a: " + nomeAlterdataSelecionado + "  (" + idPessoaSelecionada + ")");
            lblVinculo.setForeground(ACCENT_GREEN);
        }
    }

    private void salvar() {
        String usuario = campoUsuario.getText().trim();
        String senha   = new String(campoSenha.getPassword());
        String loja    = (String) comboLoja.getSelectedItem();
        String categoria = (String) comboCategoria.getSelectedItem();

        if (idPessoaSelecionada == null) { JOptionPane.showMessageDialog(this, "Procure e selecione o vendedor no Alterdata."); return; }
        if (usuario.isEmpty())           { JOptionPane.showMessageDialog(this, "Informe o nome do vendedor."); return; }
        if (usuario.length() > 100)      { JOptionPane.showMessageDialog(this, "O nome pode ter no máximo 100 caracteres."); return; }
        if (senha.isEmpty())             { JOptionPane.showMessageDialog(this, "Informe a senha."); return; }
        if (senha.length() > 50)         { JOptionPane.showMessageDialog(this, "A senha pode ter no máximo 50 caracteres."); return; }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM public.usuarios WHERE UPPER(username) = UPPER(?) AND ativo = true")) {
                ps.setString(1, usuario);
                if (ps.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Já existe um usuário ativo com o nome \"" + usuario + "\".");
                    return;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT username FROM public.usuarios WHERE idpessoa = ? AND ativo = true")) {
                ps.setString(1, idPessoaSelecionada);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Este vendedor do Alterdata já está vinculado ao usuário \""
                            + rs.getString(1) + "\".");
                    return;
                }
            }

            String msg = "Cadastrar o vendedor?\n\n"
                    + "Nome: " + usuario + "\n"
                    + "Alterdata: " + nomeAlterdataSelecionado + " (" + idPessoaSelecionada + ")\n"
                    + "Loja: " + loja + "\n"
                    + "Categoria: " + categoria;
            if (JOptionPane.showConfirmDialog(this, msg, "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO public.usuarios (username, senha, cdempresa, idpessoa, tipo, ativo, categoria) "
                            + "VALUES (?, ?, ?, ?, 'VENDEDOR', true, ?)")) {
                ps.setString(1, usuario);
                ps.setString(2, senha);
                ps.setString(3, loja);
                ps.setString(4, idPessoaSelecionada);
                ps.setString(5, categoria);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar:\n" + e.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this, "✅ Vendedor \"" + usuario + "\" cadastrado.");
        campoUsuario.setText("");
        campoSenha.setText("");
        tabelaBusca.clearSelection();
        limparSelecao();
        if (!campoBusca.getText().isBlank()) buscarAlterdata();
        if (modelCadastrados.getRowCount() > 0) carregarCadastrados();
    }

    private JPanel criarAbaCadastrados() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_PANEL);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        JButton btnAtualizar = botao("🔄  ATUALIZAR", ACCENT_BLUE);
        btnAtualizar.addActionListener(e -> carregarCadastrados());
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        lblTotalCadastrados.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTotalCadastrados.setForeground(ACCENT_GREEN);
        topo.add(lblTotalCadastrados, BorderLayout.WEST);
        JButton btnEditar = botao("✏️  EDITAR", ACCENT_ORANGE);
        btnEditar.addActionListener(e -> editarSelecionado());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(btnEditar);
        botoes.add(btnAtualizar);
        topo.add(botoes, BorderLayout.EAST);

        modelCadastrados = new DefaultTableModel(new String[]{
                "Nome (acesso)", "ID Pessoa", "Nome no Alterdata", "Loja", "Categoria", "Situação", "Cadastrado em", "id"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = estilizarTabela(new JTable(modelCadastrados));
        tabelaCadastrados = tabela;
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.removeColumn(tabela.getColumnModel().getColumn(7));
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) editarSelecionado();
            }
        });
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(240);
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(BG_CARD);

        p.add(topo, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void carregarCadastrados() {
        modelCadastrados.setRowCount(0);
        lblTotalCadastrados.setText("Carregando...");
        new SwingWorker<java.util.List<Object[]>, Void>() {
            @Override protected java.util.List<Object[]> doInBackground() throws Exception {
                java.util.List<Object[]> linhas = new java.util.ArrayList<>();
                java.util.List<String> ids = new java.util.ArrayList<>();
                String sql = "SELECT id, username, idpessoa, cdempresa, categoria, ativo, criado_em FROM public.usuarios "
                        + "WHERE UPPER(tipo) = 'VENDEDOR'" + (lojaFixa != null ? " AND cdempresa = ?" : "")
                        + " ORDER BY cdempresa, ativo DESC, username";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (lojaFixa != null) ps.setString(1, lojaFixa);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        Timestamp criado = rs.getTimestamp("criado_em");
                        String id = rs.getString("idpessoa");
                        ids.add(id);
                        linhas.add(new Object[]{
                                rs.getString("username"), id, "", rs.getString("cdempresa"), rs.getString("categoria"),
                                rs.getBoolean("ativo") ? "Ativo" : "Inativo",
                                criado == null ? "" : criado.toLocalDateTime().toLocalDate().format(BR),
                                rs.getInt("id")});
                    }
                }
                if (!ids.isEmpty()) {
                    java.util.Map<String, String> nomes = new java.util.HashMap<>();
                    try (Connection conn = DriverManager.getConnection(URL_ISHOP, USER_ISHOP, PASS_ISHOP);
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT idpessoa, nmpessoa FROM ishop.pessoas WHERE idpessoa = ANY (?)")) {
                        ps.setArray(1, conn.createArrayOf("varchar", ids.toArray()));
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) nomes.put(rs.getString("idpessoa"), rs.getString("nmpessoa"));
                    } catch (Exception ignored) { }
                    for (Object[] l : linhas) l[2] = nomes.getOrDefault((String) l[1], "não encontrado no Alterdata");
                }
                return linhas;
            }

            @Override protected void done() {
                try {
                    java.util.List<Object[]> linhas = get();
                    for (Object[] l : linhas) modelCadastrados.addRow(l);
                    long ativos = linhas.stream().filter(l -> "Ativo".equals(l[5])).count();
                    lblTotalCadastrados.setText(linhas.size() + " vendedor(es) cadastrado(s)   •   " + ativos + " ativo(s)");
                } catch (Exception e) {
                    lblTotalCadastrados.setText(" ");
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(CadastroVendedor.this, "Erro ao carregar vendedores:\n" + c.getMessage());
                }
            }
        }.execute();
    }

    private void editarSelecionado() {
        int v = tabelaCadastrados.getSelectedRow();
        if (v < 0) { JOptionPane.showMessageDialog(this, "Selecione um vendedor na lista."); return; }
        int r = tabelaCadastrados.convertRowIndexToModel(v);
        int id = (Integer) modelCadastrados.getValueAt(r, 7);
        String usuarioAtual = (String) modelCadastrados.getValueAt(r, 0);
        String idPessoa     = (String) modelCadastrados.getValueAt(r, 1);
        String nomeAlter    = (String) modelCadastrados.getValueAt(r, 2);

        JTextField campoNome = new JTextField(usuarioAtual, 20);
        JPasswordField campoNovaSenha = new JPasswordField(20);
        JComboBox<String> cbLoja = new JComboBox<>(new String[]{"001", "002", "003"});
        JComboBox<String> cbCategoria = new JComboBox<>(new String[]{"ADULTO", "INFANTIL"});
        JCheckBox chkAtivo = new JCheckBox("Ativo");
        cbLoja.setSelectedItem(modelCadastrados.getValueAt(r, 3));
        cbCategoria.setSelectedItem(modelCadastrados.getValueAt(r, 4));
        chkAtivo.setSelected("Ativo".equals(modelCadastrados.getValueAt(r, 5)));
        if (lojaFixa != null) cbLoja.setEnabled(false);
        estilizarCampo(campoNome);
        estilizarCampo(campoNovaSenha);
        estilizarCombo(cbLoja);
        estilizarCombo(cbCategoria);
        chkAtivo.setOpaque(false);
        chkAtivo.setForeground(TEXT_PRIMARY);
        chkAtivo.setFont(FONT_INPUT);

        JDialog dlg = new JDialog(this, "Editar vendedor", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_CARD);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.anchor = GridBagConstraints.WEST;

        JLabel vinculo = new JLabel("Vinculado a: " + nomeAlter + "  (" + idPessoa + ")");
        vinculo.setFont(new Font("SansSerif", Font.BOLD, 12));
        vinculo.setForeground(ACCENT_GREEN);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; p.add(vinculo, g);
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = 1; p.add(rotulo("Nome do vendedor (acesso):"), g);
        g.gridx = 1; p.add(campoNome, g);
        g.gridx = 0; g.gridy = 2; p.add(rotulo("Nova senha:"), g);
        g.gridx = 1; p.add(campoNovaSenha, g);
        g.gridx = 1; g.gridy = 3;
        JLabel dica = rotulo("Deixe em branco para manter a senha atual");
        dica.setFont(new Font("SansSerif", Font.ITALIC, 10));
        p.add(dica, g);
        g.gridx = 0; g.gridy = 4; p.add(rotulo("Loja:"), g);
        g.gridx = 1; p.add(cbLoja, g);
        g.gridx = 0; g.gridy = 5; p.add(rotulo("Categoria:"), g);
        g.gridx = 1; p.add(cbCategoria, g);
        g.gridx = 0; g.gridy = 6; p.add(rotulo("Situação:"), g);
        g.gridx = 1; p.add(chkAtivo, g);

        JButton btnSalvar = botao("💾  SALVAR", ACCENT_GREEN);
        JButton btnCancelar = botao("CANCELAR", new Color(50, 60, 90));
        JPanel bts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bts.setOpaque(false);
        bts.add(btnCancelar);
        bts.add(btnSalvar);
        g.gridx = 0; g.gridy = 7; g.gridwidth = 2; g.anchor = GridBagConstraints.EAST;
        g.insets = new Insets(14, 6, 0, 6);
        p.add(bts, g);

        btnCancelar.addActionListener(e -> dlg.dispose());
        btnSalvar.addActionListener(e -> {
            if (salvarEdicao(dlg, id, idPessoa, campoNome.getText().trim(), new String(campoNovaSenha.getPassword()),
                    (String) cbLoja.getSelectedItem(), (String) cbCategoria.getSelectedItem(), chkAtivo.isSelected())) {
                dlg.dispose();
                carregarCadastrados();
            }
        });

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private boolean salvarEdicao(Component pai, int id, String idPessoa, String usuario, String novaSenha,
                                 String loja, String categoria, boolean ativo) {
        if (usuario.isEmpty())        { JOptionPane.showMessageDialog(pai, "Informe o nome do vendedor."); return false; }
        if (usuario.length() > 100)   { JOptionPane.showMessageDialog(pai, "O nome pode ter no máximo 100 caracteres."); return false; }
        if (novaSenha.length() > 50)  { JOptionPane.showMessageDialog(pai, "A senha pode ter no máximo 50 caracteres."); return false; }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            if (ativo) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM public.usuarios WHERE UPPER(username) = UPPER(?) AND ativo = true AND id <> ?")) {
                    ps.setString(1, usuario);
                    ps.setInt(2, id);
                    if (ps.executeQuery().next()) {
                        JOptionPane.showMessageDialog(pai, "Já existe outro usuário ativo com o nome \"" + usuario + "\".");
                        return false;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT username FROM public.usuarios WHERE idpessoa = ? AND ativo = true AND id <> ?")) {
                    ps.setString(1, idPessoa);
                    ps.setInt(2, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(pai, "Este vendedor do Alterdata já está vinculado ao usuário ativo \""
                                + rs.getString(1) + "\".");
                        return false;
                    }
                }
            }

            boolean trocaSenha = !novaSenha.isEmpty();
            String sql = "UPDATE public.usuarios SET username = ?, cdempresa = ?, categoria = ?, ativo = ?"
                    + (trocaSenha ? ", senha = ?" : "") + " WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, usuario);
                ps.setString(i++, loja);
                ps.setString(i++, categoria);
                ps.setBoolean(i++, ativo);
                if (trocaSenha) ps.setString(i++, novaSenha);
                ps.setInt(i, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(pai, "Erro ao salvar:\n" + e.getMessage());
            return false;
        }
        JOptionPane.showMessageDialog(pai, "✅ Vendedor \"" + usuario + "\" atualizado.");
        return true;
    }

    private JLabel rotulo(String t) {
        JLabel l = new JLabel(t); l.setFont(FONT_LABEL); l.setForeground(TEXT_SECONDARY); return l;
    }

    private void estilizarCampo(JTextField f) {
        f.setFont(FONT_INPUT); f.setForeground(TEXT_PRIMARY); f.setBackground(BG_DARK); f.setCaretColor(ACCENT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(6, 8, 6, 8)));
    }

    private void estilizarCombo(JComboBox<String> c) {
        c.setFont(FONT_INPUT); c.setForeground(TEXT_PRIMARY); c.setBackground(BG_DARK);
        c.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1)); c.setPreferredSize(new Dimension(120, 30));
    }

    private JButton botao(String texto, Color cor) {
        JButton b = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(cor.darker());
                else if (getModel().isRollover()) g2.setColor(cor.brighter());
                else g2.setColor(cor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(FONT_BTN); b.setForeground(BG_DARK); b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false); b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTable estilizarTabela(JTable t) {
        t.setFont(FONT_TABLE); t.setForeground(TEXT_PRIMARY); t.setBackground(BG_CARD); t.setGridColor(BORDER_COLOR);
        t.setSelectionBackground(new Color(40, 60, 90)); t.setSelectionForeground(TEXT_PRIMARY); t.setRowHeight(26);
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBackground(new Color(16, 24, 40)); t.getTableHeader().setForeground(TEXT_SECONDARY);
        return t;
    }
}
