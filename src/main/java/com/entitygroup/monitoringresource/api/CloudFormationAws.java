/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.entitygroup.monitoringresource.api.util.GlobalVariables;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 *
 * @author Maximino Llovera
 */
public class CloudFormationAws {

    private static AmazonCloudFormation cloudformation = null;
    private static final Logger logger = Logger.getLogger(CloudFormationAws.class);

    public static AmazonCloudFormation getAmazonCloudFormation() {
        if (cloudformation != null) {
            return cloudformation;
        }
        AmazonCloudFormationClientBuilder withRegion = AmazonCloudFormationClientBuilder.standard().withRegion(GlobalVariables.REGION);
        cloudformation = withRegion.build();
        return cloudformation;
    }

    public CloudFormationAws() {
        getAmazonCloudFormation();
    }

    public List<StackResourceSummary> getStackResources(String stackName) {
        try {
            String nextToken = null;
            ListStackResourcesRequest request = new ListStackResourcesRequest().withStackName(stackName);
            List<StackResourceSummary> result = new LinkedList<>();
            do {
                request.setNextToken(nextToken);
                final ListStackResourcesResult response = CloudFormationAws.cloudformation.listStackResources(request);
                result.addAll(response.getStackResourceSummaries());
                nextToken = response.getNextToken();
            } while (null != nextToken);

//            DescribeStackResourcesResult describeStackResources
//                    = CloudFormationAws.cloudformation
//                            .describeStackResources(
//                                    new DescribeStackResourcesRequest()
//                                            .withStackName(stackName));
            //describeStackResources.getStackResources()
//            logger.info(result);
            return result;
        } catch (AmazonClientException e) {
            logger.error(e);
        }
        return null;
    }

    public String waitForCompletion(String stackName) throws Exception {
        DescribeStacksRequest wait = new DescribeStacksRequest();
        wait.setStackName(stackName);
        Boolean completed = false;
        String stackStatus = "Unknown";
        String stackReason = "";

        System.out.print("Waiting");

        while (!completed) {
            List<Stack> stacks = CloudFormationAws.cloudformation.describeStacks(wait).getStacks();
            if (stacks.isEmpty()) {
                completed = true;
                stackStatus = "NO_SUCH_STACK";
                stackReason = "Stack has been deleted";
            } else {
                for (Stack stack : stacks) {
                    if (stack.getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString())
                            || stack.getStackStatus().equals(StackStatus.CREATE_FAILED.toString())
                            || stack.getStackStatus().equals(StackStatus.ROLLBACK_FAILED.toString())
                            || stack.getStackStatus().equals(StackStatus.DELETE_FAILED.toString())) {
                        completed = true;
                        stackStatus = stack.getStackStatus();
                        stackReason = stack.getStackStatusReason();
                    }
                }
            }

            // Show we are waiting
            System.out.print(".");

            // Not done yet so sleep for 10 seconds.
            if (!completed) {
                Thread.sleep(10000);
            }
        }

        // Show we are done
        System.out.print("done\n");

        return stackStatus + " (" + stackReason + ")";
    }

}
