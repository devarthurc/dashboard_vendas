import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Dashboard extends JFrame {

    
    private static final Color BG_DARK       = new Color(10, 14, 23);
    private static final Color BG_PANEL      = new Color(16, 22, 36);
    private static final Color BG_CARD       = new Color(22, 30, 48);
    private static final Color BORDER_COLOR  = new Color(35, 48, 75);
    private static final Color TEXT_PRIMARY  = new Color(230, 235, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 135, 165);
    private static final Color ACCENT_GREEN  = new Color(0, 230, 150);
    private static final Color ACCENT_ORANGE = new Color(255, 140, 60);
    private static final Color ACCENT_BLUE   = new Color(60, 160, 255);
    private static final Color ACCENT_PURPLE = new Color(160, 100, 255);
    private static final Color ACCENT_TEAL   = new Color(0, 210, 200);
    private static final Color ACCENT_RED    = new Color(255, 80, 100);
    private static final Color ACCENT_GOLD   = new Color(255, 200, 60);

    
    private static final Font FONT_CARD_VALUE  = new Font("SansSerif", Font.BOLD, 28);
    private static final Font FONT_CARD_LABEL  = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_RANK_TITLE  = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_RANK_NAME   = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_RANK_BADGE  = new Font("SansSerif", Font.BOLD, 9);
    private static final Font FONT_FILTER_LBL  = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_FILTER_FIELD = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BTN         = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_TITLE       = new Font("SansSerif", Font.BOLD, 16);

    
    private final String URL        = Config.getIshopUrl();
    private final String USER       = Config.getIshopUser();
    private final String PASS       = Config.getIshopPassword();
    private final String URL_LOGIN  = Config.getVendedoresUrl();
    private final String USER_LOGIN = Config.getVendedoresUser();
    private final String PASS_LOGIN = Config.getVendedoresPassword();

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    
    private JFormattedTextField dtInicio = criarCampoData(hojeFormatado());
    private JFormattedTextField dtFim    = criarCampoData(hojeFormatado());

    private static String hojeFormatado() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return String.format("%02d/%02d/%04d",
                c.get(java.util.Calendar.DAY_OF_MONTH),
                c.get(java.util.Calendar.MONTH) + 1,
                c.get(java.util.Calendar.YEAR));
    }

    private JTextField          loja     = new JTextField("001", 3);
    private JComboBox<Vendedor> comboVendedor = new JComboBox<>();

    
    private JPanel painelCards   = new JPanel(new ResponsiveGridLayout(200, 12, 12));
    private JPanel painelRanking = new JPanel(new ResponsiveGridLayout(400, 12, 12, true));

    
    private String vendedorLogin     = null;        
    private String empresaLogin      = null;        
    private String tipoLogin         = "VENDEDOR"; 
    private String nomeUsuarioLogado = null;        
    private String categoriaLogin    = "ADULTO";       
    private String categoriaBusca    = "ADULTO";       
    private List<Vendedor> rankingMesCache = null;         
    private boolean modoMensal = true;                         
    private JRadioButton rbModoMensal       = null;
    private JRadioButton rbModoProporcional = null;
    private JPanel  painelFiltros = null;
    private JPanel  painelAcoesTop = null;                      
    private JLabel lblSaudacao = new JLabel();          


    private static class PainelRolavel extends JPanel implements Scrollable {
        PainelRolavel() {
            super(new BorderLayout(0, 12));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientacao, int direcao) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientacao, int direcao) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }


        @Override
        public boolean getScrollableTracksViewportHeight() {
            if (getParent() instanceof JViewport) {
                return ((JViewport) getParent()).getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }

    private Dashboard() {
        setTitle("ANÁLISE DE VENDAS POR VENDEDOR");
        setSize(1400, 900);
        setMinimumSize(new Dimension(1000, 650));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_DARK);

        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBackground(BG_DARK);
        main.setBorder(new EmptyBorder(10, 16, 10, 16));

        main.add(criarCabecalho(), BorderLayout.NORTH);

        painelCards.setBackground(BG_DARK);
        painelRanking.setBackground(BG_DARK);

        PainelRolavel centro = new PainelRolavel();
        centro.setBackground(BG_DARK);
        centro.add(painelCards, BorderLayout.NORTH);
        centro.add(painelRanking, BorderLayout.CENTER);

        JScrollPane scrollCentro = new JScrollPane(centro);
        scrollCentro.setBorder(null);
        scrollCentro.getViewport().setBackground(BG_DARK);
        scrollCentro.setBackground(BG_DARK);
        scrollCentro.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCentro.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollCentro.getVerticalScrollBar().setUnitIncrement(16);

        main.add(scrollCentro, BorderLayout.CENTER);
        add(main);

        carregarVendedores();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    
    
    
    public Dashboard(String empresa, String idpessoa, String tipo, String nome, String categoria) {
        this();
        this.empresaLogin      = empresa;
        this.vendedorLogin     = idpessoa;
        this.tipoLogin         = (tipo != null) ? tipo.toUpperCase() : "VENDEDOR";
        this.nomeUsuarioLogado = nome != null ? nome : "";
        this.categoriaLogin    = (categoria != null) ? categoria.toUpperCase() : "ADULTO";
        atualizarSaudacao();
        carregarModoIndicador();
        
        if (painelFiltros != null) {
            if (isGerente()) {
                
                rbModoMensal       = criarRadioModo("📅 Mensal");
                rbModoProporcional = criarRadioModo("📊 Proporcional");

                ButtonGroup grupoModo = new ButtonGroup();
                grupoModo.add(rbModoMensal);
                grupoModo.add(rbModoProporcional);

                JPanel painelModo = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                painelModo.setOpaque(false);
                painelModo.add(rbModoMensal);
                painelModo.add(rbModoProporcional);

                atualizarBotaoModo();

                java.awt.event.ActionListener modoListener = e -> {
                    modoMensal = rbModoMensal.isSelected();
                    salvarModoIndicador();
                    atualizarDashboard();
                };
                rbModoMensal.addActionListener(modoListener);
                rbModoProporcional.addActionListener(modoListener);

                if (painelAcoesTop != null) {
                    painelAcoesTop.add(painelModo, 0);
                    painelAcoesTop.revalidate();
                    painelAcoesTop.repaint();
                }
            }
            
            
            painelFiltros.revalidate();
            painelFiltros.repaint();
        }

        loja.setText(empresa);
        loja.setEditable(false);   

        carregarVendedores();

        if (isGerente()) {
            
            
            Vendedor todos = new Vendedor();
            todos.id   = null;
            todos.nome = "— TODOS OS VENDEDORES —";
            comboVendedor.insertItemAt(todos, 0);
            comboVendedor.setSelectedIndex(0);
        } else {
            
            selecionarVendedorLogado();
            comboVendedor.setEnabled(false);
        }
    }


    
    private void carregarModoIndicador() {
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN)) {
            conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS public.config_dashboard ("
                            + "cdempresa VARCHAR(10) PRIMARY KEY, modo_indicador VARCHAR(20) NOT NULL DEFAULT 'MENSAL')").execute();
            String loja = empresaLogin != null ? empresaLogin : this.loja.getText().trim();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT modo_indicador FROM public.config_dashboard WHERE cdempresa = ?");
            ps.setString(1, loja);
            ResultSet rs = ps.executeQuery();
            modoMensal = !rs.next() || "MENSAL".equals(rs.getString("modo_indicador"));
        } catch (Exception ignored) {}
        atualizarBotaoModo();
    }

    private void salvarModoIndicador() {
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN)) {
            String loja = empresaLogin != null ? empresaLogin : this.loja.getText().trim();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO public.config_dashboard (cdempresa, modo_indicador) VALUES (?,?) "
                            + "ON CONFLICT (cdempresa) DO UPDATE SET modo_indicador = EXCLUDED.modo_indicador");
            ps.setString(1, loja);
            ps.setString(2, modoMensal ? "MENSAL" : "PROPORCIONAL");
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void atualizarBotaoModo() {
        if (rbModoMensal == null || rbModoProporcional == null) return;
        rbModoMensal.setSelected(modoMensal);
        rbModoProporcional.setSelected(!modoMensal);
        rbModoMensal.setToolTipText("Compara os indicadores com a meta do mês inteiro");
        rbModoProporcional.setToolTipText("Compara os indicadores com a meta proporcional ao período (De/Até) selecionado");
    }

    private JRadioButton criarRadioModo(String texto) {
        JRadioButton rb = new JRadioButton(texto);
        rb.setFont(FONT_BTN);
        rb.setForeground(TEXT_PRIMARY);
        rb.setBackground(BG_DARK);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return rb;
    }

    private boolean isGerente() {
        return "GERENTE".equals(tipoLogin);
    }

    
    
    
    private void atualizarSaudacao() {
        if (nomeUsuarioLogado == null || nomeUsuarioLogado.isEmpty()) return;
        String icone = isGerente() ? "👔" : "👤";
        String cargo = isGerente() ? "Gerente" : "Vendedor(a)";
        lblSaudacao.setText(icone + "  " + getSaudacao() + ", " + nomeUsuarioLogado + "!  •  " + cargo);
    }

    private String getSaudacao() {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hora >= 5  && hora < 12) return "Bom dia";
        if (hora >= 12 && hora < 18) return "Boa tarde";
        return "Boa noite";
    }

    private JPanel criarCabecalho() {
        JPanel cab = new JPanel(new BorderLayout(0, 4));
        cab.setBackground(BG_DARK);

        
        lblSaudacao.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblSaudacao.setForeground(ACCENT_GREEN);

        JPanel titRow = new JPanel(new BorderLayout(0, 0));
        titRow.setBackground(BG_DARK);
        titRow.add(lblSaudacao, BorderLayout.WEST);

        
        JButton btnSenha  = criarBotaoTopo("🔑 SENHA",  new Color(60, 90, 140));
        JButton btnSair   = criarBotaoTopo("🚪 SAIR",   new Color(80, 30, 40));
        btnSenha.addActionListener(e -> alterarSenha());
        btnSair .addActionListener(e -> sairSistema());

        JPanel painelAcoesTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelAcoesTop.setOpaque(false);
        painelAcoesTop.add(btnSenha);
        painelAcoesTop.add(btnSair);
        this.painelAcoesTop = painelAcoesTop;

        titRow.add(painelAcoesTop, BorderLayout.EAST);

        
        JPanel filtros = new JPanel(new FiltrosLayout(comboVendedor, 200, 8, 6));
        filtros.setBackground(BG_DARK);

        filtros.add(criarRotulo("📅 De:"));
        filtros.add(estilizarCampo(dtInicio));
        filtros.add(criarBotaoCalendario(dtInicio));
        filtros.add(criarRotulo("📅 Até:"));
        filtros.add(estilizarCampo(dtFim));
        filtros.add(criarBotaoCalendario(dtFim));
        filtros.add(criarRotulo("🏪 Loja:"));
        filtros.add(estilizarCampo(loja));
        filtros.add(criarRotulo("💼 Vendedor:"));
        filtros.add(estilizarCombo(comboVendedor));

        JButton btnBuscar = criarBotaoIcone("🔍", ACCENT_GREEN);
        btnBuscar.addActionListener(e -> atualizarDashboard());
        btnBuscar.setToolTipText("Buscar");
        filtros.add(btnBuscar);

        painelFiltros = filtros;

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BORDER_COLOR);

        cab.add(titRow,  BorderLayout.NORTH);
        cab.add(filtros, BorderLayout.CENTER);
        cab.add(sep,     BorderLayout.SOUTH);
        return cab;
    }

    @FunctionalInterface interface Formatador { String fmt(double v); }

    private JPanel criarCard(String icone, String titulo,
                             double realizado, double metaHoje, double metaTotal,
                             Color accent, Formatador fmt) {

        final boolean temMeta   = metaHoje > 0 && metaTotal > 0;
        final double  perc      = temMeta ? realizado / metaHoje  : 0; 
        final double  percTotal = temMeta ? realizado / metaTotal : 0; 
        
        final Color   cor     = !temMeta       ? TEXT_PRIMARY
                : perc >= 1.0    ? ACCENT_GREEN
                : perc >= 0.70   ? ACCENT_ORANGE
                : ACCENT_RED;
        final Color accent_ = accent;

        
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accent_);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        
        JLabel lblIcone = new JLabel(icone) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent_.darker().darker());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblIcone.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblIcone.setForeground(accent_);
        lblIcone.setOpaque(false);
        lblIcone.setBorder(new EmptyBorder(3, 6, 3, 6));

        
        JLabel lblTitulo = new JLabel(titulo.toUpperCase());
        lblTitulo.setFont(FONT_CARD_LABEL);
        lblTitulo.setForeground(TEXT_SECONDARY);
        lblTitulo.setBorder(new EmptyBorder(4, 0, 2, 0));

        
        JLabel lblValor = new JLabel(fmt.fmt(realizado));
        lblValor.setFont(FONT_CARD_VALUE);
        lblValor.setForeground(TEXT_PRIMARY);

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);
        conteudo.add(lblIcone);
        conteudo.add(lblTitulo);
        conteudo.add(lblValor);

        if (temMeta) {
            final Color cor_ = cor;
            final double fill_ = Math.min(perc, 1.0);
            double percTotalShow = percTotal * 100; 
            double percHojeShow  = perc * 100;       
            double falta = Math.max(metaTotal - realizado, 0);

            
            JLabel lblPerc = new JLabel(df.format(percTotalShow) + "% da meta");
            lblPerc.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblPerc.setForeground(cor_);

            
            JPanel barra = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(35, 48, 75));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    int fw = (int)(getWidth() * fill_);
                    if (fw > 2) { g2.setColor(cor_); g2.fillRoundRect(0, 0, fw, getHeight(), getHeight(), getHeight()); }
                    g2.dispose();
                }
            };
            barra.setOpaque(false);
            barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            barra.setPreferredSize(new Dimension(0, 8));

            
            String txtFalta = falta > 0
                    ? "Falta: " + fmt.fmt(falta)
                    : "✅  Meta batida!";
            JLabel lblFalta = new JLabel(txtFalta);
            lblFalta.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblFalta.setForeground(falta > 0 ? TEXT_SECONDARY : ACCENT_GREEN);

            conteudo.add(Box.createVerticalStrut(5));
            conteudo.add(lblPerc);
            conteudo.add(Box.createVerticalStrut(3));
            conteudo.add(barra);
            conteudo.add(Box.createVerticalStrut(3));
            conteudo.add(lblFalta);
        }


        card.add(conteudo, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(200, Math.max(card.getPreferredSize().height, 100)));
        return card;
    }

    
    private JPanel criarCard(String icone, String titulo, String valorStr, Color accent) {
        return criarCard(icone, titulo, 0, 0, 0, accent, v -> valorStr);
    }
    
    private JPanel criarCardMensal(String icone, String titulo,
                                   double realizado, double meta,
                                   Color accent, Formatador fmt) {
        final boolean temMeta = meta > 0;
        final double  perc    = temMeta ? realizado / meta : 0;
        final double  percShow= perc * 100;
        final Color   cor     = !temMeta     ? TEXT_PRIMARY
                : perc >= 1.0  ? ACCENT_GREEN
                : perc >= 0.70 ? ACCENT_ORANGE
                : ACCENT_RED;
        final Color  accent_ = accent;
        final double fill_   = Math.min(perc, 1.0);
        final Color  cor_    = cor;

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accent_);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblIcone = new JLabel(icone) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent_.darker().darker());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblIcone.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblIcone.setForeground(accent_);
        lblIcone.setOpaque(false);
        lblIcone.setBorder(new EmptyBorder(3, 6, 3, 6));

        JLabel lblTitulo = new JLabel(titulo.toUpperCase());
        lblTitulo.setFont(FONT_CARD_LABEL);
        lblTitulo.setForeground(TEXT_SECONDARY);
        lblTitulo.setBorder(new EmptyBorder(4, 0, 2, 0));

        
        JLabel lblValor = new JLabel(fmt.fmt(realizado));
        lblValor.setFont(FONT_CARD_VALUE);
        lblValor.setForeground(TEXT_PRIMARY);

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);
        conteudo.add(lblIcone);
        conteudo.add(lblTitulo);
        conteudo.add(lblValor);

        if (temMeta) {
            
            JLabel lblPerc = new JLabel(df.format(percShow) + "% da meta");
            lblPerc.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblPerc.setForeground(cor_);

            
            JPanel barra = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(35, 48, 75));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    int fw = (int)(getWidth() * fill_);
                    if (fw > 2) { g2.setColor(cor_); g2.fillRoundRect(0, 0, fw, getHeight(), getHeight(), getHeight()); }
                    g2.dispose();
                }
            };
            barra.setOpaque(false);
            barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            barra.setPreferredSize(new Dimension(0, 8));

            
            double falta = Math.max(meta - realizado, 0);

            JLabel lblFalta = new JLabel(falta > 0 ? "Falta: " + fmt.fmt(falta) : "✅  Meta batida!");
            lblFalta.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblFalta.setForeground(falta > 0 ? TEXT_SECONDARY : ACCENT_GREEN);

            conteudo.add(Box.createVerticalStrut(5));
            conteudo.add(lblPerc);
            conteudo.add(Box.createVerticalStrut(3));
            conteudo.add(barra);
            conteudo.add(Box.createVerticalStrut(3));
            conteudo.add(lblFalta);
        }


        if (!temMeta) {
            JLabel lblSemMeta = new JLabel("Sem meta cadastrada");
            lblSemMeta.setFont(new Font("SansSerif", Font.ITALIC, 10));
            lblSemMeta.setForeground(TEXT_SECONDARY);
            conteudo.add(Box.createVerticalStrut(4));
            conteudo.add(lblSemMeta);
        }


        if (!temMeta) {
            JLabel lblSemMeta = new JLabel("Sem meta cadastrada");
            lblSemMeta.setFont(new Font("SansSerif", Font.ITALIC, 10));
            lblSemMeta.setForeground(TEXT_SECONDARY);
            conteudo.add(Box.createVerticalStrut(4));
            conteudo.add(lblSemMeta);
        }

        card.add(conteudo, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(200, Math.max(card.getPreferredSize().height, 100)));
        return card;
    }

    
    
    
    private int contarVendedores(String cdempresa, String categoria) {
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT quantidade FROM public.qtd_vendedores WHERE cdempresa = ? AND categoria = ?")) {
            ps.setString(1, cdempresa);
            ps.setString(2, categoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("quantidade");
        } catch (Exception ignored) {}
        return 1; 
    }

    private double buscarMetaVenda(String cdempresa, java.sql.Date dataIni, java.sql.Date dataFim) {
        if (categoriaBusca == null) {
            
            String sql = "SELECT categoria, COALESCE(SUM(valor_meta),0) AS total "
                    + "FROM public.metas_venda "
                    + "WHERE cdempresa = ? AND data BETWEEN ? AND ? "
                    + "GROUP BY categoria";
            try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cdempresa);
                ps.setDate(2, dataIni);
                ps.setDate(3, dataFim);
                ResultSet rs = ps.executeQuery();
                double total = 0;
                while (rs.next()) {
                    String cat   = rs.getString("categoria");
                    double meta  = rs.getDouble("total");
                    int    qtd   = contarVendedores(cdempresa, cat);
                    total += meta * qtd;
                }
                return total;
            } catch (Exception ignored) {}
            return 0;
        } else {
            
            
            String sql = "SELECT COALESCE(SUM(valor_meta),0) AS total FROM public.metas_venda "
                    + "WHERE cdempresa = ? AND data BETWEEN ? AND ? AND categoria = ?";
            try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cdempresa);
                ps.setDate(2, dataIni);
                ps.setDate(3, dataFim);
                ps.setString(4, categoriaBusca);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double meta = rs.getDouble("total");
                    
                    
                    return meta;
                }
            } catch (Exception ignored) {}
            return 0;
        }
    }

    
    
    private double[] buscarMetaIndicadores(String cdempresa, int mes, int ano) {
        String sql = (categoriaBusca == null)
                
                ? "SELECT categoria, ticket, clientes, produtos, prod_cliente, margem "
                + "FROM public.metas_indicadores WHERE cdempresa = ? AND mes = ? AND ano = ?"
                
                : "SELECT ticket, clientes, produtos, prod_cliente, margem "
                + "FROM public.metas_indicadores "
                + "WHERE cdempresa = ? AND mes = ? AND ano = ? AND categoria = ?";
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cdempresa);
            ps.setInt(2, mes);
            ps.setInt(3, ano);
            if (categoriaBusca != null) ps.setString(4, categoriaBusca);
            ResultSet rs = ps.executeQuery();
            if (categoriaBusca == null) {
                
                double ticket = 0, clientes = 0, produtos = 0, prodCli = 0, margem = 0;
                double ticketAdulto = 0, prodCliAdulto = 0;
                int totalVend = 0;
                while (rs.next()) {
                    String cat = rs.getString("categoria");
                    int qtd    = contarVendedores(cdempresa, cat);
                    totalVend += qtd;
                    double tkt = rs.getDouble("ticket");
                    double pc  = rs.getDouble("prod_cliente");
                    ticket   += tkt * qtd;
                    clientes += rs.getDouble("clientes") * qtd;
                    produtos += rs.getDouble("produtos") * qtd;
                    prodCli  += pc * qtd;
                    margem   += rs.getDouble("margem")   * qtd;
                    
                    if ("ADULTO".equals(cat)) {
                        ticketAdulto  = tkt;
                        prodCliAdulto = pc;
                    }
                }
                if (totalVend > 0) {
                    
                    
                    return new double[]{
                            ticketAdulto,   
                            clientes,
                            produtos,
                            prodCliAdulto,  
                            margem / totalVend
                    };
                }
            } else {
                if (rs.next()) {
                    
                    return new double[]{
                            rs.getDouble("ticket"),
                            rs.getDouble("clientes"),
                            rs.getDouble("produtos"),
                            rs.getDouble("prod_cliente"),
                            rs.getDouble("margem")
                    };
                }
            }
        } catch (Exception ignored) {}
        return new double[]{0, 0, 0, 0, 0};
    }

    private void montarCards(Vendedor v) {
        painelCards.removeAll();
        if (v == null) return;

        String lojaVal = empresaLogin != null ? empresaLogin : loja.getText().trim();

        
        java.sql.Date dataIni, dataFim, dataHoje;
        try {
            dataIni = parseData(dtInicio.getText());
            dataFim = parseData(dtFim.getText());
        } catch (Exception e) {
            dataIni = dataFim = new java.sql.Date(System.currentTimeMillis());
        }
        
        java.util.Calendar _hoje = java.util.Calendar.getInstance();
        _hoje.set(java.util.Calendar.HOUR_OF_DAY, 0);
        _hoje.set(java.util.Calendar.MINUTE, 0);
        _hoje.set(java.util.Calendar.SECOND, 0);
        _hoje.set(java.util.Calendar.MILLISECOND, 0);
        dataHoje = new java.sql.Date(_hoje.getTimeInMillis());
        java.sql.Date fimAteHoje = dataFim.after(dataHoje) ? dataHoje : dataFim;

        
        java.util.Calendar calIni = java.util.Calendar.getInstance();
        calIni.setTime(dataIni);
        int mesIni = calIni.get(java.util.Calendar.MONTH) + 1;
        int anoIni = calIni.get(java.util.Calendar.YEAR);

        
        java.util.Calendar cMes = java.util.Calendar.getInstance();
        cMes.set(anoIni, mesIni - 1, 1);
        int  totalDiasMes = cMes.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        long diasFiltro   = (dataFim.getTime() - dataIni.getTime()) / (1000L * 60 * 60 * 24) + 1;
        long diasAteHoje  = (fimAteHoje.getTime() - dataIni.getTime()) / (1000L * 60 * 60 * 24) + 1;

        
        
        double mVendaHoje  = buscarMetaVenda(lojaVal, dataIni, fimAteHoje);
        
        java.util.Calendar cMesVenda = java.util.Calendar.getInstance();
        cMesVenda.set(anoIni, mesIni - 1, 1, 0, 0, 0);
        cMesVenda.set(java.util.Calendar.MILLISECOND, 0);
        java.sql.Date _iniMesVenda = new java.sql.Date(cMesVenda.getTimeInMillis());
        cMesVenda.set(java.util.Calendar.DAY_OF_MONTH,
                cMesVenda.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        java.sql.Date _fimMesVenda = new java.sql.Date(cMesVenda.getTimeInMillis());
        double mVendaTotal = buscarMetaVenda(lojaVal, _iniMesVenda, _fimMesVenda);

        
        double[] ind = buscarMetaIndicadores(lojaVal, mesIni, anoIni);
        

        
        
        
        double fator = modoMensal ? 1.0
                : Math.min((double) diasAteHoje / totalDiasMes, 1.0);

        
        
        double metaVenda    = modoMensal ? mVendaTotal : mVendaHoje;
        
        double metaTicket   = ind[0];
        double metaProdCli  = ind[3];
        double metaMargem   = ind[4];
        
        double metaClientes = ind[1] * fator;
        double metaProdutos = ind[2] * fator;

        
        Formatador fmtR = x -> "R$ " + df.format(x);
        Formatador fmtN = x -> String.valueOf((int) Math.round(x));
        Formatador fmtP = x -> df.format(x) + "%";

        
        
        
        
        Vendedor vInd = v; 
        if (modoMensal) {
            
            java.util.Calendar cMesInd = java.util.Calendar.getInstance();
            cMesInd.set(anoIni, mesIni - 1, 1, 0, 0, 0);
            cMesInd.set(java.util.Calendar.MILLISECOND, 0);
            java.sql.Date _iniMes = new java.sql.Date(cMesInd.getTimeInMillis());
            cMesInd.set(java.util.Calendar.DAY_OF_MONTH,
                    cMesInd.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            java.sql.Date _fimMes = new java.sql.Date(cMesInd.getTimeInMillis());

            String idVendInd = isGerente()
                    ? (((Vendedor)comboVendedor.getSelectedItem()).id)
                    : vendedorLogin;

            if (idVendInd != null) {
                Vendedor tmp = carregarResumo(_iniMes, _fimMes, lojaVal, idVendInd);
                if (tmp != null) vInd = tmp;
            } else {
                List<Vendedor> rankMes = carregarRanking(_iniMes, _fimMes, lojaVal);
                Vendedor tmp = consolidarLoja(rankMes);
                if (tmp != null) vInd = tmp;
            }
        }

        painelCards.add(criarCardMensal("💰", "Venda Total",    v.venda,                metaVenda,    ACCENT_GREEN,  fmtR));
        painelCards.add(criarCardMensal("✅", "Ticket Médio",   vInd.ticket,            metaTicket,   ACCENT_ORANGE, fmtR));
        painelCards.add(criarCardMensal("👥", "Clientes",       vInd.clientes,          metaClientes, ACCENT_BLUE,   fmtN));
        painelCards.add(criarCardMensal("📦", "Produtos",       vInd.produtos,          metaProdutos, ACCENT_PURPLE, fmtN));
        painelCards.add(criarCardMensal("🛒", "Prod / Cliente", vInd.produtosPorCliente,metaProdCli,  ACCENT_GOLD,   fmtN));
        painelCards.add(criarCardMensal("📈", "Margem",         vInd.margem,            metaMargem,   ACCENT_TEAL,   fmtP));

        painelCards.revalidate();
        painelCards.repaint();
    }


    
    
    
    private JPanel criarRankingVisual(List<Vendedor> lista, String tipo, Color accent,
                                      String icone, String titulo, boolean mostrarValor) {
        JPanel painel = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        painel.setOpaque(false);
        painel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel titRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titRow.setOpaque(false);
        JLabel lblIcone = new JLabel(icone);
        lblIcone.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblIcone.setForeground(accent);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FONT_RANK_TITLE);
        lblTitulo.setForeground(TEXT_PRIMARY);
        titRow.add(lblIcone);
        titRow.add(lblTitulo);
        painel.add(titRow, BorderLayout.NORTH);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        List<Vendedor> ordenado = lista.stream().sorted((a, b) -> switch (tipo) {
            case "venda"       -> Double.compare(b.venda, a.venda);
            case "ticket"      -> Double.compare(b.ticket, a.ticket);
            case "clientes"    -> Integer.compare(b.clientes, a.clientes);
            case "prodCliente" -> Double.compare(b.produtosPorCliente, a.produtosPorCliente);
            case "margem"      -> Double.compare(b.margem, a.margem);
            default            -> Integer.compare(b.produtos, a.produtos);
        }).limit(5).toList();

        double max = ordenado.isEmpty() ? 1 : switch (tipo) {
            case "venda"       -> ordenado.get(0).venda;
            case "ticket"      -> ordenado.get(0).ticket;
            case "clientes"    -> ordenado.get(0).clientes;
            case "prodCliente" -> ordenado.get(0).produtosPorCliente;
            case "margem"      -> ordenado.get(0).margem;
            default            -> ordenado.get(0).produtos;
        };

        Color[] medalhas = {
                new Color(255, 200, 60),
                new Color(180, 190, 200),
                new Color(190, 120, 60),
                TEXT_SECONDARY, TEXT_SECONDARY
        };

        for (int i = 0; i < ordenado.size(); i++) {
            Vendedor v = ordenado.get(i);
            double valor = switch (tipo) {
                case "venda"       -> v.venda;
                case "ticket"      -> v.ticket;
                case "clientes"    -> v.clientes;
                case "prodCliente" -> v.produtosPorCliente;
                case "margem"      -> v.margem;
                default            -> v.produtos;
            };
            double perc  = max == 0 ? 0 : valor / max;
            final double perc_  = perc;
            final Color  corMed = medalhas[i];
            final int    rank   = i;

            JPanel item = new JPanel();
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            item.setOpaque(false);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            JPanel linhaTexto = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            linhaTexto.setOpaque(false);

            JLabel badge = new JLabel(String.valueOf(rank + 1)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(corMed);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(FONT_RANK_BADGE);
            badge.setForeground(BG_DARK);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setPreferredSize(new Dimension(18, 18));
            badge.setOpaque(false);

            String nomeExibido = v.nome.length() > 22 ? v.nome.substring(0, 20) + "..." : v.nome;
            JLabel lblNome = new JLabel(nomeExibido);
            lblNome.setFont(FONT_RANK_NAME);
            lblNome.setForeground(TEXT_PRIMARY);

            linhaTexto.add(badge);
            linhaTexto.add(lblNome);

            
            if (mostrarValor) {
                String valorFormatado = switch (tipo) {
                    case "venda"       -> "R$ " + df.format(valor);
                    case "ticket"      -> "R$ " + df.format(valor);
                    case "clientes"    -> String.valueOf((int) valor);
                    case "prodCliente" -> String.valueOf((int) valor);
                    case "margem"      -> df.format(valor) + "%";
                    default            -> String.valueOf((int) valor);
                };
                JLabel lblValor = new JLabel(valorFormatado);
                lblValor.setFont(new Font("SansSerif", Font.BOLD, 10));
                lblValor.setForeground(accent);
                linhaTexto.add(lblValor);
            }

            JPanel linhaBarra = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int h = getHeight();
                    g2.setColor(new Color(35, 48, 75));
                    g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                    int fillW = (int)(getWidth() * perc_);
                    if (fillW > 2) {
                        g2.setColor(accent);
                        g2.fillRoundRect(0, 0, fillW, h, h, h);
                    }
                    g2.dispose();
                }
            };
            linhaBarra.setOpaque(false);
            linhaBarra.setPreferredSize(new Dimension(0, 7));
            linhaBarra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 7));

            JPanel barraWrapper = new JPanel(new BorderLayout());
            barraWrapper.setOpaque(false);
            barraWrapper.setBorder(new EmptyBorder(2, 4, 4, 4));
            barraWrapper.add(linhaBarra, BorderLayout.CENTER);

            item.add(linhaTexto);
            item.add(barraWrapper);
            container.add(item);
            if (i < ordenado.size() - 1) container.add(Box.createVerticalStrut(2));
        }

        painel.add(container, BorderLayout.CENTER);
        painel.setPreferredSize(new Dimension(320, Math.max(painel.getPreferredSize().height, 220)));
        return painel;
    }

    private void montarRankings(List<Vendedor> lista) {
        painelRanking.removeAll();
        painelRanking.add(criarRankingVisual(lista, "venda",       ACCENT_GREEN,  "💰", "RANKING VENDAS",       isGerente()));
        painelRanking.add(criarRankingVisual(lista, "ticket",      ACCENT_ORANGE, "✅", "RANKING TICKET MÉDIO", isGerente()));
        painelRanking.add(criarRankingVisual(lista, "clientes",    ACCENT_BLUE,   "👥", "RANKING CLIENTES",     isGerente()));
        painelRanking.add(criarRankingVisual(lista, "produtos",    ACCENT_PURPLE, "📦", "RANKING PRODUTOS",     isGerente()));
        painelRanking.add(criarRankingVisual(lista, "prodCliente", ACCENT_GOLD,   "🛒", "RANKING PROD / CLIENTE", isGerente()));
        painelRanking.add(criarRankingVisual(lista, "margem",      ACCENT_TEAL,   "📈", "RANKING MARGEM",       isGerente()));
        painelRanking.revalidate();
        painelRanking.repaint();
    }


    
    public JPanel gerarPainelConteudo(java.sql.Date ini, java.sql.Date fim,
                                      String lojaVal, String idVendedor) {
        
        JPanel cards   = new JPanel(new GridLayout(1, 6, 12, 12));
        JPanel ranking = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setBackground(BG_DARK);
        ranking.setBackground(BG_DARK);

        
        JPanel cardsOrig   = painelCards;
        JPanel rankingOrig = painelRanking;
        painelCards   = cards;
        painelRanking = ranking;

        
        try {
            
            SimpleDateFormat sdfTmp = new SimpleDateFormat("dd/MM/yyyy");
            dtInicio.setValue(null); dtInicio.setText(sdfTmp.format(ini));
            dtFim.setValue(null);    dtFim.setText(sdfTmp.format(fim));
            loja.setText(lojaVal);

            List<Vendedor> rankingLista = carregarRanking(ini, fim, lojaVal);
            rankingMesCache = rankingLista;
            montarRankings(rankingLista);

            if (idVendedor != null) {
                categoriaBusca = buscarCategoriaVendedor(idVendedor);
                montarCards(carregarResumo(ini, fim, lojaVal, idVendedor));
            } else {
                categoriaBusca = null;
                montarCards(consolidarLoja(rankingLista));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        painelCards   = cardsOrig;
        painelRanking = rankingOrig;

        
        JPanel resultado = new JPanel(new BorderLayout(0, 12));
        resultado.setBackground(BG_DARK);
        resultado.add(cards,   BorderLayout.NORTH);
        resultado.add(ranking, BorderLayout.CENTER);
        return resultado;
    }

    
    
    
    private void atualizarDashboard() {
        try {
            
            if (!isGerente()) carregarModoIndicador();

            java.sql.Date ini   = parseData(dtInicio.getText());
            java.sql.Date fim   = parseData(dtFim.getText());

            
            
            if (!isGerente()) {
                java.util.Calendar hoje = java.util.Calendar.getInstance();
                int mesAtual = hoje.get(java.util.Calendar.MONTH) + 1;
                int anoAtual = hoje.get(java.util.Calendar.YEAR);

                
                java.util.Calendar mesPassadoCal = (java.util.Calendar) hoje.clone();
                mesPassadoCal.add(java.util.Calendar.MONTH, -1);
                int mesAnterior = mesPassadoCal.get(java.util.Calendar.MONTH) + 1;
                int anoAnterior = mesPassadoCal.get(java.util.Calendar.YEAR);

                java.util.Calendar calIni = java.util.Calendar.getInstance();
                calIni.setTime(ini);
                java.util.Calendar calFim = java.util.Calendar.getInstance();
                calFim.setTime(fim);

                int mesIniSel = calIni.get(java.util.Calendar.MONTH) + 1;
                int mesFimSel = calFim.get(java.util.Calendar.MONTH) + 1;
                int anoIniSel = calIni.get(java.util.Calendar.YEAR);
                int anoFimSel = calFim.get(java.util.Calendar.YEAR);

                
                boolean noMesAtual    = mesIniSel == mesAtual    && mesFimSel == mesAtual;
                boolean noMesAnterior = mesIniSel == mesAnterior && mesFimSel == mesAnterior;
                boolean mesOk  = noMesAtual || noMesAnterior;
                boolean anoOk  = anoIniSel == anoFimSel;
                boolean anoVal = anoIniSel >= 2023 && anoIniSel <= hoje.get(java.util.Calendar.YEAR);

                if (!mesOk || !anoOk || !anoVal) {
                    String msg;
                    if (!mesOk)       msg = "Você só pode consultar o mês atual (" + mesAtual + "/" + anoAtual
                                            + ") ou o mês anterior (" + mesAnterior + "/" + anoAnterior + ").";
                    else if (!anoOk)  msg = "O ano de início e fim devem ser iguais.";
                    else              msg = "Ano inválido. Consulte entre 2023 e " + hoje.get(java.util.Calendar.YEAR) + ".";
                    JOptionPane.showMessageDialog(this, msg, "Acesso restrito", JOptionPane.WARNING_MESSAGE);
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                    java.util.Calendar c1 = java.util.Calendar.getInstance();
                    c1.set(java.util.Calendar.DAY_OF_MONTH, 1);
                    java.util.Calendar c2 = java.util.Calendar.getInstance();
                    c2.set(java.util.Calendar.DAY_OF_MONTH, c2.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                    dtInicio.setValue(null); dtInicio.setText(sdf.format(c1.getTime()));
                    dtFim.setValue(null);    dtFim.setText(sdf.format(c2.getTime()));
                    return;
                }
            }

            String lojaVal = empresaLogin != null ? empresaLogin : loja.getText().trim();

            List<Vendedor> rankingLista = carregarRanking(ini, fim, lojaVal);
            montarRankings(rankingLista);

            
            java.util.Calendar cMes = java.util.Calendar.getInstance();
            cMes.setTime(ini);
            int _mes = cMes.get(java.util.Calendar.MONTH);
            int _ano = cMes.get(java.util.Calendar.YEAR);
            cMes.set(_ano, _mes, 1, 0, 0, 0);
            cMes.set(java.util.Calendar.MILLISECOND, 0);
            java.sql.Date _iniMes = new java.sql.Date(cMes.getTimeInMillis());
            cMes.set(java.util.Calendar.DAY_OF_MONTH, cMes.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            java.sql.Date _fimMes = new java.sql.Date(cMes.getTimeInMillis());

            
            boolean filtroEhMesInteiro = ini.equals(_iniMes) && fim.compareTo(_fimMes) >= 0;
            if (filtroEhMesInteiro) {
                rankingMesCache = rankingLista;
            } else {
                rankingMesCache = carregarRanking(_iniMes, _fimMes, lojaVal);
            }

            if (isGerente()) {
                Vendedor selecionado = (Vendedor) comboVendedor.getSelectedItem();
                if (selecionado == null || selecionado.id == null) {
                    categoriaBusca = null;
                    Vendedor totalLoja = consolidarLoja(rankingLista);
                    montarCards(totalLoja);
                } else {
                    categoriaBusca = buscarCategoriaVendedor(selecionado.id);
                    montarCards(carregarResumo(ini, fim, lojaVal, selecionado.id));
                }
            } else {
                categoriaBusca = categoriaLogin;
                montarCards(carregarResumo(ini, fim, lojaVal, vendedorLogin));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    
    private String buscarCategoriaVendedor(String idpessoa) {
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT categoria FROM public.usuarios WHERE idpessoa = ?")) {
            ps.setString(1, idpessoa);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("categoria");
        } catch (Exception ignored) {}
        return "ADULTO"; 
    }

    



    private Vendedor consolidarLoja(List<Vendedor> lista) {
        Vendedor total = new Vendedor();
        total.nome = "TOTAL DA LOJA";
        double somaVenda = 0, somaCusto = 0;
        int somaClientes = 0, somaProdutos = 0;
        for (Vendedor v : lista) {
            somaVenda    += v.venda;
            somaClientes += v.clientes;
            somaProdutos += v.produtos;
        }
        total.venda    = somaVenda;
        total.clientes = somaClientes;
        total.produtos = somaProdutos;
        total.ticket   = somaClientes > 0 ? somaVenda / somaClientes : 0;
        total.produtosPorCliente = somaClientes > 0 ? (double) somaProdutos / somaClientes : 0;

        
        
        double somaMargemPonderada = 0;
        for (Vendedor v : lista) {
            somaMargemPonderada += v.margem * v.venda;
        }
        total.margem = somaVenda > 0 ? somaMargemPonderada / somaVenda : 0;

        return total;
    }

    
    
    
    private String getQuery(boolean filtroVendedor) {
        return """
            WITH comissao AS (
                SELECT p.idpessoa, p.nmpessoa, SUM(ci.vlbasecomissao) AS total_comissao
                FROM ishop.comitem ci
                JOIN ishop.documen d ON d.iddocumento = ci.iddocumento
                JOIN ishop.pessoas p ON p.idpessoa = ci.idpessoa
                WHERE d.dtreferencia BETWEEN ? AND ?
                  AND COALESCE(d.stdocumentocancelado, '') <> '*'
                  AND ci.cdempvend = ?
                  """ + (filtroVendedor ? " AND p.idpessoa = ? " : "") + """
                GROUP BY p.idpessoa, p.nmpessoa
            ),
            documentos AS (
                SELECT p.idpessoa, COUNT(DISTINCT d.iddocumento) AS total_clientes
                FROM ishop.comitem ci
                JOIN ishop.documen d ON d.iddocumento = ci.iddocumento
                JOIN ishop.pessoas p ON p.idpessoa = ci.idpessoa
                JOIN ishop.operaca o ON o.idoperacao = d.idoperacao
                WHERE d.dtreferencia BETWEEN ? AND ?
                  AND COALESCE(d.stdocumentocancelado, '') <> '*'
                  AND o.nmoperacao NOT ILIKE '%ESTORNO%'
                GROUP BY p.idpessoa
            ),
            itens AS (
                SELECT ci.idpessoa,
                       SUM(CASE WHEN di.tpoperacao = 'V' THEN di.qtitem ELSE -di.qtitem END) AS qt_itens
                FROM ishop.comitem ci
                JOIN ishop.docitem di ON ci.iddocitem = di.iddocumentoitem
                WHERE di.dtreferencia BETWEEN ? AND ?
                  AND ci.cdempresa = ?
                  AND ci.cdempvend = ?
                  AND COALESCE(di.stdocumentocancelado, '') <> '*'
                GROUP BY ci.idpessoa
            )
            SELECT c.idpessoa, c.nmpessoa,
                   ROUND(c.total_comissao::numeric, 2) AS total_comissao,
                   COALESCE(i.qt_itens, 0) AS qt_itens,
                   COALESCE(d.total_clientes, 0) AS total_clientes,
                   ROUND((c.total_comissao::numeric / NULLIF(d.total_clientes,0)::numeric), 2) AS ticket_medio,
                   ROUND((COALESCE(i.qt_itens,0)::numeric / NULLIF(d.total_clientes,0)::numeric)) AS produtos_por_cliente
            FROM comissao c
            LEFT JOIN documentos d ON d.idpessoa = c.idpessoa
            LEFT JOIN itens i ON i.idpessoa = c.idpessoa
            ORDER BY c.total_comissao DESC
            """;
    }

    private Vendedor carregarResumo(java.sql.Date ini, java.sql.Date fim, String loja, String vend) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(getQuery(true))) {
            int i = 1;
            ps.setDate(i++, ini); ps.setDate(i++, fim); ps.setString(i++, loja); ps.setString(i++, vend);
            ps.setDate(i++, ini); ps.setDate(i++, fim);
            ps.setDate(i++, ini); ps.setDate(i++, fim); ps.setString(i++, loja); ps.setString(i++, loja);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Vendedor v = new Vendedor();
                v.nome               = rs.getString("nmpessoa");
                v.venda              = rs.getDouble("total_comissao");
                v.ticket             = rs.getDouble("ticket_medio");
                v.clientes           = rs.getInt("total_clientes");
                v.produtos           = rs.getInt("qt_itens");
                v.produtosPorCliente = rs.getDouble("produtos_por_cliente");
                double[] d = buscarCustoEVenda(ini, fim, loja, vend);
                v.margem = v.venda <= 0 ? 0 : ((v.venda - d[0]) / v.venda) * 100;
                return v;
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
        return null;
    }

    private List<Vendedor> carregarRanking(java.sql.Date ini, java.sql.Date fim, String loja) {
        List<Vendedor> lista = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(getQuery(false))) {
            int i = 1;
            ps.setDate(i++, ini); ps.setDate(i++, fim); ps.setString(i++, loja);
            ps.setDate(i++, ini); ps.setDate(i++, fim);
            ps.setDate(i++, ini); ps.setDate(i++, fim); ps.setString(i++, loja); ps.setString(i++, loja);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Vendedor v = new Vendedor();
                v.id                 = rs.getString("idpessoa");
                v.nome               = rs.getString("nmpessoa");
                v.venda              = rs.getDouble("total_comissao");
                v.ticket             = rs.getDouble("ticket_medio");
                v.clientes           = rs.getInt("total_clientes");
                v.produtos           = rs.getInt("qt_itens");
                v.produtosPorCliente = rs.getDouble("produtos_por_cliente");
                double[] d = buscarCustoEVenda(ini, fim, loja, v.id);
                v.margem = v.venda <= 0 ? 0 : ((v.venda - d[0]) / v.venda) * 100;
                lista.add(v);
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
        return lista;
    }

    private double[] buscarCustoEVenda(java.sql.Date ini, java.sql.Date fim, String loja, String vendedor) {
        String sql = """
            SELECT SUM(di.vlcusto) AS total_custo, SUM(ci.vlbasecomissao) AS total_venda
            FROM ishop.docitem di
            JOIN ishop.operaca op ON di.idoperacao = op.idoperacao
            JOIN ishop.comitem ci ON ci.iddocitem = di.iddocumentoitem
            JOIN ishop.pessoas p ON p.idpessoa = ci.idpessoa
            WHERE di.dtreferencia BETWEEN ? AND ?
              AND di.stdocumentocancelado <> '*'
              AND op.nmoperacao LIKE '%VEND%'
              AND di.cdempresa = ?
              AND p.idpessoa = ?
            """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, ini); ps.setDate(2, fim); ps.setString(3, loja); ps.setString(4, vendedor);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new double[]{rs.getDouble("total_custo"), rs.getDouble("total_venda")};
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
        return new double[]{0, 0};
    }

    
    
    
    private void alterarSenha() {
        
        JPasswordField nova      = new JPasswordField();
        JPasswordField confirmar = new JPasswordField();
        Object[] msg = {"Nova senha:", nova, "Confirmar senha:", confirmar};
        int op = JOptionPane.showConfirmDialog(this, msg, "Alterar Senha", JOptionPane.OK_CANCEL_OPTION);
        if (op != JOptionPane.OK_OPTION) return;
        String s1 = new String(nova.getPassword());
        String s2 = new String(confirmar.getPassword());
        if (s1.isEmpty())      { JOptionPane.showMessageDialog(this, "Senha não pode ser vazia"); return; }
        if (!s1.equals(s2))    { JOptionPane.showMessageDialog(this, "Senhas não conferem");      return; }
        try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
             PreparedStatement ps = conn.prepareStatement("UPDATE usuarios SET senha = ? WHERE idpessoa = ?")) {
            ps.setString(1, s1);
            ps.setString(2, vendedorLogin);
            int l = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, l > 0 ? "Senha alterada com sucesso" : "Usuário não encontrado");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void sairSistema() { new Login().setVisible(true); dispose(); }

    private void carregarVendedores() {
        comboVendedor.removeAllItems();

        if (isGerente()) {
            
            
            
            try (Connection conn = DriverManager.getConnection(URL_LOGIN, USER_LOGIN, PASS_LOGIN);
                 PreparedStatement ps = conn.prepareStatement("""
                    SELECT idpessoa, username AS nmpessoa
                    FROM public.usuarios
                    WHERE cdempresa = ?
                      AND tipo = 'VENDEDOR'
                      AND ativo = true
                    ORDER BY username
                 """)) {
                ps.setString(1, empresaLogin != null ? empresaLogin : loja.getText().trim());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Vendedor v = new Vendedor();
                    v.id   = rs.getString("idpessoa");
                    v.nome = rs.getString("nmpessoa");
                    comboVendedor.addItem(v);
                }
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro vendedores:\n" + e.getMessage()); }

        } else {
            
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement ps = conn.prepareStatement("""
                    SELECT DISTINCT p.idpessoa, p.nmpessoa
                    FROM ishop.comitem ci
                    JOIN ishop.pessoas p ON p.idpessoa = ci.idpessoa
                    WHERE ci.cdempvend = ?
                    ORDER BY p.nmpessoa
                 """)) {
                ps.setString(1, loja.getText().trim());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Vendedor v = new Vendedor();
                    v.id   = rs.getString("idpessoa");
                    v.nome = rs.getString("nmpessoa");
                    comboVendedor.addItem(v);
                }
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erro vendedores:\n" + e.getMessage()); }
        }
    }

    private void selecionarVendedorLogado() {
        if (vendedorLogin == null) return;
        for (int i = 0; i < comboVendedor.getItemCount(); i++) {
            if (comboVendedor.getItemAt(i).id != null
                    && comboVendedor.getItemAt(i).id.equals(vendedorLogin)) {
                comboVendedor.setSelectedIndex(i);
                break;
            }
        }
    }

    
    
    
    private static final int CAL_DIAS  = 0;
    private static final int CAL_MESES = 1;
    private static final int CAL_ANOS  = 2;

    private JButton criarBotaoCalendario(JFormattedTextField campoAlvo) {
        JButton btn = new JButton("📅") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g2.setColor(new Color(0, 160, 100));
                else if (getModel().isRollover()) g2.setColor(new Color(30, 60, 90));
                else                              g2.setColor(new Color(22, 35, 58));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(ACCENT_GREEN);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> abrirCalendario(campoAlvo, btn));
        return btn;
    }

    private void abrirCalendario(JFormattedTextField campoAlvo, JButton origem) {

        
        java.util.Calendar _cal = java.util.Calendar.getInstance();
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            _cal.setTime(sdf.parse(campoAlvo.getText()));
        } catch (Exception ignored) {}

        
        final int[] estado = {
                _cal.get(java.util.Calendar.YEAR),
                _cal.get(java.util.Calendar.MONTH),
                _cal.get(java.util.Calendar.DAY_OF_MONTH),
                CAL_DIAS
        };
        
        final int[] selecao = { estado[0], estado[1], estado[2] };

        
        JDialog popup = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), false);
        popup.setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_CARD);
        root.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        
        JLabel lblTitulo = new JLabel("", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitulo.setForeground(TEXT_PRIMARY);
        lblTitulo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblTitulo.setBorder(new EmptyBorder(0, 4, 0, 4));

        JButton btnPrev = estilizarNavBtn("◀");
        JButton btnNext = estilizarNavBtn("▶");

        JPanel navPanel = new JPanel(new BorderLayout(4, 0));
        navPanel.setBackground(new Color(16, 24, 40));
        navPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        navPanel.add(btnPrev,  BorderLayout.WEST);
        navPanel.add(lblTitulo, BorderLayout.CENTER);
        navPanel.add(btnNext,  BorderLayout.EAST);

        
        JPanel areaCentral = new JPanel(new BorderLayout());
        areaCentral.setBackground(BG_CARD);
        areaCentral.setBorder(new EmptyBorder(6, 8, 8, 8));

        root.add(navPanel,    BorderLayout.NORTH);
        root.add(areaCentral, BorderLayout.CENTER);
        popup.add(root);

        
        String[] nomeMes  = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        String[] abrevMes = {"Jan","Fev","Mar","Abr","Mai","Jun",
                "Jul","Ago","Set","Out","Nov","Dez"};

        
        
        java.util.function.BiFunction<String,Boolean,JLabel> criarCelula = (texto, destaque) -> {
            JLabel lbl = new JLabel(texto, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (destaque) {
                        g2.setColor(ACCENT_GREEN);
                        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 8, 8);
                    } else if (getClientProperty("hover") != null) {
                        g2.setColor(new Color(40, 60, 90));
                        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", destaque ? Font.BOLD : Font.PLAIN, 12));
            lbl.setForeground(destaque ? BG_DARK : TEXT_PRIMARY);
            lbl.setOpaque(false);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!destaque) { lbl.putClientProperty("hover","1"); lbl.repaint(); }
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    lbl.putClientProperty("hover", null); lbl.repaint();
                }
            });
            return lbl;
        };

        
        Runnable[] desenhar = new Runnable[1];
        desenhar[0] = () -> {
            areaCentral.removeAll();

            if (estado[3] == CAL_DIAS) {
                
                lblTitulo.setText(nomeMes[estado[1]] + "  " + estado[0]);

                btnPrev.addActionListener(null); 
                btnNext.addActionListener(null);

                JPanel grade = new JPanel(new GridLayout(0, 7, 2, 3));
                grade.setBackground(BG_CARD);

                
                for (String s : new String[]{"D","S","T","Q","Q","S","S"}) {
                    JLabel h = new JLabel(s, SwingConstants.CENTER);
                    h.setFont(new Font("SansSerif", Font.BOLD, 10));
                    h.setForeground(TEXT_SECONDARY);
                    h.setBorder(new EmptyBorder(0, 0, 4, 0));
                    grade.add(h);
                }

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(estado[0], estado[1], 1);
                int primeiroDia = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
                int totalDias   = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

                for (int x = 0; x < primeiroDia; x++) grade.add(new JLabel(""));

                for (int dia = 1; dia <= totalDias; dia++) {
                    final int d = dia;
                    boolean sel = (d == selecao[2] && estado[1] == selecao[1] && estado[0] == selecao[0]);
                    JLabel cel = criarCelula.apply(String.valueOf(d), sel);
                    cel.setPreferredSize(new Dimension(32, 28));
                    cel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            campoAlvo.setValue(null);
                            campoAlvo.setText(String.format("%02d/%02d/%04d", d, estado[1]+1, estado[0]));
                            popup.dispose();
                        }
                    });
                    grade.add(cel);
                }
                areaCentral.add(grade, BorderLayout.CENTER);

            } else if (estado[3] == CAL_MESES) {
                
                lblTitulo.setText(String.valueOf(estado[0]));
                JPanel grade = new JPanel(new GridLayout(3, 4, 4, 4));
                grade.setBackground(BG_CARD);
                for (int m = 0; m < 12; m++) {
                    final int mes = m;
                    boolean sel = (m == selecao[1] && estado[0] == selecao[0]);
                    JLabel cel = criarCelula.apply(abrevMes[m], sel);
                    cel.setPreferredSize(new Dimension(58, 36));
                    cel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            estado[1] = mes;
                            estado[3] = CAL_DIAS;
                            desenhar[0].run();
                        }
                    });
                    grade.add(cel);
                }
                areaCentral.add(grade, BorderLayout.CENTER);

            } else {
                
                
                int anoBase = (estado[0] / 12) * 12;
                lblTitulo.setText(anoBase + " – " + (anoBase + 11));
                JPanel grade = new JPanel(new GridLayout(3, 4, 4, 4));
                grade.setBackground(BG_CARD);
                for (int i = 0; i < 12; i++) {
                    final int ano = anoBase + i;
                    boolean sel = (ano == selecao[0]);
                    JLabel cel = criarCelula.apply(String.valueOf(ano), sel);
                    cel.setPreferredSize(new Dimension(58, 36));
                    cel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            estado[0] = ano;
                            estado[3] = CAL_MESES;
                            desenhar[0].run();
                        }
                    });
                    grade.add(cel);
                }
                areaCentral.add(grade, BorderLayout.CENTER);
            }

            areaCentral.revalidate();
            areaCentral.repaint();
            popup.pack();
        };

        
        lblTitulo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (estado[3] == CAL_DIAS)       estado[3] = CAL_MESES;
                else if (estado[3] == CAL_MESES) estado[3] = CAL_ANOS;
                desenhar[0].run();
            }
        });
        btnPrev.addActionListener(e -> {
            if      (estado[3] == CAL_DIAS)  { estado[1]--; if (estado[1]<0)  { estado[1]=11; estado[0]--; } }
            else if (estado[3] == CAL_MESES) { estado[0]--; }
            else                             { estado[0] -= 12; }
            desenhar[0].run();
        });
        btnNext.addActionListener(e -> {
            if      (estado[3] == CAL_DIAS)  { estado[1]++; if (estado[1]>11) { estado[1]=0;  estado[0]++; } }
            else if (estado[3] == CAL_MESES) { estado[0]++; }
            else                             { estado[0] += 12; }
            desenhar[0].run();
        });
        desenhar[0].run();
        java.awt.Point p = origem.getLocationOnScreen();
        popup.setLocation(p.x, p.y + origem.getHeight() + 2);
        popup.setVisible(true);

        popup.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            public void windowLostFocus(java.awt.event.WindowEvent e) { popup.dispose(); }
        });
    }

    private JButton estilizarNavBtn(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setForeground(ACCENT_GREEN);
        b.setBackground(new Color(16, 24, 40));
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JFormattedTextField criarCampoData(String valorInicial) {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            JFormattedTextField campo = new JFormattedTextField(mask);
            campo.setColumns(10);
            campo.setText(valorInicial);
            return campo;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private java.sql.Date parseData(String txt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            return new java.sql.Date(sdf.parse(txt).getTime());
        } catch (Exception e) { throw new RuntimeException("Data inválida: " + txt); }
    }

    private JLabel criarRotulo(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(FONT_FILTER_LBL);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    private JComponent estilizarCampo(JTextField f) {
        f.setFont(FONT_FILTER_FIELD);
        f.setForeground(TEXT_PRIMARY);
        f.setBackground(BG_CARD);
        f.setCaretColor(ACCENT_GREEN);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 6, 5, 6)));
        return f;
    }

    private JComboBox<Vendedor> estilizarCombo(JComboBox<Vendedor> c) {
        c.setFont(FONT_FILTER_FIELD);
        c.setForeground(TEXT_PRIMARY);
        c.setBackground(BG_CARD);
        c.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        c.setPreferredSize(new Dimension(200, 32));
        return c;
    }

    private JButton criarBotao(String texto, Color accent, Color bg) {
        JButton b = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g2.setColor(accent.darker());
                else if (getModel().isRollover()) g2.setColor(accent.brighter());
                else                              g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_BTN);
        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton criarBotaoTopo(String texto, Color accent) {
        JButton b = criarBotao(texto, accent, BG_DARK);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setBorder(new EmptyBorder(3, 12, 3, 12));
        return b;
    }


    private JButton criarBotaoIcone(String icone, Color accent) {
        JButton b = criarBotao(icone, accent, BG_DARK);
        b.setFont(new Font("SansSerif", Font.PLAIN, 14));
        b.setBorder(new EmptyBorder(5, 10, 5, 10));
        return b;
    }

    
    
    
    
    
}