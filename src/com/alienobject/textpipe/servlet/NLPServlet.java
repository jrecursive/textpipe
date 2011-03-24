package com.alienobject.textpipe.servlet;

import com.alienobject.textpipe.nlp.HTMLTextExtractor;
import com.alienobject.textpipe.spiders.URLTools;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;
import nlp.NLP;
import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NLPServlet extends HttpServlet {
    static NLP nlp = null;
    private ExecutorService pool = Executors.newCachedThreadPool(); // newFixedThreadPool(16);

    @Override
    public void init(ServletConfig config) throws ServletException {
        /* will init itself if necessary */
        try {
            dbg("com.alienobject.textpipe.servlet.NLPSerlvet: init(" + config + ")");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest servletRequest,
                      HttpServletResponse servletResponse) throws ServletException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException {
        try {
            JSONObject res = new JSONObject();
            String action = request.getParameter("do");
            String pretty = request.getParameter("pretty");
            if (pretty == null) pretty = "false";
            if (action == null) return; // throw new Exception("Must specify 'do'.");
            dbg("action: " + action);

            // HTML PROCESSING & EXTRACTION
            if (action.equals("extract")) {
                /*
                String[] what = { "text", "metadata", "links" };
                if (request.getParameter("what") != null) {
                    what = request.getParameter("what").split(",");
                }*/
                String u = request.getParameter("url");
                res = munch_url(u);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                response.flushBuffer();
                return;

            } else if (action.equals("extract_many")) {
                String body_str = null;
                MultipartParser mp = new MultipartParser(request, 1024 * 1024);
                Part part;
                while ((part = mp.readNextPart()) != null) {
                    String part_name = part.getName();
                    if (part.getName().equals("body")) {
                        if (part.isParam()) {
                            ParamPart paramPart = (ParamPart) part;
                            body_str = paramPart.getStringValue("UTF-8");
                        }
                    }
                }
                //dbg("body_str = " + body_str);
                JSONArray urls = new JSONArray(body_str);

                long t_st = System.currentTimeMillis();
                Set<Future<JSONObject>> fRes = new HashSet<Future<JSONObject>>();
                for (int i = 0; i < urls.length(); i++) {
                    String u = urls.getString(i);
                    Callable<JSONObject> callable =
                            new UrlExtractCallable(u);
                    Thread.sleep(50);
                    Future<JSONObject> future = pool.submit(callable);
                    fRes.add(future);
                }
                Set<Future<JSONObject>> complete = new HashSet<Future<JSONObject>>();
                while (true) {
                    for (Future<JSONObject> f : fRes) {
                        if (!complete.contains(f) && f.isDone()) {
                            complete.add(f);
                            JSONObject fjo = f.get();
                            String _url = fjo.getString("_url");
                            fjo.remove("_url");
                            res.put(_url, fjo);
                        }
                    }
                    if (complete.size() == fRes.size()) break;
                }
                fRes = null;
                complete = null;
                mp = null;
                part = null;
                urls = null;
                System.gc();

                long t_end = System.currentTimeMillis();
                long t_elapsed = (t_end - t_st);
                dbg("extract_many: t_elapsed: " + t_elapsed);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                response.flushBuffer();
                return;

                // WORDNET LOOKUP
            } else if (action.equals("wn")) {
                long wn_st = System.currentTimeMillis();
                String ss = null;

                if (request.getParameter("post") == null) {
                    ss = request.getParameter("s");
                } else {
                    MultipartParser mp = new MultipartParser(request, 1024 * 128);
                    Part part;
                    while ((part = mp.readNextPart()) != null) {
                        String part_name = part.getName();
                        if (part.getName().equals("body")) {
                            if (part.isParam()) {
                                ParamPart paramPart = (ParamPart) part;
                                ss = paramPart.getStringValue("UTF-8");
                            }
                        }
                    }
                }

                if (nlp == null) {
                    dbg("new NLP()");
                    nlp = new NLP();
                }

                String[] s_ar = ss.split(",");
                JSONArray result = new JSONArray();
                for (String s : s_ar) {
                    s = s.trim();
                    JSONArray res1 = nlp.wordnet_lookup(s);
                    for (int i = 0; i < res1.length(); i++) {
                        JSONArray ar1 = res1.getJSONArray(i);
                        for (int j = 0; j < ar1.length(); j++) {
                            JSONObject ar1_obj = ar1.getJSONObject(j);
                            ar1_obj.put("__word", s);
                            result.put(ar1_obj);
                        }
                    }
                }

                //res.put("result", nlp.wordnet_lookup(s));
                res.put("result", result);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                response.flushBuffer();
                long wn_end = System.currentTimeMillis();
                dbg("wn request in " + (wn_end - wn_st) + "ms");
                return;

                // PART OF SPEECH
            } else if (action.equals("pos")) {
                if (nlp == null) {
                    nlp = new NLP();
                }
                String text = request.getParameter("text");
                nlp.process_pos(text);
                String[] tokens = nlp.get_pos_tokens();
                String[] tags = nlp.get_pos_tags();
                JSONArray pos_ar = new JSONArray();
                for (int i = 0; i < tokens.length; i++) {
                    JSONObject obj = new JSONObject();
                    obj.put("token", tokens[i]);
                    obj.put("tag", tags[i]);
                    pos_ar.put(obj);
                }
                res.put("pos", pos_ar);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                return;

                // SENTENCE EXTRACTION
            } else if (action.equals("sentences")) {
                if (nlp == null) nlp = new NLP();
                String text = request.getParameter("text");
                ArrayList sentences = nlp.extract_sentences(text);
                JSONArray s_ar = new JSONArray();
                for (int i = 0; i < sentences.size(); i++) {
                    //dbg(">>> " + sentences.get(i));
                    s_ar.put(sentences.get(i));
                }
                res.put("sentences", s_ar);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                return;

                // ENTITY EXTRACTION
            } else if (action.equals("entities")) {
                if (nlp == null) nlp = new NLP();
                String text = request.getParameter("text");
                ArrayList entities = nlp.extract_entities(text);
                JSONArray s_ar = new JSONArray();
                for (int i = 0; i < entities.size(); i++) {
                    System.out.println(entities.toString());
                    String entstr = (String) entities.get(i);
                    String ent[] = entstr.split("\\|");
                    System.out.println("ent = '" + Arrays.toString(ent) + "'");
                    JSONObject obj = new JSONObject();
                    obj.put("type", ent[0]);
                    obj.put("entity", ent[1]);
                    s_ar.put(obj);
                }
                res.put("entities", s_ar);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                return;

            } else if (action.equals("resolve_url")) {
                res = resolve_url(request.getParameter("url"));
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                return;

            } else if (action.equals("resolve_urls")) {
                String body_str = null;
                MultipartParser mp = new MultipartParser(request, 1024 * 1024);
                Part part;
                while ((part = mp.readNextPart()) != null) {
                    String part_name = part.getName();
                    if (part.getName().equals("body")) {
                        if (part.isParam()) {
                            ParamPart paramPart = (ParamPart) part;
                            body_str = paramPart.getStringValue("UTF-8");
                        }
                    }
                }
                //dbg("body_str = " + body_str);
                JSONArray urls = new JSONArray(body_str);

                long t_st = System.currentTimeMillis();
                Set<Future<JSONObject>> fRes = new HashSet<Future<JSONObject>>();
                for (int i = 0; i < urls.length(); i++) {
                    String u = urls.getString(i);
                    Callable<JSONObject> callable =
                            new ResolveUrlCallable(u);
                    Future<JSONObject> future = pool.submit(callable);
                    Thread.sleep(10);
                    fRes.add(future);
                }
                Set<Future<JSONObject>> complete = new HashSet<Future<JSONObject>>();
                while (true) {
                    for (Future<JSONObject> f : fRes) {
                        if (!complete.contains(f) && f.isDone()) {
                            complete.add(f);
                            JSONObject fjo = f.get();
                            String _url = fjo.getString("_url");
                            fjo.remove("_url");
                            res.put(_url, fjo);
                        }
                    }
                    if (complete.size() == fRes.size()) break;
                }

                long t_end = System.currentTimeMillis();
                long t_elapsed = (t_end - t_st);
                dbg("resolve_urls: t_elapsed: " + t_elapsed);
                dbg(response, new String(res.toString(4).getBytes(), "UTF-8"));
                return;

            } else {
                res.put("error", "action not recognized");
            }

            if (pretty.equals("true")) {
                response.getWriter().print(res.toString(4).replaceAll("[^\\p{ASCII}]", " "));
            } else {
                response.getWriter().print(res.toString().replaceAll("[^\\p{ASCII}]", " "));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static JSONObject munch_url(String u) throws Exception {
        JSONObject res = new JSONObject();
        String text = HTMLTextExtractor.geturl(u);
        res.put("html", text);

        /* tika */

        Tika tika = new Tika();
        String tikaText = tika.parseToString(new URL(u));
        //tikaText = normalizeText(tikaText);
        res.put("tika-text", tikaText);

        /* - */

        ArrayList<String> emails = new ArrayList<String>();

        // text & named links (swing parser)
        res.put("text", HTMLTextExtractor.extract(text));
        JSONObject nljson = new JSONObject(URLTools.getTextLinksFromHTML(text));
        JSONObject good_nljson = new JSONObject();
        //res.put("named-links", nljson);

        // lucene metadata extractor
        res.put("metadata",
                (new URLTools()).dumpHTMLMetadata(text));

        // pure link ripper
        JSONArray links_ar = new JSONArray();
        List<String> links = URLTools.getLinksFromHTML(text);
        //System.out.println("LINKS LIST: " + links);
        URL url = new URL(u);
        String url_host = url.getHost();
        String url_protocol = url.getProtocol();
        /* todo port ? */
        String u_pfx = url_protocol + "://" +
                url_host;
        for (String href : links) {
            String ohref = href;
            if (href == null) continue;
            if (href.toLowerCase().indexOf("javascript:") != -1) continue;
            if (href.indexOf("mailto:") != -1) {
                emails.add(href.replace("mailto:", ""));
                continue;
            }
            if (!href.toLowerCase().startsWith("http") &&
                    !href.toLowerCase().startsWith("https") &&
                    !href.startsWith("#")) {
                if (href.indexOf("mailto:") != -1) {
                    // do something cool & useful
                    continue;
                }
                href = u_pfx + (href.startsWith("/") ? "" : "/") + href;
            }
            if (href.startsWith("#")) continue;
            if (nljson.has(ohref)) {
                String lbl = nljson.getString(ohref);
                nljson.remove(ohref);
                good_nljson.put(lbl.trim(), href.trim());
            }
            links_ar.put(href);
        }
        res.put("links", links_ar);
        res.put("named-links", good_nljson);
        JSONArray jemails = new JSONArray();
        for (String email : emails) {
            jemails.put(email);
        }
        res.put("email-addresses", jemails);
        tika = null;
        return res;
    }

    public static JSONObject resolve_url(String u) throws Exception {
        JSONObject res = null;
        URL url = new URL(u);
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.setConnectTimeout(6000);
        uc.setReadTimeout(6000);
        uc.setRequestMethod("HEAD");
        uc.setRequestProperty("Connection", "Close");
        uc.connect();
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        /*
        String s;
        while ((s = in.readLine()) != null) {
            // nop; remove?
        }
        */
        in.close();
        uc.disconnect();
        Map<String, List<String>> hdrs = uc.getHeaderFields();
        res = new JSONObject();
        JSONArray no_key_ar = new JSONArray();
        for (String k : hdrs.keySet()) {
            List<String> vals = hdrs.get(k);
            JSONArray j_vals = new JSONArray();
            if (vals.size() > 1) {
                for (String val : vals) {
                    j_vals.put(val);
                }
                if (k == null) {
                    no_key_ar.put(j_vals);
                } else {
                    res.put(k, j_vals);
                }
            } else {
                if (k == null) {
                    no_key_ar.put(vals.get(0));
                } else {
                    res.put(k, vals.get(0));
                }
            }
        }
        res.put("_no_key", no_key_ar);
        //dbg("hdrs = " + res.toString(2));
        in = null;
        uc = null;
        url = null;
        return res;
    }

    private void dbg(String s) throws Exception {
        System.out.println("com.alienobject.textpipe.servlet.NLPSerlvet: " + s);
    }

    private void dbg(HttpServletResponse r, String s) throws Exception {
        r.getWriter().print(s);
    }

    private String normalizeText(String s) throws Exception {
        String[] x = s.split("\n");
        String lines = "";
        for (String str : x) {
            str = str.replace("\t", " ");
            while (str.indexOf("  ") != -1) {
                str = str.replace("  ", " ");
            }
            str = str.trim();
            if (str.equals("")) continue;

            String[] z = str.split(" ");
            if (z.length < 3) {
                z = null;
                continue;
            } else {
                z = null;
            }

            lines += str + "\n";
            str = null;
        }
        x = null;
        return lines;
    }

    public class ResolveUrlCallable implements Callable {
        private String url;

        public ResolveUrlCallable(String url) {
            this.url = url;
        }

        public JSONObject call() {
            JSONObject res = new JSONObject();
            try {
                res.put("_url", this.url);
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
            try {
                res.put("data", NLPServlet.resolve_url(this.url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return res;
        }
    }

    public class UrlExtractCallable implements Callable {
        private String url;

        // eek :X
        public UrlExtractCallable(String url) {
            this.url = url;
        }

        /*
        private String geturl(String url) throws Exception {
            StringBuffer sb = new StringBuffer();
            URL u = new URL(url);
            BufferedReader in = new BufferedReader(
               new InputStreamReader(u.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                sb.append(inputLine);
            in.close();
            return sb.toString();
        }
        */

        public JSONObject call() {
            JSONObject res = new JSONObject();
            try {
                res.put("_url", this.url);
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
            try {
                /*
                String url = "http://localhost:8192/?do=extract&url=" + java.net.URLEncoder.encode(this.url);
                String json_str = geturl(url);
                res = new JSONObject(json_str);
                */
                res = NLPServlet.munch_url(this.url);
                res.put("_url", this.url);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return res;
        }
    }


}
