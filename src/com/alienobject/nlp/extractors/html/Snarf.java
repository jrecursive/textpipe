package com.alienobject.nlp.extractors.html;

import sun.net.www.protocol.http.HttpURLConnection;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;

class Snarf {

    public Snarf() {
    }

    @SuppressWarnings("deprecation")
    public String fetch(String u) throws Exception { /* old skool */
        System.out.println("<snarf> fetching: " + u);
        String s;
        URL url = new URL(u);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.setReadTimeout(60000);
        uc.setConnectTimeout(20000);
        InputStream is = uc.getInputStream();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        StringBuffer sb = new StringBuffer();
        while ((s = dis.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();
    }

}
