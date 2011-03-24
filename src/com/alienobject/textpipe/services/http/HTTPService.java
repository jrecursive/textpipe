package com.alienobject.textpipe.services.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HTTPService {

    private static volatile HTTPService INSTANCE;
    private ExecutorService executor = null;

    private static synchronized HTTPService tryCreateInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HTTPService();
        }
        return INSTANCE;
    }

    public static HTTPService getInstance() {
        HTTPService service = INSTANCE;
        if (service == null) {
            service = tryCreateInstance();
        }
        return service;
    }

    private HTTPService() {
        this.executor = Executors.newCachedThreadPool();
    }

    public HTTPResponse request(HTTPRequest request) throws InterruptedException, ExecutionException {
        return requestAsync(request).get();
    }

    Future<HTTPResponse> requestAsync(HTTPRequest request) {
        return executor.submit(request);
    }

    public static String downloadStream(InputStream in) throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        try {
            copy(in, out);
        } finally {
            in.close();
            out.close();
        }
        return out.toString();

    }

    private static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount == -1) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }


}
