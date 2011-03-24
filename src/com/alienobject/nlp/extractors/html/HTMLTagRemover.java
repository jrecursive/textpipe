package com.alienobject.nlp.extractors.html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HTMLTagRemover {
    private final static Pattern p1 = Pattern.compile("<(script|style)[^>]*>[^<]*</(script|style)>",
            Pattern.CASE_INSENSITIVE);
    private final static Pattern p2 = Pattern.compile("<[^>]*>");
    private final static String multiplenewlines = "(\\n{1,2})(\\s*\\n)+";

    public static String strip(String content) {

        Matcher m1 = p1.matcher(content);
        int count = 0;
        while (m1.find()) {
            count++;
        }
        //System.out.println("Removed " + count + " script & style tags");
        content = m1.replaceAll("");
        Matcher m2 = p2.matcher(content);
        count = 0;
        while (m2.find()) {
            count++;
        }
        content = m2.replaceAll("");
        //System.out.println("Removed " + count + " other tags.");
        content = content.replaceAll(multiplenewlines, "$1");
        return content;
    }
}

