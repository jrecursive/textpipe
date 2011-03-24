package com.alienobject.textpipe.nlp.entities;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Quote {
    private String attribution;
    private String quote;

    public Quote(String attrib, String q) {
        this.attribution = attrib;
        this.quote = q;
    }

    public Quote() {
    }

    public void setAttribution(String attr) {
        this.attribution = attr;
    }

    public void setQuote(String q) {
        this.quote = q;
    }

    public String getAttribution() {
        return attribution;
    }

    public String getQuote() {
        return quote;
    }

    Map getAsMap() {
        Map map = new HashMap();
        map.put("attribution", attribution);
        map.put("quote", quote);
        return map;
    }

    public JSONObject getAsJSONObject() {
        return new JSONObject(getAsMap());
    }

    public String toString() {
        String s = null;
        try {
            s = getAsJSONObject().toString(4);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }
}
