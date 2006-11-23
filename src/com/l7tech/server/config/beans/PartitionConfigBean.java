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
        List<PartitionInformation.EndpointHolder> endpoints = partitionInfo.getEndpointsList();

        explanations.add(insertTab + (isNewPartition?"Creating new partition \"":"Updating partition \"") + partName + "\"");

        for (PartitionInformation.EndpointHolder endpoint : endpoints) {
            explanations.add(insertTab + "  Endpoint = " + endpoint);
        }
    }

    public void setPartition(PartitionInformation pi) {
        this.partitionInfo = pi;
    }

    public PartitionInformation getPartitionInfo() {
        return partitionInfo;
    }
}
