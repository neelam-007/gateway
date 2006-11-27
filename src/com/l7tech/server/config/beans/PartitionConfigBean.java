package com.l7tech.server.config.beans;

import com.l7tech.server.partition.PartitionInformation;

import java.util.Map;

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
        Map<PartitionInformation.EndpointType,PartitionInformation.EndpointHolder> endpoints = partitionInfo.getEndpoints();

        explanations.add(insertTab + (isNewPartition?"Creating new partition \"":"Updating partition \"") + partName + "\"");

        for (PartitionInformation.EndpointType endpointType : endpoints.keySet()) {
            explanations.add(insertTab + "  Endpoint = " + endpoints.get(endpointType));
        }
    }

    public void setPartition(PartitionInformation pi) {
        this.partitionInfo = pi;
    }

    public PartitionInformation getPartitionInfo() {
        return partitionInfo;
    }
}
