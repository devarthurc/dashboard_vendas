import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


public class VendasAuxiliares extends JFrame {

    private static final Color BG_DARK        = new Color(10, 14, 23);
    private static final Color BG_PANEL       = new Color(16, 22, 36);
    private static final Color BG_CARD        = new Color(22, 30, 48);
    private static final Color BORDER_COLOR   = new Color(35, 48, 75);
    private static final Color TEXT_PRIMARY   = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 135, 165);
    private static final Color ACCENT_GREEN   = new Color(0, 230, 150);
    private static final Color ACCENT_ORANGE  = new Color(255, 140, 60);
    private static final Color ACCENT_BLUE    = new Color(60, 160, 255);
    private static final Color ACCENT_RED     = new Color(255, 80, 100);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_INPUT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BTN   = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_TABLE = new Font("SansSerif", Font.PLAIN, 11);

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] LOJAS = {"001", "002", "003"};


    private static final String[] COLUNAS_PADRAO =
            {"quantidade_produtos", "idpessoa", "cdempresa", "valor_vendido", "data_venda", "clientes"};

    private static final String[] COLUNAS_OBRIGATORIAS =
            {"quantidade_produtos", "idpessoa", "cdempresa", "valor_vendido", "data_venda"};

    private final String URL  = Config.getVendedoresUrl();
    private final String USER = Config.getVendedoresUser();
    private final String PASS = Config.getVendedoresPassword();
    private final String URL_ISHOP  = Config.getIshopUrl();
    private final String USER_ISHOP = Config.getIshopUser();
    private final String PASS_ISHOP = Config.getIshopPassword();

    private final DecimalFormat df = new DecimalFormat("#,##0.00",
            java.text.DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
    private final String lojaFixa;


    private final JComboBox<String>   comboLoja   = new JComboBox<>(LOJAS);
    private final JComboBox<Vendedor> comboFiltroVend = new JComboBox<>();
    private final JFormattedTextField dtIni = criarCampoData(LocalDate.now().withDayOfMonth(1).format(BR));
    private final JFormattedTextField dtFim = criarCampoData(LocalDate.now().format(BR));


    private DefaultTableModel modelLanc;
    private JTable tabelaLanc;
    private final JLabel lblTotalLanc = new JLabel(" ");

    private Integer idEmEdicao = null;
    private final JFormattedTextField campoData  = criarCampoData("");
    private final JComboBox<Vendedor> comboVendForm = new JComboBox<>();
    private final JTextField campoQtd   = new JTextField("1", 5);
    private final JTextField campoValor = new JTextField("0,00", 9);
    private final JTextField campoClientes = new JTextField("0", 4);
    private final JLabel lblModo = new JLabel();


    private JTextArea areaCola;


    private DefaultTableModel modelResumo;
    private final JLabel lblTotalResumo = new JLabel(" ");

    private JTabbedPane abas;


    private final Map<String, String> nomes = new HashMap<>();

    public VendasAuxiliares(String lojaFixa) {
        this.lojaFixa = (lojaFixa == null || lojaFixa.isBlank()) ? null : lojaFixa.trim();
        setTitle("🧾  VENDAS AUXILIARES");
        setSize(1180, 740);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);
        if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) setExtendedState(JFrame.MAXIMIZED_BOTH);
        else setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        garantirTabela();

        if (this.lojaFixa != null) {
            comboLoja.setSelectedItem(this.lojaFixa);
            comboLoja.setEnabled(false);
        }

        add(criarCabecalho(), BorderLayout.NORTH);

        abas = new JTabbedPane();
        abas.setBackground(BG_PANEL); abas.setForeground(TEXT_PRIMARY); abas.setFont(FONT_BTN);
        abas.addTab("✏️  Lançamentos", criarAbaLancamentos());
        abas.addTab("📊  Resumo por dia (Alterdata x Auxiliar)", criarAbaResumo());
        abas.addChangeListener(e -> { if (abas.getSelectedIndex() == 1) carregarResumo(); });
        add(abas, BorderLayout.CENTER);

        comboLoja.addActionListener(e -> { carregarVendedores(); buscar(); });
        carregarVendedores();
        limparFormulario();
        buscar();
    }


    private void garantirTabela() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS public.vendas_auxiliar ("
                    + "id SERIAL PRIMARY KEY, "
                    + "idpessoa VARCHAR(20) NOT NULL, "
                    + "cdempresa VARCHAR(10) NOT NULL, "
                    + "data_venda DATE NOT NULL, "
                    + "quantidade_produtos INTEGER NOT NULL DEFAULT 0, "
                    + "valor_vendido NUMERIC(15,2) NOT NULL DEFAULT 0, "
                    + "clientes INTEGER NOT NULL DEFAULT 0, "
                    + "criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "alterado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            st.execute("ALTER TABLE public.vendas_auxiliar ADD COLUMN IF NOT EXISTS clientes INTEGER NOT NULL DEFAULT 0");
            st.execute("CREATE INDEX IF NOT EXISTS idx_vendas_auxiliar_loja_data "
                    + "ON public.vendas_auxiliar (cdempresa, data_venda, idpessoa)");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não foi possível criar/verificar a tabela vendas_auxiliar:\n"
                    + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }


    private JPanel criarCabecalho() {
        JPanel cab = new JPanel(new BorderLayout(0, 8));
        cab.setBackground(BG_DARK);
        cab.setBorder(new EmptyBorder(14, 16, 10, 16));

        JPanel titRow = new JPanel(new BorderLayout());
        titRow.setBackground(BG_DARK);
        JLabel t = new JLabel("🧾  VENDAS AUXILIARES");
        t.setFont(FONT_TITLE); t.setForeground(TEXT_PRIMARY);
        JLabel regra = new JLabel("<html><font color='#7887a5'>Dia com lançamento aqui → o Dashboard usa este valor. "
                + "Dia sem lançamento → continua puxando do Alterdata (nada é preenchido automaticamente).</font></html>");
        regra.setFont(FONT_LABEL);
        titRow.add(t, BorderLayout.NORTH);
        titRow.add(regra, BorderLayout.SOUTH);

        estilizarCombo(comboLoja);
        estilizarComboVend(comboFiltroVend, 240);
        estilizarCampo(dtIni); estilizarCampo(dtFim);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setBackground(BG_DARK);
        filtros.add(rotulo("🏪 Loja:"));     filtros.add(comboLoja);
        filtros.add(rotulo("📅 De:"));       filtros.add(dtIni);
        filtros.add(rotulo("📅 Até:"));      filtros.add(dtFim);
        filtros.add(rotulo("💼 Vendedor:")); filtros.add(comboFiltroVend);
        JButton btnBuscar = botao("🔍  BUSCAR", ACCENT_GREEN);
        btnBuscar.addActionListener(e -> buscar());
        filtros.add(btnBuscar);

        JSeparator sep = new JSeparator(); sep.setForeground(BORDER_COLOR);
        cab.add(titRow, BorderLayout.NORTH);
        cab.add(filtros, BorderLayout.CENTER);
        cab.add(sep, BorderLayout.SOUTH);
        return cab;
    }

    private void buscar() {
        carregarLancamentos();
        if (abas != null && abas.getSelectedIndex() == 1) carregarResumo();
    }

    private String lojaAtual() { return (String) comboLoja.getSelectedItem(); }

    private String vendedorFiltro() {
        Vendedor v = (Vendedor) comboFiltroVend.getSelectedItem();
        return v == null ? null : v.id;
    }

    private void carregarVendedores() {
        nomes.clear();
        comboFiltroVend.removeAllItems();
        comboVendForm.removeAllItems();
        Vendedor todos = new Vendedor(); todos.id = null; todos.nome = "— TODOS OS VENDEDORES —";
        comboFiltroVend.addItem(todos);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT idpessoa, username FROM public.usuarios "
                             + "WHERE cdempresa = ? AND tipo = 'VENDEDOR' AND ativo = true ORDER BY username")) {
            ps.setString(1, lojaAtual());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("idpessoa"), nome = rs.getString("username");
                nomes.put(id, nome);
                comboFiltroVend.addItem(vend(id, nome + "  (" + id + ")"));
                comboVendForm.addItem(vend(id, nome + "  (" + id + ")"));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar vendedores:\n" + e.getMessage());
        }
    }

    private static Vendedor vend(String id, String nome) {
        Vendedor v = new Vendedor(); v.id = id; v.nome = nome; return v;
    }

    private String nomeDe(String id) { return nomes.getOrDefault(id, id); }


    private JPanel criarAbaLancamentos() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_PANEL); p.setBorder(new EmptyBorder(12, 16, 12, 16));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, criarPainelImportacao(), criarPainelTabela());
        split.setDividerLocation(340); split.setDividerSize(6);
        split.setBorder(null); split.setBackground(BG_PANEL);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarPainelImportacao() {
        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBackground(BG_PANEL);
        painel.setPreferredSize(new Dimension(330, 0));

        JLabel tit = new JLabel("📋  Importar (colar do Excel ou abrir .xlsx)");
        tit.setFont(new Font("SansSerif", Font.BOLD, 11)); tit.setForeground(ACCENT_BLUE);
        JLabel dica = new JLabel("<html><font color='#7887a5'><i>Colunas: quantidade_produtos, idpessoa, cdempresa,<br>"
                + "valor_vendido, data_venda, clientes (pode colar com o cabeçalho).<br>"
                + "Cada vendedor/loja/dia importado <b>substitui</b> os lançamentos<br>"
                + "que já existiam naquele dia.</i></font></html>");
        dica.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JPanel topo = new JPanel(new BorderLayout(0, 3));
        topo.setBackground(BG_PANEL); topo.add(tit, BorderLayout.NORTH); topo.add(dica, BorderLayout.SOUTH);

        areaCola = new JTextArea();
        areaCola.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaCola.setForeground(TEXT_PRIMARY); areaCola.setBackground(BG_CARD);
        areaCola.setCaretColor(ACCENT_GREEN); areaCola.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(areaCola);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        JPanel bots = new JPanel(new GridLayout(1, 3, 6, 0)); bots.setBackground(BG_PANEL);
        JButton btnArquivo  = botao("📂 ABRIR", ACCENT_BLUE);
        JButton btnImportar = botao("⬇️ IMPORTAR", ACCENT_GREEN);
        JButton btnLimpar   = botao("🗑️ LIMPAR", new Color(50, 60, 90));
        bots.add(btnArquivo); bots.add(btnImportar); bots.add(btnLimpar);
        btnArquivo.addActionListener(e -> importarArquivo());
        btnImportar.addActionListener(e -> importarTextoColado());
        btnLimpar.addActionListener(e -> areaCola.setText(""));

        painel.add(topo, BorderLayout.NORTH);
        painel.add(scroll, BorderLayout.CENTER);
        painel.add(bots, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarPainelTabela() {
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBackground(BG_PANEL);


        estilizarCampo(campoData); estilizarCampo(campoQtd); estilizarCampo(campoValor); estilizarCampo(campoClientes);
        campoClientes.setToolTipText("Clientes do vendedor no dia. Informe em UMA linha do dia e deixe 0 nas demais.");
        estilizarComboVend(comboVendForm, 230);
        lblModo.setFont(new Font("SansSerif", Font.BOLD, 11));

        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); linha1.setOpaque(false);
        linha1.add(lblModo);
        linha1.add(rotulo("📅 Data:"));     linha1.add(campoData);
        linha1.add(rotulo("💼 Vendedor:")); linha1.add(comboVendForm);

        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); linha2.setOpaque(false);
        linha2.add(rotulo("📦 Qtd:"));        linha2.add(campoQtd);
        linha2.add(rotulo("💰 Valor (R$):")); linha2.add(campoValor);
        linha2.add(rotulo("👥 Clientes:"));   linha2.add(campoClientes);
        JButton btnNovo    = botao("➕ NOVO",    new Color(50, 60, 90));
        JButton btnSalvar  = botao("💾 SALVAR",  ACCENT_GREEN);
        JButton btnExcluir = botao("🗑️ EXCLUIR", ACCENT_RED);
        JButton btnExcluirFiltro = botao("↩️ VOLTAR P/ ALTERDATA", ACCENT_ORANGE);
        btnExcluirFiltro.setToolTipText("Apaga todos os lançamentos do filtro atual (loja, período e vendedor). "
                + "Esses dias voltam a usar a venda do Alterdata.");
        linha2.add(btnNovo); linha2.add(btnSalvar); linha2.add(btnExcluir);

        JPanel form = new JPanel(new GridLayout(2, 1, 0, 6));
        form.setBackground(BG_CARD);
        form.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(8, 10, 8, 10)));
        form.add(linha1); form.add(linha2);

        btnNovo.addActionListener(e -> { tabelaLanc.clearSelection(); limparFormulario(); });
        btnSalvar.addActionListener(e -> salvarLancamento());
        btnExcluir.addActionListener(e -> excluirSelecionados());
        btnExcluirFiltro.addActionListener(e -> excluirFiltro());


        modelLanc = new DefaultTableModel(new String[]{"ID", "Data", "Loja", "Vendedor", "ID Pessoa", "Qtd", "Valor", "Clientes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) { case 0, 5, 7 -> Integer.class; case 6 -> Double.class; case 1 -> LocalDate.class; default -> String.class; };
            }
        };
        tabelaLanc = estilizarTabela(new JTable(modelLanc));
        tabelaLanc.setRowSorter(new TableRowSorter<>(modelLanc));
        tabelaLanc.getColumnModel().getColumn(0).setMaxWidth(60);
        tabelaLanc.getColumnModel().getColumn(2).setMaxWidth(60);
        tabelaLanc.getColumnModel().getColumn(5).setMaxWidth(60);
        tabelaLanc.getColumnModel().getColumn(7).setMaxWidth(70);
        tabelaLanc.setDefaultRenderer(LocalDate.class, rendererData());
        tabelaLanc.setDefaultRenderer(Double.class, rendererMoeda());
        tabelaLanc.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            if (tabelaLanc.getSelectedRowCount() == 1) preencherFormulario(tabelaLanc.convertRowIndexToModel(tabelaLanc.getSelectedRow()));
        });

        JScrollPane scroll = new JScrollPane(tabelaLanc);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(BG_CARD);

        lblTotalLanc.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTotalLanc.setForeground(ACCENT_GREEN);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(lblTotalLanc, BorderLayout.CENTER);
        rodape.add(btnExcluirFiltro, BorderLayout.EAST);

        painel.add(form, BorderLayout.NORTH);
        painel.add(scroll, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private void carregarLancamentos() {
        if (modelLanc == null) return;
        modelLanc.setRowCount(0);
        LocalDate ini = lerData(dtIni), fim = lerData(dtFim);
        if (ini == null || fim == null) { lblTotalLanc.setText("Informe o período."); return; }
        String vend = vendedorFiltro();
        String sql = "SELECT id, data_venda, cdempresa, idpessoa, quantidade_produtos, valor_vendido, clientes "
                + "FROM public.vendas_auxiliar WHERE cdempresa = ? AND data_venda BETWEEN ? AND ?"
                + (vend != null ? " AND idpessoa = ?" : "")
                + " ORDER BY data_venda, idpessoa, id";
        double total = 0; int qtd = 0, cli = 0;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lojaAtual()); ps.setDate(2, java.sql.Date.valueOf(ini)); ps.setDate(3, java.sql.Date.valueOf(fim));
            if (vend != null) ps.setString(4, vend);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double valor = rs.getDouble("valor_vendido");
                int q = rs.getInt("quantidade_produtos");
                int c = rs.getInt("clientes");
                total += valor; qtd += q; cli += c;
                modelLanc.addRow(new Object[]{
                        rs.getInt("id"), rs.getDate("data_venda").toLocalDate(), rs.getString("cdempresa"),
                        nomeDe(rs.getString("idpessoa")), rs.getString("idpessoa"), q, valor, c});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar lançamentos:\n" + e.getMessage());
        }
        lblTotalLanc.setText(modelLanc.getRowCount() + " lançamento(s)   •   Qtd: " + qtd
                + "   •   Clientes: " + cli + "   •   Total: R$ " + df.format(total));
    }

    private void preencherFormulario(int row) {
        idEmEdicao = (Integer) modelLanc.getValueAt(row, 0);
        campoData.setText(((LocalDate) modelLanc.getValueAt(row, 1)).format(BR));
        selecionarVendedor(comboVendForm, (String) modelLanc.getValueAt(row, 4));
        campoQtd.setText(String.valueOf(modelLanc.getValueAt(row, 5)));
        campoValor.setText(df.format((Double) modelLanc.getValueAt(row, 6)));
        campoClientes.setText(String.valueOf(modelLanc.getValueAt(row, 7)));
        lblModo.setText("✏️ Editando #" + idEmEdicao);
        lblModo.setForeground(ACCENT_ORANGE);
    }

    private void limparFormulario() {
        idEmEdicao = null;
        campoData.setValue(null);
        campoData.setText(LocalDate.now().format(BR));
        Vendedor f = (Vendedor) comboFiltroVend.getSelectedItem();
        if (f != null && f.id != null) selecionarVendedor(comboVendForm, f.id);
        campoQtd.setText("1");
        campoValor.setText("0,00");
        campoClientes.setText("0");
        lblModo.setText("➕ Novo lançamento");
        lblModo.setForeground(ACCENT_GREEN);
    }

    private void selecionarVendedor(JComboBox<Vendedor> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (Objects.equals(combo.getItemAt(i).id, id)) { combo.setSelectedIndex(i); return; }
        }

        Vendedor v = vend(id, id + "  (fora da lista de ativos)");
        combo.addItem(v); combo.setSelectedItem(v);
    }

    private void salvarLancamento() {
        LocalDate data = lerData(campoData);
        if (data == null) { JOptionPane.showMessageDialog(this, "Informe uma data válida."); return; }
        Vendedor v = (Vendedor) comboVendForm.getSelectedItem();
        if (v == null || v.id == null) { JOptionPane.showMessageDialog(this, "Selecione o vendedor."); return; }
        Integer qtd, clientes; Double valor;
        try {
            qtd = (int) Math.round(numero(campoQtd.getText()));
            valor = numero(campoValor.getText());
            clientes = (int) Math.round(numero(campoClientes.getText()));
            if (clientes < 0) throw new Exception();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Quantidade, valor ou clientes inválido."); return; }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement ps;
            if (idEmEdicao == null) {
                ps = conn.prepareStatement("INSERT INTO public.vendas_auxiliar "
                        + "(idpessoa, cdempresa, data_venda, quantidade_produtos, valor_vendido, clientes) VALUES (?,?,?,?,?,?)");
            } else {
                ps = conn.prepareStatement("UPDATE public.vendas_auxiliar SET idpessoa=?, cdempresa=?, data_venda=?, "
                        + "quantidade_produtos=?, valor_vendido=?, clientes=?, alterado_em=CURRENT_TIMESTAMP WHERE id=?");
                ps.setInt(7, idEmEdicao);
            }
            ps.setString(1, v.id); ps.setString(2, lojaAtual()); ps.setDate(3, java.sql.Date.valueOf(data));
            ps.setInt(4, qtd); ps.setDouble(5, valor); ps.setInt(6, clientes);
            ps.executeUpdate(); ps.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar:\n" + ex.getMessage()); return;
        }
        limparFormulario();
        buscar();
    }

    private void excluirSelecionados() {
        int[] rows = tabelaLanc.getSelectedRows();
        if (rows.length == 0) { JOptionPane.showMessageDialog(this, "Selecione um ou mais lançamentos."); return; }
        if (JOptionPane.showConfirmDialog(this, "Excluir " + rows.length + " lançamento(s)?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM public.vendas_auxiliar WHERE id = ?")) {
            for (int r : rows) {
                ps.setInt(1, (Integer) modelLanc.getValueAt(tabelaLanc.convertRowIndexToModel(r), 0));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + ex.getMessage()); }
        limparFormulario();
        buscar();
    }

    private void excluirFiltro() {
        LocalDate ini = lerData(dtIni), fim = lerData(dtFim);
        if (ini == null || fim == null) { JOptionPane.showMessageDialog(this, "Informe o período."); return; }
        String vend = vendedorFiltro();
        String quem = vend == null ? "TODOS os vendedores" : nomeDe(vend);
        String msg = "Apagar todos os lançamentos auxiliares de:\n\n"
                + "Loja: " + lojaAtual() + "\nPeríodo: " + ini.format(BR) + " a " + fim.format(BR)
                + "\nVendedor: " + quem + "\n\nEsses dias voltarão a usar a venda do Alterdata.";
        if (JOptionPane.showConfirmDialog(this, msg, "Voltar para o Alterdata",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        String sql = "DELETE FROM public.vendas_auxiliar WHERE cdempresa = ? AND data_venda BETWEEN ? AND ?"
                + (vend != null ? " AND idpessoa = ?" : "");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lojaAtual()); ps.setDate(2, java.sql.Date.valueOf(ini)); ps.setDate(3, java.sql.Date.valueOf(fim));
            if (vend != null) ps.setString(4, vend);
            int n = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, n + " lançamento(s) apagado(s).");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + ex.getMessage()); }
        limparFormulario();
        buscar();
    }


    private void importarArquivo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Planilha Excel (*.xlsx)", "xlsx"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            importar(XlsxLeitor.lerPrimeiraPlanilha(f), f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível ler o arquivo:\n" + ex.getMessage());
        }
    }

    private void importarTextoColado() {
        String texto = areaCola.getText();
        if (texto.isBlank()) { JOptionPane.showMessageDialog(this, "Cole os dados primeiro."); return; }
        List<List<String>> linhas = new ArrayList<>();
        for (String l : texto.split("\\r?\\n")) {
            if (l.isBlank()) continue;
            String[] partes = l.contains("\t") ? l.split("\t", -1) : l.trim().split(";|\\s{2,}|\\s+");
            linhas.add(new ArrayList<>(Arrays.asList(partes)));
        }
        if (importar(linhas, "texto colado")) areaCola.setText("");
    }

    private record Linha(String idpessoa, String loja, LocalDate data, int qtd, double valor, int clientes) {}


    private boolean importar(List<List<String>> linhas, String origem) {
        if (linhas.isEmpty()) { JOptionPane.showMessageDialog(this, "Nenhuma linha encontrada."); return false; }


        Map<String, Integer> col = new HashMap<>();
        List<String> primeira = linhas.get(0);
        for (int i = 0; i < primeira.size(); i++) {
            String h = primeira.get(i).trim().toLowerCase();
            for (String nome : COLUNAS_PADRAO) if (h.equals(nome)) col.put(nome, i);
        }
        int inicio = 0;
        boolean temObrigatorias = Arrays.stream(COLUNAS_OBRIGATORIAS).allMatch(col::containsKey);
        if (temObrigatorias) inicio = 1;
        else if (!col.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cabeçalho incompleto. Colunas esperadas:\n" + String.join(", ", COLUNAS_PADRAO));
            return false;
        } else for (int i = 0; i < COLUNAS_PADRAO.length; i++) col.put(COLUNAS_PADRAO[i], i);

        if (inicio == 0 && primeira.size() < COLUNAS_PADRAO.length) col.remove("clientes");
        boolean temClientes = col.containsKey("clientes");

        List<Linha> ok = new ArrayList<>();
        StringBuilder erros = new StringBuilder(); int nErros = 0;
        for (int n = inicio; n < linhas.size(); n++) {
            List<String> l = linhas.get(n);
            if (l.stream().allMatch(String::isBlank)) continue;
            try {
                String id   = celula(l, col.get("idpessoa")).trim();
                String loja = normalizarLoja(celula(l, col.get("cdempresa")));
                LocalDate d = parseDataFlex(celula(l, col.get("data_venda")));
                int qtd     = (int) Math.round(numero(celula(l, col.get("quantidade_produtos"))));
                double val  = Math.round(numero(celula(l, col.get("valor_vendido"))) * 100.0) / 100.0;
                int cli     = 0;
                if (temClientes) {
                    String c = col.get("clientes") < l.size() ? l.get(col.get("clientes")) : "";
                    cli = c == null || c.isBlank() ? 0 : (int) Math.round(numero(c));
                    if (cli < 0) throw new Exception("clientes negativo");
                }
                if (id.isEmpty()) throw new Exception("idpessoa vazio");
                if (lojaFixa != null && !lojaFixa.equals(loja)) throw new Exception("loja " + loja + " diferente da sua loja (" + lojaFixa + ")");
                ok.add(new Linha(id, loja, d, qtd, val, cli));
            } catch (Exception ex) {
                nErros++;
                if (nErros <= 15) erros.append("• linha ").append(n + 1).append(": ").append(ex.getMessage()).append("\n");
            }
        }
        if (ok.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma linha válida.\n\n" + erros); return false;
        }


        Set<String> grupos = new TreeSet<>();
        Set<String> lojas = new TreeSet<>();
        LocalDate min = ok.get(0).data(), max = min;
        double total = 0; int totalCli = 0;
        for (Linha x : ok) {
            totalCli += x.clientes();
            grupos.add(x.loja() + "|" + x.idpessoa() + "|" + x.data());
            lojas.add(x.loja());
            if (x.data().isBefore(min)) min = x.data();
            if (x.data().isAfter(max)) max = x.data();
            total += x.valor();
        }
        Set<String> desconhecidos = vendedoresDesconhecidos(ok);

        StringBuilder resumo = new StringBuilder();
        resumo.append("Origem: ").append(origem).append("\n\n")
              .append(ok.size()).append(" linha(s) válida(s)   •   Total: R$ ").append(df.format(total))
              .append("   •   Clientes: ").append(totalCli).append("\n")
              .append("Loja(s): ").append(String.join(", ", lojas)).append("\n")
              .append("Período: ").append(min.format(BR)).append(" a ").append(max.format(BR)).append("\n")
              .append(grupos.size()).append(" combinação(ões) vendedor/dia serão SUBSTITUÍDAS\n")
              .append("(os lançamentos auxiliares que já existirem nesses dias serão apagados antes).\n");
        if (!temClientes)
            resumo.append("\n⚠️ Coluna \"clientes\" não encontrada: os clientes desses dias ficarão 0\n"
                    + "(o ticket médio e prod/cliente desses dias ficarão zerados no Dashboard).\n");
        if (!desconhecidos.isEmpty())
            resumo.append("\n⚠️ Vendedores não encontrados no cadastro de usuários: ")
                  .append(String.join(", ", desconhecidos)).append("\n");
        if (nErros > 0)
            resumo.append("\n⚠️ ").append(nErros).append(" linha(s) ignorada(s):\n").append(erros)
                  .append(nErros > 15 ? "…\n" : "");
        resumo.append("\nConfirmar importação?");

        JTextArea txt = new JTextArea(resumo.toString(), Math.min(22, resumo.toString().split("\n").length + 1), 60);
        txt.setEditable(false);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(txt), "Importar vendas auxiliares",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(
                         "DELETE FROM public.vendas_auxiliar WHERE cdempresa=? AND idpessoa=? AND data_venda=?");
                 PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO public.vendas_auxiliar (idpessoa, cdempresa, data_venda, quantidade_produtos, valor_vendido, clientes) "
                                 + "VALUES (?,?,?,?,?,?)")) {
                for (String g : grupos) {
                    String[] k = g.split("\\|");
                    del.setString(1, k[0]); del.setString(2, k[1]); del.setDate(3, java.sql.Date.valueOf(k[2]));
                    del.addBatch();
                }
                del.executeBatch();
                for (Linha x : ok) {
                    ins.setString(1, x.idpessoa()); ins.setString(2, x.loja()); ins.setDate(3, java.sql.Date.valueOf(x.data()));
                    ins.setInt(4, x.qtd()); ins.setDouble(5, x.valor()); ins.setInt(6, x.clientes());
                    ins.addBatch();
                }
                ins.executeBatch();
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro na importação (nada foi gravado):\n" + ex.getMessage());
            return false;
        }
        JOptionPane.showMessageDialog(this, "✅ " + ok.size() + " linha(s) importada(s).");


        if (lojaFixa == null && lojas.size() == 1) comboLoja.setSelectedItem(lojas.iterator().next());
        dtIni.setValue(null); dtIni.setText(min.format(BR));
        dtFim.setValue(null); dtFim.setText(max.format(BR));
        comboFiltroVend.setSelectedIndex(0);
        buscar();
        return true;
    }

    private Set<String> vendedoresDesconhecidos(List<Linha> linhas) {
        Set<String> ids = new TreeSet<>();
        for (Linha x : linhas) ids.add(x.idpessoa());
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT idpessoa FROM public.usuarios WHERE idpessoa = ANY (?)")) {
            ps.setArray(1, conn.createArrayOf("varchar", ids.toArray()));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.remove(rs.getString(1));
        } catch (Exception ignored) { return Collections.emptySet(); }
        return ids;
    }

    private static String celula(List<String> l, Integer idx) throws Exception {
        if (idx == null || idx >= l.size()) throw new Exception("coluna faltando");
        return l.get(idx) == null ? "" : l.get(idx);
    }


    private JPanel criarAbaResumo() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_PANEL); p.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel info = new JLabel("<html><font color='#7887a5'>Por vendedor e dia do filtro: venda do Alterdata, venda auxiliar e "
                + "qual delas o Dashboard está usando.</font></html>");
        info.setFont(FONT_LABEL);

        modelResumo = new DefaultTableModel(new String[]{"Data", "Vendedor", "ID Pessoa",
                "Alterdata (R$)", "Auxiliar (R$)", "Usado no Dashboard (R$)", "Fonte"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return switch (c) { case 0 -> LocalDate.class; case 3, 4, 5 -> Double.class; default -> String.class; };
            }
        };
        JTable t = estilizarTabela(new JTable(modelResumo));
        t.setRowSorter(new TableRowSorter<>(modelResumo));
        t.setDefaultRenderer(LocalDate.class, rendererData());
        t.setDefaultRenderer(Double.class, rendererMoeda());
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, s, f, r, c);
                comp.setForeground("AUXILIAR".equals(v) ? ACCENT_ORANGE : ACCENT_GREEN);
                setFont(FONT_BTN);
                return comp;
            }
        });
        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(BG_CARD);

        lblTotalResumo.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTotalResumo.setForeground(ACCENT_GREEN);

        p.add(info, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(lblTotalResumo, BorderLayout.SOUTH);
        return p;
    }

    private void carregarResumo() {
        modelResumo.setRowCount(0);
        LocalDate ini = lerData(dtIni), fim = lerData(dtFim);
        if (ini == null || fim == null) { lblTotalResumo.setText("Informe o período."); return; }
        String loja = lojaAtual(), vend = vendedorFiltro();


        Map<String, Double> sistema = new TreeMap<>(), auxiliar = new TreeMap<>();
        Map<String, String> nomesAlterdata = new HashMap<>();

        String sqlSis = """
            SELECT p.idpessoa, p.nmpessoa, CAST(d.dtreferencia AS date) AS dia, SUM(ci.vlbasecomissao) AS venda
            FROM ishop.comitem ci
            JOIN ishop.documen d ON d.iddocumento = ci.iddocumento
            JOIN ishop.pessoas p ON p.idpessoa = ci.idpessoa
            WHERE d.dtreferencia BETWEEN ? AND ?
              AND COALESCE(d.stdocumentocancelado, '') <> '*'
              AND ci.cdempvend = ?
            """ + (vend != null ? " AND p.idpessoa = ? " : "") + """
            GROUP BY p.idpessoa, p.nmpessoa, CAST(d.dtreferencia AS date)
            """;
        boolean alterdataOk = true;
        try (Connection conn = DriverManager.getConnection(URL_ISHOP, USER_ISHOP, PASS_ISHOP);
             PreparedStatement ps = conn.prepareStatement(sqlSis)) {
            ps.setDate(1, java.sql.Date.valueOf(ini)); ps.setDate(2, java.sql.Date.valueOf(fim)); ps.setString(3, loja);
            if (vend != null) ps.setString(4, vend);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String k = rs.getString("idpessoa") + "|" + rs.getDate("dia").toLocalDate();
                sistema.merge(k, rs.getDouble("venda"), Double::sum);
                nomesAlterdata.put(rs.getString("idpessoa"), rs.getString("nmpessoa"));
            }
        } catch (Exception e) { alterdataOk = false; }

        String sqlAux = "SELECT idpessoa, data_venda, SUM(valor_vendido) AS venda FROM public.vendas_auxiliar "
                + "WHERE cdempresa = ? AND data_venda BETWEEN ? AND ?" + (vend != null ? " AND idpessoa = ?" : "")
                + " GROUP BY idpessoa, data_venda";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sqlAux)) {
            ps.setString(1, loja); ps.setDate(2, java.sql.Date.valueOf(ini)); ps.setDate(3, java.sql.Date.valueOf(fim));
            if (vend != null) ps.setString(4, vend);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) auxiliar.put(rs.getString("idpessoa") + "|" + rs.getDate("data_venda").toLocalDate(), rs.getDouble("venda"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao ler vendas auxiliares:\n" + e.getMessage());
        }

        Set<String> chaves = new TreeSet<>(sistema.keySet());
        chaves.addAll(auxiliar.keySet());
        double tSis = 0, tAux = 0, tUsado = 0; int diasAux = 0;
        for (String k : chaves) {
            String[] p = k.split("\\|");
            Double s = sistema.get(k), a = auxiliar.get(k);
            double usado = a != null ? a : (s != null ? s : 0);
            String nome = nomes.containsKey(p[0]) ? nomes.get(p[0]) : nomesAlterdata.getOrDefault(p[0], p[0]);
            modelResumo.addRow(new Object[]{LocalDate.parse(p[1]), nome, p[0], s, a, usado, a != null ? "AUXILIAR" : "ALTERDATA"});
            if (s != null) tSis += s;
            if (a != null) { tAux += a; diasAux++; }
            tUsado += usado;
        }
        lblTotalResumo.setText((alterdataOk ? "" : "⚠️ Alterdata indisponível   •   ")
                + "Alterdata: R$ " + df.format(tSis) + "   •   Auxiliar: R$ " + df.format(tAux)
                + " (" + diasAux + " vendedor/dia)   •   Usado no Dashboard: R$ " + df.format(tUsado));
    }


    static double numero(String s) {
        String t = s == null ? "" : s.replace("R$", "").replace("\u00A0", "").replace(" ", "").trim();
        if (t.isEmpty()) return 0;
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
        return Double.parseDouble(t);
    }


    static String normalizarLoja(String s) throws Exception {
        String t = s == null ? "" : s.trim();
        if (t.endsWith(".0")) t = t.substring(0, t.length() - 2);
        if (t.isEmpty()) throw new Exception("cdempresa vazio");
        if (t.matches("\\d+") && t.length() < 3) t = "0".repeat(3 - t.length()) + t;
        return t;
    }


    static LocalDate parseDataFlex(String s) throws Exception {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new Exception("data vazia");
        if (t.matches("\\d+(\\.\\d+)?")) {
            double serial = Double.parseDouble(t);
            if (serial < 1 || serial > 100000) throw new Exception("data inválida: " + s);
            return LocalDate.of(1899, 12, 30).plusDays((long) Math.floor(serial));
        }
        t = t.split("[ T]")[0];
        for (String padrao : new String[]{"dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "yyyy-MM-dd", "dd-MM-yyyy"}) {
            try { return LocalDate.parse(t, DateTimeFormatter.ofPattern(padrao)); } catch (Exception ignored) {}
        }
        throw new Exception("data inválida: " + s);
    }

    private static LocalDate lerData(JFormattedTextField f) {
        try { return LocalDate.parse(f.getText().trim(), BR); } catch (Exception e) { return null; }
    }


    private DefaultTableCellRenderer rendererData() {
        return new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v == null ? "" : ((LocalDate) v).format(BR)); }
        };
    }

    private DefaultTableCellRenderer rendererMoeda() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v == null ? "—" : "R$ " + df.format((Double) v)); }
        };
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        return r;
    }

    private JLabel rotulo(String t) { JLabel l = new JLabel(t); l.setFont(FONT_LABEL); l.setForeground(TEXT_SECONDARY); return l; }

    private JTextField estilizarCampo(JTextField f) {
        f.setFont(FONT_INPUT); f.setForeground(TEXT_PRIMARY); f.setBackground(BG_CARD); f.setCaretColor(ACCENT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private JComboBox<String> estilizarCombo(JComboBox<String> c) {
        c.setFont(FONT_INPUT); c.setForeground(TEXT_PRIMARY); c.setBackground(BG_CARD);
        c.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1)); c.setPreferredSize(new Dimension(90, 30));
        return c;
    }

    private void estilizarComboVend(JComboBox<Vendedor> c, int largura) {
        c.setFont(FONT_INPUT); c.setForeground(TEXT_PRIMARY); c.setBackground(BG_CARD);
        c.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1)); c.setPreferredSize(new Dimension(largura, 30));
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
        b.setBorderPainted(false); b.setFocusPainted(false); b.setBorder(new EmptyBorder(8, 12, 8, 12));
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

    private static JFormattedTextField criarCampoData(String valor) {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####"); mask.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mask); f.setColumns(10);
            if (!valor.isEmpty()) f.setText(valor);
            return f;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
