/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api.util;

/**
 *
 * @author Maximino Llovera
 */
public class GlobalVariables {

    public static final String ACCESS_KEY = System.getenv("AWS_AccessKey");
    public static final String SECRET_KEY = System.getenv("AWS_SecretKey");
    /**
     * REGION by default is us-east-1 but if set an Environment variable it will
     * get it from there.
     */
    public static String REGION = System.getenv("REGION") == null
            ? "us-east-1" : System.getenv("REGION");
}
