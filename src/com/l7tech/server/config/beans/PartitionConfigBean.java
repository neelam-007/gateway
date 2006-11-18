package com.l7tech.server.config.beans;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:28:36 AM
 */
public class PartitionConfigBean extends BaseConfigurationBean{
    private final static String NAME = "Partitioning Configuration";
    private final static String DESCRIPTION = "Configures Gateway Partitions";

    private boolean isNewPartition;
    private String partitionName;

    public PartitionConfigBean() {
        super(NAME, DESCRIPTION);

    }

    public void reset() {
        isNewPartition = false;
        partitionName = "";
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (isNewPartition)
            explanations.add(insertTab + "Creating new partition \"" + partitionName + "\"");
        else
            explanations.add(insertTab + "Updating partition \"" + partitionName + "\"");
    }


    public boolean isNewPartition() {
        return isNewPartition;
    }

    public void setNewPartition(boolean newPartition) {
        isNewPartition = newPartition;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }
}
