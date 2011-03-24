package com.alienobject.textpipe.services.http;

public enum HTTPMethod {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    POST("POST"),
    HEAD("HEAD");

    private String methodName;

    HTTPMethod(String methodName) {
        this.methodName = methodName;
    }

    public String toString() {
        return this.methodName;
    }
}


