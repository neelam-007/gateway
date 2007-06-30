package com.l7tech.common.http.prov.apache;

import java.util.Map;

import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.HostConfiguration;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

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
        cache = new ConcurrentHashMap();
    }

    /**
     * This method ignores the given host configuration and returns the default.
     *
     * @param hostConfiguration Ignored, returns {@link #getDefaultMaxConnectionsPerHost}
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

    public Object getParameter(final String name) {
        return getCachedParameter(name);
    }

    public void setParameter(final String name, final Object value) {
       setCachedParameter(name, value);
    }

    //- PRIVATE

    private static final Object NULL_OBJECT = new Object();

    private final int maxConnectionsPerHost;
    private final int maxTotalConnections;
    private final Map cache;

    private Object getCachedParameter(final String name) {
        Object value = cache.get(name);

        if (value == null) {
            value = super.getParameter(name);
            setCachedParameter(name, value);
        } else if (value == NULL_OBJECT) {
            value = null;
        }

        return value;
    }

    private void setCachedParameter(final String name, final Object value) {
        if (name != null && value != null)
            cache.put(name, value);
        else if (value == null && name != null)
            cache.put(name, NULL_OBJECT);
    }
}
