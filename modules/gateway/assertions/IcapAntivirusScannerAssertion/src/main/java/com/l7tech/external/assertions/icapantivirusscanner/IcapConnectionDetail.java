package com.l7tech.external.assertions.icapantivirusscanner;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A bean to hold the server information.
 */
public final class IcapConnectionDetail implements Serializable {

    private String connectionName;
    private String hostname;
    private int port = 1344;
    private String serviceName = "avscan";
    private int timeout = 30000; //30 seconds

    private Map<String, String> serviceParameters;

    public IcapConnectionDetail() {
        serviceParameters = new HashMap<String, String>();
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(final String connectionName) {
        this.connectionName = connectionName;
    }

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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getServiceParameters() {
        return serviceParameters;
    }

    public void setServiceParameters(final Map<String, String> serviceParameters) {
        this.serviceParameters = new HashMap<String, String>(serviceParameters);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final IcapConnectionDetail that = (IcapConnectionDetail) o;

        if (port != that.port) return false;
        if (connectionName != null ? !connectionName.equals(that.connectionName) : that.connectionName != null)
            return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionName != null ? connectionName.hashCode() : 0;
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder(connectionName).append(" @ ").append(hostname).append(":").append(port).append("/").append(serviceName).toString();
    }
}
