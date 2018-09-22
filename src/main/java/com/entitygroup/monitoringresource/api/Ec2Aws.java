/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitygroup.monitoringresource.api;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeDhcpOptionsRequest;
import com.amazonaws.services.ec2.model.DescribeDhcpOptionsResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeNatGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcPeeringConnectionsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysResult;
import com.amazonaws.services.ec2.model.DhcpOptions;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.NatGateway;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcPeeringConnection;
import com.amazonaws.services.ec2.model.VpnGateway;
import com.entitygroup.monitoringresource.api.util.GlobalVariables;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 *
 * @author Maximino Llovera
 */
public class Ec2Aws {

    private static AmazonEC2 ec2 = null;
    private static final Logger logger = Logger.getLogger(Ec2Aws.class);

    public static AmazonEC2 getAmazonEc2() {
        //AmazonSNSClient snsClient;
        if (ec2 != null) {
            return ec2;
        }
        /*if (GlobalVariables.ACCESS_KEY != null && !GlobalVariables.ACCESS_KEY.isEmpty()
                && GlobalVariables.SECRET_KEY != null && !GlobalVariables.SECRET_KEY.isEmpty()) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(GlobalVariables.ACCESS_KEY, GlobalVariables.SECRET_KEY);
            ec2 = new AmazonEC2Client(awsCreds);
            ec2.setRegion(RegionUtils.getRegion(GlobalVariables.REGION));
        } else {*/
        AmazonEC2ClientBuilder withRegion = AmazonEC2ClientBuilder
                .standard().withRegion(GlobalVariables.REGION);
        ec2 = withRegion.build();
        //}
        return ec2;
    }

    public Ec2Aws() {
        getAmazonEc2();
    }

    public List<Tag> getTagsFromInstance(String instanceId, List<String> specificTags) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        //List<String> lstInstanceId = new LinkedList(){instanceId};
        request.setInstanceIds(Arrays.asList(instanceId));

        DescribeInstancesResult result = ec2.describeInstances(request);
        List<Reservation> reservations = result.getReservations();
        List<Instance> instances;
        for (Reservation res : reservations) {
            instances = res.getInstances();
            for (Instance ins : instances) {
                if (ins.getTags() != null && !ins.getTags().isEmpty()) {
                    List<Tag> tags = ins.getTags().stream()
                            .filter(tag -> !tag.getKey().startsWith("aws:")
                            && !tag.getKey().startsWith("scheduler:")
                            && (specificTags == null || specificTags.isEmpty()
                            || specificTags.contains(tag.getKey())))
                            .collect(Collectors.toList());
                    return tags;
                }
            }
        }
        return null;
    }

    public void tagInstanceAndResources(String instanceId, List<Tag> tags) {
        if (tags != null && !tags.isEmpty()) {
            tagEbsVolumes(instanceId, tags);
            tagNetworkInterface(instanceId, tags);
            tagEip(instanceId, tags);
        } else {
            logger.info("isn't foud any tag in this instanceId: " + instanceId
                    + ", we didn't make changes");
        }
    }

    private void tagEbsVolumes(String instanceId, List<Tag> tagsFromInstance) {
        String id = "";
        try {
            logger.info("Adding tags to EBS Volume");
            DescribeVolumesRequest request = new DescribeVolumesRequest().withFilters(
                    new Filter().withName("attachment.instance-id").withValues(instanceId));
            DescribeVolumesResult response = ec2.describeVolumes(request);
            if (response != null) {
                List<Volume> volumes = response.getVolumes();
                for (Volume volume : volumes) {
                    id = volume.getVolumeId();
                    logger.info("   *EBS: " + id);
                    List<Tag> tags = volume.getTags();
                    tags = mixTagsForce(tags, tagsFromInstance);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags on EBS Volumes"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    private void tagEip(String instanceId, List<Tag> tagsFromInstance) {
        String id = "";
        try {
            logger.info("Adding tags to EIP");
            DescribeAddressesRequest dar = new DescribeAddressesRequest().withFilters(
                    new Filter().withName("instance-id").withValues(instanceId));
            DescribeAddressesResult response = ec2.describeAddresses(dar);
            if (response != null) {
                List<Address> addresses = response.getAddresses();
                for (Address address : addresses) {
                    id = address.getAllocationId();
                    logger.info("   *EIP: " + id);
                    //tags = mixTags(tags, tagsFromInstance);
                    createTag(id, tagsFromInstance);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags on EIP"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    private void tagNetworkInterface(String instanceId, List<Tag> tagsFromInstance) {
        String id = "";
        try {
            logger.info("Adding tags to NetworkInterface");
            DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest().withFilters(
                    new Filter().withName("attachment.instance-id").withValues(instanceId));
            DescribeNetworkInterfacesResult response = ec2.describeNetworkInterfaces(request);
            if (response != null) {
                List<NetworkInterface> networkInterfaces = response.getNetworkInterfaces();
                for (NetworkInterface item : networkInterfaces) {
                    id = item.getNetworkInterfaceId();
                    List<Tag> tags = item.getTagSet();
                    logger.info("   NetInterface: " + id);
                    tags = mixTagsForce(tags, tagsFromInstance);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags on NetworkInterface"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagVpc(List<Vpc> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to Vpc");
            if (items != null && !items.isEmpty()) {
                for (Vpc item : items) {
                    id = item.getVpcId();
                    List<Tag> tags = item.getTags();
                    logger.info("   Vpc-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in Vpc"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagSubnets(List<Subnet> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to Subnets");
            if (items != null && !items.isEmpty()) {
                for (Subnet item : items) {
                    id = item.getSubnetId();
                    List<Tag> tags = item.getTags();
                    logger.info("   Subnet-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in Subnet"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagInternetGateway(List<InternetGateway> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to InternetGateway");
            if (items != null && !items.isEmpty()) {
                for (InternetGateway item : items) {
                    id = item.getInternetGatewayId();
                    List<Tag> tags = item.getTags();
                    logger.info("   InternetGateway-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in InternetGateway"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagVPNGateway(List<VpnGateway> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to VPNGateway");
            if (items != null && !items.isEmpty()) {
                for (VpnGateway item : items) {
                    id = item.getVpnGatewayId();
                    List<Tag> tags = item.getTags();
                    logger.info("   VpnGateway-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in VpnGateway"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagVPCPeeringConnection(List<VpcPeeringConnection> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to VPCPeeringConnection");
            if (items != null && !items.isEmpty()) {
                for (VpcPeeringConnection item : items) {
                    id = item.getVpcPeeringConnectionId();
                    List<Tag> tags = item.getTags();
                    logger.info("   VpcPeeringConnection-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in VpcPeeringConnection"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagRouteTable(List<RouteTable> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to RouteTable");
            if (items != null && !items.isEmpty()) {
                for (RouteTable item : items) {
                    id = item.getRouteTableId();
                    List<Tag> tags = item.getTags();
                    logger.info("   RouteTable-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in RouteTable"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagNatGateway(List<NatGateway> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to NatGateway");
            if (items != null && !items.isEmpty()) {
                for (NatGateway item : items) {
                    id = item.getNatGatewayId();
                    List<Tag> tags = item.getTags();
                    logger.info("   NatGateway-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in NatGateway"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagDhcpOption(List<DhcpOptions> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to DhcpOptions");
            if (items != null && !items.isEmpty()) {
                for (DhcpOptions item : items) {
                    id = item.getDhcpOptionsId();
                    List<Tag> tags = item.getTags();
                    logger.info("   DhcpOption-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in DhcpOption"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagInstance(List<Instance> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to Instance");
            if (items != null && !items.isEmpty()) {
                for (Instance item : items) {
                    id = item.getInstanceId();
                    List<Tag> tags = item.getTags();
                    logger.info("   Instance-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                    tagInstanceAndResources(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in Instance"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void tagSecurityGroup(List<SecurityGroup> items, List<Tag> additionalTags) {
        String id = "";
        try {
            logger.info("Adding tags to SecurityGroup");
            if (items != null && !items.isEmpty()) {
                for (SecurityGroup item : items) {
                    id = item.getGroupId();
                    List<Tag> tags = item.getTags();
                    logger.info("   SecurityGroup-Id: " + id);
                    tags = mixTagsForce(tags, additionalTags);
                    createTag(id, tags);
                }
            }
        } catch (Exception ex) {
            logger.error("Error creating tags in SecurityGroup"
                    + (id != null && id.isEmpty() ? ", with ID: " + id : ""));
            logger.error(ex);
        }
    }

    public void createTag(String idResource, List<Tag> tags) {
        CreateTagsRequest resource = new CreateTagsRequest().withResources(idResource);
        resource.setTags(tags);
        ec2.createTags(resource);
    }

    public static List<Tag> mixTags(List<Tag> tags, List<Tag> tagsFromInstance) {
        if (tags != null && !tags.isEmpty()) {
            for (Tag tagIn : tagsFromInstance) {
                List<Tag> item = tags.stream()
                        .filter(tag -> tag.getKey().equals(tagIn.getKey()))
                        .collect(Collectors.toList());
                if (item == null || item.isEmpty()) {
                    tags.add(tagIn);
                }
            }
            List<Tag> tagsFiltered = tags.stream()
                    .filter(tag -> !tag.getKey().startsWith("aws:")
                    && !tag.getKey().startsWith("scheduler:"))
                    .collect(Collectors.toList());
            return tagsFiltered;
        } else {
            return tagsFromInstance;
        }

    }

    public static List<Tag> mixTagsForce(List<Tag> tags, List<Tag> tagsFromInstance) {
        if (tags != null && !tags.isEmpty()) {
            for (Tag tagIn : tagsFromInstance) {
                Optional<Tag> tagAux = tags.stream().filter(
                        tag -> tag.getKey().equals(tagIn.getKey())).findFirst();
                if (tagAux != null && tagAux.isPresent()) {
                    if ((tagAux.get().getValue() == null
                            || tagAux.get().getValue().isEmpty())
                            && (tagIn.getValue() != null
                            && !tagIn.getValue().isEmpty())) {
                        tagAux.get().setValue(tagIn.getValue());
                    }
                } else {
                    tags.add(tagIn);
                }

                List<Tag> item = tags.stream()
                        .filter(tag -> tag.getKey().equals(tagIn.getKey()))
                        .collect(Collectors.toList());
                if (item == null || item.isEmpty()) {

                } else if (tagIn.getValue() == null
                        || tagIn.getValue().isEmpty()) {
                    if (tagAux != null && tagAux.isPresent()) {
                        tagAux.get().setValue(tagIn.getValue());
                    }
                }
            }
            List<Tag> tagsFiltered = tags.stream()
                    .filter(tag -> !tag.getKey().startsWith("aws:")
                    && !tag.getKey().startsWith("scheduler:")
                    && !tag.getKey().equalsIgnoreCase("Name"))
                    .collect(Collectors.toList());
            return tagsFiltered;
        } else {
            return tagsFromInstance;
        }

    }

    //<editor-fold defaultstate="collapsed" desc="Describe Resources">
    public List<Vpc> describeVpcs(List<String> vpcId) {
        if (vpcId == null || vpcId.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeVpcsRequest request = new DescribeVpcsRequest()
                .withVpcIds(vpcId);
        DescribeVpcsResult result = ec2.describeVpcs(request);
        return result.getVpcs();
    }

    public List<Subnet> describeSubnets(List<String> subnetIds) {
        if (subnetIds == null || subnetIds.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeSubnetsRequest request = new DescribeSubnetsRequest()
                .withSubnetIds(subnetIds);
        DescribeSubnetsResult result = ec2.describeSubnets(request);
        return result.getSubnets();
    }

    public List<InternetGateway> describeInternetGateways(List<String> internetGateways) {
        if (internetGateways == null || internetGateways.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest()
                .withInternetGatewayIds(internetGateways);
        DescribeInternetGatewaysResult result = ec2.describeInternetGateways(request);
        return result.getInternetGateways();
    }

    public List<VpnGateway> describeVPNGateways(List<String> vpnGateways) {
        if (vpnGateways == null || vpnGateways.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest()
                .withVpnGatewayIds(vpnGateways);
        DescribeVpnGatewaysResult result = ec2.describeVpnGateways(request);
        return result.getVpnGateways();
    }

    public List<VpcPeeringConnection> describeVPCPeeringConnections(List<String> vpcPeeringConnections) {
        if (vpcPeeringConnections == null || vpcPeeringConnections.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeVpcPeeringConnectionsRequest request = new DescribeVpcPeeringConnectionsRequest()
                .withVpcPeeringConnectionIds(vpcPeeringConnections);
        DescribeVpcPeeringConnectionsResult result = ec2.describeVpcPeeringConnections(request);
        return result.getVpcPeeringConnections();
    }

    public List<RouteTable> describeRouteTables(List<String> routeTables) {
        if (routeTables == null || routeTables.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeRouteTablesRequest request = new DescribeRouteTablesRequest()
                .withRouteTableIds(routeTables);
        DescribeRouteTablesResult result = ec2.describeRouteTables(request);
        return result.getRouteTables();
    }

    public List<NatGateway> describeNatGateways(List<String> natGateway) {
        if (natGateway == null || natGateway.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeNatGatewaysRequest request = new DescribeNatGatewaysRequest()
                .withNatGatewayIds(natGateway);
        DescribeNatGatewaysResult result = ec2.describeNatGateways(request);
        return result.getNatGateways();
    }

    public List<DhcpOptions> describeDhcpOptions(List<String> dhcpOptions) {
        if (dhcpOptions == null || dhcpOptions.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeDhcpOptionsRequest request = new DescribeDhcpOptionsRequest()
                .withDhcpOptionsIds(dhcpOptions);
        DescribeDhcpOptionsResult result = ec2.describeDhcpOptions(request);
        return result.getDhcpOptions();
    }

    public List<Instance> describeInstances(List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeInstancesRequest request = new DescribeInstancesRequest()
                .withInstanceIds(instances);
        DescribeInstancesResult result = ec2.describeInstances(request);
        List<Reservation> reservations = result.getReservations();
        List<Instance> lstInstances = new LinkedList<>();
        if (reservations != null && !reservations.isEmpty()) {
            reservations.forEach((res) -> {
                lstInstances.addAll(res.getInstances());
            });
        }
        return lstInstances;
    }

    public List<SecurityGroup> describeSecurityGroups(List<String> securityGroup) {
        if (securityGroup == null || securityGroup.isEmpty()) {
            return new LinkedList<>();
        }
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
                .withGroupIds(securityGroup);
        DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request);
        return result.getSecurityGroups();
    }
//</editor-fold>
}
