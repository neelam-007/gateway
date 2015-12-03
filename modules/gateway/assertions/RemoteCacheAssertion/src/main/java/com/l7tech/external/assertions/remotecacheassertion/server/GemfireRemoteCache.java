package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 6/6/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class GemfireRemoteCache implements RemoteCache {

    private static final Logger LOGGER = Logger.getLogger(GemfireRemoteCache.class.getName());
    public static final String PROPERTY_SERVERS = "servers";
    public static final String PROPERTY_CACHE_NAME = "cacheName";
    public static final String PROPERTY_CACHE_OPTION = "cacheOption";
    public static final String PROPERTY_LOG_FILE = "log-file";
    public static final String PROPERTY_DEPLOY_WOKRING_DIR = "deploy-working-dir";
    private static final String CACHE_XML_FILE_NAME = "GemfireRegionExpiration";
    private static final String TEMP_DIR_NAME = "SSG-GemFire-Cache";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/" + TEMP_DIR_NAME;

    private static final String CACHE_XML_FILE =
            "<?xml version=\"1.0\"?>\n" +
                    "<!DOCTYPE client-cache PUBLIC\n" +
                    "\"-//GemStone Systems, Inc.//GemFire Declarative Caching 6.6//EN\"\n" +
                    "\"http://www.gemstone.com/dtd/cache6_6.dtd\">\n" +
                    "<client-cache>\n" +
                    "  <region name=CACHE-REGION refid=\"CACHING_PROXY\">\n" +
                    "   <region-attributes>\n" +
                    "       <entry-time-to-live>\n" +
                    "           <expiration-attributes timeout=CACHE_TIMEOUT action=\"destroy\"/>\n" +
                    "       </entry-time-to-live>\n" +
                    "   </region-attributes>\n" +
                    "  </region>\n" +
                    "</client-cache>";

    private RemoteCacheEntity remoteCacheEntity;
    private HashMap<String, String> properties;

    private GemFireClassLoader gemfireClassLoader;

    private Object region;
    private Object clientCache;
    private Object clientCacheFactory;

    public GemfireRemoteCache(RemoteCacheEntity entity, GemFireClassLoader gemFireClassLoader) throws Exception {
        this.remoteCacheEntity = entity;
        this.gemfireClassLoader = gemFireClassLoader;
        this.properties = this.remoteCacheEntity.getProperties();
        final String cacheName = properties.get(PROPERTY_CACHE_NAME);
        final String servers = properties.get(PROPERTY_SERVERS);

        String cacheConfigFilePath = null;

        try {
            cacheConfigFilePath = setCacheXmlFile(cacheName);
            initClient(cacheName, servers, cacheConfigFilePath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred during Gemfire cache initialization.", e);
            throw e;
        } catch (NoClassDefFoundError ne) {
            LOGGER.log(Level.WARNING, "Error occurred during Gemfire cache initialization. Unable to find GemFire jar file in /lib/ext");
            throw new Exception("Error occurred during Gemfire cache initialization. Unable to find GemFire jar file in /lib/ext");
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
     * Constructor used for junit testing
     */
    GemfireRemoteCache(GemFireClassLoader gemfireClassLoader, Object region, Object clientCache) {
        this.gemfireClassLoader = gemfireClassLoader;
        this.region = region;
        this.clientCache = clientCache;
    }

    /*
     * Connect to the distributed system as a peer and declare a region
     */
    public void initClient(String cacheName, String servers, String pathToCacheFile) throws Exception {

        LOGGER.log(Level.INFO, "Initializing GemFire client cache with servers: " + servers + " cacheName: " + cacheName);

        clientCacheFactory = gemfireClassLoader.createClientCacheFactory();
        gemfireClassLoader.set(clientCacheFactory, "cache-xml-file", pathToCacheFile);
        gemfireClassLoader.setPoolSubscriptionEnabled(clientCacheFactory, true);

        initClientCache(servers);
        //create client cache
        try {
            shutdown();
            clientCache = gemfireClassLoader.createClientCache(clientCacheFactory);
            LOGGER.log(Level.INFO, "GemFire client cache is created");
            //get the region that is set up in the cache-xml-file
            region = gemfireClassLoader.getRegion(clientCache, cacheName);
        } catch (IllegalStateException ise) {
            LOGGER.log(Level.WARNING, "A connection to a distributed system already exists in this VM.");
        } catch (Exception e) {
            throw new Exception("An error occurred during GemFire Initialization", e);
        }
    }

    /*
     *  Load the system properties analog to gemfire.properties into the CacheFactory
     */
    private void initClientCache(String servers) throws Exception {

        // By default the deploy-working-dir of gemfire is System.getProperty("user.dir")  which is not writable.
        // To silence the exception, if the property is not defined, we set it to the tmp directory
        if (!properties.containsKey(PROPERTY_DEPLOY_WOKRING_DIR)) {
            properties.put(PROPERTY_DEPLOY_WOKRING_DIR, TEMP_DIR);
            LOGGER.log(Level.INFO, "Setting gemfire deploy-working-dir to: " + TEMP_DIR);
        }

        for (String property : properties.keySet()) {
            if (!property.contains(PROPERTY_CACHE_NAME) &&
                    !property.contains(PROPERTY_SERVERS) &&
                    !property.contains(PROPERTY_CACHE_OPTION) &&
                    !property.contains(PROPERTY_LOG_FILE)) {
                Object propertyValue = properties.get(property);

                gemfireClassLoader.set(clientCacheFactory, property, String.valueOf(propertyValue));
            }
        }

        String[] serversList = servers.split(",");

        if (serversList.length == 0) {
            LOGGER.log(Level.WARNING, "Server list is empty.");
            throw new Exception("Server list is empty");
        }
        String cacheOption = properties.get(PROPERTY_CACHE_OPTION);
        try {
            for (String server : serversList) {

                String address = server.substring(0, server.indexOf(':')).trim();
                String port = server.substring(server.indexOf(':') + 1).trim();
                switch (cacheOption){
                    case "locator":
                        gemfireClassLoader.addPoolLocator(clientCacheFactory, address, Integer.parseInt(port));
                        break;
                    case "server":
                        gemfireClassLoader.addPoolServer(clientCacheFactory, address, Integer.parseInt(port));
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Neither locator nor server was selected.");
                        throw new Exception("Wrong selection in the ClientCacheFactory");
                }
            }
        } catch (StringIndexOutOfBoundsException ex) {
            LOGGER.log(Level.WARNING, "Unable to parse port or address from servers list.", ex);
            throw ex;
        }
    }

    /*
     * Build temporarily cache-xml-file that is used to set up the cache timeout
     */
    private String setCacheXmlFile(String regionName) throws IOException {
        final int timeoutSec = remoteCacheEntity.getTimeout();

        String cacheConfig = new String(CACHE_XML_FILE);
        cacheConfig = cacheConfig.replaceFirst("CACHE-REGION", "\"" + regionName + "\"");
        cacheConfig = cacheConfig.replaceFirst("CACHE_TIMEOUT", "\"" + String.valueOf(timeoutSec) + "\"");

        File tempDir = new File(System.getProperty("java.io.tmpdir"), TEMP_DIR_NAME);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        final File cacheConfigFile = new File(tempDir, CACHE_XML_FILE_NAME + "_" + regionName + "_.xml");
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

    @Override
    public void set(String key, CachedMessageData value, int expiry) throws Exception {
        try {
            if (!gemfireClassLoader.isDestroyed(region)) {
                gemfireClassLoader.put(region, key, value.toByteArray());
            } else {
                throw new Exception("Region is destroyed");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to perform put in GemFire", e.getCause());
            throw new Exception(e);
        }
    }

    @Override
    public void remove(String key) throws Exception {
        try {
            if (!gemfireClassLoader.isDestroyed(region)) {
                gemfireClassLoader.remove(region, key);
            } else {
                throw new Exception("Region is destroyed");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot remove entry from region: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public CachedMessageData get(String key) throws Exception {
        if (gemfireClassLoader.isDestroyed(region)) {
            throw new Exception("Region is destroyed");
        }

        Object entry = gemfireClassLoader.getEntry(region, key);

        if (entry == null) {
            throw new Exception("Key doesn't exist in the region");
        }

        if (gemfireClassLoader.isEntryDestroyed(entry)) {
            throw new Exception("The key entry is destroyed");
        }

        Object value = gemfireClassLoader.getEntryValue(entry);

        if (!(value instanceof byte[])) {
            throw new Exception("Key exists in the region, but is not of the type byte[]");
        }

        return new CachedMessageData((byte[]) value);
    }

    @Override
    public void shutdown() {
        if (clientCache != null) {
            try {
                gemfireClassLoader.close(clientCache);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Not able to close cache connection", e);
            }
        }
    }
}
