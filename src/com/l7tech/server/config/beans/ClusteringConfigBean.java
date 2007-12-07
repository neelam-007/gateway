package com.l7tech.server.config.beans;

import com.l7tech.server.config.ClusteringType;
import com.l7tech.server.config.ConfigurationType;
import com.l7tech.server.config.SharedWizardInfo;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * User: megery
 * Date: Aug 12, 2005
 */
public class ClusteringConfigBean extends BaseConfigurationBean {

    private boolean isNewHostName;
    private String clusterHostname;
    private String localHostName;
    private ClusteringType clusterType;
    private ConfigurationType configType;
    private PartitionInformation partitionInformation;

    private final static String NAME = "Clustering Configuration";
    private final static String DESCRIPTION = "Configures the cluster properties for an SSG";

    public ClusteringConfigBean() {
        super(NAME, DESCRIPTION);
        init();
    }

    private void init() {
        try {
            setLocalHostName(InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        setNewHostName(false);
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());

        ConfigurationType configType = SharedWizardInfo.getInstance().getConfigType();
        switch (configType) {
            case CONFIG_STANDALONE:
                explanations.add(insertTab + "Configuring a standalone SSG (No clustering)");
                break;
            case CONFIG_CLUSTER:
                ClusteringType clusterType = SharedWizardInfo.getInstance().getClusterType();
                switch (clusterType) {
                    case CLUSTER_MASTER:
                        explanations.add(insertTab + "Configuring the first node in a cluster. Settings will be saved to the database.");
                        break;
                    default:
                        explanations.add(insertTab + "Configuring a new node in the cluster. Settings will be cloned from the master node.");
                        break;
                    }
                break;
        }

    }

    public void reset() {
//        isNewHostName = false;
//        clusterHostname = "";
//        localHostName  = "";
//        clusterType = ClusteringType.CLUSTER_MASTER;
    }

    public String getClusterHostname() {
        return clusterHostname;
    }

//    public String getClusterHostname() {
//        if (StringUtils.isEmpty(this.clusterHostname))
//            setClusterHostname(getPartitionInformation().getOSSpecificFunctions().getClusterHostName());
//
//        return this.clusterHostname;
//    }

    public void setClusterHostname(String clusterHostname) {

        this.clusterHostname = clusterHostname;
    }

    public boolean isNewHostName() {
        return isNewHostName;
    }

    public void setNewHostName(boolean newHostName) {
        isNewHostName = newHostName;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public ClusteringType getClusterType() {
        return clusterType;
    }

    public void setClusterType(ClusteringType clusterType) {
        this.clusterType = clusterType;
    }

    public ConfigurationType getConfigType() {
        return configType;
    }

    public void setConfigType(ConfigurationType configType) {
        this.configType = configType;
    }

    public PartitionInformation getPartitionInformation() {
        if (partitionInformation == null) partitionInformation = PartitionManager.getInstance().getActivePartition();
        return partitionInformation;
    }

    public void setPartitionInformation(PartitionInformation partitionInformation) {
        this.partitionInformation = partitionInformation;
    }
}
