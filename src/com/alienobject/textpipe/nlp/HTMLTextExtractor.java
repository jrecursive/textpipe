package com.alienobject.textpipe.nlp;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classifier;
import com.aliasi.util.AbstractExternalizable;
import com.alienobject.textpipe.text.StripHTML;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class HTMLTextExtractor {
    static File modelFile = null;
    static Classifier classifier;

    public HTMLTextExtractor() {
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void init()
            throws Exception {
        if (modelFile == null) {
            modelFile = new File("data/models/langid-leipzig-classifier");
            dbg("reading classifier model [" + modelFile + "]");
            classifier =
                    (Classifier) AbstractExternalizable.readObject(modelFile);
        }
    }

    public String extractTextFromURL(String url)
            throws Exception {
        return extractText(geturl(url));
    }

    public String isGoodEnglish(String ss) {
        String s = ss.toLowerCase().trim();
        StringBuffer good_buf = new StringBuffer();
        ArrayList good = new ArrayList();

        LPTools lt = new LPTools();
        try {

            String l_score = getLanguageScore(s);
            Float fl_score = Float.parseFloat(l_score);
            if (fl_score <= -4.0) {
                return null;
            }

            /*String l_id = getLanguageId(s); // eh
            if (!l_id.equals("en")) {
                return null;
            }*/

            good.add(s);
            good_buf.append(s);
            good_buf.append(" ");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            lt.releaseResources();
        }
        if (good_buf.toString().trim().equals("")) return null;
        return good_buf.toString().trim();
    }


    public String extractText(String text)
            throws Exception {

        StripHTML striphtml = new StripHTML();
        String content = striphtml.striphtml(text);
        content = StringEscapeUtils.unescapeHtml(content);
        content = StringEscapeUtils.unescapeXml(content);
        text = content;

        ArrayList quotes_ar = new ArrayList();
        StringBuffer qbuf = new StringBuffer();
        StringBuffer good_buf = new StringBuffer();

        LPTools lt = new LPTools();
        try {
            lt.buildSentences(text);

            ArrayList punted = new ArrayList();
            ArrayList good = new ArrayList();
            String s;

            for (int i = 1; ((s = lt.nextSentence()) != null); i++) {
                String l_score = getLanguageScore(s);
                String l_id = getLanguageId(s); // eh
                if (!l_id.equals("en")) {
                    punted.add("[classification] " + l_id + ", " + l_score + ", " + s);
                    continue;
                }
                Float fl_score = Float.parseFloat(l_score);
                if (fl_score <= -4.0) {
                    punted.add("[score] " + l_id + ", " + l_score + ", " + s);
                    continue;
                }
                good.add(s);
                good_buf.append(s);
                good_buf.append(" ");
            }


            ArrayList quotes = new ArrayList();
            String speaker = null;
            String pt_s = "";
            for (int i = 0; i < good.size(); i++) {
                s = (String) good.get(i);
                s = pt_s + s;
                //dbg("\n" + i + " [good] >> " + s);
                if (s.indexOf(".\"") != -1 ||
                        s.indexOf(",\"") != -1) {
                    quotes.add(s);
                    qbuf.append(s);
                    qbuf.append(" ");
                    pt_s = "";
                } else if (s.startsWith("\"")) {
                    pt_s = s;
                }
            }
        } finally {
            lt.releaseResources();
        }

        striphtml = null;
        return good_buf.toString();
    }

    public static String geturl(String u) throws Exception {
        System.out.println("HTMLTextExtractor: geturl: u = " + u);
        String s;
        StringBuffer sb = new StringBuffer();
        URL url = new URL(u);
        /*
        InputStream is = url.openStream();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        while ((s = dis.readLine()) != null) { 
            sb.append(s);
            sb.append(" ");
        }
        */
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.setConnectTimeout(5000);
        uc.setReadTimeout(5000);
        uc.setRequestMethod("GET");
        uc.connect();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        uc.getInputStream()));
        while ((s = in.readLine()) != null)
            sb.append(s).append(" ");
        in.close();
        uc.disconnect();
        url = null;
        in = null;
        uc = null;
        in = null;
        return sb.toString().trim();
    }

    public String getLanguageScore(String text) throws Exception {
        Classification classification = classifier.classify(text);
        String cl = classification.toString();
        String[] cl_ar = cl.split("\n");
        String[] cl2_ar = cl_ar[1].split(" ");
        return cl2_ar[1];
    }

    public String getLanguageId(String text) throws Exception {
        Classification classification = classifier.classify(text);
        String cl = classification.toString();
        String[] cl_ar = cl.split("\n");
        String[] cl2_ar = cl_ar[1].split(" ");
        return cl2_ar[0].replace("0=", "");
    }

    private void dbg(String s) {
        System.out.println("HTMLTextExtractor: " + s);
    }

    public static String extract(String text) throws Exception {
        HTMLTextExtractor qe = new HTMLTextExtractor();
        String s = qe.extractText(text);
        qe = null;
        return s;
    }

    public static void main(String args[])
            throws Exception {
        HTMLTextExtractor qe = new HTMLTextExtractor();
        for (int i = 0; i < args.length; i++) {
            System.out.println("scraping url: " + args[i]);
            String s = geturl(args[i]);
            s = qe.extractText(s);
            System.out.println("extracted text: " + s);
            System.out.println("\n---------------------\n");
        }
    }
}

