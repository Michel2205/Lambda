/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.ec2.model.Tag;
import com.entitygroup.monitoringresource.api.util.GlobalVariables;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author Maximino Llovera
 */
public class DynamoDbAws {

    private static DynamoDB dynamoDB = null;
    private static final Logger logger = Logger.getLogger(CloudFormationAws.class);

    public static DynamoDB getAmazonDynamoDb() {
        if (dynamoDB != null) {
            return dynamoDB;
        }
//        AwsClientBuilder.EndpointConfiguration endpointConfiguration
//                = new AwsClientBuilder.EndpointConfiguration("https://dynamodb." + GlobalVariables.REGION + ".amazonaws.com", GlobalVariables.REGION);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(GlobalVariables.REGION)
                //                .withEndpointConfiguration(endpointConfiguration)
                .build();

        dynamoDB = new DynamoDB(client);
//        dynamoDB = new DynamoDB(
//                AmazonDynamoDBClientBuilder.standard().withRegion(
//                        GlobalVariables.REGION).build());;
        return dynamoDB;
    }

    public DynamoDbAws() {
        getAmazonDynamoDb();
    }

    public void getQueryResultByProjectId(String tableName, String projectId) {
        Table table = dynamoDB.getTable(tableName);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("PROJECT_ID = :v_id")
                .withValueMap(new ValueMap().withString(":v_id", projectId))
                .withMaxPageSize(10);

        ItemCollection<QueryOutcome> items = table.query(spec);

        System.out.println("\nfindRepliesForAThread results:");

        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toJSONPretty());
        }
    }

    public List<Tag> listCommonTagsByProject(String bucketName, String keyFile) {

        S3Aws s3 = new S3Aws();
        String fileName = s3.getObjectFile(bucketName, keyFile);
        List<Tag> tags = new LinkedList<>();
        if (fileName != null && !fileName.isEmpty()) {
            Properties appProps = new Properties();
            try {
                appProps.load(new FileInputStream(fileName));
                for (Object entry : appProps.keySet()) {
                    Tag tag = new Tag();
                    tag.setKey(String.valueOf(entry));
                    tag.setValue(appProps.getProperty(String.valueOf(entry)));
                    tags.add(tag);
                }
            } catch (IOException ex) {
                logger.error(ex);
            }
        }
        return tags;
    }
}
