package com.l7tech.server.config.beans;

import com.l7tech.server.partition.PartitionInformation;

import java.util.List;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:28:36 AM
 */
public class PartitionConfigBean extends BaseConfigurationBean{
    private final static String NAME = "Partitioning Configuration";
    private final static String DESCRIPTION = "Configures Gateway Partitions";

    private boolean isNewPartition;
    PartitionInformation partitionInfo;

    public PartitionConfigBean() {
        super(NAME, DESCRIPTION);

    }

    public void reset() {
        isNewPartition = false;
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        String partName = partitionInfo.getPartitionId();
        List<PartitionInformation.HttpEndpointHolder> httpEndpoints = partitionInfo.getHttpEndpoints();
        List<PartitionInformation.OtherEndpointHolder> otherEndpoints = partitionInfo.getOtherEndpoints();

        explanations.add(insertTab + (isNewPartition?"Creating new partition \"" + partName + "\"":"Updating \"" + partName + "\" partition"));

        for (PartitionInformation.HttpEndpointHolder endpoint : httpEndpoints) {
            explanations.add(insertTab + "    " + endpoint);
        }

        for (PartitionInformation.OtherEndpointHolder otherEndpoint : otherEndpoints) {
            explanations.add(insertTab + "    " + otherEndpoint.endpointType.getName() + "=" + otherEndpoint.getPort());
        }
    }

    public void setPartition(PartitionInformation pi) {
        this.partitionInfo = pi;
    }

    public PartitionInformation getPartitionInfo() {
        return partitionInfo;
    }
}
