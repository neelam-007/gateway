package com.l7tech.cluster;

/*
 * This class encapsulates the data to be displayed in the cluster status panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class GatewayStatus {

    public GatewayStatus(ClusterInfo clusterInfo, int status, int loadSharing, int requestFailure) {
        this.clusterInfo = clusterInfo;
        this.status = status;
        this.loadSharing = loadSharing;
        this.requestFailure = requestFailure;
    }

   /**
     * node name
     */
    public String getName() {
        return clusterInfo.getName();
    }

   /**
     * node status
     */
   public int getStatus() {
        return status;
    }

   /**
     * load sharing (%)
     */
    public int getLoadSharing() {
        return loadSharing;
    }

   /**
     * Request Failure (%)
     */
    public int getRequestFailure() {
        return requestFailure;
    }

   /**
     * Load Average for the last minute
     */
    public double getAvgLoad() {
        return clusterInfo.getAvgLoad();
    }

   /**
     * timestamp of when the node last booted
     */
    public long getUptime() {
        return clusterInfo.getUptime();
    }

   /**
     * direct ip address of this node
     */
    public String getAddress() {
        return clusterInfo.getAddress();
    }


    private final ClusterInfo clusterInfo;
    private final int status;
    private final int loadSharing;
    private final int requestFailure;
}
