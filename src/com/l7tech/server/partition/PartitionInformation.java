package com.l7tech.server.partition;

/**
 * User: megery
 * Date: Nov 10, 2006
 * Time: 10:15:56 AM
 */
public class PartitionInformation {
    public static final String PARTITIONS_BASE = "etc/conf/partitions/";
    public static final String TEMPLATE_PARTITION_NAME = "partitiontemplate_";
    String partitionId;

    public PartitionInformation(String partitionId) {
        this.partitionId = partitionId;
    }
}
