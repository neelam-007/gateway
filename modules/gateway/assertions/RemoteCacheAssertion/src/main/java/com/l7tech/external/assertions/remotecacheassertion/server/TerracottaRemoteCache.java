package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 15/11/11
 * Time: 4:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class TerracottaRemoteCache implements RemoteCache {
    public static final String TERRACOTTA_EVICTION_POLICY_PROPERTY_STRING = "terracotta.eviction.policy";
    public static final String TERRACOTTA_MAXBYTESHEAP_PROPERTY_STRING = "terracotta.maxBytesHeap";
    public static final String TERRACOTTA_MAXBYTESOFFHEAP_PROPERTY_STRING = "terracotta.maxBytesOffHeap";
    public static final String TERRACOTTA_DEFAULT_TIME_TO_LIVE_SECONDS_PROPERTY_STRING = "terracotta.defaultTimeToLiveSeconds";
    public static final String TERRACOTTA_DEFAULT_TIME_TO_IDLE_PROPERTY_STRING = "terracotta.defaultTimeToIdleSeconds";
    public static final String TERRACOTTA_ORPHAN_EVICTION_PROPERTY_STRING = "terracotta.orphanEviction";
    public static final String TERRACOTTA_ORPHAN_EVICTION_PERIOD_PROPERTY_STRING = "terracotta.orphanEvictionPeriod";
    public static final String TERRACOTTA_SIZEOFMAXDEPTH_PROPERTY_STRING = "terracotta.sizeOfPolicy.MaxDepth";
    public static final String TERRACOTTA_SIZEOFMAXDEPTHEXCEEDEDBEHAVIOR_PROPERTY_STRING = "terracotta.sizeOfPolicy.MaxDepthExceededBehavior";
    public static final String EHCACHE_CONFIG_FILE_PROPERTY_STRING = "ehcache.configuration.file";

    public static final String CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY = "remote.cache.terracotta.connectionTimeout";
    public static final String DEFAULT_CONNECTION_TIMEOUT = "5000";

    private static final Logger LOGGER = Logger.getLogger(TerracottaRemoteCache.class.getName());
    public static final String PROPERTY_URLS = "urls";
    public static final String PROPERTY_CACHE_NAME = "cacheName";

    private TerracottaToolkitClassLoader terracottaToolkitClassLoader = null;

    private Object cacheManager;
    private Object cache;

    /**
     * Constructor used for testing only
     */
    TerracottaRemoteCache(TerracottaToolkitClassLoader tkClassLoader, Object cacheManager, Object cache) {
        this.terracottaToolkitClassLoader = tkClassLoader;
        this.cacheManager = cacheManager;
        this.cache = cache;
    }

    public TerracottaRemoteCache(RemoteCacheEntity remoteCacheEntity, ClusterPropertyManager propertyManager, TerracottaToolkitClassLoader classLoader)
            throws Exception {
        this.terracottaToolkitClassLoader = classLoader;

        if (StringUtils.isNotBlank(remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS))) {
            String ehCacheConfigFile = terracottaToolkitClassLoader.getSystemProperty(EHCACHE_CONFIG_FILE_PROPERTY_STRING);
            if (null == ehCacheConfigFile) {
                ehCacheConfigFile = System.getProperty(EHCACHE_CONFIG_FILE_PROPERTY_STRING);
            }

            boolean loadFromFile = false;
            File configFile = null;
            if (ehCacheConfigFile != null) {
                LOGGER.info("Ehcache configuration file at (" + ehCacheConfigFile + ") will try to load.");
                configFile = new File(ehCacheConfigFile);
                if (configFile.exists() && configFile.canRead()) {
                    loadFromFile = true;
                } else {
                    LOGGER.warning("Failed to load Ehcache configuration file. Unable to read file.");
                }
            } else {
                LOGGER.info("Ehcache configuration file is not specified.");
            }

            Object configuration = null;

            boolean configurationInitialized = false;
            if (loadFromFile) {
                try {
                    configuration = terracottaToolkitClassLoader.configurationFactoryParseConfiguration(configFile);
                    Object tcc = terracottaToolkitClassLoader.configurationGetTerracottaConfiguration(configuration);

                    if (null == tcc) {
                        tcc = terracottaToolkitClassLoader.newTerracottaClientConfiguration();
                        terracottaToolkitClassLoader.terracottaClientConfigurationSetRejoin(tcc, true);
                        terracottaToolkitClassLoader.terracottaClientConfigurationSetUrl(tcc, remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS));

                        terracottaToolkitClassLoader.configurationAddTerracottaConfigMethod(configuration, tcc);
                    } else {
                        terracottaToolkitClassLoader.terracottaClientConfigurationSetUrl(tcc, remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS));
                    }

                    configurationInitialized = true;
                    LOGGER.info("Ehcache configuration successfully loaded from file.");
                } catch (Exception e) {
                    LOGGER.warning("Failed to load Ehcache configuration file. Falling back to programmatic configuration.");
                }
            }

            if (!configurationInitialized) {
                LOGGER.info("Creating Ehcache configuration programmatically.");
                Object tobc = createTimeoutBehaviorConfiguration();
                Object nc = createNonstopConfiguration(remoteCacheEntity, tobc);
                Object tcc = createTerracottaClientConfiguration(remoteCacheEntity);
                Object tc = createTerracottaConfiguration(nc);
                Object spc = createSizeOfPolicyConfiguration();
                Object cacheConfiguration = createCacheConfiguration(remoteCacheEntity, spc, tc);
                configuration = createConfiguration(remoteCacheEntity, cacheConfiguration, tcc);
            }

            String connectionTimeout = null;
            try {
                connectionTimeout = propertyManager.getProperty(TerracottaRemoteCache.CONNECTION_TIMEOUT_CLUSTER_WIDE_PROPERTY);
            } catch (FindException e) {
                //Do nothing, as it will be set to the default value for the connection timeout on Terracotta
            }

            if (StringUtils.isBlank(connectionTimeout)) {
                LOGGER.log(Level.INFO, "There is no Terracotta connection timeout set, using default of 5 seconds");
                connectionTimeout = TerracottaRemoteCache.DEFAULT_CONNECTION_TIMEOUT;
            }

            System.setProperty("com.tc.tc.config.total.timeout", connectionTimeout);

            try {
                cacheManager = terracottaToolkitClassLoader.newCacheManager(configuration);
                cache = terracottaToolkitClassLoader.cacheManagerGetCache(cacheManager, remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_CACHE_NAME));
            } catch (IllegalStateException e) {
                LOGGER.log(Level.INFO, "Error retrieving Terracotta cache which is in illegal state: " + ExceptionUtils.getMessage(e));
                LOGGER.log(Level.FINE, "Error retrieving Terracotta cache which is in illegal state: ", e);
            } catch (ClassCastException e) {
                LOGGER.log(Level.INFO, "Error retrieving Terracotta cache, cast exception: ", ExceptionUtils.getMessage(e));
                LOGGER.log(Level.FINE, "Error retrieving Terracotta cache which is in illegal state: ", e);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error retrieving Terracotta cache, exception: ", ExceptionUtils.getMessage(e));
                LOGGER.log(Level.FINE, "Error retrieving Terracotta cache: ", e);
            }
        }
    }

    private Object createTimeoutBehaviorConfiguration() throws Exception {
        Object tobc = terracottaToolkitClassLoader.newTimeoutBehaviourConfiguration();
        terracottaToolkitClassLoader.timeoutBehaviourConfigurationSetType(tobc, terracottaToolkitClassLoader.getTimeoutBehaviorConfiguration_EXCEPTION_TYPE_NAME());

        return tobc;
    }

    private Object createNonstopConfiguration(RemoteCacheEntity remoteCacheEntity, Object tobc) throws Exception {
        Object nc = terracottaToolkitClassLoader.newNonstopConfiguration();
        terracottaToolkitClassLoader.nonstopConfigurationSetEnabled(nc, true);
        terracottaToolkitClassLoader.nonstopConfigurationSetTimeoutMillis(nc, remoteCacheEntity.getTimeout() * 1000);
        terracottaToolkitClassLoader.nonstopConfigurationAddTimeoutBehavior(nc, tobc);

        return nc;
    }

    private Object createTerracottaClientConfiguration(RemoteCacheEntity remoteCacheEntity) throws Exception {
        Object tcc = terracottaToolkitClassLoader.newTerracottaClientConfiguration();
        terracottaToolkitClassLoader.terracottaClientConfigurationSetRejoin(tcc, true);
        terracottaToolkitClassLoader.terracottaClientConfigurationSetUrl(tcc, remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS));

        return tcc;
    }

    private Object createTerracottaConfiguration(Object nc) throws Exception {
        Object tc = terracottaToolkitClassLoader.newTerracottaConfiguration();
        //We always want to force to be set as a cluster, because we do not want SSG to alone act as a Terracotta Server.
        //This will just cause more pain for SSG.
        terracottaToolkitClassLoader.terracottaConfigurationSetClustered(tc, true);
        terracottaToolkitClassLoader.terracottaConfigurationSetLocalCacheEnabled(tc, true);
        terracottaToolkitClassLoader.terracottaConfigurationAddNonstop(tc, nc);

        String orphanEvictionString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_ORPHAN_EVICTION_PROPERTY_STRING);
        if (StringUtils.isNotBlank(orphanEvictionString)) {
            terracottaToolkitClassLoader.terracottaConfigurationSetOrphanEviction(tc, Boolean.parseBoolean(orphanEvictionString));
        } else {
            terracottaToolkitClassLoader.terracottaConfigurationSetOrphanEviction(tc, true);
        }

        String orphanEvictionPeriodString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_ORPHAN_EVICTION_PERIOD_PROPERTY_STRING);
        if (StringUtils.isNotBlank(orphanEvictionPeriodString)) {
            terracottaToolkitClassLoader.terracottaConfigurationSetOrphanEvictionPeriod(tc, Integer.parseInt(orphanEvictionPeriodString));
        } else {
            terracottaToolkitClassLoader.terracottaConfigurationSetOrphanEvictionPeriod(tc, terracottaToolkitClassLoader.getTerracottaConfiguration_DEFAULT_ORPHAN_EVICTION_PERIOD());
        }

        return tc;
    }

    private Object createSizeOfPolicyConfiguration() throws Exception {
        Object spc = terracottaToolkitClassLoader.newSizeOfPolicyConfiguration();

        String sizeOfPolicyMaxDepth = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_SIZEOFMAXDEPTH_PROPERTY_STRING);
        if (StringUtils.isNotBlank(sizeOfPolicyMaxDepth)) {
            terracottaToolkitClassLoader.sizeOfPolicyConfigurationSetMaxDepth(spc, Integer.parseInt(sizeOfPolicyMaxDepth));
        } else {
            terracottaToolkitClassLoader.sizeOfPolicyConfigurationSetMaxDepth(spc, terracottaToolkitClassLoader.getSizeOfPolicyConfiguration_DEFAULT_MAX_SIZEOF_DEPTH());
        }

        String sizeOfPolicyMaxDepthExceededBehaviorString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_SIZEOFMAXDEPTHEXCEEDEDBEHAVIOR_PROPERTY_STRING);
        if (StringUtils.isNotBlank(sizeOfPolicyMaxDepthExceededBehaviorString)) {
            terracottaToolkitClassLoader.sizeOfPolicyConfigurationSetMaxDepthExceededBehavior(spc, terracottaToolkitClassLoader.getMaxDepthExceededBehaviourValue(sizeOfPolicyMaxDepthExceededBehaviorString).toString());
        } else {
            terracottaToolkitClassLoader.sizeOfPolicyConfigurationSetMaxDepthExceededBehavior(spc, terracottaToolkitClassLoader.getSizeOfPolicyConfiguration_DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR());
        }

        return spc;
    }

    private Object createCacheConfiguration(RemoteCacheEntity remoteCacheEntity, Object spc, Object tc) throws Exception {
        Object cacheConfiguration = terracottaToolkitClassLoader.newCacheConfiguration();

        terracottaToolkitClassLoader.cacheConfigurationEternal(cacheConfiguration, false);
        terracottaToolkitClassLoader.cacheConfigurationSetName(cacheConfiguration, remoteCacheEntity.getProperties().get(TerracottaRemoteCache.PROPERTY_CACHE_NAME));
        terracottaToolkitClassLoader.cacheConfigurationSetOverflowToOffHeap(cacheConfiguration, true);//We want to keep default to true..so that Terracotta server gets the excess cache.

        String memoryEvictionPolicyString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_EVICTION_POLICY_PROPERTY_STRING);
        Object memoryStoreEvictionPolicy;
        if (memoryEvictionPolicyString != null) {
            memoryStoreEvictionPolicy = terracottaToolkitClassLoader.getMemoryStoreEvictionPolicyValue(memoryEvictionPolicyString);
            if (null == memoryStoreEvictionPolicy) {
                throw new RuntimeException("The Memory Store Eviction Policy set is not of known type.");
            }
        } else {
            //The default that we ship with SSG is LRU.
            memoryStoreEvictionPolicy = terracottaToolkitClassLoader.getMemoryStoreEvictionPolicyValue("LRU");
        }
        terracottaToolkitClassLoader.cacheConfigurationSetMemoryStoreEvictionPolicyFromObject(cacheConfiguration, memoryStoreEvictionPolicy);

        String defaultTimeToLiveSeconds = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_DEFAULT_TIME_TO_LIVE_SECONDS_PROPERTY_STRING);
        if (StringUtils.isNotBlank(defaultTimeToLiveSeconds)) {
            terracottaToolkitClassLoader.cacheConfigurationSetTimeToLiveSeconds(cacheConfiguration, Integer.parseInt(defaultTimeToLiveSeconds));
        } else {
            terracottaToolkitClassLoader.cacheConfigurationSetTimeToLiveSeconds(cacheConfiguration, 0);
        }

        String defaultTimeToIdle = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_DEFAULT_TIME_TO_IDLE_PROPERTY_STRING);
        if (StringUtils.isNotBlank(defaultTimeToIdle)) {
            terracottaToolkitClassLoader.cacheConfigurationSetTimeToIdleSeconds(cacheConfiguration, Integer.parseInt(defaultTimeToIdle));
        } else {
            //The default value should be to not keep anything in
            // memory unless it is specified how long to be kept in memory in SSG policy.
            terracottaToolkitClassLoader.cacheConfigurationSetTimeToIdleSeconds(cacheConfiguration, 0);
        }

        terracottaToolkitClassLoader.cacheConfigurationAddSizeOfPolicy(cacheConfiguration, spc);
        terracottaToolkitClassLoader.cacheConfigurationAddTerracotta(cacheConfiguration, tc);

        return cacheConfiguration;
    }

    private Object createConfiguration(RemoteCacheEntity remoteCacheEntity, Object cacheConfiguration, Object tcc) throws Exception {
        Object configuration = terracottaToolkitClassLoader.newConfiguration();

        long heapMaxSize = Runtime.getRuntime().maxMemory();
        long maxHeapForTerracottaClient = (heapMaxSize * 10) / 100;

        String maxBytesHeapSysPropString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_MAXBYTESHEAP_PROPERTY_STRING);
        if (StringUtils.isNotBlank(maxBytesHeapSysPropString)) {
            terracottaToolkitClassLoader.configurationSetMaxBytesLocalHeap(configuration, maxBytesHeapSysPropString);
        } else {
            // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
            // Any attempt will result in an OutOfMemoryException.
            LOGGER.info("Setting the default heap size for Terracotta to 10% of current JVM heap:" + maxHeapForTerracottaClient + " bytes");
            terracottaToolkitClassLoader.configurationSetMaxBytesLocalHeap(configuration, maxHeapForTerracottaClient);//This needs to be calucated to 10% of xmx or whatever is configured.
        }

        String maxBytesOffHeapSysPropString = getSystemProperty(TerracottaRemoteCache.TERRACOTTA_MAXBYTESOFFHEAP_PROPERTY_STRING);
        if (StringUtils.isNotBlank(maxBytesOffHeapSysPropString)) {
            terracottaToolkitClassLoader.configurationSetMaxBytesLocalOffHeap(configuration, maxBytesHeapSysPropString);
        } else {
            //Get that value, and set it below.
            terracottaToolkitClassLoader.configurationSetMaxBytesLocalOffHeap(configuration, maxHeapForTerracottaClient);//This needs to be greater than 1M
        }

        terracottaToolkitClassLoader.configurationSetName(configuration, remoteCacheEntity.getName());
        terracottaToolkitClassLoader.configurationAddCache(configuration, cacheConfiguration);
        terracottaToolkitClassLoader.configurationAddTerracottaConfigMethod(configuration, tcc);

        return configuration;
    }

    private String getSystemProperty(String key) {
        String value = terracottaToolkitClassLoader.getSystemProperty(key);
        if (value != null) {
            return value;
        } else {
            return System.getProperty(key);
        }
    }

    public CachedMessageData get(String key) throws Exception {
        try {
            //If the terracotta client helper class is not null, then process as normally
            if (cache != null) {
                Object element = terracottaToolkitClassLoader.cacheGet(cache, key);
                if (null == element) {
                    throw new Exception("Cached entry not found for key: " + key);
                }
                Object value = terracottaToolkitClassLoader.elementGetObjectValue(element);
                if (!(value instanceof CachedMessageData)) {
                    throw new Exception("Cached value not found for key: " + key);
                }
                return (CachedMessageData) value;
            } else {
                //If the terracotta client helper is null, then we have an error that occurred
                throw new RuntimeException("Error occurred because the Terracotta client is not initialized.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error occurred during invocation of Terracotta GET from cache: " + ExceptionUtils.getMessage(e));
            LOGGER.log(Level.FINE, "Error occurred during invocation of Terracotta GET from cache: ", e);
            throw e;
        }
    }

    public void set(String key, CachedMessageData value, int expiry) throws Exception {
        try {
            //If the terracotta client helper class is not null, then process as normally
            if (cache != null) {
                Object element = terracottaToolkitClassLoader.newElement(key, value);
                terracottaToolkitClassLoader.elementSetTimeToLive(element, expiry);
                terracottaToolkitClassLoader.cachePut(cache, element);
            } else {
                //If the terracotta client helper is null, then we have an error that occurred
                throw new RuntimeException("Error occurred because the Terracotta client is not initialized.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error occurred during invocation of Terracotta SET to cache: " + ExceptionUtils.getMessage(e));
            LOGGER.log(Level.FINE, "Error occurred during invocation of Terracotta SET to cache: ", e);
            throw e;
        }
    }

    @Override
    public void remove(String key) throws Exception {
        try {
            if (cache != null) {
                terracottaToolkitClassLoader.cacheRemove(cache, key);
            } else {
                //If the terracotta client helper is null, then we have an error that occurred
                throw new RuntimeException("Error occurred because the Terracotta client is not initialized.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error occurred during invocation of Terracotta REMOVE to cache " + ExceptionUtils.getMessage(e));
            LOGGER.log(Level.FINE, "Error occurred during invocation of Terracotta REMOVE to cache ", e);
            throw e;
        }
    }

    public void shutdown() {
        synchronized (TerracottaRemoteCache.class) {
            try {
                if (cache != null) {
                    terracottaToolkitClassLoader.cacheManagerShutdown(cacheManager);
                    cacheManager = null;
                    cache = null;
                } else {
                    //If the terracotta client helper is null, then we have an error that occurred
                    throw new RuntimeException("Error occurred because the Terracotta client is not initialized.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error occurred during invocation of Terracotta shutdown: " + ExceptionUtils.getMessage(e));
                LOGGER.log(Level.FINE, "Error occurred during invocation of Terracotta shutdown: ", e);
            }
        }
    }
}
