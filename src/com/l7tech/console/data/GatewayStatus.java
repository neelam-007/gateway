package com.l7tech.console.data;

/*
 * This class encapsulates the data to be displayed in the cluster status panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class GatewayStatus {

    public GatewayStatus(int status, String name, int loadSharing, int requestFailure, double loadAvg, String uptime, String ipAddress) {
        this.name = name;
        this.status = status;
        this.loadSharing = loadSharing;
        this.requestFailure = requestFailure;
        this.loadAvg = loadAvg;
        this.uptime = uptime;
        this.ipAddress = ipAddress;
    }

   /**
     * node name
     */
    public String getName() {
        return name;
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
    public double getLoadAvg() {
        return loadAvg;
    }

   /**
     * timestamp of when the node last booted
     */
    public String getUptime() {
        return uptime;
    }

   /**
     * direct ip address of this node
     */
    public String getIpAddress() {
        return ipAddress;
    }

    private final String name;
    private final int status;
    private final int loadSharing;
    private final int requestFailure;
    private final double loadAvg;
    private final String uptime;
    private final String ipAddress;
}
