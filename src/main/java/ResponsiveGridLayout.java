import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class ResponsiveGridLayout implements LayoutManager {

    private final int minItemWidth;
    private final int hgap;
    private final int vgap;
    private final boolean esticarAltura;

    public ResponsiveGridLayout(int minItemWidth, int hgap, int vgap) {
        this(minItemWidth, hgap, vgap, false);
    }


    public ResponsiveGridLayout(int minItemWidth, int hgap, int vgap, boolean esticarAltura) {
        this.minItemWidth = minItemWidth;
        this.hgap = hgap;
        this.vgap = vgap;
        this.esticarAltura = esticarAltura;
    }

    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int width = resolveWidth(target);
            int availableWidth = Math.max(width - insets.left - insets.right, minItemWidth);

            int columns = calcularColunas(availableWidth, target.getComponentCount());
            int totalHeight = calcularAlturaTotal(target, columns);

            return new Dimension(width, totalHeight + insets.top + insets.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return preferredLayoutSize(target);
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int width = target.getWidth() > 0 ? target.getWidth() : resolveWidth(target);
            int availableWidth = Math.max(width - insets.left - insets.right, minItemWidth);

            int n = target.getComponentCount();
            int columns = calcularColunas(availableWidth, n);
            if (columns <= 0) return;

            
            List<int[]> linhas = new ArrayList<>(); 
            List<Integer> alturasNaturais = new ArrayList<>();

            int i = 0;
            while (i < n) {
                int rowStart = i;
                int itensNaLinha = 0;
                int j = i;
                while (j < n && itensNaLinha < columns) {
                    if (target.getComponent(j).isVisible()) itensNaLinha++;
                    j++;
                }
                int rowEnd = j;

                if (itensNaLinha == 0) { i = rowEnd; continue; }

                int alturaLinha = 0;
                for (int k = rowStart; k < rowEnd; k++) {
                    Component c = target.getComponent(k);
                    if (!c.isVisible()) continue;
                    alturaLinha = Math.max(alturaLinha, c.getPreferredSize().height);
                }

                linhas.add(new int[]{rowStart, rowEnd, itensNaLinha});
                alturasNaturais.add(alturaLinha);

                i = rowEnd;
            }

            if (linhas.isEmpty()) return;

            
            int extraPorLinha = 0;
            if (esticarAltura) {
                int alturaNaturalTotal = 0;
                for (int h : alturasNaturais) alturaNaturalTotal += h;
                alturaNaturalTotal += (linhas.size() - 1) * vgap;

                int alturaDisponivel = target.getHeight() - insets.top - insets.bottom;
                if (alturaDisponivel > alturaNaturalTotal) {
                    extraPorLinha = (alturaDisponivel - alturaNaturalTotal) / linhas.size();
                }
            }

            int x0 = insets.left;
            int y = insets.top;

            for (int idx = 0; idx < linhas.size(); idx++) {
                int[] linha = linhas.get(idx);
                int rowStart = linha[0], rowEnd = linha[1], itensNaLinha = linha[2];
                int rowHeight = alturasNaturais.get(idx) + extraPorLinha;

                int colWidthLinha = (availableWidth - (itensNaLinha - 1) * hgap) / itensNaLinha;

                int col = 0;
                for (int k = rowStart; k < rowEnd; k++) {
                    Component c = target.getComponent(k);
                    if (!c.isVisible()) continue;

                    int x = x0 + col * (colWidthLinha + hgap);
                    c.setBounds(x, y, colWidthLinha, rowHeight);
                    col++;
                }

                y += rowHeight + vgap;
            }
        }
    }

    private int calcularColunas(int availableWidth, int totalItens) {
        int colunas = Math.max(1, (availableWidth + hgap) / (minItemWidth + hgap));
        return Math.max(1, Math.min(colunas, Math.max(totalItens, 1)));
    }

    private int calcularAlturaTotal(Container target, int columns) {
        int n = target.getComponentCount();
        if (n == 0 || columns <= 0) return 0;

        int total = 0;
        int col = 0;
        int rowHeight = 0;
        int rows = 0;

        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) continue;

            rowHeight = Math.max(rowHeight, c.getPreferredSize().height);
            col++;
            if (col >= columns) {
                total += rowHeight;
                rows++;
                col = 0;
                rowHeight = 0;
            }
        }
        if (col > 0) { total += rowHeight; rows++; }
        if (rows > 1) total += (rows - 1) * vgap;

        return total;
    }

    private int resolveWidth(Container target) {
        Window janela = SwingUtilities.getWindowAncestor(target);
        if (janela != null && janela.getWidth() > 0) {
            return janela.getWidth();
        }
        Container container = target;
        while (container.getSize().width == 0 && container.getParent() != null) {
            container = container.getParent();
        }
        int w = container.getWidth();
        return w > 0 ? w : minItemWidth;
    }
}
