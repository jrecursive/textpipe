package com.alienobject.textpipe;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;

public class NLPServer {
    private static boolean running = false;

    private NLPServer() {
        dbg("Hi!");
    }

    private void jetty()
            throws Exception {

        dbg("setting us up the jetty...");

        Server server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setHost("0.0.0.0");
        int HTTP_PORT = 5060;
        connector.setPort(HTTP_PORT);
        server.setConnectors(new Connector[]{connector});

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping("com.alienobject.textpipe.servlet.NLPServlet", "/");

        server.start();
        dbg("jetty open for business.");
        server.join();
    }

    public static void main(String args[]) throws Exception {
        while (true) {
            try {
                NLPServer nlpServer = new NLPServer();
                nlpServer.jetty();
            } catch (Exception ex) {
                dbg("Exception: ");
                ex.printStackTrace();
                dbg("Resarting jetty in 5s... ");
                try {
                    Thread.sleep(5000);
                } catch (Exception fu) {
                    // lulz
                }
            }
        }
    }

    private static void dbg(String s) {
        System.out.println("[NLPServer] " + s);
    }
}
