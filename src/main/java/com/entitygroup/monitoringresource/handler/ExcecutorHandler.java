/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.entitygroup.monitoringresource.LambdaHelper;
import com.entitygroup.monitoringresource.api.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;

/**
 *
 * @author Maximino Llovera
 */
public class ExcecutorHandler implements RequestHandler<Object, String> {

    private static final Logger logger = Logger.getLogger(ExcecutorHandler.class);

    @Override
    public String handleRequest(Object input, Context context) {
        LambdaHelper app = new LambdaHelper();
        logger.info("Starting...");
        logger.info("Received:" + new Gson().toJson(input));
        try {
            JsonParser parser = new JsonParser();
            JsonObject objIn = parser.parse(new Gson().toJson(input)).getAsJsonObject();
            JsonObject detail = JsonUtil.getJSONObject(objIn, "detail");
            if (detail != null) {
                JsonObject requestParams = JsonUtil.getJSONObject(detail, "requestParameters");
                if (requestParams != null) {
                    String stackName = JsonUtil.getString(requestParams, "stackName");
                    logger.info(stackName);
                    app.processStackToTagging(stackName);
                } else {
                    logger.info("No 'requestParams' item found");
                }
            } else {
                logger.info("No 'detail' item found");
            }

            logger.info(input.getClass().getName());

        } catch (Exception ex) {
            logger.info("Was an error ");
            logger.info(ex.getMessage());
        }
        logger.info("Finish...");
        return "success";
    }
}
