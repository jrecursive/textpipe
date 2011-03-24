package com.alienobject.textpipe.servlet;

import com.alienobject.textpipe.nlp.QuoteExtractor;
import com.alienobject.textpipe.nlp.entities.Quote;
import org.json.JSONArray;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

public class QuoteExtraction extends HttpServlet {
    private static QuoteExtractor qe = null;

    /* fire this up on object creation (it takes a bit) */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            if (qe == null) {
                qe = new QuoteExtractor();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("* cannot instantiate QuoteExtractor!");
            //System.exit(-1);
        }
    }

    @Override
    public void doGet(HttpServletRequest servletRequest,
                      HttpServletResponse servletResponse) throws ServletException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    public void doPost(HttpServletRequest servletRequest,
                       HttpServletResponse servletResponse) throws ServletException {
        try {
            JSONArray quotes_ar = getQuotes(servletRequest.getParameter("text"));
            if (servletRequest.getParameter("debug") != null) {
                servletResponse.addHeader("Content-Type", "text/plain");
            } else {
                servletResponse.addHeader("Content-Type", "application/json");
            }
            servletResponse.getWriter().print(quotes_ar.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    JSONArray getQuotes(String text) throws Exception {
        ArrayList<Quote> quotes = qe.extractQuotes(text);
        JSONArray quotes_ar = new JSONArray();
        for (Quote q : quotes) {
            quotes_ar.put(q.getAsJSONObject());
        }
        return quotes_ar;
    }


    private void dbg(String s) {
        System.out.println("[" + this.toString() + "] QuoteExtraction: " + s);
    }
}
