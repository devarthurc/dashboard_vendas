import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import javax.swing.SwingUtilities;

public class FiltrosLayout implements LayoutManager {

    private final Component itemFlexivel;
    private final int larguraMinimaFlex;
    private final int hgap;
    private final int vgap;

    public FiltrosLayout(Component itemFlexivel, int larguraMinimaFlex, int hgap, int vgap) {
        this.itemFlexivel = itemFlexivel;
        this.larguraMinimaFlex = larguraMinimaFlex;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int width = resolveWidth(target);
            int availableWidth = Math.max(width - insets.left - insets.right, larguraMinimaFlex);
            int height = calcularAltura(target, availableWidth) + insets.top + insets.bottom;
            return new Dimension(width, height);
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
            int availableWidth = Math.max(width - insets.left - insets.right, larguraMinimaFlex);

            int n = target.getComponentCount();
            int x0 = insets.left;
            int y = insets.top;

            int rowStart = 0;
            int rowWidthUsada = 0;
            int itensNaLinha = 0;

            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (!c.isVisible()) continue;

                int larguraItem = (c == itemFlexivel) ? larguraMinimaFlex : c.getPreferredSize().width;
                int extra = (itensNaLinha == 0) ? 0 : hgap;

                if (itensNaLinha > 0 && rowWidthUsada + extra + larguraItem > availableWidth) {
                    int rowHeight = alturaLinha(target, rowStart, i);
                    posicionarLinha(target, rowStart, i, x0, y, availableWidth, rowHeight);
                    y += rowHeight + vgap;
                    rowStart = i;
                    rowWidthUsada = 0;
                    itensNaLinha = 0;
                    extra = 0;
                }

                rowWidthUsada += extra + larguraItem;
                itensNaLinha++;
            }

            if (itensNaLinha > 0) {
                int rowHeight = alturaLinha(target, rowStart, n);
                posicionarLinha(target, rowStart, n, x0, y, availableWidth, rowHeight);
            }
        }
    }

    private int alturaLinha(Container target, int from, int to) {
        int h = 0;
        for (int k = from; k < to; k++) {
            Component c = target.getComponent(k);
            if (!c.isVisible()) continue;
            h = Math.max(h, c.getPreferredSize().height);
        }
        return h;
    }

    private void posicionarLinha(Container target, int from, int to, int x0, int y, int availableWidth, int rowHeight) {
        
        int fixedSum = 0;
        int count = 0;
        boolean temFlex = false;
        for (int k = from; k < to; k++) {
            Component c = target.getComponent(k);
            if (!c.isVisible()) continue;
            count++;
            if (c == itemFlexivel) { temFlex = true; }
            else fixedSum += c.getPreferredSize().width;
        }
        if (count == 0) return;

        int gaps = (count - 1) * hgap;
        int flexWidth = temFlex ? Math.max(availableWidth - fixedSum - gaps, larguraMinimaFlex) : 0;

        int x = x0;
        for (int k = from; k < to; k++) {
            Component c = target.getComponent(k);
            if (!c.isVisible()) continue;

            int w = (c == itemFlexivel) ? flexWidth : c.getPreferredSize().width;
            int h = c.getPreferredSize().height;
            c.setBounds(x, y, w, h);
            x += w + hgap;
        }
    }

    private int calcularAltura(Container target, int availableWidth) {
        int n = target.getComponentCount();
        int total = 0;
        int rowWidthUsada = 0;
        int rowHeight = 0;
        boolean primeiraLinha = true;

        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) continue;

            int larguraItem = (c == itemFlexivel) ? larguraMinimaFlex : c.getPreferredSize().width;
            int extra = (rowWidthUsada == 0 ? 0 : hgap);

            if (rowWidthUsada > 0 && rowWidthUsada + extra + larguraItem > availableWidth) {
                total += rowHeight + (primeiraLinha ? 0 : vgap);
                primeiraLinha = false;
                rowWidthUsada = 0;
                rowHeight = 0;
                extra = 0;
            }

            rowWidthUsada += extra + larguraItem;
            rowHeight = Math.max(rowHeight, c.getPreferredSize().height);
        }
        if (rowWidthUsada > 0) {
            total += rowHeight + (primeiraLinha ? 0 : vgap);
        }
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
        return w > 0 ? w : larguraMinimaFlex;
    }
}
