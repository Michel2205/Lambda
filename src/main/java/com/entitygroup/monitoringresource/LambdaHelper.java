package com.entitygroup.monitoringresource;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.ec2.model.DhcpOptions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.NatGateway;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.amazonaws.services.ec2.model.VpnGateway;
import com.entitygroup.monitoringresource.api.CloudFormationAws;
import com.entitygroup.monitoringresource.api.DynamoDbAws;
import com.entitygroup.monitoringresource.api.Ec2Aws;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class LambdaHelper {

    private static final Logger logger = Logger.getLogger(LambdaHelper.class);
    private List<Vpc> describeVpcs = new LinkedList<>();
    private List<Subnet> describeSubnets = new LinkedList<>();
    private List<InternetGateway> describeInternetGateways = new LinkedList<>();
    private List<VpnGateway> describeVPNGateways = new LinkedList<>();
    private List<VpcPeeringConnection> describeVPCPeeringConnections = new LinkedList<>();
    private List<RouteTable> describeRouteTables = new LinkedList<>();
    private List<NatGateway> describeNatGateways = new LinkedList<>();
    private List<DhcpOptions> describeDhcpOptions = new LinkedList<>();
    private List<Instance> describeInstances = new LinkedList<>();
    private List<SecurityGroup> describeSecurityGroups = new LinkedList<>();
    List<Tag> tagsPrincipal = new LinkedList<>();
    Ec2Aws ec2 = new Ec2Aws();
    private final String SPECIFIC_TAGS = System.getenv("AWS_SPECIFIC_TAGS") != null
            && !System.getenv("AWS_SPECIFIC_TAGS").isEmpty() ? System.getenv("AWS_SPECIFIC_TAGS") : "";
    private final String BUCKET_TAGS = System.getenv("AWS_BUCKET_TAGS") != null
            && !System.getenv("AWS_BUCKET_TAGS").isEmpty() ? System.getenv("AWS_BUCKET_TAGS") : "s3-us-east-1-p-abg-shared-cloudformation";
    private final String BUCKET_KEY_TAGS = System.getenv("AWS_BUCKET_KEY_TAGS") != null
            && !System.getenv("AWS_BUCKET_KEY_TAGS").isEmpty() ? System.getenv("AWS_BUCKET_KEY_TAGS") : "management/infrastructure/tags/";

//    public void cloneTags(String instanceId) {
//        List<String> tags = SPECIFIC_TAGS == null
//                || SPECIFIC_TAGS.isEmpty() ? null
//                : Arrays.asList(SPECIFIC_TAGS.split(","));
//
//        ec2.tagElements(instanceId, tags);
//    }
    public void processStackToTagging(String stackName) {
        CloudFormationAws cf = new CloudFormationAws();
        try {
            //cf.waitForCompletion(stackName);
            List<StackResourceSummary> stackResources = cf.getStackResources(stackName);
            if (stackResources != null && !stackResources.isEmpty()) {
                String projectId = prepareElements(stackResources);
                if (projectId != null && !projectId.isEmpty()) {
                    DynamoDbAws db = new DynamoDbAws();
                    List<Tag> tags = db.listCommonTagsByProject(BUCKET_TAGS, String.format("%s%s.properties", BUCKET_KEY_TAGS, projectId));
                    if (tags != null && !tags.isEmpty()) {
                        tagsPrincipal = Ec2Aws.mixTagsForce(tagsPrincipal, tags);
                        tagsPrincipal = tagsPrincipal.stream()
                                .filter(tag -> !tag.getKey().equals("Name"))
                                .collect(Collectors.toList());
                        System.out.println(tagsPrincipal);
                        ec2.tagVpc(describeVpcs, tagsPrincipal);
                        ec2.tagSubnets(describeSubnets, tagsPrincipal);
                        ec2.tagInternetGateway(describeInternetGateways, tagsPrincipal);
                        ec2.tagVPNGateway(describeVPNGateways, tagsPrincipal);
                        ec2.tagVPCPeeringConnection(describeVPCPeeringConnections, tagsPrincipal);
                        ec2.tagRouteTable(describeRouteTables, tagsPrincipal);
                        ec2.tagNatGateway(describeNatGateways, tagsPrincipal);
                        ec2.tagDhcpOption(describeDhcpOptions, tagsPrincipal);
                        ec2.tagInstance(describeInstances, tagsPrincipal);
                        ec2.tagSecurityGroup(describeSecurityGroups, tagsPrincipal);
                    }
                }
            } else {
                logger.info("No resources found");
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
        /**
         * AWS::EC2::VPC AWS::EC2:: AWS::EC2:: AWS::EC2:: ----AWS::EC2::
         * AWS::EC2:: AWS::EC2:: AWS::EC2::
         */
    }

    private String prepareElements(List<StackResourceSummary> resource) {
        Ec2Aws ec2 = new Ec2Aws();
        String projectId = "";
        List<String> lstVpc = new LinkedList<>();
        List<String> lstSubnet = new LinkedList<>();
        List<String> lstInternetGateway = new LinkedList<>();
        List<String> lstVPNGateway = new LinkedList<>();
        List<String> lstVPCPeeringConnection = new LinkedList<>();
        List<String> lstRouteTable = new LinkedList<>();
        List<String> lstNatGateway = new LinkedList<>();
        List<String> lstDHCPOptions = new LinkedList<>();
        List<String> lstInstance = new LinkedList<>();
        List<String> lstSecutityGroups = new LinkedList<>();
        for (StackResourceSummary item : resource) {
            switch (item.getResourceType()) {
                case "AWS::EC2::VPC":
                    lstVpc.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::Subnet":
                    lstSubnet.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::InternetGateway":
                    lstInternetGateway.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::VPNGateway":
                    lstVPNGateway.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::VPCPeeringConnection":
                    lstVPCPeeringConnection.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::RouteTable":
                    lstRouteTable.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::NatGateway":
                    lstNatGateway.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::DHCPOptions":
                    lstDHCPOptions.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::Instance":
                    lstInstance.add(item.getPhysicalResourceId());
                    break;
                case "AWS::EC2::SecurityGroup":
                    lstSecutityGroups.add(item.getPhysicalResourceId());
                    break;
            }
        }
        if (!lstVpc.isEmpty()) {
            describeVpcs = ec2.describeVpcs(lstVpc);
            if (projectId.isEmpty()
                    && describeVpcs != null && !describeVpcs.isEmpty()) {
                for (Vpc item : describeVpcs) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstSubnet.isEmpty()) {
            describeSubnets = ec2.describeSubnets(lstSubnet);
            if (projectId.isEmpty()
                    && describeSubnets != null && !describeSubnets.isEmpty()) {
                for (Subnet item : describeSubnets) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstInternetGateway.isEmpty()) {
            describeInternetGateways = ec2.describeInternetGateways(lstInternetGateway);
            if (projectId.isEmpty()
                    && describeInternetGateways != null && !describeInternetGateways.isEmpty()) {
                for (InternetGateway item : describeInternetGateways) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstVPNGateway.isEmpty()) {
            describeVPNGateways = ec2.describeVPNGateways(lstVPNGateway);
            if (projectId.isEmpty()
                    && describeVPNGateways != null && !describeVPNGateways.isEmpty()) {
                for (VpnGateway item : describeVPNGateways) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstVPCPeeringConnection.isEmpty()) {
            describeVPCPeeringConnections = ec2.describeVPCPeeringConnections(lstVPCPeeringConnection);
            if (projectId.isEmpty()
                    && describeVPCPeeringConnections != null && !describeVPCPeeringConnections.isEmpty()) {
                for (VpcPeeringConnection item : describeVPCPeeringConnections) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstRouteTable.isEmpty()) {
            describeRouteTables = ec2.describeRouteTables(lstRouteTable);
            if (projectId.isEmpty()
                    && describeRouteTables != null && !describeRouteTables.isEmpty()) {
                for (RouteTable item : describeRouteTables) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstNatGateway.isEmpty()) {
            describeNatGateways = ec2.describeNatGateways(lstNatGateway);
            if (projectId.isEmpty()
                    && describeNatGateways != null && !describeNatGateways.isEmpty()) {
                for (NatGateway item : describeNatGateways) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        break;
                    }
                }
            }
        }
        if (!lstDHCPOptions.isEmpty()) {
            describeDhcpOptions = ec2.describeDhcpOptions(lstDHCPOptions);
            if (projectId.isEmpty()
                    && describeDhcpOptions != null && !describeDhcpOptions.isEmpty()) {
                for (DhcpOptions item : describeDhcpOptions) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstInstance.isEmpty()) {
            describeInstances = ec2.describeInstances(lstInstance);
            if (projectId.isEmpty()
                    && describeInstances != null && !describeInstances.isEmpty()) {
                for (Instance item : describeInstances) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        if (!lstSecutityGroups.isEmpty()) {
            describeSecurityGroups = ec2.describeSecurityGroups(lstSecutityGroups);
            if (projectId.isEmpty()
                    && describeSecurityGroups != null && !describeSecurityGroups.isEmpty()) {
                for (SecurityGroup item : describeSecurityGroups) {
                    projectId = getProjectIdTag(item.getTags());
                    if (!projectId.isEmpty()) {
                        tagsPrincipal = item.getTags();
                        break;
                    }
                }
            }
        }
        return projectId;
    }

    private String getProjectIdTag(List<Tag> tsgs) {
        String projectId = "";
        if (tsgs != null && !tsgs.isEmpty()) {
            Optional<Tag> tagProjectId = tsgs.stream().filter(item -> item.getKey().equalsIgnoreCase("ProjectID")).findFirst();
            if (tagProjectId != null && tagProjectId.isPresent()) {
                projectId = tagProjectId.get().getValue();
            }
        }
        return projectId;
    }
}
