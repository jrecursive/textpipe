package com.alienobject.textpipe.services.http;

import com.alienobject.textpipe.services.http.HTTPResponse.Type;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class HTTPRequest implements Callable<HTTPResponse> {

    public enum RedirectPolicy {
        NOFOLLOW,
        FOLLOW
    }

    protected HTTPMethod method = null;
    protected URL url = null;
    protected Map<String, String> headers = null;
    protected String contentType = Type.TEXT_PLAIN.toString();
    protected String body = null;
    protected String userAgent = "Javier/HTTPService 1.0";
    protected static int defaultConnectTimeout = 20000;
    protected static int defaultReadTimeout = 20000;
    protected int connectTimeout;
    protected int readTimeout;
    protected RedirectPolicy followRedirects = RedirectPolicy.FOLLOW;
    protected HTTPResponse response = null;
    protected long ifModifiedSince = -1;
    protected HttpURLConnection connection = null;


    public HTTPRequest(HTTPMethod method, URL url, Map<String, String> headers) {
        this.method = method;
        this.url = url;
        this.headers = new HashMap<String, String>(headers);
        this.connectTimeout = HTTPRequest.defaultConnectTimeout;
        this.readTimeout = HTTPRequest.defaultReadTimeout;
        this.response = new HTTPResponse();
    }

    public HTTPRequest(URL url) {
        this(HTTPMethod.GET, url, new HashMap<String, String>());
    }

    public HTTPRequest(String url) throws MalformedURLException {
        this(new URL(url));
    }

    protected HttpURLConnection createConnection() throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method.toString());
        for (String k : headers.keySet()) {
            connection.setRequestProperty(k, headers.get(k));
        }
        return connection;
    }

    public HTTPMethod getMethod() {
        return method;
    }

    public void setMethod(HTTPMethod method) {
        this.method = method;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public static synchronized int getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public static synchronized void setDefaultConnectTimeout(
            int defaultConnectTimeout) {
        HTTPRequest.defaultConnectTimeout = defaultConnectTimeout;
    }

    public static synchronized int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public static synchronized void setDefaultReadTimeout(int defaultReadTimeout) {
        HTTPRequest.defaultReadTimeout = defaultReadTimeout;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean hasBody() {
        return body != null;
    }

    public long getContentLength() {
        if (body != null) {
            return body.length();
        }
        return 0;
    }

    public RedirectPolicy getFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(RedirectPolicy followRedirects) {
        this.followRedirects = followRedirects;
    }

    public HTTPResponse getResponse() {
        return response;
    }

    public void setResponse(HTTPResponse response) {
        this.response = response;
    }

    public HTTPResponse call() throws IOException {
        doRequest();
        return this.response;
    }

    public void doRequest() throws IOException {
        this.connection = makeConnection();
        initializeConnection();
        performRequest();
    }

    protected void readResponseBody(HttpURLConnection connection) {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String urlContent;
        try {
            urlContent = HTTPService.downloadStream(inputStream);
            response.setBody(urlContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void writeRequestBody() {
        OutputStream outputStream;
        try {
            outputStream = connection.getOutputStream();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        try {
            outputStreamWriter.write(getBody());
            outputStreamWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void performRequest() throws IOException {
        response.setResponseCode(connection.getResponseCode());
        response.setContentType(connection.getContentType());
        response.setResponseHeaders(connection.getHeaderFields());
        if (hasBody()) {
            writeRequestBody();
        }
        readResponseBody(connection);
        response.setExpires(connection.getExpiration());
        response.setLastModified(connection.getLastModified());
        response.setDate(connection.getDate());
    }

    protected void initializeConnection() {

    }

    protected HttpURLConnection makeConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        if (this.followRedirects == RedirectPolicy.FOLLOW) {
            connection.setInstanceFollowRedirects(true);
        }
        if (hasBody()) {
            connection.setDoOutput(true);
        }
        if (ifModifiedSince != -1) {
            connection.setIfModifiedSince(ifModifiedSince);
        }
        connection.setRequestProperty("User-Agent", getUserAgent());
        return connection;
    }

    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    public static void main(String[] args) throws Exception {
        HTTPRequest r = new HTTPRequest(new URL("http://www.cnn.com"));
        System.out.println(HTTPService.getInstance().request(r).getResponseHeaders());
    }

}
