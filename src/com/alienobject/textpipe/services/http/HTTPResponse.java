package com.alienobject.textpipe.services.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPResponse {

    public enum Type {
        TEXT_HTML("text/html"),
        TEXT_PLAIN("text/plain"),
        APPLICATION_JSON("application/json");

        private String type;

        Type(String type) {
            this.type = type;
        }

        public String toString() {
            return this.type;
        }
    }

    protected int responseCode;
    protected Map<String, List<String>> responseHeaders;
    protected String contentType;
    protected String body;
    protected long lastModified;
    protected long expires;
    protected long date;

    public HTTPResponse() {
        this.responseCode = -1;
        this.responseHeaders = new HashMap<String, List<String>>();
        this.contentType = null;
        this.body = null;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public String getContentType() {
        return responseHeaders.get("Content-Type").get(0);
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        Map<String, List<String>> hdrs = new HashMap<String, List<String>>(responseHeaders);
        hdrs.remove(null);
        this.responseHeaders = hdrs;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setContentType(Type contentType) {
        setContentType(contentType.toString());
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

}
