package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.assertion.base.util.classloaders.UploadJarClassLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.logging.Logger;

/**
 * Loads classes that are related to the terracotta remote cache.
 */
public class TerracottaToolkitClassLoader extends UploadJarClassLoader {

    private static final Logger logger = Logger.getLogger(TerracottaToolkitClassLoader.class.getName());

    private static final String TERRACOTTA_LICENSE_KEY_PATH_PROP = "com.tc.productkey.path";
    private static final String TERRACOTTA_LICENSE_KEY = "terracotta-license.key";

    private Properties systemProperties;

    private Class configurationFactoryClass;
    private Method configurationFactoryParseConfigurationFileMethod;

    private Class timeoutBehaviorConfigurationClass;
    private Constructor timeoutBehaviourConfigurationConstructor;
    private Method timeoutBehaviourConfigurationSetTypeMethod;

    private Class nonstopConfigurationClass;
    private Constructor nonstopConfigurationConstructor;
    private Method nonstopConfigurationSetEnabledMethod;
    private Method nonstopConfigurationSetTimeoutMillisMethod;
    private Method nonstopConfigurationAddTimeoutBehaviorMethod;

    private Class terracottaClientConfigurationClass;
    private Constructor terracottaClientConfigurationConstructor;
    private Method terracottaClientConfigurationSetRejoinMethod;
    private Method terracottaClientConfigurationSetUrlMethod;

    private Class terracottaConfigurationClass;
    private Constructor terracottaConfigurationConstructor;
    private Method terracottaConfigurationSetClusteredMethod;
    private Method terracottaConfigurationSetLocalCacheEnabledMethod;
    private Method terracottaConfigurationAddNonstopMethod;
    private Method terracottaConfigurationSetOrphanEvictionMethod;
    private Method terracottaConfigurationSetOrphanEvictionPeriodMethod;

    private Class cacheConfigurationClass;
    private Constructor cacheConfigurationConstructor;
    private Method cacheConfigurationEternalMethod;
    private Method cacheConfigurationSetNameMethod;
    private Method cacheConfigurationSetOverflowToOffHeapMethod;
    private Method cacheConfigurationSetMemoryStoreEvictionPolicyFromObjectMethod;
    private Method cacheConfigurationSetTimeToLiveSecondsMethod;
    private Method cacheConfigurationSetTimeToIdleSecondsMethod;
    private Method cacheConfigurationAddSizeOfPolicyMethod;
    private Method cacheConfigurationAddTerracottaMethod;

    private Class configurationClass;
    private Constructor configurationConstructor;
    private Method configurationSetMaxBytesLocalHeapLongMethod;
    private Method configurationSetMaxBytesLocalHeapStringMethod;
    private Method configurationSetMaxBytesLocalOffHeapLongMethod;
    private Method configurationSetMaxBytesLocalOffHeapStringMethod;
    private Method configurationSetNameMethod;
    private Method configurationAddCacheMethod;
    private Method configurationAddTerracottaConfigMethod;
    private Method configurationGetTerracottaConfigurationMethod;

    private Class cacheManagerClass;
    private Constructor cacheManagerConstructor;
    private Method cacheManagerGetCacheMethod;
    private Method cacheManagerShutdownMethod;

    private Class memoryStoreEvictionPolicyEnumClass;
    private HashMap<String, Object> memoryStoreEvictionPolicyEnumValues;

    private Class maxDepthExceededBehaviourClass;
    private HashMap<String, Object> maxDepthExceededBehaviourEnumValues;

    private Class sizeOfPolicyConfigurationClass;
    private Constructor sizeOfPolicyConfigurationConstructor;
    private Method sizeOfPolicyConfigurationSetMaxDepthMethod;
    private Method sizeOfPolicyConfigurationSetMaxDepthExceededBehaviorMethod;

    private Class elementClass;
    private Constructor elementConstructor;
    private Method elementGetObjectValueMethod;
    private Method elementSetTimeToLiveMethod;

    private Class cacheClass;
    private Method cacheGetMethod;
    private Method cachePutMethod;
    private Method cacheRemoveMethod;

    private String timeoutBehaviorConfiguration_EXCEPTION_TYPE_NAME;
    private int terracottaConfiguration_DEFAULT_ORPHAN_EVICTION_PERIOD;
    private int sizeOfPolicyConfiguration_DEFAULT_MAX_SIZEOF_DEPTH;
    private String sizeOfPolicyConfiguration_DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR;

    private static TerracottaToolkitClassLoader INSTANCE;

    /**
     * Retrieves teh instance of the terracotta class loader.
     *
     * @param parent the parent class loader
     * @param ssgHome the SSG home folder
     *
     * @return the instance of the terracotta class loader
     */
    public static TerracottaToolkitClassLoader getInstance(ClassLoader parent, String ssgHome) {
        synchronized (TerracottaToolkitClassLoader.class) {
            if (INSTANCE == null) {
                INSTANCE = new TerracottaToolkitClassLoader(parent, ssgHome);
            }

            return INSTANCE;
        }
    }

    @Override
    protected Permissions getPermissions() {
        Permissions permissions = new Permissions();

        permissions.add(new AllPermission());

        return permissions;
    }

    /**
     * The default constructor for the terracotta class loader.
     *
     * @param parent the parent class loader
     * @param ssgHome the SSG home folder
     */
    private TerracottaToolkitClassLoader(ClassLoader parent, String ssgHome) {
        super(parent, ssgHome);

        System.setProperty(TERRACOTTA_LICENSE_KEY_PATH_PROP, getLicenseKeyPath());

        File systemPropertiesFile = new File(ssgHome + File.separator + "etc" + File.separator + "conf" + File.separator + "system.properties");
        systemProperties = new Properties();
        if(systemPropertiesFile.exists()) {
            try {
                systemProperties.load(new FileInputStream(systemPropertiesFile));
            } catch(Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Retrieves the JAR path to the terracotta JARs.
     *
     * @return the JAR path
     */
    @Override
    public String getJarPath() {
        return ssgHome + File.separator + "var" + File.separator + "lib" + File.separator + "terracotta" + File.separator;
    }

    /**
     * Defines the libraries that are required by terracotta.
     *
     * @return the libraries required to upload
     */
    @Override
    public Map<String, String> getDefinedLibrariesToUpload() {
        Map<String, String> uploadJars = new HashMap<>();
        uploadJars.put("terracotta-toolkit-runtime-ee.jar", "364c2ca9cfcbb31089a25d45c07c0937");
        uploadJars.put("ehcache-ee.jar", "11f7dd8d8f8fc82104b65adcbaef2eac");
        uploadJars.put("terracotta-license.key", "");
        return uploadJars;
    }

    @Override
    protected boolean customLoadingRules(JarEntry entry, ByteArrayOutputStream baos) {

        boolean ruleMatched = false;

//  mkwan TODO: disabled loading third party library classes within the jar file.
//              These libraries are loaded by the classloader of terracotta.
//              Enabling this code block would cause problem with loading particular classes multiple times.
//
//        if (!entry.getName().startsWith("L1/slf4j-api-1.6.6.jar/") && entry.getName().matches("L1/[^/]+\\.jar/.*\\.class_terracotta")) {
//            ruleMatched = true;
//            String entryName = entry.getName();
//            entryName = entryName.substring(entryName.indexOf('/', 3) + 1);
//            String className = entryName.substring(0, entryName.length() - 17).replace('/', '.');
//            availableClassBytes.put(className, baos.toByteArray());
//
//            // Define the package
//            String packageName = className.substring(0, className.lastIndexOf('.'));
//            if (!loadedPackages.containsKey(packageName) && getPackage(packageName) == null) {
//                Package p = definePackage(packageName, null, null, null, null, null, null, null);
//                loadedPackages.put(packageName, p);
//            }
//        } else if (!entry.getName().startsWith("L1/slf4j-api-1.6.6.jar/") && entry.getName().matches("L1/[^/]+\\.jar/.*")) {
//            ruleMatched = true;
//            String entryName = entry.getName();
//            entryName = entryName.substring(entryName.indexOf('/', 3) + 1);
//            availableResourceBytes.put(entryName, baos.toByteArray());
//        } else if (entry.getName().matches("TIMs/[^/]+\\.jar/.*\\.class_terracotta")) {
//            ruleMatched = true;
//            String entryName = entry.getName();
//            entryName = entryName.substring(entryName.indexOf('/', 5) + 1);
//            String className = entryName.substring(0, entryName.length() - 17).replace('/', '.');
//            availableClassBytes.put(className, baos.toByteArray());
//
//            // Define the package
//            String packageName = className.substring(0, className.lastIndexOf('.'));
//            if (!loadedPackages.containsKey(packageName) && getPackage(packageName) == null) {
//                Package p = definePackage(packageName, null, null, null, null, null, null, null);
//                loadedPackages.put(packageName, p);
//            }
//        } else if (entry.getName().matches("TIMs/[^/]+\\.jar/.*")) {
//            ruleMatched = true;
//            String entryName = entry.getName();
//            entryName = entryName.substring(entryName.indexOf('/', 5) + 1);
//            availableResourceBytes.put(entryName, baos.toByteArray());
//        }

        return ruleMatched;
    }

    /**
     * Method which will retrieve the path to the terracotta license key.
     *
     * @return the terracotta license key path
     */
    public String getLicenseKeyPath() {
        return getJarPath() + TERRACOTTA_LICENSE_KEY;
    }


    /**
     * The post library load actions that need to be taken by the terracotta class loader. After the required libraries
     * have been loaded, this method is fired in order to do whatever actions are necessary post library loading.
     *
     * @throws Exception exception on error
     */
    @Override
    protected void postLibraryLoad() throws Exception {

        try {
            configurationFactoryClass = loadClass("net.sf.ehcache.config.ConfigurationFactory");
            configurationFactoryParseConfigurationFileMethod = configurationFactoryClass.getMethod("parseConfiguration", java.io.File.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.ConfigurationFactory");
        }

        try {
            timeoutBehaviorConfigurationClass = loadClass("net.sf.ehcache.config.TimeoutBehaviorConfiguration");
            timeoutBehaviourConfigurationConstructor = timeoutBehaviorConfigurationClass.getConstructor();
            timeoutBehaviourConfigurationSetTypeMethod = timeoutBehaviorConfigurationClass.getMethod("setType", String.class);

            timeoutBehaviorConfiguration_EXCEPTION_TYPE_NAME = (String) timeoutBehaviorConfigurationClass.getField("EXCEPTION_TYPE_NAME").get(timeoutBehaviorConfigurationClass);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.TimeoutBehaviorConfiguration");
        }

        try {
            nonstopConfigurationClass = loadClass("net.sf.ehcache.config.NonstopConfiguration");
            nonstopConfigurationConstructor = nonstopConfigurationClass.getConstructor();
            nonstopConfigurationSetEnabledMethod = nonstopConfigurationClass.getMethod("setEnabled", Boolean.TYPE);
            nonstopConfigurationSetTimeoutMillisMethod = nonstopConfigurationClass.getMethod("setTimeoutMillis", Long.TYPE);
            nonstopConfigurationAddTimeoutBehaviorMethod = nonstopConfigurationClass.getMethod("addTimeoutBehavior", timeoutBehaviorConfigurationClass);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.NonstopConfiguration");
        }

        try {
            terracottaClientConfigurationClass = loadClass("net.sf.ehcache.config.TerracottaClientConfiguration");
            terracottaClientConfigurationConstructor = terracottaClientConfigurationClass.getConstructor();
            terracottaClientConfigurationSetRejoinMethod = terracottaClientConfigurationClass.getMethod("setRejoin", Boolean.TYPE);
            terracottaClientConfigurationSetUrlMethod = terracottaClientConfigurationClass.getMethod("setUrl", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.TerracottaClientConfiguration");
        }

        try {
            terracottaConfigurationClass = loadClass("net.sf.ehcache.config.TerracottaConfiguration");
            terracottaConfigurationConstructor = terracottaConfigurationClass.getConstructor();
            terracottaConfigurationSetClusteredMethod = terracottaConfigurationClass.getMethod("setClustered", Boolean.TYPE);
            terracottaConfigurationSetLocalCacheEnabledMethod = terracottaConfigurationClass.getMethod("setLocalCacheEnabled", Boolean.TYPE);
            terracottaConfigurationAddNonstopMethod = terracottaConfigurationClass.getMethod("addNonstop", nonstopConfigurationClass);
            terracottaConfigurationSetOrphanEvictionMethod = terracottaConfigurationClass.getMethod("setOrphanEviction", Boolean.TYPE);
            terracottaConfigurationSetOrphanEvictionPeriodMethod = terracottaConfigurationClass.getMethod("setOrphanEvictionPeriod", Integer.TYPE);

            terracottaConfiguration_DEFAULT_ORPHAN_EVICTION_PERIOD = (Integer) terracottaConfigurationClass.getField("DEFAULT_ORPHAN_EVICTION_PERIOD").get(terracottaConfigurationClass);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.TerracottaConfiguration");
        }

        try {
            memoryStoreEvictionPolicyEnumClass = loadClass("net.sf.ehcache.store.MemoryStoreEvictionPolicy");
            Field field = memoryStoreEvictionPolicyEnumClass.getDeclaredField("CLOCK");
            getMemoryStoreEvictionPolicyEnumValues().put(field.getName(), field.get(memoryStoreEvictionPolicyEnumClass));
            field = memoryStoreEvictionPolicyEnumClass.getDeclaredField("FIFO");
            getMemoryStoreEvictionPolicyEnumValues().put(field.getName(), field.get(memoryStoreEvictionPolicyEnumClass));
            field = memoryStoreEvictionPolicyEnumClass.getDeclaredField("LFU");
            getMemoryStoreEvictionPolicyEnumValues().put(field.getName(), field.get(memoryStoreEvictionPolicyEnumClass));
            field = memoryStoreEvictionPolicyEnumClass.getDeclaredField("LRU");
            getMemoryStoreEvictionPolicyEnumValues().put(field.getName(), field.get(memoryStoreEvictionPolicyEnumClass));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Unable to load the class net.sf.ehcache.store.MemoryStoreEvictionPolicy");
        }

        try {
            maxDepthExceededBehaviourClass = loadClass("net.sf.ehcache.config.SizeOfPolicyConfiguration$MaxDepthExceededBehavior");
            for (Field field : maxDepthExceededBehaviourClass.getFields()) {
                if (field.isEnumConstant()) {
                    try {
                        getMaxDepthExceededBehaviourEnumValues().put(field.getName(), field.get(maxDepthExceededBehaviourClass));
                    } catch (IllegalAccessException e) {
                        // Ignore
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            logger.warning("Unable to load the class net.sf.ehcache.store.MemoryStoreEvictionPolicy");
        }

        try {
            sizeOfPolicyConfigurationClass = loadClass("net.sf.ehcache.config.SizeOfPolicyConfiguration");
            sizeOfPolicyConfigurationConstructor = sizeOfPolicyConfigurationClass.getConstructor();
            sizeOfPolicyConfigurationSetMaxDepthMethod = sizeOfPolicyConfigurationClass.getMethod("setMaxDepth", Integer.TYPE);
            sizeOfPolicyConfigurationSetMaxDepthExceededBehaviorMethod = sizeOfPolicyConfigurationClass.getMethod("setMaxDepthExceededBehavior", String.class);

            sizeOfPolicyConfiguration_DEFAULT_MAX_SIZEOF_DEPTH = (Integer) sizeOfPolicyConfigurationClass.getField("DEFAULT_MAX_SIZEOF_DEPTH").get(sizeOfPolicyConfigurationClass);
            sizeOfPolicyConfiguration_DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR = sizeOfPolicyConfigurationClass.getField("DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR").get(sizeOfPolicyConfigurationClass).toString();
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.SizeOfPolicyConfiguration");
        }

        try {
            cacheConfigurationClass = loadClass("net.sf.ehcache.config.CacheConfiguration");
            cacheConfigurationConstructor = cacheConfigurationClass.getConstructor();
            cacheConfigurationEternalMethod = cacheConfigurationClass.getMethod("eternal", Boolean.TYPE);
            cacheConfigurationSetNameMethod = cacheConfigurationClass.getMethod("setName", String.class);
            cacheConfigurationSetOverflowToOffHeapMethod = cacheConfigurationClass.getMethod("setOverflowToOffHeap", Boolean.TYPE);
            cacheConfigurationSetMemoryStoreEvictionPolicyFromObjectMethod = cacheConfigurationClass.getMethod("setMemoryStoreEvictionPolicyFromObject", memoryStoreEvictionPolicyEnumClass);
            cacheConfigurationSetTimeToLiveSecondsMethod = cacheConfigurationClass.getMethod("setTimeToLiveSeconds", Long.TYPE);
            cacheConfigurationSetTimeToIdleSecondsMethod = cacheConfigurationClass.getMethod("setTimeToIdleSeconds", Long.TYPE);
            cacheConfigurationAddSizeOfPolicyMethod = cacheConfigurationClass.getMethod("addSizeOfPolicy", sizeOfPolicyConfigurationClass);
            cacheConfigurationAddTerracottaMethod = cacheConfigurationClass.getMethod("addTerracotta", terracottaConfigurationClass);
        } catch (ClassNotFoundException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.CacheConfiguration");
        } catch (NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.CacheConfiguration");
        }

        try {
            configurationClass = loadClass("net.sf.ehcache.config.Configuration");
            configurationConstructor = configurationClass.getConstructor();
            configurationSetMaxBytesLocalHeapLongMethod = configurationClass.getMethod("setMaxBytesLocalHeap", Long.class);
            configurationSetMaxBytesLocalHeapStringMethod = configurationClass.getMethod("setMaxBytesLocalHeap", String.class);
            configurationSetMaxBytesLocalOffHeapLongMethod = configurationClass.getMethod("setMaxBytesLocalOffHeap", Long.class);
            configurationSetMaxBytesLocalOffHeapStringMethod = configurationClass.getMethod("setMaxBytesLocalOffHeap", String.class);
            configurationSetNameMethod = configurationClass.getMethod("setName", String.class);
            configurationAddCacheMethod = configurationClass.getMethod("addCache", cacheConfigurationClass);
            configurationAddTerracottaConfigMethod = configurationClass.getMethod("addTerracottaConfig", terracottaClientConfigurationClass);
            configurationGetTerracottaConfigurationMethod = configurationClass.getMethod("getTerracottaConfiguration");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.config.Configuration");
        }

        try {
            cacheManagerClass = loadClass("net.sf.ehcache.CacheManager");
            cacheManagerConstructor = cacheManagerClass.getConstructor(configurationClass);
            cacheManagerGetCacheMethod = cacheManagerClass.getMethod("getCache", String.class);
            cacheManagerShutdownMethod = cacheManagerClass.getMethod("shutdown");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.CacheManager");
        }

        try {
            elementClass = loadClass("net.sf.ehcache.Element");
            elementConstructor = elementClass.getConstructor(Object.class, Object.class);
            elementGetObjectValueMethod = elementClass.getMethod("getObjectValue");
            elementSetTimeToLiveMethod = elementClass.getMethod("setTimeToLive", Integer.TYPE);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.CacheManager");
        }

        try {
            cacheClass = loadClass("net.sf.ehcache.Cache");
            cacheGetMethod = cacheClass.getMethod("get", Object.class);
            cachePutMethod = cacheClass.getMethod("put", elementClass);
            cacheRemoveMethod = cacheClass.getMethod("remove", Object.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.warning("Unable to load the class net.sf.ehcache.Cache");
        }
    }

    /**
     * Retrieves the package from the loaded packages of this class loader, if not found; then it will relay
     * back to the parent class loader.
     *
     * @param name the name of the package
     * @return the loaded package
     */
    @Override
    protected Package getPackage(String name) {
        if (loadedPackages.containsKey(name)) {
            return loadedPackages.get(name);
        } else {
            return super.getPackage(name);
        }
    }

    private HashMap<String, Object> getMaxDepthExceededBehaviourEnumValues() {
        if (maxDepthExceededBehaviourEnumValues == null) {
            maxDepthExceededBehaviourEnumValues = new HashMap<>();
        }
        return maxDepthExceededBehaviourEnumValues;
    }

    private HashMap<String, Object> getMemoryStoreEvictionPolicyEnumValues() {
        if (memoryStoreEvictionPolicyEnumValues == null) {
            memoryStoreEvictionPolicyEnumValues = new HashMap<>();
        }
        return memoryStoreEvictionPolicyEnumValues;
    }

    public Object configurationFactoryParseConfiguration(File file) throws Exception {
        return configurationFactoryParseConfigurationFileMethod.invoke(configurationFactoryClass, file);
    }

    public Object newTimeoutBehaviourConfiguration() throws Exception {
        return timeoutBehaviourConfigurationConstructor.newInstance();
    }

    public void timeoutBehaviourConfigurationSetType(Object timeoutBehaviour, String type) throws Exception {
        timeoutBehaviourConfigurationSetTypeMethod.invoke(timeoutBehaviour, type);
    }

    public Object newNonstopConfiguration() throws Exception {
        return nonstopConfigurationConstructor.newInstance();
    }

    public void nonstopConfigurationSetEnabled(Object nonstopConfiguration, boolean enabled) throws Exception {
        nonstopConfigurationSetEnabledMethod.invoke(nonstopConfiguration, enabled);
    }

    public void nonstopConfigurationSetTimeoutMillis(Object nonstopConfiguration, long timeoutMillis) throws Exception {
        nonstopConfigurationSetTimeoutMillisMethod.invoke(nonstopConfiguration, timeoutMillis);
    }

    public void nonstopConfigurationAddTimeoutBehavior(Object nonstopConfiguration, Object timeoutBehavior) throws Exception {
        nonstopConfigurationAddTimeoutBehaviorMethod.invoke(nonstopConfiguration, timeoutBehavior);
    }

    public Object newTerracottaClientConfiguration() throws Exception {
        return terracottaClientConfigurationConstructor.newInstance();
    }

    public void terracottaClientConfigurationSetRejoin(Object terracottaClientConfiguration, boolean rejoin) throws Exception {
        terracottaClientConfigurationSetRejoinMethod.invoke(terracottaClientConfiguration, rejoin);
    }

    public void terracottaClientConfigurationSetUrl(Object terracottaClientConfiguration, String url) throws Exception {
        terracottaClientConfigurationSetUrlMethod.invoke(terracottaClientConfiguration, url);
    }

    public Object newTerracottaConfiguration() throws Exception {
        return terracottaConfigurationConstructor.newInstance();
    }

    public void terracottaConfigurationSetClustered(Object terracottaConfiguration, boolean clustered) throws Exception {
        terracottaConfigurationSetClusteredMethod.invoke(terracottaConfiguration, clustered);
    }

    public void terracottaConfigurationSetLocalCacheEnabled(Object terracottaConfiguration, boolean localCacheEnabled) throws Exception {
        terracottaConfigurationSetLocalCacheEnabledMethod.invoke(terracottaConfiguration, localCacheEnabled);
    }

    public void terracottaConfigurationAddNonstop(Object terracottaConfiguration, Object nonstopConfiguration) throws Exception {
        terracottaConfigurationAddNonstopMethod.invoke(terracottaConfiguration, nonstopConfiguration);
    }

    public void terracottaConfigurationSetOrphanEviction(Object terracottaConfiguration, boolean orphanEviction) throws Exception {
        terracottaConfigurationSetOrphanEvictionMethod.invoke(terracottaConfiguration, orphanEviction);
    }

    public void terracottaConfigurationSetOrphanEvictionPeriod(Object terracottaConfiguration, int orphanEvictionPeriod) throws Exception {
        terracottaConfigurationSetOrphanEvictionPeriodMethod.invoke(terracottaConfiguration, orphanEvictionPeriod);
    }

    public Object newCacheConfiguration() throws Exception {
        return cacheConfigurationConstructor.newInstance();
    }

    public void cacheConfigurationEternal(Object cacheConfiguration, boolean eternal) throws Exception {
        cacheConfigurationEternalMethod.invoke(cacheConfiguration, eternal);
    }

    public void cacheConfigurationSetName(Object cacheConfiguration, String name) throws Exception {
        cacheConfigurationSetNameMethod.invoke(cacheConfiguration, name);
    }

    public void cacheConfigurationSetOverflowToOffHeap(Object cacheConfiguration, boolean overflowToOffHeap) throws Exception {
        cacheConfigurationSetOverflowToOffHeapMethod.invoke(cacheConfiguration, overflowToOffHeap);
    }

    public Object getMemoryStoreEvictionPolicyValue(String name) {
        return getMemoryStoreEvictionPolicyEnumValues().get(name);
    }

    public Object getMaxDepthExceededBehaviourValue(String name) {
        return getMaxDepthExceededBehaviourEnumValues().get(name);
    }

    public void cacheConfigurationSetMemoryStoreEvictionPolicyFromObject(Object cacheConfiguration, Object memoryStoreEvictionPolicy) throws Exception {
        cacheConfigurationSetMemoryStoreEvictionPolicyFromObjectMethod.invoke(cacheConfiguration, memoryStoreEvictionPolicy);
    }

    public void cacheConfigurationSetTimeToLiveSeconds(Object cacheConfiguration, long timeToLiveSeconds) throws Exception {
        cacheConfigurationSetTimeToLiveSecondsMethod.invoke(cacheConfiguration, timeToLiveSeconds);
    }

    public void cacheConfigurationSetTimeToIdleSeconds(Object cacheConfiguration, long timeToIdleSeconds) throws Exception {
        cacheConfigurationSetTimeToIdleSecondsMethod.invoke(cacheConfiguration, timeToIdleSeconds);
    }

    public void cacheConfigurationAddSizeOfPolicy(Object cacheConfiguration, Object sizeOfPolicyConfiguration) throws Exception {
        cacheConfigurationAddSizeOfPolicyMethod.invoke(cacheConfiguration, sizeOfPolicyConfiguration);
    }

    public void cacheConfigurationAddTerracotta(Object cacheConfiguration, Object terracottaConfiguration) throws Exception {
        cacheConfigurationAddTerracottaMethod.invoke(cacheConfiguration, terracottaConfiguration);
    }

    public Object newConfiguration() throws Exception {
        return configurationConstructor.newInstance();
    }

    public void configurationSetMaxBytesLocalHeap(Object cacheConfiguration, Long maxBytesOnHeap) throws Exception {
        configurationSetMaxBytesLocalHeapLongMethod.invoke(cacheConfiguration, maxBytesOnHeap);
    }

    public void configurationSetMaxBytesLocalHeap(Object cacheConfiguration, String maxBytesOnHeap) throws Exception {
        configurationSetMaxBytesLocalHeapStringMethod.invoke(cacheConfiguration, maxBytesOnHeap);
    }

    public void configurationSetMaxBytesLocalOffHeap(Object cacheConfiguration, Long maxBytesOffHeap) throws Exception {
        configurationSetMaxBytesLocalOffHeapLongMethod.invoke(cacheConfiguration, maxBytesOffHeap);
    }

    public void configurationSetMaxBytesLocalOffHeap(Object cacheConfiguration, String maxBytesOffHeap) throws Exception {
        configurationSetMaxBytesLocalOffHeapStringMethod.invoke(cacheConfiguration, maxBytesOffHeap);
    }

    public void configurationSetName(Object cacheConfiguration, String name) throws Exception {
        configurationSetNameMethod.invoke(cacheConfiguration, name);
    }

    public void configurationAddCache(Object configuration, Object cacheConfiguration) throws Exception {
        configurationAddCacheMethod.invoke(configuration, cacheConfiguration);
    }

    public void configurationAddTerracottaConfigMethod(Object configuration, Object terracottaConfiguration) throws Exception {
        configurationAddTerracottaConfigMethod.invoke(configuration, terracottaConfiguration);
    }

    public Object configurationGetTerracottaConfiguration(Object configuration) throws Exception {
        return configurationGetTerracottaConfigurationMethod.invoke(configuration);
    }

    public Object newCacheManager(Object configuration) throws Exception {
        System.setProperty(TERRACOTTA_LICENSE_KEY_PATH_PROP, getLicenseKeyPath());
        return cacheManagerConstructor.newInstance(configuration);
    }

    public Object cacheManagerGetCache(Object cacheManager, String name) throws Exception {
        return cacheManagerGetCacheMethod.invoke(cacheManager, name);
    }

    public void cacheManagerShutdown(Object cacheManager) throws Exception {
        cacheManagerShutdownMethod.invoke(cacheManager);
    }

    public Object newSizeOfPolicyConfiguration() throws Exception {
        return sizeOfPolicyConfigurationConstructor.newInstance();
    }

    public void sizeOfPolicyConfigurationSetMaxDepth(Object configuration, int maxDepth) throws Exception {
        sizeOfPolicyConfigurationSetMaxDepthMethod.invoke(configuration, maxDepth);
    }

    public void sizeOfPolicyConfigurationSetMaxDepthExceededBehavior(Object configuration, String maxDepthExceededBehavior) throws Exception {
        sizeOfPolicyConfigurationSetMaxDepthExceededBehaviorMethod.invoke(configuration, maxDepthExceededBehavior);
    }

    public Object newElement(Object key, Object value) throws Exception {
        return elementConstructor.newInstance(key, value);
    }

    public Object elementGetObjectValue(Object configuration) throws Exception {
        return elementGetObjectValueMethod.invoke(configuration);
    }

    public void elementSetTimeToLive(Object element, int timeToLiveSeconds) throws Exception {
        elementSetTimeToLiveMethod.invoke(element, timeToLiveSeconds);
    }

    public Object cacheGet(Object cache, Object key) throws Exception {
        return cacheGetMethod.invoke(cache, key);
    }

    public void cachePut(Object cache, Object element) throws Exception {
        cachePutMethod.invoke(cache, element);
    }

    public boolean cacheRemove(Object cache, Object key) throws InvocationTargetException, IllegalAccessException {
        return (boolean) cacheRemoveMethod.invoke(cache, key);
    }

    public String getTimeoutBehaviorConfiguration_EXCEPTION_TYPE_NAME() {
        return timeoutBehaviorConfiguration_EXCEPTION_TYPE_NAME;
    }

    public int getTerracottaConfiguration_DEFAULT_ORPHAN_EVICTION_PERIOD() {
        return terracottaConfiguration_DEFAULT_ORPHAN_EVICTION_PERIOD;
    }

    public int getSizeOfPolicyConfiguration_DEFAULT_MAX_SIZEOF_DEPTH() {
        return sizeOfPolicyConfiguration_DEFAULT_MAX_SIZEOF_DEPTH;
    }

    public String getSizeOfPolicyConfiguration_DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR() {
        return sizeOfPolicyConfiguration_DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR;
    }

    public String getSystemProperty(String key) {
        return systemProperties.getProperty(key);
    }
}
