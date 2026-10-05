import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class CadastroMetas extends JFrame {

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
    private static final Color ACCENT_PURPLE  = new Color(200, 80, 255);
    private static final Color ACCENT_GOLD    = new Color(255, 200, 60);
    private static final Color ACCENT_TEAL    = new Color(0, 210, 200);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_INPUT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BTN   = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_TABLE = new Font("SansSerif", Font.PLAIN, 11);

    private final String URL  = Config.getVendedoresUrl();
    private final String USER = Config.getVendedoresUser();
    private final String PASS = Config.getVendedoresPassword();

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    private final String tipoUsuario;
    private final String lojaUsuario;

    private JComboBox<String> comboLoja      = new JComboBox<>(new String[]{"001","002","003"});
    private JComboBox<String> comboCategoria = new JComboBox<>(new String[]{"ADULTO","INFANTIL"});

    private JTextArea         areaCola;
    private DefaultTableModel modelVenda;
    private JTable            tabelaVenda;

    private JComboBox<String> comboMes = new JComboBox<>(
            new String[]{"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"});
    private JSpinner spinnerAno;
    private JTextField fTicket   = new JTextField("0,00", 10);
    private JTextField fClientes = new JTextField("0", 8);
    private JTextField fProdutos = new JTextField("0", 8);
    private JTextField fProdCli  = new JTextField("0,00", 8);
    private JTextField fMargem   = new JTextField("0,00", 8);

    private JTabbedPane abasPrincipais = null; 
    private JSpinner spinnerAdulto    = new JSpinner(new SpinnerNumberModel(0,0,200,1));
    private JSpinner spinnerInfantil  = new JSpinner(new SpinnerNumberModel(0,0,200,1));
    private JLabel   lblAtualAdulto   = new JLabel("—");
    private JLabel   lblAtualInfantil = new JLabel("—");

    
    public CadastroMetas(String tipo, String loja) {
        this.tipoUsuario = tipo != null ? tipo : "SUPERVISOR";
        this.lojaUsuario = loja;
        setTitle("📋  CADASTRO DE METAS — " + tipoUsuario);
        setSize(980, 700);
        setLocationRelativeTo(null);
        if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) setExtendedState(JFrame.MAXIMIZED_BOTH);
        else setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        
        if (lojaUsuario != null) {
            comboLoja.setSelectedItem(lojaUsuario);
            comboLoja.setEnabled(false);
        }
        
        add(criarCabecalhoSemListeners(), BorderLayout.NORTH);
        add(criarAbas(),                 BorderLayout.CENTER);
        
        comboLoja.addActionListener(e -> recarregarTudo());
        comboCategoria.addActionListener(e -> recarregarTudo());
        
        if ("SUPERVISOR".equalsIgnoreCase(tipoUsuario)) {
            adicionarAbaDashboard();
        }
    }

    public CadastroMetas() { this("SUPERVISOR", null); }

    
    private JPanel criarCabecalhoSemListeners() {
        JPanel cab = new JPanel(new BorderLayout(0,8));
        cab.setBackground(BG_DARK);
        cab.setBorder(new EmptyBorder(14,16,10,16));

        JPanel titRow = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        titRow.setBackground(BG_DARK);
        JLabel t = new JLabel("📋  CADASTRO DE METAS"); t.setFont(FONT_TITLE); t.setForeground(TEXT_PRIMARY);
        JLabel s = new JLabel("  /  " + tipoUsuario.charAt(0) + tipoUsuario.substring(1).toLowerCase());
        s.setFont(new Font("SansSerif",Font.PLAIN,12)); s.setForeground(TEXT_SECONDARY);
        titRow.add(t); titRow.add(s);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        filtros.setBackground(BG_DARK);
        filtros.add(rotulo("🏪 Loja:")); filtros.add(estilizarCombo(comboLoja));
        filtros.add(rotulo("👥 Categoria:")); filtros.add(estilizarCombo(comboCategoria));
        JSeparator sep = new JSeparator(); sep.setForeground(BORDER_COLOR);
        JButton btnVendasAux = botao("🧾  VENDAS AUXILIARES", ACCENT_ORANGE);
        btnVendasAux.setToolTipText("Lançar/alterar vendas que substituem a comissão do Alterdata no Dashboard");
        btnVendasAux.addActionListener(e -> new VendasAuxiliares(lojaUsuario).setVisible(true));
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(BG_DARK);
        topo.add(titRow, BorderLayout.WEST);
        JButton btnCadVendedor = botao("👤  CADASTRAR VENDEDOR", ACCENT_BLUE);
        btnCadVendedor.addActionListener(e -> new CadastroVendedor(lojaUsuario).setVisible(true));
        JPanel botoesTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoesTopo.setBackground(BG_DARK);
        botoesTopo.add(btnCadVendedor);
        botoesTopo.add(btnVendasAux);
        topo.add(botoesTopo, BorderLayout.EAST);

        cab.add(topo, BorderLayout.NORTH);
        cab.add(filtros, BorderLayout.CENTER);
        cab.add(sep, BorderLayout.SOUTH);
        return cab;
    }

    
    private JTabbedPane criarAbas() {
        JTabbedPane abas = new JTabbedPane();
        abas.setBackground(BG_PANEL); abas.setForeground(TEXT_PRIMARY); abas.setFont(FONT_BTN);
        abas.addTab("💰  Venda Diária",    criarAbaVenda());
        abas.addTab("📊  Indicadores",     criarAbaIndicadores());
        abas.addTab("👥  Qtd. Vendedores", criarAbaVendedores());
        abas.addChangeListener(e -> {
            switch (abas.getSelectedIndex()) {
                case 0 -> carregarTabelaVenda();
                case 1 -> carregarIndicadores();
                case 2 -> carregarQuantidades();
                case 3 -> atualizarAbaDashboard();
            }
        });
        abasPrincipais = abas;
        return abas;
    }

    private void recarregarTudo() { carregarTabelaVenda(); carregarIndicadores(); carregarQuantidades(); }

    
    private Dashboard dashboardEmbutido = null;

    private void adicionarAbaDashboard() {
        if (abasPrincipais == null) return;
        
        JPanel abaHost = new JPanel(new BorderLayout());
        abaHost.setBackground(BG_DARK);
        abasPrincipais.addTab("📊  Dashboard", abaHost);
    }

    private void atualizarAbaDashboard() {
        if (abasPrincipais == null) return;
        int idx = abasPrincipais.getTabCount() - 1;
        JPanel abaHost = (JPanel) abasPrincipais.getComponentAt(idx);
        abaHost.removeAll();

        String loja = (String) comboLoja.getSelectedItem();

        
        JPanel dashContent = criarConteudoDashboard(loja);
        abaHost.add(dashContent, BorderLayout.CENTER);
        abaHost.revalidate();
        abaHost.repaint();
    }

    private JPanel criarConteudoDashboard(String loja) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        String hoje = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());
        JFormattedTextField dtIni = criarCampoData(hoje);
        JFormattedTextField dtFim = criarCampoData(hoje);
        estilizarCampo(dtIni); estilizarCampo(dtFim);

        
        JComboBox<Vendedor> comboVend = new JComboBox<>();
        comboVend.setFont(FONT_INPUT); comboVend.setForeground(TEXT_PRIMARY);
        comboVend.setBackground(BG_CARD); comboVend.setPreferredSize(new Dimension(220, 30));
        carregarVendedoresCombo(comboVend, loja);

        
        JComboBox<String> comboLojaD = new JComboBox<>(new String[]{"001","002","003"});
        comboLojaD.setSelectedItem(loja);
        estilizarCombo(comboLojaD);
        comboLojaD.addActionListener(e -> carregarVendedoresCombo(comboVend, (String) comboLojaD.getSelectedItem()));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setBackground(BG_DARK);
        filtros.add(rotulo("🏪 Loja:"));    filtros.add(comboLojaD);
        filtros.add(rotulo("📅 De:"));      filtros.add(dtIni);
        filtros.add(rotulo("📅 Até:"));     filtros.add(dtFim);
        filtros.add(rotulo("💼 Vendedor:")); filtros.add(comboVend);
        JButton btnBuscar = botao("🔍  BUSCAR", ACCENT_GREEN);
        filtros.add(btnBuscar);

        JPanel corpo = new JPanel(new BorderLayout(0, 10));
        corpo.setBackground(BG_DARK);

        
        Dashboard[] dashRef = new Dashboard[1];

        btnBuscar.addActionListener(e -> {
            corpo.removeAll();
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                sdf.setLenient(false);
                java.sql.Date ini = new java.sql.Date(sdf.parse(dtIni.getText()).getTime());
                java.sql.Date fim = new java.sql.Date(sdf.parse(dtFim.getText()).getTime());
                String lojaAtual = (String) comboLojaD.getSelectedItem();
                Vendedor sel = (Vendedor) comboVend.getSelectedItem();
                String idVend = (sel != null && sel.id != null) ? sel.id : null;

                if (dashRef[0] == null) {
                    dashRef[0] = new Dashboard(lojaAtual, idVend, "GERENTE", "SUPERVISOR", "ADULTO");
                }
                JPanel conteudo = dashRef[0].gerarPainelConteudo(ini, fim, lojaAtual, idVend);
                corpo.add(new JScrollPane(conteudo), BorderLayout.CENTER);
            } catch (Exception ex) {
                JLabel erro = new JLabel("Erro: " + ex.getMessage());
                erro.setForeground(ACCENT_RED);
                corpo.add(erro, BorderLayout.CENTER);
            }
            corpo.revalidate(); corpo.repaint();
        });

        p.add(filtros, BorderLayout.NORTH);
        p.add(corpo,   BorderLayout.CENTER);
        return p;
    }

    private void carregarVendedoresCombo(JComboBox<Vendedor> combo, String loja) {
        combo.removeAllItems();
        
        Vendedor todos = new Vendedor(); todos.id = null; todos.nome = "— TODOS OS VENDEDORES —";
        combo.addItem(todos);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT idpessoa, username AS nmpessoa FROM public.usuarios "
                             + "WHERE cdempresa=? AND tipo='VENDEDOR' AND ativo=true ORDER BY username")) {
            ps.setString(1, loja);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Vendedor v = new Vendedor();
                v.id   = rs.getString("idpessoa");
                v.nome = rs.getString("nmpessoa");
                combo.addItem(v);
            }
        } catch (Exception ignored) {}
    }

    
    private JPanel criarAbaVenda() {
        JPanel p = new JPanel(new BorderLayout(0,10));
        p.setBackground(BG_PANEL); p.setBorder(new EmptyBorder(12,16,12,16));

        
        JPanel painelCola = new JPanel(new BorderLayout(0,6));
        painelCola.setBackground(BG_PANEL); painelCola.setPreferredSize(new Dimension(320,0));

        JLabel lblTitCola = new JLabel("📋  Cole aqui os dados do Excel:");
        lblTitCola.setFont(new Font("SansSerif",Font.BOLD,11)); lblTitCola.setForeground(ACCENT_BLUE);
        JLabel lblDica = new JLabel("<html><font color='#7887a5'><i>Copie as colunas Data + Valor do Excel<br>Formato: 01/04/2026 [TAB] 1800,00</i></font></html>");
        lblDica.setFont(new Font("SansSerif",Font.PLAIN,10));

        JPanel topCola = new JPanel(new BorderLayout(0,3));
        topCola.setBackground(BG_PANEL); topCola.add(lblTitCola, BorderLayout.NORTH); topCola.add(lblDica, BorderLayout.SOUTH);

        areaCola = new JTextArea();
        areaCola.setFont(new Font("Monospaced",Font.PLAIN,11));
        areaCola.setForeground(TEXT_PRIMARY); areaCola.setBackground(BG_CARD);
        areaCola.setCaretColor(ACCENT_GREEN); areaCola.setBorder(new EmptyBorder(8,8,8,8));
        JScrollPane scrollCola = new JScrollPane(areaCola);
        scrollCola.setBorder(BorderFactory.createLineBorder(BORDER_COLOR,1));

        JPanel botsCola = new JPanel(new GridLayout(1,2,8,0)); botsCola.setBackground(BG_PANEL);
        JButton btnImportar = botao("⬇️  IMPORTAR", ACCENT_GREEN);
        JButton btnLimpar   = botao("🗑️  LIMPAR",   new Color(50,60,90));
        botsCola.add(btnImportar); botsCola.add(btnLimpar);
        btnImportar.addActionListener(e -> importarDoExcel());
        btnLimpar  .addActionListener(e -> areaCola.setText(""));

        painelCola.add(topCola,   BorderLayout.NORTH);
        painelCola.add(scrollCola, BorderLayout.CENTER);
        painelCola.add(botsCola,  BorderLayout.SOUTH);

        
        JPanel painelTabela = new JPanel(new BorderLayout(0,8));
        painelTabela.setBackground(BG_PANEL);

        JFormattedTextField campoData  = criarCampoData("");
        JTextField          campoValor = new JTextField("0,00", 10);
        estilizarCampo(campoData); estilizarCampo(campoValor);

        JPanel formInd = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        formInd.setBackground(BG_CARD);
        formInd.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR,1), new EmptyBorder(8,12,8,12)));
        formInd.add(rotulo("📅 Data:")); formInd.add(campoData);
        formInd.add(rotulo("💰 Meta (R$):")); formInd.add(campoValor);
        JButton btnSalvarInd  = botao("💾 SALVAR",  ACCENT_GREEN);
        JButton btnExcluirInd = botao("🗑️ EXCLUIR", ACCENT_RED);
        formInd.add(btnSalvarInd); formInd.add(btnExcluirInd);

        modelVenda = new DefaultTableModel(new String[]{"ID","Data","Valor Meta"},0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaVenda = estilizarTabela(new JTable(modelVenda));
        tabelaVenda.getColumnModel().getColumn(0).setMaxWidth(55);
        tabelaVenda.getColumnModel().getColumn(1).setPreferredWidth(120);
        tabelaVenda.getColumnModel().getColumn(2).setPreferredWidth(140);

        tabelaVenda.getSelectionModel().addListSelectionListener(e -> {
            int row = tabelaVenda.getSelectedRow();
            if (row >= 0) {
                campoData.setText((String) modelVenda.getValueAt(row,1));
                campoValor.setText(modelVenda.getValueAt(row,2).toString()
                        .replace("R$ ","").replace(".","").replace(",","."));
            }
        });
        btnSalvarInd .addActionListener(e -> salvarVendaIndividual(campoData, campoValor));
        btnExcluirInd.addActionListener(e -> excluirVendaSelecionada());

        JScrollPane scrollTab = new JScrollPane(tabelaVenda);
        scrollTab.setBorder(BorderFactory.createLineBorder(BORDER_COLOR,1));
        scrollTab.getViewport().setBackground(BG_CARD);

        painelTabela.add(formInd,   BorderLayout.NORTH);
        painelTabela.add(scrollTab, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelCola, painelTabela);
        split.setDividerLocation(330); split.setDividerSize(6);
        split.setBorder(null); split.setBackground(BG_PANEL);

        p.add(split, BorderLayout.CENTER);
        carregarTabelaVenda();
        return p;
    }

    private void importarDoExcel() {
        String texto = areaCola.getText().trim();
        if (texto.isEmpty()) { JOptionPane.showMessageDialog(this,"Cole os dados primeiro"); return; }
        String loja = (String) comboLoja.getSelectedItem();
        String cat  = (String) comboCategoria.getSelectedItem();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); sdf.setLenient(false);
        int ok=0, erro=0; StringBuilder erros = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(URL,USER,PASS)) {
            for (String linha : texto.split("\\n")) {
                linha = linha.trim(); if (linha.isEmpty()) continue;
                String[] p = linha.split("\\t|\\s{2,}");
                if (p.length < 2) { erro++; erros.append("• ").append(linha).append("\n"); continue; }
                try {
                    java.sql.Date data = new java.sql.Date(sdf.parse(p[0].trim()).getTime());
                    double valor = Double.parseDouble(p[1].trim().replace("R$","").replace(" ","").replace(".","").replace(",","."));
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO public.metas_venda (cdempresa,categoria,data,valor_meta) VALUES(?,?,?,?) "
                                    + "ON CONFLICT ON CONSTRAINT metas_venda_unique DO UPDATE SET valor_meta=EXCLUDED.valor_meta");
                    ps.setString(1,loja); ps.setString(2,cat); ps.setDate(3,data); ps.setDouble(4,valor);
                    ps.executeUpdate(); ps.close(); ok++;
                } catch (Exception ex) { erro++; erros.append("• ").append(linha).append(" → ").append(ex.getMessage()).append("\n"); }
            }
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Erro de conexão: "+ex.getMessage()); return; }
        String msg = "✅ " + ok + " linha(s) importada(s)";
        if (erro > 0) msg += "\n⚠️ " + erro + " linha(s) com erro:\n" + erros;
        JOptionPane.showMessageDialog(this, msg);
        areaCola.setText(""); carregarTabelaVenda();
    }

    private void salvarVendaIndividual(JFormattedTextField campoData, JTextField campoValor) {
        try {
            String dataStr = campoData.getText().trim();
            if (dataStr.contains("_")||dataStr.isEmpty()) { JOptionPane.showMessageDialog(this,"Informe a data"); return; }
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); sdf.setLenient(false);
            java.sql.Date data = new java.sql.Date(sdf.parse(dataStr).getTime());
            double valor = Double.parseDouble(campoValor.getText().trim().replace(".","").replace(",","."));
            String loja=(String)comboLoja.getSelectedItem(), cat=(String)comboCategoria.getSelectedItem();
            try (Connection conn = DriverManager.getConnection(URL,USER,PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO public.metas_venda (cdempresa,categoria,data,valor_meta) VALUES(?,?,?,?) "
                                 + "ON CONFLICT ON CONSTRAINT metas_venda_unique DO UPDATE SET valor_meta=EXCLUDED.valor_meta")) {
                ps.setString(1,loja); ps.setString(2,cat); ps.setDate(3,data); ps.setDouble(4,valor);
                ps.executeUpdate();
            }
            campoData.setText(""); campoValor.setText("0,00"); carregarTabelaVenda();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
    }

    private void excluirVendaSelecionada() {
        int row = tabelaVenda.getSelectedRow();
        if (row<0) { JOptionPane.showMessageDialog(this,"Selecione uma linha"); return; }
        int id = (int) modelVenda.getValueAt(row,0);
        if (JOptionPane.showConfirmDialog(this,"Excluir esta meta?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try (Connection conn = DriverManager.getConnection(URL,USER,PASS);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM public.metas_venda WHERE id=?")) {
            ps.setInt(1,id); ps.executeUpdate(); carregarTabelaVenda();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
    }

    private void carregarTabelaVenda() {
        if (modelVenda==null) return;
        modelVenda.setRowCount(0);
        String loja=(String)comboLoja.getSelectedItem(), cat=(String)comboCategoria.getSelectedItem();
        try (Connection conn=DriverManager.getConnection(URL,USER,PASS);
             PreparedStatement ps=conn.prepareStatement(
                     "SELECT id,data,valor_meta FROM public.metas_venda WHERE cdempresa=? AND categoria=? ORDER BY data")) {
            ps.setString(1,loja); ps.setString(2,cat);
            ResultSet rs=ps.executeQuery();
            SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy");
            while (rs.next()) modelVenda.addRow(new Object[]{rs.getInt("id"), sdf.format(rs.getDate("data")), "R$ "+df.format(rs.getDouble("valor_meta"))});
        } catch (Exception ex) {  }
    }

    
    private JPanel criarAbaIndicadores() {
        JPanel p = new JPanel(new BorderLayout(0,14));
        p.setBackground(BG_PANEL); p.setBorder(new EmptyBorder(16,16,16,16));

        Calendar cal = Calendar.getInstance();
        comboMes.setSelectedIndex(cal.get(Calendar.MONTH));
        spinnerAno = new JSpinner(new SpinnerNumberModel(cal.get(Calendar.YEAR),2020,2099,1));
        spinnerAno.setEditor(new JSpinner.NumberEditor(spinnerAno,"#"));

        JPanel selMes = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); selMes.setBackground(BG_PANEL);
        selMes.add(rotulo("📅 Mês:")); selMes.add(estilizarCombo(comboMes));
        selMes.add(rotulo("Ano:"));    selMes.add(estilizarSpinner(spinnerAno));
        JButton btnCarregar = botao("🔍  CARREGAR", ACCENT_BLUE);
        selMes.add(btnCarregar); btnCarregar.addActionListener(e -> carregarIndicadores());

        JPanel grid = new JPanel(new GridLayout(5,2,12,12)); grid.setBackground(BG_CARD);
        grid.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR,1), new EmptyBorder(20,24,20,24)));
        grid.add(rotuloDestaque("✅  Ticket Médio (R$):",  ACCENT_ORANGE)); grid.add(estilizarCampo(fTicket));
        grid.add(rotuloDestaque("👥  Clientes:",           ACCENT_BLUE));   grid.add(estilizarCampo(fClientes));
        grid.add(rotuloDestaque("📦  Produtos:",           new Color(160,100,255))); grid.add(estilizarCampo(fProdutos));
        grid.add(rotuloDestaque("🛒  Prod / Cliente:",     ACCENT_GOLD));   grid.add(estilizarCampo(fProdCli));
        grid.add(rotuloDestaque("📈  Margem (%):",         ACCENT_TEAL));   grid.add(estilizarCampo(fMargem));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); botoes.setBackground(BG_PANEL);
        JButton btnSalvar = botao("💾  SALVAR INDICADORES", ACCENT_GREEN);
        botoes.add(btnSalvar); btnSalvar.addActionListener(e -> salvarIndicadores());

        p.add(selMes,  BorderLayout.NORTH);
        p.add(grid,    BorderLayout.CENTER);
        p.add(botoes,  BorderLayout.SOUTH);
        carregarIndicadores(); return p;
    }

    private void carregarIndicadores() {
        if (comboMes==null||spinnerAno==null) return;
        String loja=(String)comboLoja.getSelectedItem(), cat=(String)comboCategoria.getSelectedItem();
        int mes=comboMes.getSelectedIndex()+1, ano=(int)spinnerAno.getValue();
        try (Connection conn=DriverManager.getConnection(URL,USER,PASS);
             PreparedStatement ps=conn.prepareStatement(
                     "SELECT ticket,clientes,produtos,prod_cliente,margem FROM public.metas_indicadores WHERE cdempresa=? AND categoria=? AND mes=? AND ano=?")) {
            ps.setString(1,loja); ps.setString(2,cat); ps.setInt(3,mes); ps.setInt(4,ano);
            ResultSet rs=ps.executeQuery();
            if (rs.next()) {
                fTicket.setText(df.format(rs.getDouble("ticket")));
                fClientes.setText(String.valueOf((int)rs.getDouble("clientes")));
                fProdutos.setText(String.valueOf((int)rs.getDouble("produtos")));
                fProdCli.setText(df.format(rs.getDouble("prod_cliente")));
                fMargem.setText(df.format(rs.getDouble("margem")));
            } else { fTicket.setText("0,00"); fClientes.setText("0"); fProdutos.setText("0"); fProdCli.setText("0,00"); fMargem.setText("0,00"); }
        } catch (Exception ex) {  }
    }

    private void salvarIndicadores() {
        String loja=(String)comboLoja.getSelectedItem(), cat=(String)comboCategoria.getSelectedItem();
        int mes=comboMes.getSelectedIndex()+1, ano=(int)spinnerAno.getValue();
        try {
            double ticket=Double.parseDouble(fTicket.getText().replace(".","").replace(",","."));
            double clientes=Double.parseDouble(fClientes.getText().replace(".","").replace(",","."));
            double produtos=Double.parseDouble(fProdutos.getText().replace(".","").replace(",","."));
            double prodCli=Double.parseDouble(fProdCli.getText().replace(".","").replace(",","."));
            double margem=Double.parseDouble(fMargem.getText().replace(".","").replace(",","."));
            try (Connection conn=DriverManager.getConnection(URL,USER,PASS);
                 PreparedStatement ps=conn.prepareStatement(
                         "INSERT INTO public.metas_indicadores(cdempresa,categoria,mes,ano,ticket,clientes,produtos,prod_cliente,margem) VALUES(?,?,?,?,?,?,?,?,?) "
                                 + "ON CONFLICT ON CONSTRAINT metas_ind_unique DO UPDATE SET ticket=EXCLUDED.ticket,clientes=EXCLUDED.clientes,produtos=EXCLUDED.produtos,prod_cliente=EXCLUDED.prod_cliente,margem=EXCLUDED.margem")) {
                ps.setString(1,loja); ps.setString(2,cat); ps.setInt(3,mes); ps.setInt(4,ano);
                ps.setDouble(5,ticket); ps.setDouble(6,clientes); ps.setDouble(7,produtos); ps.setDouble(8,prodCli); ps.setDouble(9,margem);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this,"✅ Indicadores salvos com sucesso!");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
    }

    
    private JPanel criarAbaVendedores() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PANEL); p.setBorder(new EmptyBorder(20,40,20,40));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(8,8,8,8); g.weightx=1;

        JLabel titulo = new JLabel("Quantidade de Vendedores por Categoria");
        titulo.setFont(new Font("SansSerif",Font.BOLD,13)); titulo.setForeground(TEXT_PRIMARY);
        g.gridx=0; g.gridy=0; g.gridwidth=2; p.add(titulo,g);

        JLabel aviso = new JLabel("ℹ️  Dado persistente — usado para calcular a meta geral da loja no dashboard");
        aviso.setFont(new Font("SansSerif",Font.PLAIN,10)); aviso.setForeground(ACCENT_BLUE);
        g.gridy=1; p.add(aviso,g); g.gridwidth=1;

        g.gridx=0; g.gridy=2; p.add(criarCardQtd("👔  ADULTO",   ACCENT_BLUE,   spinnerAdulto,   lblAtualAdulto),g);
        g.gridx=1; g.gridy=2; p.add(criarCardQtd("👶  INFANTIL", ACCENT_PURPLE, spinnerInfantil, lblAtualInfantil),g);

        JButton btnSalvar = botao("💾  SALVAR QUANTIDADES", ACCENT_GREEN);
        g.gridx=0; g.gridy=3; g.gridwidth=2; p.add(btnSalvar,g);
        btnSalvar.addActionListener(e -> salvarQuantidades());

        carregarQuantidades(); return p;
    }

    private JPanel criarCardQtd(String titulo, Color accent, JSpinner spinner, JLabel lblAtual) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(accent); g2.fillRoundRect(0,0,4,getHeight(),4,4);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(16,20,16,20));
        GridBagConstraints g=new GridBagConstraints(); g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1; g.insets=new Insets(4,0,4,0);
        JLabel lTit=new JLabel(titulo); lTit.setFont(new Font("SansSerif",Font.BOLD,13)); lTit.setForeground(accent);
        g.gridy=0; card.add(lTit,g);
        JLabel lAT=new JLabel("Quantidade salva:"); lAT.setFont(FONT_LABEL); lAT.setForeground(TEXT_SECONDARY);
        g.gridy=1; card.add(lAT,g);
        lblAtual.setFont(new Font("SansSerif",Font.BOLD,26)); lblAtual.setForeground(TEXT_PRIMARY);
        g.gridy=2; card.add(lblAtual,g);
        JLabel lNT=new JLabel("Nova quantidade:"); lNT.setFont(FONT_LABEL); lNT.setForeground(TEXT_SECONDARY);
        g.gridy=3; card.add(lNT,g);
        estilizarSpinner(spinner); g.gridy=4; card.add(spinner,g);
        return card;
    }

    private void carregarQuantidades() {
        String loja=(String)comboLoja.getSelectedItem();
        try (Connection conn=DriverManager.getConnection(URL,USER,PASS)) {
            
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS public.qtd_vendedores("
                    + "id SERIAL PRIMARY KEY,cdempresa VARCHAR(10) NOT NULL,categoria VARCHAR(20) NOT NULL,"
                    + "quantidade INTEGER NOT NULL DEFAULT 0,CONSTRAINT qtd_vend_unique UNIQUE(cdempresa,categoria))").execute();
            PreparedStatement ps=conn.prepareStatement("SELECT categoria,quantidade FROM public.qtd_vendedores WHERE cdempresa=?");
            ps.setString(1,loja); ResultSet rs=ps.executeQuery();
            int adulto=0, infantil=0;
            while (rs.next()) {
                if ("ADULTO"  .equals(rs.getString("categoria"))) adulto   =rs.getInt("quantidade");
                if ("INFANTIL".equals(rs.getString("categoria"))) infantil =rs.getInt("quantidade");
            }
            lblAtualAdulto.setText(String.valueOf(adulto));   spinnerAdulto.setValue(adulto);
            lblAtualInfantil.setText(String.valueOf(infantil)); spinnerInfantil.setValue(infantil);
        } catch (Exception ex) { lblAtualAdulto.setText("0"); lblAtualInfantil.setText("0"); }
    }

    private void salvarQuantidades() {
        String loja=(String)comboLoja.getSelectedItem();
        int qtdA=(int)spinnerAdulto.getValue(), qtdI=(int)spinnerInfantil.getValue();
        try (Connection conn=DriverManager.getConnection(URL,USER,PASS)) {
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS public.qtd_vendedores("
                    + "id SERIAL PRIMARY KEY,cdempresa VARCHAR(10) NOT NULL,categoria VARCHAR(20) NOT NULL,"
                    + "quantidade INTEGER NOT NULL DEFAULT 0,CONSTRAINT qtd_vend_unique UNIQUE(cdempresa,categoria))").execute();
            for (String[] cv : new String[][]{{"ADULTO",String.valueOf(qtdA)},{"INFANTIL",String.valueOf(qtdI)}}) {
                PreparedStatement ps=conn.prepareStatement(
                        "INSERT INTO public.qtd_vendedores(cdempresa,categoria,quantidade) VALUES(?,?,?) "
                                + "ON CONFLICT ON CONSTRAINT qtd_vend_unique DO UPDATE SET quantidade=EXCLUDED.quantidade");
                ps.setString(1,loja); ps.setString(2,cv[0]); ps.setInt(3,Integer.parseInt(cv[1]));
                ps.executeUpdate(); ps.close();
            }
            JOptionPane.showMessageDialog(this,"✅ Quantidades salvas!"); carregarQuantidades();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
    }

    
    private JLabel rotulo(String t) { JLabel l=new JLabel(t); l.setFont(FONT_LABEL); l.setForeground(TEXT_SECONDARY); return l; }
    private JLabel rotuloDestaque(String t, Color c) { JLabel l=new JLabel(t); l.setFont(new Font("SansSerif",Font.BOLD,12)); l.setForeground(c); return l; }

    private JTextField estilizarCampo(JTextField f) {
        f.setFont(FONT_INPUT); f.setForeground(TEXT_PRIMARY); f.setBackground(BG_CARD); f.setCaretColor(ACCENT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR,1),new EmptyBorder(5,8,5,8))); return f;
    }
    private JFormattedTextField estilizarCampo(JFormattedTextField f) {
        f.setFont(FONT_INPUT); f.setForeground(TEXT_PRIMARY); f.setBackground(BG_CARD); f.setCaretColor(ACCENT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR,1),new EmptyBorder(5,8,5,8))); return f;
    }
    private JComboBox<String> estilizarCombo(JComboBox<String> c) {
        c.setFont(FONT_INPUT); c.setForeground(TEXT_PRIMARY); c.setBackground(BG_CARD);
        c.setBorder(BorderFactory.createLineBorder(BORDER_COLOR,1)); c.setPreferredSize(new Dimension(130,30)); return c;
    }
    private static void fixSpinner(JSpinner s, Color bg, Color fg) {
        s.setBackground(bg); s.setForeground(fg);
        s.setBorder(BorderFactory.createLineBorder(new Color(35,48,75),1));
        JComponent ed = s.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)ed).getTextField();
            tf.setOpaque(true); tf.setBackground(bg); tf.setForeground(fg);
            tf.setFont(new Font("SansSerif",Font.BOLD,13));
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setBorder(new EmptyBorder(4,8,4,8));
            
            tf.addHierarchyListener(e2 -> {
                tf.setBackground(bg); tf.setForeground(fg);
            });
        }
    }
    private JSpinner estilizarSpinner(JSpinner s) {
        s.setPreferredSize(new Dimension(120,32));
        fixSpinner(s, BG_CARD, TEXT_PRIMARY);
        return s;
    }
    private JButton botao(String texto, Color cor) {
        JButton b=new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(cor.darker());
                else if (getModel().isRollover()) g2.setColor(cor.brighter());
                else g2.setColor(cor);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8); g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(FONT_BTN); b.setForeground(BG_DARK); b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false); b.setBorder(new EmptyBorder(8,16,8,16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private JTable estilizarTabela(JTable t) {
        t.setFont(FONT_TABLE); t.setForeground(TEXT_PRIMARY); t.setBackground(BG_CARD); t.setGridColor(BORDER_COLOR);
        t.setSelectionBackground(new Color(40,60,90)); t.setSelectionForeground(TEXT_PRIMARY); t.setRowHeight(28);
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,11));
        t.getTableHeader().setBackground(new Color(16,24,40)); t.getTableHeader().setForeground(TEXT_SECONDARY); return t;
    }
    private static JFormattedTextField criarCampoData(String valor) {
        try {
            MaskFormatter mask=new MaskFormatter("##/##/####"); mask.setPlaceholderCharacter('_');
            JFormattedTextField f=new JFormattedTextField(mask); f.setColumns(10);
            if (!valor.isEmpty()) f.setText(valor); return f;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}