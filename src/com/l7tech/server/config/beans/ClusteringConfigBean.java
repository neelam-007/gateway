package com.l7tech.server.config.beans;

import com.l7tech.server.config.ClusteringType;
import com.l7tech.server.config.ConfigurationType;
import com.l7tech.server.config.SharedWizardInfo;
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


    private final static String NAME = "Clustering Configuration";
    private final static String DESCRIPTION = "Configures the cluster properties for an SSG";

    private final static String NOTHING_TO_DO_INFO = "Cluster configuration will be skipped";
    private final static String CLUSTER_HOSTFILE_UPDATE_INFO = "Update cluster hostname: ";

    public static final int CLUSTER_NONE = 0;
    public static final int CLUSTER_NEW = 1;
    public static final int CLUSTER_JOIN = 2;

    public static class ClusterTypePair {
        private String clusterTypeDescription;
        private Integer clusterType;

        public ClusterTypePair(String clusterTypeDescription, Integer clusterType) {
            this.clusterTypeDescription = clusterTypeDescription;
            this.clusterType = clusterType;
        }

        public String getClusterTypeDescription() {
            return clusterTypeDescription;
        }

        public Integer getClusterType() {
            return clusterType;
        }
    }

    public static ClusterTypePair[] clusterTypes = new ClusterTypePair[] {
        new ClusterTypePair("I don't want to set up a cluster (or this SSG already belongs to one", new Integer(CLUSTER_NONE)),
        new ClusterTypePair("I want to create a new cluster", new Integer(CLUSTER_NEW)),
        new ClusterTypePair("I would like this SSG to join an existing cluster", new Integer(CLUSTER_JOIN)),
    };

    public ClusteringConfigBean() {
        super(NAME, DESCRIPTION);
        ELEMENT_KEY = this.getClass().getName();
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
        isNewHostName = false;
        clusterHostname = "";
        localHostName  = "";
        clusterType = ClusteringType.CLUSTER_MASTER;
    }

    public void setClusterHostname(String hostName) {
        this.clusterHostname = hostName;
    }

    public String getClusterHostname() {
        if (StringUtils.isEmpty(this.clusterHostname))
            setClusterHostname(getOsFunctions().getClusterHostName());
        
        return this.clusterHostname;
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

    public void setDoClusterType(ClusteringType clusterType) {
        this.clusterType= clusterType;
    }

    public ClusteringType getClusterType() {
        return clusterType;
    }
}
