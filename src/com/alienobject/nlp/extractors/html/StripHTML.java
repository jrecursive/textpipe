package com.alienobject.nlp.extractors.html;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.ElementRemover;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

public class StripHTML {

    String strip(String url)
            throws Exception {
        return strip(url, true);
    }

    public static String fixhtml(String content)
            throws Exception {
        ElementRemover remover = new ElementRemover();
        //remover.removeElement("script");
        //remover.removeElement("a");
        StringWriter sw = new StringWriter();
        org.cyberneko.html.filters.Writer writer =
                new org.cyberneko.html.filters.Writer(sw, "UTF-8");
        XMLDocumentFilter[] filters = {
                remover,
                writer,
        };
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        byte[] byteArray = content.getBytes("ISO-8859-1"); // todo: choose a charset
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);
        XMLInputSource source = new XMLInputSource(null, null, null, baos, "UTF-8");
        parser.parse(source);
        //String text = HTMLTagRemover.strip(sw.toString());
        baos.close();
        byteArray = null;
        baos = null;
        return sw.toString();
    }

    public String striphtml(String content)
            throws Exception {
        ElementRemover remover = new ElementRemover();
        remover.removeElement("script");
        remover.removeElement("a");
        StringWriter sw = new StringWriter();
        org.cyberneko.html.filters.Writer writer =
                new org.cyberneko.html.filters.Writer(sw, "UTF-8");
        XMLDocumentFilter[] filters = {
                remover,
                writer,
        };
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        byte[] byteArray = content.getBytes("ISO-8859-1"); // choose a charset
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);
        XMLInputSource source = new XMLInputSource(null, null, null, baos, "UTF-8");
        parser.parse(source);
        String text = HTMLTagRemover.strip(sw.toString());
        baos.close();
        byteArray = null;
        baos = null;
        return text;
    }

    String strip(String url, boolean secondPass)
            throws Exception {

        ElementRemover remover = new ElementRemover();

        /* a reminder how to spare particular elements:
            remover.acceptElement("b", null);
            remover.acceptElement("a", new String[] { "href" });
        */

        // completely remove scripts & links
        remover.removeElement("script");
        remover.removeElement("a");

        StringWriter sw = new StringWriter();

        org.cyberneko.html.filters.Writer writer =
                new org.cyberneko.html.filters.Writer(sw, "UTF-8");

        XMLDocumentFilter[] filters = {
                remover,
                writer,
        };

        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        //parser.setProperty("http://cyberneko.org/html/features/scanner/fix-mswindows-refs", true);
        XMLInputSource source = new XMLInputSource(null, url, null);
        parser.parse(source);

        String text;
        if (secondPass) {
            text = HTMLTagRemover.strip(sw.toString());
        } else {
            text = sw.toString();
        }

        return text;
    }

    public static void main(String args[])
            throws Exception {

        for (String s : args) {
            String text = (new StripHTML()).strip(s);
            String lines[] = text.split("\n");
            for (String line : lines) {
                //System.out.println(s + ": " + line);
                System.out.println(line);
            }
        }
    }
}
