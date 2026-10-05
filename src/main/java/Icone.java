import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class Icone {

    private static final int[] TAMANHOS = {16, 24, 32, 48, 64, 128, 256};
    private static List<Image> imagens;

    private Icone() {}

    public static synchronized List<Image> imagens() {
        if (imagens == null) {
            List<Image> lista = new ArrayList<>();
            for (int t : TAMANHOS) {
                URL url = Icone.class.getResource("/icones/icone_" + t + ".png");
                if (url != null) lista.add(new ImageIcon(url).getImage());
            }
            imagens = lista;
            if (!lista.isEmpty()) {
                try { JOptionPane.getRootFrame().setIconImages(lista); } catch (HeadlessException ignored) { }
            }
        }
        return imagens;
    }

    public static void aplicar(Window janela) {
        List<Image> lista = imagens();
        if (!lista.isEmpty()) janela.setIconImages(lista);
    }
}
