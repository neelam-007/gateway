package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 6/14/12
 * Time: 12:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoherenceRemoteCache implements RemoteCache {

    private static final Logger LOGGER = Logger.getLogger(CoherenceRemoteCache.class.getName());

    public static final String PROP_CACHE_NAME = "cacheName";
    public static final String PROP_SERVERS = "servers";

    public static final String CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY = "remote.cache.coherence.connectionTimeout";
    public static final String DEFAULT_CONNECTION_TIMEOUT = "5000";

    private static final String CACHE_CONFIG =
            "<?xml version=\"1.0\"?>\n" +
                    "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "   xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"\n" +
                    "   xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config\n" +
                    "   coherence-cache-config.xsd\">\n" +
                    "  <caching-scheme-mapping>\n" +
                    "    <cache-mapping>\n" +
                    "      <cache-name>CACHE-NAME-REPLACEMENT</cache-name>\n" +
                    "      <scheme-name>remote</scheme-name>\n" +
                    "    </cache-mapping>\n" +
                    "  </caching-scheme-mapping>\n" +
                    "  <caching-schemes>\n" +
                    "    <remote-cache-scheme>\n" +
                    "      <scheme-name>remote</scheme-name>\n" +
                    "      <service-name>SERVICE-NAME-REPLACEMENT</service-name>\n" +
                    "      <initiator-config>\n" +
                    "        <tcp-initiator>\n" +
                    "          <remote-addresses>\n" +
                    "SOCKET-ADDRESSES-REPLACEMENT" +
                    "          </remote-addresses>\n" +
                    "          <connect-timeout>CONNECT-TIMEOUT-REPLACEMENT</connect-timeout>\n" +
                    "        </tcp-initiator>\n" +
                    "        <outgoing-message-handler>\n" +
                    "          <request-timeout>REQUEST-TIMEOUT-REPLACEMENT</request-timeout>\n" +
                    "        </outgoing-message-handler>\n" +
                    "      </initiator-config>\n" +
                    "    </remote-cache-scheme>\n" +
                    "  </caching-schemes>\n" +
                    "</cache-config>";

    private static final String SOCKET_ADDRESS =
            "            <socket-address>\n" +
                    "              <address>ADDRESS-REPLACEMENT</address>\n" +
                    "              <port>PORT-REPLACEMENT</port>\n" +
                    "            </socket-address>\n";

    private final RemoteCacheEntity remoteCacheEntity;

    private Object cache;
    private CoherenceClassLoader coherenceClassLoader;

    public CoherenceRemoteCache(RemoteCacheEntity remoteCacheEntity, ClusterPropertyManager clusterPropertyManager, CoherenceClassLoader coherenceClassLoader) throws Exception {
        this.remoteCacheEntity = remoteCacheEntity;
        this.coherenceClassLoader = coherenceClassLoader;

        final HashMap<String, String> properties = this.remoteCacheEntity.getProperties();
        final String cacheName = properties.get(CoherenceRemoteCache.PROP_CACHE_NAME);
        String cacheConfigFilePath = null;

        try {
            cacheConfigFilePath = createCacheConfigurationFile(clusterPropertyManager);
            Object configurationFactory = coherenceClassLoader.newConfigurationFactory(cacheConfigFilePath);
            cache = coherenceClassLoader.ensureCache(configurationFactory, cacheName);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred during cache initialization.", e);
            throw e;
        } finally {
            // Delete temporary file.
            //
            if (cacheConfigFilePath != null) {
                File cacheConfigFile = new File(cacheConfigFilePath);
                if (cacheConfigFile.exists()) {
                    cacheConfigFile.delete();
                }
            }
        }
    }

    /**
     * Constructor for the purpose of unit test
     */
    CoherenceRemoteCache(RemoteCacheEntity entity, CoherenceClassLoader coherenceClassLoader, Object cache) {
        this.remoteCacheEntity = entity;
        this.coherenceClassLoader = coherenceClassLoader;
        this.cache = cache;
    }

    @Override
    public CachedMessageData get(String key) throws Exception {
        Object value = coherenceClassLoader.getCache(cache, key);
        if (null == value) {
            throw new Exception("Cached entry not found.");
        } else {
            if (!(value instanceof CachedMessageData)) {
                throw new Exception("Unexpected cache value.");
            }
            return (CachedMessageData) value;
        }
    }

    @Override
    public void set(String key, CachedMessageData value, int expiry) throws Exception {
        int entryExpiry;

        // Allow value of 0 and -1 for coherence (0 means use default, -1 means never expire)
        // to pass onto coherence.
        switch (expiry) {
            case 0:
            case -1:
                entryExpiry = expiry;
                break;
            default:
                entryExpiry = expiry * 1000;
                break;
        }

        // Convert expiry time from seconds to milliseconds.
        coherenceClassLoader.putCache(cache, key, value, entryExpiry);
    }

    @Override
    public void remove(String key) throws Exception {
        coherenceClassLoader.removeFromCache(cache, key);
    }

    @Override
    public void shutdown() {
        if (cache != null) {
            coherenceClassLoader.release(cache);
        }
    }

    private String createCacheConfigurationFile(ClusterPropertyManager clusterPropertyManager) throws IOException {
        final HashMap<String, String> properties = remoteCacheEntity.getProperties();
        final String cacheName = properties.get(CoherenceRemoteCache.PROP_CACHE_NAME);
        final String servers = properties.get(CoherenceRemoteCache.PROP_SERVERS);
        final int timeoutSec = remoteCacheEntity.getTimeout();
        final String remoteCacheEntityGoid = remoteCacheEntity.getGoid().toString();

        String connectionTimeout = null;
        try {
            connectionTimeout = clusterPropertyManager.getProperty(CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
        } catch (FindException e) {
            connectionTimeout = null;
        }

        if (connectionTimeout == null) {
            connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        }

        // Create socket address configuration
        //
        StringBuilder socketAddresses = new StringBuilder();
        String[] serversList = servers.split(",");
        for (String server : serversList) {
            String socketAddress = new String(SOCKET_ADDRESS);
            socketAddress = socketAddress.replaceFirst("ADDRESS-REPLACEMENT", server.substring(0, server.indexOf(':')).trim());
            socketAddress = socketAddress.replaceFirst("PORT-REPLACEMENT", server.substring(server.indexOf(':') + 1).trim());
            socketAddresses.append(socketAddress);
        }

        // Create cache configuration
        //
        String cacheConfig = new String(CACHE_CONFIG);
        cacheConfig = cacheConfig.replaceFirst("CACHE-NAME-REPLACEMENT", cacheName);
        cacheConfig = cacheConfig.replaceFirst("SERVICE-NAME-REPLACEMENT", "ExtendTcpCacheService-" + UUID.randomUUID().toString());
        cacheConfig = cacheConfig.replaceFirst("SOCKET-ADDRESSES-REPLACEMENT", socketAddresses.toString());
        cacheConfig = cacheConfig.replaceFirst("CONNECT-TIMEOUT-REPLACEMENT", connectionTimeout + "ms");
        cacheConfig = cacheConfig.replaceFirst("REQUEST-TIMEOUT-REPLACEMENT", Integer.toString(timeoutSec) + "s");

        // TBD (kpak) - Limit Max size

        // Create a temporary directory
        //
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "SSG-Coherence-Cache");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        // Create a cache configuration file. The file is named "remote cache OID".xml
        //
        final File cacheConfigFile = new File(tempDir, remoteCacheEntityGoid.toString() + ".xml");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(cacheConfigFile);
            fos.write(cacheConfig.getBytes());

            return cacheConfigFile.getPath();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }
}