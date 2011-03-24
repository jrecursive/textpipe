package com.alienobject.nlp.extractors.html;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TagUtils {
    private final static Pattern[] TAG_STRIPPERS = new Pattern[]{
            Pattern.compile("\\<\\!--.*?-->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\<a.*?a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\<script.*?script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\<style.*?style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\<.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("&lt;.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    };
    private final static Pattern[] TAG_MODDERS = new Pattern[]{
            Pattern.compile("\\<p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\<br>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
    };
    private final static Pattern[] COLLAPSE_NEWLINE = new Pattern[]{
            Pattern.compile("\\n+\\s+"),
            Pattern.compile("\\n+")
    };

    static public String getStrippedText(String raw) {
        String text = raw;
        Matcher m;
        for (Pattern stripper : TAG_MODDERS) {
            m = stripper.matcher(text);
            text = m.replaceAll("\n\n");
        }
        for (Pattern stripper : TAG_STRIPPERS) {
            m = stripper.matcher(text);
            text = m.replaceAll(" ");
        }
        m = COLLAPSE_NEWLINE[0].matcher(text);
        text = m.replaceAll("\n\n");
        m = COLLAPSE_NEWLINE[0].matcher(text);
        text = m.replaceAll("\n\n");
        text = StringEscapeUtils.unescapeHtml(text);
        return text;
    }

}

