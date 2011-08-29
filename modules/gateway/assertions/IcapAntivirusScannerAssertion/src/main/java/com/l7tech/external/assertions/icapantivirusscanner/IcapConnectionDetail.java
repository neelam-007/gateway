package com.l7tech.external.assertions.icapantivirusscanner;

import java.io.Serializable;

/**
 * <p>A bean to hold the ICAP server information.</p>
 *
 * @author Ken Diep
 */
public final class IcapConnectionDetail implements Serializable {

    private String hostname;
    private int port = 1344;
    private String serviceName = "avscan";
    private int timeout = 30; //30 seconds

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    public int getTimeoutMilli(){
        return timeout * 1000;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return new StringBuilder("icap://").append(getHostname()).append(":").append(getPort()).append("/").append(getServiceName()).toString();
    }
}
