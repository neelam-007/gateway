package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.assertion.base.util.classloaders.UploadJarClassLoader;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AllPermission;
import java.security.Permissions;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The coherence class loader, which will upload a few of the required JARs and allow
 * for the use of this third-party library.
 */
public class CoherenceClassLoader extends UploadJarClassLoader {

    private static final Logger logger = Logger.getLogger(CoherenceClassLoader.class.getName());

    private static CoherenceClassLoader INSTANCE;

    private Constructor<?> configurationFactoryConstructor;
    private Method ensureCacheMethod;
    private Method getMethod;
    private Method putMethod;
    private Method releaseMethod;
    private Method removeMethod;

    /**
     * Returns an instance of the coherence class loader.
     *
     * @param parent  the parent class loader
     * @param ssgHome the SSG home folder
     * @return the coherence class loader
     */
    public static CoherenceClassLoader getInstance(ClassLoader parent, String ssgHome) {
        if (INSTANCE == null) {
            INSTANCE = new CoherenceClassLoader(parent, ssgHome);
        }

        return INSTANCE;
    }

    /**
     * Default constructor for the coherence class loader.
     *
     * @param parent  the parent class loader
     * @param ssgHome the SSG home folder
     */
    private CoherenceClassLoader(ClassLoader parent, String ssgHome) {
        super(parent, ssgHome);
    }

    @Override
    protected Permissions getPermissions() {
        // We might want to consider finding out all the permissions needed for running coherence
        Permissions permissions = new Permissions();

        permissions.add(new AllPermission());

        return permissions;
    }

    /**
     * Returns the JAR path to the coherence third party libraries.
     *
     * @return the JAR path to the coherence libraries
     */
    @Override
    public String getJarPath() {
        return ssgHome + File.separator + "var" + File.separator + "lib" + File.separator + "coherence" + File.separator;
    }

    /**
     * Defines the third-party jars that are required for use (third-party libraries).
     *
     * @return the third party libraries that are required
     */
    @Override
    public Map<String, String> getDefinedLibrariesToUpload() {
        Map<String, String> uploadJars = new HashMap<String, String>();
        uploadJars.put("coherence.jar", "f7d69104a79b24559ad8b00f5fb72ffd");
        return uploadJars;
    }

    /**
     * The actions to perform once the initialization of the class loader has happened, this means that the third
     * party libraries have been uploaded and loaded into memory.
     *
     * @throws Exception exception on reflection issues
     */
    @Override
    protected void postLibraryLoad() throws Exception {
        try {
            Class configurationFactoryClass = findClass("com.tangosol.net.DefaultConfigurableCacheFactory");
            configurationFactoryConstructor = configurationFactoryClass.getConstructor(String.class);
            ensureCacheMethod = configurationFactoryClass.getMethod("ensureCache", String.class, ClassLoader.class);

            Class namedCacheClass = findClass("com.tangosol.net.NamedCache");
            getMethod = namedCacheClass.getMethod("get", Object.class);
            putMethod = namedCacheClass.getMethod("put", Object.class, Object.class, long.class);
            removeMethod = namedCacheClass.getMethod("remove", Object.class);
            releaseMethod = namedCacheClass.getMethod("release");

        } catch (ClassNotFoundException e) {
            logger.warning("Unable to load the class com.tangosol.net.DefaultConfigurableCacheFactory");
        } catch (NoSuchMethodException e) {
            logger.warning("Unable to load the class com.tangosol.net.DefaultConfigurableCacheFactory");
        }
    }

    /**
     * The custom loading rules, this means for particular types of files that we perform some custom operations
     * in order to use them appropriately in the class loader. In the coherence classloader we explode the XSD files
     * to the file system in order to do schema validation; other files are relayed to the parent classloader for
     * approrpirate loading behavior.
     *
     * @param entry the entry to load
     * @param baos  the byte array output stream to use
     * @return if the entry was loaded (true), or if it is still left to be loaded (false)
     */
    @Override
    protected boolean customLoadingRules(JarEntry entry, ByteArrayOutputStream baos) {
        boolean ruleMatched = false;

        //This method will load XSD and write them to the file system for use later
        if (entry.getName().endsWith(".xsd")) {
            ruleMatched = true;
            String entryName = entry.getName();
            entryName = entryName.substring(entryName.indexOf('/', 3) + 1);

            FileOutputStream fos = null;

            try {
                File xsdFile = new File(getJarPath() + entryName);
                if (!xsdFile.exists()) {
                    xsdFile.createNewFile();
                }
                fos = new FileOutputStream(xsdFile);
                fos.write(baos.toByteArray());
            } catch (Exception e) {
                logger.log(Level.WARNING, "There was a problem writing the XSD to the file system.", e);
            } finally {
                ResourceUtils.closeQuietly(fos);
            }
        }

        return ruleMatched;
    }

    /**
     * The overridden getResource method will look and see if the required resource is an XSD file; if so it will
     * load the file from the file system (because that is previously where we had written them to); otherwise
     * if it is a different kind of resource then we will just relay the request back to the parent class loader.
     *
     * @param name the resource name
     * @return the URL to the resource
     */
    @Override
    public URL getResource(final String name) {
        if (name.endsWith("xsd")) {
            try {
                return new URL(null, (new File(getJarPath() + name)).toURI().toString(), new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        File xsdFile = new File(getJarPath() + name);
                        FileInputStream fis = null;
                        ByteArrayOutputStream baos = null;

                        try {
                            fis = new FileInputStream(xsdFile);
                            baos = new ByteArrayOutputStream();
                            IOUtils.copyStream(fis, baos);
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Problem opening up stream to read XSD files.");
                        } finally {
                            ResourceUtils.closeQuietly(fis);
                            ResourceUtils.closeQuietly(baos);
                        }

                        return new CustomURLConnection(baos.toByteArray());
                    }
                });
            } catch (MalformedURLException e) {
                return null;
            }
        } else {
            return super.getResource(name);
        }
    }

    public Object newConfigurationFactory(String sName) throws Exception {
        return configurationFactoryConstructor.newInstance(sName);
    }

    public Object ensureCache(Object object, String sCacheName) throws Exception {
        return ensureCacheMethod.invoke(object, sCacheName, this);
    }

    public Object putCache(Object object, String key, CachedMessageData data, long expiry) throws Exception {
        return putMethod.invoke(object, key, data, expiry);
    }

    public void release(Object object) {
        try {
            releaseMethod.invoke(object);
        } catch (Exception e) {
            logger.log(Level.WARNING, "There was a problem with release the connection to the coherence cache.");
        }
    }

    public Object getCache(Object object, String key) throws Exception {
        return getMethod.invoke(object, key);
    }

    public Object removeFromCache(Object cache, String key) throws InvocationTargetException, IllegalAccessException {
        return removeMethod.invoke(cache, key);
    }
}