package com.l7tech.cluster;

/*
 * This class encapsulates the data to be displayed in the cluster status panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class GatewayStatus {

    public GatewayStatus(ClusterInfo clusterInfo) {
        this.clusterInfo = clusterInfo;
    }

   /**
     * Get node name
     */
    public String getName() {
        return clusterInfo.getName();
    }

   /**
     * Get node status
     */
   public int getStatus() {
        return status;
    }

   /**
     * Get load sharing (%)
     */
    public int getLoadSharing() {
        return loadSharing;
    }

   /**
     * Get request failure (%)
     */
    public int getRequestFailure() {
        return requestFailure;
    }

   /**
     * Get load average for the last minute
     */
    public double getAvgLoad() {
        return clusterInfo.getAvgLoad();
    }

   /**
     * Get timestamp of when the node last booted
     */
    public long getUptime() {
        return clusterInfo.getUptime();
    }

   /**
     * Get direct ip address of this node
     */
    public String getAddress() {
        return clusterInfo.getAddress();
    }


    /**
      * Set node status
      */
    public void setStatus(int status) {
         this.status = status;
     }

    /**
      * Set load sharing (%)
      */
     public void setLoadSharing(int loadSharing) {
         this.loadSharing = loadSharing;
     }

    /**
      * Set request failure (%)
      */
     public void setRequestFailure(int requestFailure) {
         this.requestFailure = requestFailure;
     }

    private final ClusterInfo clusterInfo;
    private int status;
    private int loadSharing;
    private int requestFailure;
}
