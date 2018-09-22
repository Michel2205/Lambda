/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.egblambda;

import com.entitygroup.monitoringresource.LambdaHelper;
import com.entitygroup.monitoringresource.handler.ExcecutorHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Maximino Llovera
 */
public class AppTest {

    public AppTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class App.
     */
    @Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        // TODO review the generated test code and remove the default call to fail.

    }

    /**
     * Test of createTestBucket method, of class App.
     */
    @Test
    public void testCreateTestBucket() throws Exception {
        ExcecutorHandler t = new ExcecutorHandler();
        //t.handleRequest(temp, null);
        System.out.println("createTestBucket");
        LambdaHelper app = new LambdaHelper();
        app.processStackToTagging("abg-budget-truck-dnsforwarder2-dev");
//        app.processStackToTagging("vpc-us-east-1-s-devops");
//        app.processStackToTagging("abg-rateshop-vpc-prod");
        //app.cloneTags("i-0f28f891c8ab7dae8");
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

}
