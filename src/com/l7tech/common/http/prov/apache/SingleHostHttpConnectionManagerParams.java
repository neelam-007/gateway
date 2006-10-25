package com.l7tech.common.http.prov.apache;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.HostConfiguration;

/**
 * HttpConnectionManagerParams extension that does not support multiple hosts.
 *
 * <p>This class does not have the synchronization issues of
 * HttpConnectionManagerParams.</p>
 *
 * @author Steve Jones
 */
public class SingleHostHttpConnectionManagerParams extends HttpConnectionManagerParams {

    //- PUBLIC

    public SingleHostHttpConnectionManagerParams(int maxConnectionsPerHost, int maxTotalConnections) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.maxTotalConnections = maxTotalConnections;
    }

    /**
     * This method ignores the given host configuration and returns the default.
     *
     * @param hostConfiguration Ignored, returns {@link #getDefaultMaxConnectionsPerHost}
     * @return
     */
    public int getMaxConnectionsPerHost(HostConfiguration hostConfiguration) {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(HostConfiguration hostConfiguration, int maxHostConnections) {
        throw new UnsupportedOperationException("Read only");
    }

    public int getDefaultMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setDefaultMaxConnectionsPerHost(int maxHostConnections) {
        throw new UnsupportedOperationException("Read only");
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        throw new UnsupportedOperationException("Read only");
    }

    //- PRIVATE

    private final int maxConnectionsPerHost;
    private final int maxTotalConnections;
}
