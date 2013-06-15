package com.l7tech.common.http.prov.apache.components;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 4/3/13
 *
 */
public class ClientConnectionManagerFactory {

    static final Logger log = Logger.getLogger(ClientConnectionManagerFactory.class.getName());
    private static final int DEFAULT_CONNECTIONS_PER_HOST = 1;
    private int DEFAULT_TOTAL_CONNECTIONS = 10;

    public enum ConnectionManagerType {
        BASIC, POOLING, STALE_CHECKING,
    }

    private static ClientConnectionManagerFactory _instance;

    public static synchronized ClientConnectionManagerFactory getInstance() {
        if(_instance == null) {
            _instance = new ClientConnectionManagerFactory();
        }
        return _instance;
    }

    private ClientConnectionManagerFactory() {

    }


    public ClientConnectionManager createConnectionManager(ConnectionManagerType type, int maxConnectionsPerHost, int maxTotalConnections) {
        ClientConnectionManager connectionManager = null;
        ConnectionManagerType managerType = type != null? type : ConnectionManagerType.POOLING;
        switch (managerType) {
        case STALE_CHECKING:
        case POOLING:
            PoolingClientConnectionManager poolingClientConnectionManager = new  PoolingClientConnectionManager();
            poolingClientConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerHost <= 0 ? DEFAULT_CONNECTIONS_PER_HOST : maxConnectionsPerHost);
            poolingClientConnectionManager.setMaxTotal(maxTotalConnections <= 0 ? DEFAULT_TOTAL_CONNECTIONS : maxTotalConnections);
            connectionManager = poolingClientConnectionManager;
            if(log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, "Created PoolingClientConnectionManager instance " + connectionManager);
            break;
        case BASIC:
            connectionManager = new BasicClientConnectionManager();
            if(log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, "Created BasicClientConnectionManager instance " + connectionManager);
            break;
        }

        return connectionManager;
    }

    public static void setConnectionDefaults(ClientConnectionManager connectionManager, int maxConnectionsPerHost, int maxTotalConnections) {
        if(connectionManager instanceof PoolingClientConnectionManager) {
            PoolingClientConnectionManager poolingClientConnectionManager = (PoolingClientConnectionManager) connectionManager;
            poolingClientConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerHost);
            poolingClientConnectionManager.setMaxTotal(maxTotalConnections);
        }
        else {
            if(log.isLoggable(Level.FINEST))
                log.log(Level.FINEST, "Connection Manager does not support connection defaults!");
        }

    }

}
