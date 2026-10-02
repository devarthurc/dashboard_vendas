import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class XlsxLeitor {

    private XlsxLeitor() {}

    public static List<List<String>> lerPrimeiraPlanilha(File arquivo) throws Exception {
        try (ZipFile zip = new ZipFile(arquivo)) {
            List<String> compartilhadas = lerStringsCompartilhadas(zip);
            ZipEntry planilha = localizarPrimeiraPlanilha(zip);
            if (planilha == null) throw new Exception("Nenhuma planilha encontrada no arquivo.");

            Document doc = parse(zip, planilha);
            List<List<String>> linhas = new ArrayList<>();
            NodeList rows = doc.getElementsByTagNameNS("*", "row");
            for (int r = 0; r < rows.getLength(); r++) {
                Element row = (Element) rows.item(r);
                List<String> valores = new ArrayList<>();
                NodeList cells = row.getElementsByTagNameNS("*", "c");
                for (int c = 0; c < cells.getLength(); c++) {
                    Element cel = (Element) cells.item(c);
                    int col = indiceColuna(cel.getAttribute("r"), valores.size());
                    while (valores.size() < col) valores.add("");
                    valores.add(valorCelula(cel, compartilhadas));
                }
                linhas.add(valores);
            }
            return linhas;
        }
    }

    private static String valorCelula(Element cel, List<String> compartilhadas) {
        String tipo = cel.getAttribute("t");
        if ("inlineStr".equals(tipo)) return textoDe(cel);
        String v = primeiroTexto(cel, "v");
        if (v == null) return "";
        if ("s".equals(tipo)) {
            int idx = Integer.parseInt(v.trim());
            return idx < compartilhadas.size() ? compartilhadas.get(idx) : "";
        }
        return v.trim();
    }

    private static List<String> lerStringsCompartilhadas(ZipFile zip) throws Exception {
        List<String> lista = new ArrayList<>();
        ZipEntry e = zip.getEntry("xl/sharedStrings.xml");
        if (e == null) return lista;
        NodeList sis = parse(zip, e).getElementsByTagNameNS("*", "si");
        for (int i = 0; i < sis.getLength(); i++) lista.add(textoDe((Element) sis.item(i)));
        return lista;
    }

    private static ZipEntry localizarPrimeiraPlanilha(ZipFile zip) throws Exception {
        ZipEntry wb = zip.getEntry("xl/workbook.xml");
        ZipEntry rels = zip.getEntry("xl/_rels/workbook.xml.rels");
        if (wb != null && rels != null) {
            NodeList sheets = parse(zip, wb).getElementsByTagNameNS("*", "sheet");
            if (sheets.getLength() > 0) {
                Element s = (Element) sheets.item(0);
                String rid = s.getAttributeNS(
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
                NodeList rs = parse(zip, rels).getElementsByTagNameNS("*", "Relationship");
                for (int i = 0; i < rs.getLength(); i++) {
                    Element r = (Element) rs.item(i);
                    if (rid.equals(r.getAttribute("Id"))) {
                        String alvo = r.getAttribute("Target");
                        alvo = alvo.startsWith("/") ? alvo.substring(1) : "xl/" + alvo;
                        ZipEntry e = zip.getEntry(alvo);
                        if (e != null) return e;
                    }
                }
            }
        }
        return zip.getEntry("xl/worksheets/sheet1.xml");
    }

    private static Document parse(ZipFile zip, ZipEntry e) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream in = zip.getInputStream(e)) {
            return f.newDocumentBuilder().parse(in);
        }
    }


    private static String textoDe(Element el) {
        StringBuilder sb = new StringBuilder();
        NodeList ts = el.getElementsByTagNameNS("*", "t");
        for (int i = 0; i < ts.getLength(); i++) {
            Node pai = ts.item(i).getParentNode();

            if (pai != null && "rPh".equals(pai.getLocalName())) continue;
            sb.append(ts.item(i).getTextContent());
        }
        return sb.toString();
    }

    private static String primeiroTexto(Element el, String tag) {
        NodeList l = el.getElementsByTagNameNS("*", tag);
        return l.getLength() == 0 ? null : l.item(0).getTextContent();
    }

    private static int indiceColuna(String ref, int padrao) {
        if (ref == null || ref.isEmpty()) return padrao;
        int col = 0, i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
            col = col * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
            i++;
        }
        return i == 0 ? padrao : col - 1;
    }
}
