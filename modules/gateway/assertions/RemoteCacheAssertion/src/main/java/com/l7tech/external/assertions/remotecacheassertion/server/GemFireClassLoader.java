package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.assertion.base.util.classloaders.UploadJarClassLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.logging.Logger;

/**
 * Loads classes that are related to the gem fire remote cache.
 */
public class GemFireClassLoader extends UploadJarClassLoader {

    private static final Logger logger = Logger.getLogger(GemFireClassLoader.class.getName());

    private static GemFireClassLoader INSTANCE;

    // Region.Entry
    private Method getEntryValueMethod;
    private Method isEntryDestroyedMethod;

    // Region
    private Method isDetroyedMethod;
    private Method putMethod;
    private Method getMethod;
    private Method getEntryMethod;
    private Method removeMethod;
    private Method getRegionMethod;
    private Method closeMethod;

    private Constructor clientCacheFactoryConstructor;
    private Method setMethod;
    private Method createMethod;
    private Method setPoolSubscriptionEnabledMethod;
    private Method addPoolLocatorMethod;
    private Method addPoolServerMethod;

    /**
     * Retrieves teh instance of the gem fire class loader.
     *
     * @param parent the parent class loader
     * @param ssgHome the SSG home folder
     *
     * @return the instance of the gem fire class loader
     */
    public static GemFireClassLoader getInstance(ClassLoader parent, String ssgHome) {
        if(INSTANCE == null) {
            INSTANCE = new GemFireClassLoader(parent, ssgHome);
        }

        return INSTANCE;
    }

    /**
     * The default constructor for the gem fire class loader.
     *
     * @param parent the parent class loader
     * @param ssgHome the SSG home folder
     */
    private GemFireClassLoader(ClassLoader parent, String ssgHome) {
        super(parent, ssgHome);
    }

    @Override
    protected Permissions getPermissions() {
        Permissions permissions = new Permissions();

        permissions.add(new AllPermission());

        return permissions;
    }

    /**
     * Retrieves the JAR path to the gem fire JARs.
     *
     * @return the JAR path
     */
    @Override
    public String getJarPath() {
        return ssgHome + File.separator + "var" + File.separator + "lib" + File.separator + "gemfire" + File.separator;
    }

    /**
     * Defines the libraries that are required by gem fire.
     *
     * @return the libraries required to upload
     */
    @Override
    public Map<String, String> getDefinedLibrariesToUpload() {
        Map<String, String> uploadJars = new HashMap<String, String>();
        uploadJars.put("gemfire.jar", "73247ab8b24f20fcfb6306c1e2cb1ace");
        return uploadJars;
    }

    @Override
    protected boolean customLoadingRules(JarEntry entry, ByteArrayOutputStream baos) {
        //There are no custom loading rules for the gem fire class loading process
        return false;
    }


    /**
     * The post library load actions that need to be taken by the gem fire class loader. After the required libraries
     * have been loaded, this method is fired in order to do whatever actions are necessary post library loading.
     *
     * @throws Exception exception on error
     */
    @Override
    protected void postLibraryLoad() throws Exception {
        try {
            Class regionEntryClass = findClass("com.gemstone.gemfire.cache.Region$Entry");
            getEntryValueMethod = regionEntryClass.getMethod("getValue");
            isEntryDestroyedMethod = regionEntryClass.getMethod("isDestroyed");

            Class regionClass = findClass("com.gemstone.gemfire.cache.Region");
            isDetroyedMethod = regionClass.getMethod("isDestroyed");
            putMethod = regionClass.getMethod("put", Object.class, Object.class);
            getMethod = regionClass.getMethod("get", Object.class);
            getEntryMethod = regionClass.getMethod("getEntry", Object.class);
            removeMethod = regionClass.getMethod("remove", Object.class);

            Class clientCacheClass = findClass("com.gemstone.gemfire.cache.client.ClientCache");
            getRegionMethod = clientCacheClass.getMethod("getRegion", String.class);
            closeMethod = clientCacheClass.getMethod("close");

            Class clientCacheFactory = findClass("com.gemstone.gemfire.cache.client.ClientCacheFactory");
            clientCacheFactoryConstructor = clientCacheFactory.getConstructor();
            setMethod = clientCacheFactory.getMethod("set", String.class, String.class);
            setPoolSubscriptionEnabledMethod = clientCacheFactory.getMethod("setPoolSubscriptionEnabled", boolean.class);
            createMethod = clientCacheFactory.getMethod("create");
            addPoolLocatorMethod = clientCacheFactory.getMethod("addPoolLocator", String.class, int.class);
            addPoolServerMethod = clientCacheFactory.getMethod("addPoolServer", String.class, int.class);

        } catch(ClassNotFoundException e) {
            logger.warning("Unable to load the class com.tangosol.net.DefaultConfigurableCacheFactory");
        } catch(NoSuchMethodException e) {
            logger.warning("Unable to load the class com.tangosol.net.DefaultConfigurableCacheFactory");
        }
    }

    public Object createClientCacheFactory() throws Exception {
        return clientCacheFactoryConstructor.newInstance();
    }

    public void set(Object clientCacheFactory, String key, String value) throws Exception {
        setMethod.invoke(clientCacheFactory, key, value);
    }

    public void setPoolSubscriptionEnabled(Object clientCacheFactory, boolean flag) throws Exception {
        setPoolSubscriptionEnabledMethod.invoke(clientCacheFactory, flag);
    }

    public Object createClientCache(Object clientCacheFactory) throws Exception {
        return createMethod.invoke(clientCacheFactory);
    }

    public Object getRegion(Object clientCache, String cacheName) throws Exception {
        return getRegionMethod.invoke(clientCache, cacheName);
    }

    public void addPoolLocator(Object clientCacheFactory, String address, int i) throws Exception {
        addPoolLocatorMethod.invoke(clientCacheFactory, address, i);
    }

    public void addPoolServer(Object clientCacheFactory, String address, int i) throws Exception {
        addPoolServerMethod.invoke(clientCacheFactory, address, i);
    }

    public boolean isDestroyed(Object region) throws Exception {
        return ((Boolean)isDetroyedMethod.invoke(region)).booleanValue();
    }

    public void put(Object region, String key, Object value) throws Exception {
        putMethod.invoke(region, key, value);
    }

    public Object getEntry(Object region, String key) throws Exception {
        return getEntryMethod.invoke(region, key);
    }

    public Object get(Object region, String key) throws Exception {
        return getMethod.invoke(region, key);
    }

    public void remove(Object region, String key) throws Exception {
        removeMethod.invoke(region, key);
    }

    public boolean isEntryDestroyed(Object entry) throws Exception {
        return ((Boolean) isEntryDestroyedMethod.invoke(entry));

    }

    public Object getEntryValue(Object entry) throws Exception {
        return getEntryValueMethod.invoke(entry);
    }

    public void close(Object clientCache) throws Exception {
        closeMethod.invoke(clientCache);
    }
}
