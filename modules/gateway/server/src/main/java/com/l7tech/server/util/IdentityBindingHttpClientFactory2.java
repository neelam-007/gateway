package com.l7tech.server.util;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager2;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.server.transport.http.HttpConnectionManagerListener2;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.pool.PoolStats;

public class IdentityBindingHttpClientFactory2 implements GenericHttpClientFactory {

    private ClientConnectionManager connectionManager;
    private HttpConnectionManagerListener2 listener = new HttpConnectionManagerListener2.HttpConnectionManagerListenerAdapter2();

    @Override
    public GenericHttpClient createHttpClient() {
        return createHttpClient(-1, -1, -1, -1, null);
    }

    @Override
    public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
        return new HttpComponentsClient(getHttpConnectionManager(hostConnections, totalConnections),
                connectTimeout,
                timeout,
                identity);
    }

    /**
     * Optional configuration for an HttpConnectionManagerListener
     *
     * @param listener The HttpConnectionManagerListener to use.
     */
    public void setListener(final HttpConnectionManagerListener2 listener) {
        this.listener = listener == null ?
                new HttpConnectionManagerListener2.HttpConnectionManagerListenerAdapter2() :
                listener;
    }

    private ClientConnectionManager getHttpConnectionManager(int hostConnections, int totalConnections) {
        if (connectionManager == null) {
            if (hostConnections <= 0) {
                hostConnections = HttpComponentsClient.getDefaultMaxConnectionsPerHost();
                totalConnections = HttpComponentsClient.getDefaultMaxTotalConnections();
            }
            PoolingClientConnectionManager manager = new IdentityBindingHttpConnectionManager2();
            manager.setDefaultMaxPerRoute(hostConnections);
            manager.setMaxTotal(totalConnections);
            this.connectionManager = manager;
            listener.notifyHttpConnectionManagerCreated(connectionManager);
        }
        return connectionManager;
    }

    protected PoolStats getTotalStats() {
        if (connectionManager == null) {
            return null;
        } else {
            return ((PoolingClientConnectionManager) connectionManager).getTotalStats();
        }
    }

}
