package com.l7tech.server.util;

import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpConnectionManager;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.server.ServerConfig;

/**
 * GenericHttpClientFactory that supports identity binding.
 *
 * <p>The first time a client is requested a connection manager is created and
 * is initialized using the given host/total connection settings.</p>
 *
 * <p>All subsequent clients from this factory will use the same connection
 * manager.</p>
 *
 * <p>Typically this factory will be wired via a Spring application context and
 * will be deployed as a non-singleton.</p>
 *
 * <p>WARNING: read the info above on how the max connection settings work!</p>
 *
 * @author Steve Jones
 */
public class IdentityBindingHttpClientFactory implements GenericHttpClientFactory {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public IdentityBindingHttpClientFactory() {
    }

    /**
     * Create a new GenericHttpClient.
     *
     * @return The GenericHttpClient with default/factory settings.
     */
    public GenericHttpClient createHttpClient() {
        return createHttpClient(-1, -1, -1, -1, null);
    }

    /**
     * Create a new GenericHttpClient.
     *
     * <p>The first 2 parameters are ignored if this is not the first client
     * from this factory.</p>
     *
     * @param hostConnections the maximum number of connections to any given host (ignored if not the first client)
     * @param totalConnections the maximum number of connections (ignored if not the first client)
     * @param connectTimeout The connection timeout
     * @param timeout The read timeout
     * @param identity The bound identity for this client
     * @return The GenericHttpClient with given/factory settings.
     */
    public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
        return new CommonsHttpClient(getHttpConnectionManager(hostConnections, totalConnections),
                                     connectTimeout,
                                     timeout,
                                     identity);
    }

    //- PRIVATE

    // class
    private static final Logger logger = Logger.getLogger(IdentityBindingHttpClientFactory.class.getName());

    //
    private HttpConnectionManager connectionManager;

    /**
     *
     */
    private HttpConnectionManager getHttpConnectionManager(int hmax, int tmax) {
        HttpConnectionManager httpConnectionManager = this.connectionManager;

        if (httpConnectionManager  == null) {
            if (hmax <= 0) {
                hmax = CommonsHttpClient.getDefaultMaxConnectionsPerHost();
                tmax = CommonsHttpClient.getDefaultMaxTotalConnections();
            }

            IdentityBindingHttpConnectionManager connectionManager = new IdentityBindingHttpConnectionManager();
            connectionManager.setMaxConnectionsPerHost(hmax);
            connectionManager.setMaxTotalConnections(tmax);
            connectionManager.setPerHostStaleCleanupCount(getStaleCheckCount());
            this.connectionManager = connectionManager;
            httpConnectionManager = connectionManager;
        }

        return httpConnectionManager;
    }

    /**
     * Get the stale check count to use (set using a cluster/system property)
     */
    private int getStaleCheckCount() {
        return getIntProperty(ServerConfig.PARAM_IO_STALE_CHECK_PER_INTERVAL,0,1000,1);
    }

    /**
     * Get a system property using the configured min, max and default values.
     */
    private int getIntProperty(String propName, int min, int max, int defaultValue) {
        int value = defaultValue;

        try {
            String configuredValue = ServerConfig.getInstance().getPropertyCached(propName);
            if(configuredValue!=null) {
                value = Integer.parseInt(configuredValue);

                boolean useDefault = false;
                if(value<min) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is BELOW the minimum '"+min+"', using default value '"+defaultValue+"'.");
                }
                else if(value>max) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is ABOVE the maximum '"+max+"', using default value '"+defaultValue+"'.");
                }

                if(useDefault) value = defaultValue;
            }
        }
        catch(SecurityException se) {
            logger.warning("Cannot access property '"+propName+"', using default value '"+defaultValue+"', error is: " + se.getMessage());
        }
        catch(NumberFormatException nfe) {
            logger.warning("Cannot parse property '"+propName+"', using default value '"+defaultValue+"', error is: " + nfe.getMessage());
        }

        return value;
    }

}
