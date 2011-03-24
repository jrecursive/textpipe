package com.alienobject.textpipe.nlp;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classifier;
import com.aliasi.util.AbstractExternalizable;
import com.alienobject.textpipe.nlp.entities.Quote;
import com.alienobject.textpipe.text.StripHTML;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class QuoteExtractor {
    static File modelFile = null;
    static Classifier classifier;
    boolean traceQuotes = false;
    final static StripHTML striphtml = new StripHTML();

    private LPTools lt;

    public QuoteExtractor()
            throws Exception {
        init();
        lt = new LPTools();
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

    /*
     * setTraceQuotes(true) will add tags to the attribution
     *   based on the method(s) that were usde to extract it.
    */
    public void setTraceQuotes(boolean tf) {
        traceQuotes = tf;
    }

    public ArrayList extractQuotesFromURL(String url)
            throws Exception {

        return extractQuotes(geturl(url)); // Snarf.fetch(url));
    }

    public ArrayList extractQuotes(String text)
            throws Exception {

        ArrayList quotes_ar = new ArrayList();
        lt.buildSentences(text);

        ArrayList good = new ArrayList();

        String s;
        for (int i = 1; ((s = lt.nextSentence()) != null); i++) {
            if (s == null || s.trim().equals("")) continue;
            JSONObject jo = getLanguageScoreAndId(s);
            String l_score = jo.getString("score");
            String l_id = jo.getString("id");
            if (!l_id.equals("en")) {
                continue;
            }
            Float fl_score = Float.parseFloat(l_score);
            if (fl_score <= -4.0) {
                continue;
            }
            good.add(s);
        }

        StringBuffer qbuf = new StringBuffer();
        ArrayList quotes = new ArrayList();
        String speaker = null;
        String pt_s = "";
        for (int i = 0; i < good.size(); i++) {
            s = (String) good.get(i);
            s = pt_s + s;
            trace_dbg("\n" + i + " [good] >> " + s);
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

        ArrayList ents = null;
        try {
            ents = getPersonEntities(qbuf.toString());
        } catch (Exception noContent) {
            //noContent.printStackTrace();
            trace_dbg("No content for quotes: " + text);
            return new ArrayList();
        }

        for (int j = 0; j < ents.size(); j++) {
            String ent_val = (String) ents.get(j);
            if (traceQuotes) dbg("* identified PERSON: " + ent_val);
        }

        String p_speaker = "UNKNOWN";


        for (int i = 0; i < quotes.size(); i++) {
            s = (String) quotes.get(i);
            String c_speaker = getSpeakerIfExist(s);
            if (c_speaker == null &&
                    p_speaker != null) {
                if (traceQuotes) speaker = p_speaker + " (implied)";
            } else if (c_speaker == null &&
                    p_speaker == null) {
                speaker = "UNKNOWN";
            } else {
                speaker = c_speaker;
            }

            if (speaker == null) continue;

            // match to extracted ent
            for (int j = 0; j < ents.size(); j++) {
                String ent_val = (String) ents.get(j);
                ent_val = ent_val.toLowerCase();
                String ent_comp_s = speaker.toLowerCase();
                if (ent_val.equals(ent_comp_s)) {
                    if (traceQuotes) speaker += " (matched: equals/" + ent_comp_s + ")";
                } else if (ent_comp_s.indexOf(ent_val) != -1 ||
                        ent_val.indexOf(ent_comp_s) != -1) {
                    if (traceQuotes) speaker += " (matched: indexOf/" + ent_val + ")";
                }
            }
            if (speaker.equals("UNKNOWN") ||
                    speaker.equals("ARTICLE") ||
                    speaker == null) {
                continue;
            }
            if (!s.trim().endsWith(".") &&
                    !s.trim().endsWith("\""))
                s += " [...]";
            if (traceQuotes) dbg("<" + speaker + "> " + s + "\n");
            quotes_ar.add(new Quote(speaker, s));
            p_speaker = c_speaker;
        }
        return quotes_ar;
    }

    private ArrayList getPersonEntities(String s) throws Exception {
        ArrayList p_ents = new ArrayList();
        ArrayList ents = lt.getEntities(s);
        for (int i = 0; i < ents.size(); i++) {
            String ent_s = (String) ents.get(i);
            String[] ent_tup = ent_s.split("\\|");
            String etype = ent_tup[0];
            String ent = ent_tup[1];
            if (ent.indexOf("?") != -1) continue;
            if (ent.length() < 3) continue;
            if (ent.indexOf("\n") != -1) continue;
            if (etype.equals("Person")) {
                p_ents.add(ent);
                trace_dbg("ent>> " + etype + ": " + ent);
            }
        }
        return p_ents;
    }

    private String getSpeakerIfExist(String text) throws Exception {
        String speaker = "";

        trace_dbg("getSpeakerIfExist(" + text + ")");

        lt.extractPOS(text);
        String[] tags = lt.getTags();
        String[] tokens = lt.getTokens();

        String p_token_type = "";
        String p_token = "";
        boolean find_speaker = false;
        int speaker_np_st = -1;
        String speaker_vbd = "";
        boolean vbd_post_np = false;
        boolean found_np_speaker = false;

        for (int i = 0; i < tags.length; i++) {
            String c_token = tokens[i];
            String c_token_type = tags[i];
            if (c_token.equals("\"")) {
                if (p_token.equals(".")) {
                    trace_dbg("trailing quote post attribution, treating as np");
                    return null;
                } else {
                    trace_dbg("cancel speaker scan/begin quote end scan");
                    find_speaker = false;
                }
            }
            if (p_token.equals(",") &&
                    c_token.equals("\"")) {
                // scan for speaker: vbd -> np || np -> vbd
                find_speaker = true;
                trace_dbg("looking for speaker.. ");
            }
            if (find_speaker) {
                trace_dbg("find speaker token/tag >> " + tokens[i] + " <" + tags[i] + ">");
                if (c_token_type.equals("np") ||
                        c_token_type.equals("pps") ||
                        c_token_type.equals("at")) {
                    found_np_speaker = true;
                }

                if (p_token_type.equals("vbd") &&
                        (c_token_type.equals("np") ||
                                c_token_type.equals("pps") ||
                                c_token_type.equals("at"))) {
                    trace_dbg("np start @ " + i);
                    speaker_np_st = i;
                    speaker_vbd = p_token;
                    trace_dbg("speaker verb = " + speaker_vbd);
                    break;
                }
                if ((c_token_type.equals("np") ||
                        c_token_type.equals("pps") ||
                        c_token_type.equals("at")) &&
                        !vbd_post_np) {
                    speaker_np_st = i;
                    vbd_post_np = true;
                }
                if ((p_token_type.equals("np") ||
                        p_token_type.equals("pps") ||
                        c_token_type.equals("at")) &&
                        c_token_type.equals("vbd") &&
                        vbd_post_np) {
                    speaker_vbd = c_token;
                    break;
                }
            }
            p_token = c_token;
            p_token_type = tags[i];
            //System.out.println("^^^ " + tokens[i] + " / " + tags[i]);
        }

        boolean processing_article = false;
        String snp = "";

        if (speaker_np_st != -1) {
            if (tags[speaker_np_st].equals("at"))
                processing_article = true;
        }

        if (speaker_np_st != -1) {
            for (int j = speaker_np_st; j < tags.length; j++) {
                String c_token = tokens[j];
                String c_token_type = tags[j];

                if ((c_token_type.equals("vbd") ||
                        c_token.equals(".")) &&
                        processing_article) {
                    break;
                }
                if (c_token_type.equals("np") ||
                        c_token_type.equals("pps") ||
                        processing_article) {

                    if (c_token_type.equals("np") ||
                            processing_article) {
                        speaker += c_token.trim() + " ";
                    } else {
                        snp = c_token.trim();
                    }
                } else {
                    break;
                }
            }
            speaker = speaker.trim();
            if (speaker.trim().equals("")) {
                trace_dbg("## identified speaker by 3rd. singular nominative pronoun (" + snp + ")");
                speaker = null;
            } else {
                trace_dbg("## identified speaker as '" + speaker + "'");
            }
        } else {
            if (found_np_speaker) {
                speaker = null;
                dbg("## speaker implied");
            } else {
                speaker = "UNKNOWN";
                trace_dbg("!! speaker unknown [" + text + "]");
            }
        }
        if (speaker != null) {
            speaker = speaker.replace(" .", ".");
        }
        if (processing_article &&
                speaker != null) {
            speaker = "[article] " + speaker;
            trace_dbg("~~ tossing out article quote . . [" + speaker + "]");
            speaker = "ARTICLE";
        }

        trace_dbg("------- getSpeakerIfExist [" + speaker + "] --------");

        return speaker;
    }

    private static String geturl(String u) throws Exception {
        String s;
        //URL url = new URL("http://webapptoolkit.com/r/stripurl.php?u=" + URLEncoder.encode(u));
        URL url = new URL(u);
        InputStream is = url.openStream();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        StringBuffer sb = new StringBuffer();
        while ((s = dis.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();
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

    public JSONObject getLanguageScoreAndId(String text) throws Exception {
        Classification classification = classifier.classify(text);
        String cl = classification.toString();
        String[] cl_ar = cl.split("\n");
        String[] cl2_ar = cl_ar[1].split(" ");
        JSONObject jo = new JSONObject();
        jo.put("id", cl2_ar[0].replace("0=", ""));
        jo.put("score", cl2_ar[1]);
        return jo;
    }

    private void pos_test(String text) throws Exception {
        lt.extractPOS(text);
        String[] tags = lt.getTags();
        String[] tokens = lt.getTokens();
        System.err.println("Word  / Tag");
        for (int i = 0; i < tags.length; i++)
            System.err.println("^^^ " + tokens[i] + " / " + tags[i]);
    }

    private void ent_test(String s) throws Exception {
        lt.setforIE();
        ArrayList ents = lt.getEntities(s);
        for (int i = 0; i < ents.size(); i++) {
            String ent_s = (String) ents.get(i);
            String[] ent_tup = ent_s.split("\\|");
            String etype = ent_tup[0];
            String ent = ent_tup[1];
            if (ent.length() == 1) continue;
            if (ent.indexOf("\n") != -1) continue;
            dbg("ent>> " + etype + ": " + ent);
        }
    }

    private void dbg(String s) {
        System.err.println("QuoteExtractor: " + s);
    }

    public static String getfile(String fn) throws Exception {
        File aFile = new File(fn);
        StringBuilder contents = new StringBuilder();
        BufferedReader input = new BufferedReader(new FileReader(aFile));
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } finally {
            input.close();
        }
        return contents.toString();
    }

    public static void main(String args[])
            throws Exception {
        QuoteExtractor qe = new QuoteExtractor();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-trace")) {
                qe.setTraceQuotes(true);
                continue;
            }
            String s = null;
            if (args[i].indexOf("http") == -1) {
                System.err.println("file: " + args[i]);
                s = getfile(args[i]);
            } else {
                System.err.println("url: " + args[i]);
                s = geturl(args[i]);
            }

            s = striphtml.striphtml(s);
            s = StringEscapeUtils.unescapeHtml(s);
            s = StringEscapeUtils.unescapeXml(s);

            ArrayList ar = qe.extractQuotes(s);
            System.out.println("" + ar);
        }
    }

    private void trace_dbg(String s) {
        if (traceQuotes) dbg("trace: " + s);
    }

}

