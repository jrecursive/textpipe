package com.alienobject.nlp.extractors.html;

import com.alienobject.nlp.extractors.LPHTMLTextExtractor;
import org.apache.lucene.demo.html.HTMLParser;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* HTMLUtils
 * easy to use,
 * zero setup,
 * plainly useful.
 *
 * everything else, see: commons
*/

public class HTMLUtils {
    public static String getURLContent(String u)
            throws Exception {
        URL url = new URL(u);
        InputStream is = url.openStream();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        StringBuffer sb = new StringBuffer();
        String s;
        while ((s = dis.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String getURLText(String u)
            throws Exception {
        String content = getURLContent(u);
        return (new StripHTML()).striphtml(content);
    }

    public HashMap dumpHTMLMetadata(String s)
            throws Exception {
        if (s == null || s.trim().equals("")) return new HashMap();
        byte[] byteArray = s.getBytes("ISO-8859-1"); // choose a charset
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);
        HTMLParser parser = new HTMLParser(baos);
        HashMap hm = new HashMap();
        hm.put("title", parser.getTitle());
        hm.put("summary", parser.getSummary());
        hm.put("meta", parser.getMetaTags());
        parser.suicide();
        baos.close();
        baos = null;
        byteArray = null;
        parser = null;

        return hm;
    }

    /* TODO: clean up this duplicate code */
    public static List<String> extractChunkedText(Reader reader) throws IOException {
        final ArrayList<String> text_ar = new ArrayList<String>();

        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback() {
            boolean process = false;
            LPHTMLTextExtractor te = new LPHTMLTextExtractor();
            String lmtag = "?";
            String btext = "";
            int ord = 0;

            public void handleText(final char[] data, final int pos) {
                if (!process) return;
                String s = new String(data);
                if (s.trim().equals("")) return;

                if (te.isGoodEnglish(s) != null) {
                    //s = "(" + lmtag + ") " + s;
                    //System.out.println("add " + s);
                    btext += s;
                    //text_ar.add(s);
                } else {
                    //System.out.println("fail: " + s);
                }
            }

            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) {
                lmtag = "" + tag;
                if (tag == Tag.P || tag == Tag.DIV) {
                    System.out.println("P START");
                    process = true;
                }
            }

            public void handleEndTag(Tag tag, final int pos) {
                if (tag == Tag.P || tag == Tag.DIV) {
                    System.out.println("P END");
                    btext = btext.trim();
                    if (btext.equals("")) return;
                    System.out.println("btext = " + btext);
                    text_ar.add(btext);
                    btext = "";
                    process = false;
                }
            }

            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {
            }

            public void handleComment(final char[] data, final int pos) {
            }

            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };
        parserDelegator.parse(reader, parserCallback, true);
        for (String x : text_ar) {
            System.out.println(">> " + x);
        }
        return text_ar;
    }

    /* clean up this duplicate code */
    public static List<String> extractLinks(Reader reader) throws IOException {
        final ArrayList<String> list = new ArrayList<String>();
        final ArrayList<String> text_ar = new ArrayList<String>();

        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback() {
            int ord = 0;

            public void handleText(final char[] data, final int pos) {
                text_ar.add(new String(data));
            }

            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == Tag.A) {
                    ord++;
                    String address = (String) attribute.getAttribute(Attribute.HREF);
                    list.add(address);
                }
            }

            public void handleEndTag(Tag t, final int pos) {
            }

            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {
            }

            public void handleComment(final char[] data, final int pos) {
            }

            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };
        parserDelegator.parse(reader, parserCallback, true);
        return list;
    }

    /* clean up this duplicate code */
    public static HashMap<String, String> extractTextAndLinks(Reader reader) throws IOException {
        final ArrayList<String> list = new ArrayList<String>();
        final ArrayList<String> text_ar = new ArrayList<String>();

        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback() {
            //HTMLTextExtractor te = new HTMLTextExtractor();
            boolean grabtext = false;

            public void handleText(final char[] data, final int pos) {
                String s = new String(data);
                s = s.replaceAll("[^A-Za-z ]", "");
                if (s.length() > 30) return;
                if (s != null && !s.trim().equals("") && s.trim().length() > 5)
                    if (grabtext) {
                        text_ar.add(s);
                        //System.out.println("text = " + s);
                        grabtext = false;
                    }
                /*if (te.isGoodEnglish(s)!=null) {
                    System.out.println("checking " + s);
                    text_ar.add(s);
                } else {
                    System.out.println("fail: " + s);
                }*/
            }

            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == Tag.A) {
                    String address = (String) attribute.getAttribute(Attribute.HREF);
                    //System.out.println("href = " + address);
                    if (!grabtext) {
                        list.add(address);
                        grabtext = true;
                    }
                }
            }

            public void handleEndTag(Tag t, final int pos) {
            }

            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {
            }

            public void handleComment(final char[] data, final int pos) {
            }

            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };
        parserDelegator.parse(reader, parserCallback, true);

        HashMap<String, String> hm = new HashMap<String, String>();
        if (list.size() == text_ar.size()) {
            for (int i = 0; i < list.size(); i++) {
                String href = list.get(i);
                String lbl = text_ar.get(i);
                if (href == null || lbl == null) continue;
                hm.put(href, lbl);
            }
        }

        return hm;
    }

    /* clean up this duplicate code */
    public static List<String> build_dot_tree(Reader reader) throws IOException {
        final ArrayList<String> ar = new ArrayList<String>();
        ar.add("digraph fuck {\n");


        ParserDelegator parserDelegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback() {
            String p_tag = "root";
            String c_tag = "root";
            int tagnum = 0;

            int ord = 0;

            public void handleText(final char[] data, final int pos) {
                if ((new String(data)).trim().equals("")) return;
                ar.add("\t\"" + c_tag + "\" -> " +
                        "\"" + (new String(data)).replaceAll("\"", "'") +
                        "\";\n");
            }

            public void handleStartTag(Tag tag, MutableAttributeSet attribute, int pos) {

                if (tag == Tag.SCRIPT) return;
                if (tag == Tag.HEAD) return;
                if (tag == Tag.FORM) return;
                if (tag == Tag.SELECT) return;
                if (tag == Tag.OPTION) return;
                if (tag == Tag.UL) return;
                if (tag == Tag.LI) return;
                if (tag == Tag.META) return;

                String new_tag = "" + tag + "-" + tagnum;
                c_tag = new_tag;
                ar.add("\t\"" + c_tag + "\" -> \"" + p_tag + "\";\n");
                //tagnum++;

            }

            public void handleEndTag(Tag tag, final int pos) {
                if (tag == Tag.SCRIPT) return;
                if (tag == Tag.HEAD) return;
                if (tag == Tag.FORM) return;
                if (tag == Tag.SELECT) return;
                if (tag == Tag.OPTION) return;
                if (tag == Tag.UL) return;
                if (tag == Tag.LI) return;
                if (tag == Tag.META) return;
                String new_tag = "" + tag + "-" + tagnum;
                p_tag = c_tag;
                c_tag = new_tag;
                if (!p_tag.equals(c_tag)) {
                    // hmm
                }
                tagnum++;
            }

            public void handleSimpleTag(Tag t, MutableAttributeSet a, final int pos) {
                ar.add("\t\"" + c_tag + "\" -> \"" + t + "\";\n");
            }

            public void handleComment(final char[] data, final int pos) {
            }

            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };

        parserDelegator.parse(reader, parserCallback, true);
        ar.add("\n}\n\n");

        return ar;
    }

    public static void main(String[] args) throws Exception {
        //getLinksFromURL(args[0]);
        //getTextFromURL(args[0]);
        getChunkedTextFromURL(args[0]);
        //getdottree(args[0]);
    }

    public static List<String> getLinksFromURL(String u) throws Exception {
        Snarf snarf = new Snarf();
        String content = snarf.fetch(u);
        return getLinksFromHTML(content);
    }

    public static List<String> getLinksFromHTML(String content) throws Exception {
        StringReader reader = new StringReader(content);
        List<String> links = HTMLUtils.extractLinks(reader);
        /*
        for (String link : links) {
            System.out.println("HTMLUtils.getLinksFromHTML: " + link);
        }
        */
        return links;
    }

    public static HashMap<String, String> getTextFromURL(String u) throws Exception {
        Snarf snarf = new Snarf();
        String content = snarf.fetch(u);
        return getTextLinksFromHTML(content);
    }

    public static void getdottree(String u) throws Exception {
        String content = (new Snarf()).fetch(u);
        String c2 = content.replaceAll("<br /><br />", "</p><p>");
        c2 = c2.replaceAll("<br?>[ \\\r\\\n]<br?>", "</p><p>");
        c2 = c2.replaceAll("<\\/?font[^>]*>", "");
        System.out.println("c2 = " + c2);

        //StringReader reader = new StringReader(content);
        StringReader reader;

        byte[] byteArray = c2.getBytes(); // todo: choose a charset
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        Tidy tidy = new Tidy();
        Document tdoc = tidy.parseDOM(bais, baos);

        NodeList p_ar = tdoc.getElementsByTagName("p");
        for (int i = 0; i < p_ar.getLength(); i++) {
            org.w3c.dom.Node n = p_ar.item(i);
            NodeList fuck = n.getChildNodes();
            for (int k = 0; k < fuck.getLength(); k++) {
                org.w3c.dom.Node nn = fuck.item(k);
                if (nn instanceof org.w3c.tidy.DOMTextImpl) {
                    org.w3c.tidy.DOMTextImpl fuckYou = (org.w3c.tidy.DOMTextImpl) nn;
                    System.out.println("nn(p) = " + fuckYou.getData());
                }
            }
        }

        reader = new StringReader(baos.toString());
        HTMLUtils url_toolz = new HTMLUtils();
        List<String> ar = build_dot_tree(reader);
        String s = "";
        for (String x : ar) {
            System.out.println(x);
            s += x + "\n";
        }
        BufferedWriter out = new BufferedWriter(
                new FileWriter("dom.dot", false));
        out.write(s);
        out.close();

    }

    public static List<String> getChunkedTextFromURL(String u) throws Exception {
        Snarf snarf = new Snarf();
        String content = snarf.fetch(u);

        String c2 = content.replaceAll("<br /><br />", "</p><p>");
        c2 = c2.replaceAll("<br?>[ \\\r\\\n]<br?>", "</p><p>");
        c2 = c2.replaceAll("<\\/?font[^>]*>", "");
        System.out.println("c2 = " + c2);

        //StringReader reader = new StringReader(content);
        StringReader reader;

        byte[] byteArray = c2.getBytes(); // todo: choose a charset
        ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringWriter sw = new StringWriter();
        Tidy tidy = new Tidy();
        Document tdoc = tidy.parseDOM(bais, baos);

        ArrayList<String> slippers = new ArrayList<String>();
        System.out.println(" -- p ");
        NodeList p_ar = tdoc.getElementsByTagName("p");
        slippers.addAll(dumpNodeList(p_ar));
        System.out.println(" -- div ");
        p_ar = tdoc.getElementsByTagName("div");
        slippers.addAll(dumpNodeList(p_ar));
        System.out.println(" -- span ");
        p_ar = tdoc.getElementsByTagName("span");
        slippers.addAll(dumpNodeList(p_ar));

        if (slippers.size() == 0) {
            System.out.println(" -- a ");
            p_ar = tdoc.getElementsByTagName("a");
            slippers.addAll(dumpNodeList(p_ar));
        }

        //reader = new StringReader(baos.toString());
        //System.out.println(baos.toString());

        ArrayList<String> str_ok = new ArrayList<String>();
        ArrayList<String> str_better = new ArrayList<String>();

        for (String sl : slippers) {
            sl = sl.trim();
            if ((sl.split(",")).length > 1 ||
                    (sl.split("\\.")).length > 1) {
                if (sl.toLowerCase().indexOf("all rights reserved") != -1 &&
                        sl.toLowerCase().indexOf("copyright") != -1) {
                } else {
                    str_better.add(sl);
                }
            } else {
                str_ok.add(sl);
            }
        }

        for (String sl : str_better) {
            System.out.println("better>> " + sl);
        }
        for (String sl : str_ok) {
            System.out.println("ok>> " + sl);
        }

        return slippers;
        //return url_toolz.extractChunkedText(reader);
    }

    private static ArrayList<String> dumpNodeList(NodeList p_ar) throws Exception {
        ArrayList<String> ar = new ArrayList<String>();
        LPHTMLTextExtractor te = new LPHTMLTextExtractor();
        for (int i = 0; i < p_ar.getLength(); i++) {
            org.w3c.dom.Node n = p_ar.item(i);
            NodeList fuck = n.getChildNodes();
            for (int k = 0; k < fuck.getLength(); k++) {
                org.w3c.dom.Node nn = fuck.item(k);
                if (nn instanceof org.w3c.tidy.DOMTextImpl) {
                    org.w3c.tidy.DOMTextImpl fuckYou = (org.w3c.tidy.DOMTextImpl) nn;
                    String z = fuckYou.getData().trim();
                    if (z.equals("")) continue;
                    if ((z.split(" ")).length < 5) continue;
                    if (te.isGoodEnglish(z) != null) {
                        //System.out.println("z = " + z);
                        ar.add(z);
                    }
                }
            }
        }
        return ar;
    }

    public static HashMap<String, String> getTextLinksFromHTML(String content) throws Exception {
        StringReader reader = new StringReader(content);
        HashMap<String, String> links = HTMLUtils.extractTextAndLinks(reader);
        //System.out.println(links);
        return links;
    }
}

