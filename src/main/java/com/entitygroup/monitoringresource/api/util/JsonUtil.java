/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api.util;

import org.apache.log4j.Logger;
//import org.json.JSONObject;
import com.google.gson.JsonObject;

/**
 *
 * @author Maximino Llovera
 */
public class JsonUtil {

    private static final Logger logger = Logger.getLogger(JsonUtil.class);

    public static String getString(JsonObject object, String key) {
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            logger.error("Key not found:" + key);
            return "";
        }
    }

    public static JsonObject getJSONObject(JsonObject object, String key) {
        try {
            return object.getAsJsonObject(key);
        } catch (Exception ex) {
            logger.error("Key not found:" + key);
            return null;
        }
    }
}
