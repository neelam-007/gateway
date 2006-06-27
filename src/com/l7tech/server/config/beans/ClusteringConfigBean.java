package com.l7tech.server.config.beans;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 12, 2005
 * Time: 11:26:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClusteringConfigBean extends BaseConfigurationBean {
   private static final String updateHostsFileLine =
        "<li>UPDATE HOSTS FILE:" +
            "<p>add a line containing the IP address for this SSG node, then the cluster host name, then this SSG node's hostname" + eol +
            "<dl>" + eol +
                "<dt>ex:</dt>" + eol +
                    "<dd>192.168.1.186      ssgcluster.domain.com ssgnode1.domain.com</dd>" + eol +
            "</dl>" + eol +
        "</li>" + eol;

    private static final String timeSyncLine =
        "<li>TIME SYNCHRONIZATION:" +
            "<p>Please ensure time is synchronized among all SSG nodes within the cluster" + eol +
        "</li>" + eol;

    private boolean isNewHostName;
    private String hostname;
    private String localHostName;
    private int clusterType;


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
        setClusterHostname(null);

        try {
            setLocalHostName(InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        setNewHostName(false);
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());

        if (clusterType == CLUSTER_NONE )  {
            explanations.add(insertTab + NOTHING_TO_DO_INFO);
        } else {
            explanations.add(insertTab + CLUSTER_HOSTFILE_UPDATE_INFO + getClusterHostname());
        }
    }

    public void reset() {
        isNewHostName = false;
        hostname = "";
        localHostName  = "";
        clusterType = CLUSTER_NONE;
    }

    public void setClusterHostname(String hostName) {
        this.hostname = hostName;
    }

    public String getClusterHostname() {
        return this.hostname;
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

    public void setDoClusterType(int clusterType) {
        this.clusterType= clusterType;
    }

    public int getClusterType() {
        return clusterType;
    }

    public List<String> getManualSteps() {
        List<String> steps = new ArrayList<String>();
        if (clusterType != CLUSTER_NONE) {
            steps.add(updateHostsFileLine);
            steps.add(timeSyncLine);
            steps.add("<br>");
        }
        return steps;
    }

}
