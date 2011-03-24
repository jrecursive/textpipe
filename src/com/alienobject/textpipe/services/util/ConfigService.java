package com.alienobject.textpipe.services.util;

import org.json.JSONException;
import org.json.JSONObject;


public class ConfigService {

    private static volatile ConfigService INSTANCE;

    private JSONObject jsonObject = null;

    private ConfigService() {
        this.jsonObject = new JSONObject();
    }

    private static synchronized ConfigService tryCreateInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigService();
        }
        return INSTANCE;
    }

    public static ConfigService getInstance() {
        ConfigService service = INSTANCE;
        if (service == null) {
            service = tryCreateInstance();
        }
        return service;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getPropertyString(String propName) {
        try {
            return this.jsonObject.getJSONObject("properties").getString(propName);
        } catch (JSONException e) {
            return null;
        }
    }
}
