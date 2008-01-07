package com.l7tech.server.config.beans;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

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
//    PartitionInformation partitionInfo;

    public PartitionConfigBean() {
        super(NAME, DESCRIPTION);

    }

    public void reset() {
        isNewPartition = false;
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        PartitionInformation pInfo = PartitionManager.getInstance().getActivePartition();
        String partName = pInfo.getPartitionId();
        List<PartitionInformation.EndpointHolder> endpoints = pInfo.getEndpoints();

        explanations.add(insertTab + (isNewPartition?"Creating new partition \"" + partName + "\"":"Updating \"" + partName + "\" partition"));

        for (PartitionInformation.EndpointHolder endpoint : endpoints) {
            if (!endpoint.isEnabled())
                continue;

            if (endpoint instanceof PartitionInformation.OtherEndpointHolder) {
                PartitionInformation.OtherEndpointHolder otherEndpoint = (PartitionInformation.OtherEndpointHolder) endpoint;
                explanations.add(insertTab + "    " + otherEndpoint.endpointType.getName() + "=" + otherEndpoint.getPort());
            } else {
                explanations.add(insertTab + "    " + endpoint);
            }
        }
    }

//    public PartitionInformation getPartitionInfo() {
//        return partitionInfo;
//    }

//    public void setPartitionInfo(PartitionInformation partitionInfo) {
//        this.partitionInfo = partitionInfo;
//    }
}
