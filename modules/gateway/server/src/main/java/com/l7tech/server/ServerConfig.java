package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyListener;
import com.l7tech.util.*;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides cached access to Gateway global configuration items, including (once the
 * database and cluster property manager have initialized) cluster properties.
 *
 * @author alex
 */
public class ServerConfig implements ClusterPropertyListener, Config {

    //- PUBLIC

    /** If testmode property set to true, all properties are considered mutable, not just the ones with .mutable = true. */
    public static final String PROP_TEST_MODE = "com.l7tech.server.serverconfig.testmode";

    public static final long DEFAULT_CACHE_AGE = 30000L;

    public static final String PROPS_PATH_PROPERTY = "com.l7tech.server.serverConfigPropertiesPath";
    public static final String PROPS_RESOURCE_PROPERTY = "com.l7tech.server.serverConfigPropertiesResource";
    public static final String PROPS_PATH_DEFAULT = "/ssg/etc/conf/serverconfig.properties";
    public static final String PROPS_RESOURCE_PATH = "resources/serverconfig.properties";

    public static final String PROPS_OVER_PATH_PROPERTY = "com.l7tech.server.serverConfigOverridePropertiesPath";
    public static final String PROPS_OVER_PATH_DEFAULT =
            System.getProperty("com.l7tech.server.home") == null
            ? "/ssg/etc/conf/serverconfig_override.properties"
            : System.getProperty("com.l7tech.server.home") + File.separator + "etc" + File.separator + "conf" + File.separator + "serverconfig_override.properties";

    private static final String SUFFIX_SYSPROP = ".systemProperty";
    private static final String SUFFIX_GETSYSPROP = ".getSystemProperty";
    private static final String SUFFIX_SETSYSPROP = ".setSystemProperty";
    private static final String SUFFIX_DESC = ".description";
    private static final String SUFFIX_DEFAULT = ".default";
    private static final String SUFFIX_VISIBLE = ".visible";
    private static final String SUFFIX_CLUSTER_KEY = ".clusterProperty";
    private static final String SUFFIX_CLUSTER_AGE = ".clusterPropertyAge";
    private static final int CLUSTER_DEFAULT_AGE = 30000;

    public static ServerConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Register a group of new serverConfig properties dynamically.  The database will be checked for up-to-date
     * values afterward, as long as the cluster property manager is ready.
     *
     * @param newProps  an array of 4-tuples, each of which is
     *                 [serverConfigPropertyName, clusterPropertyName, description, defaultValue].
     */
    public void registerServerConfigProperties(String[][] newProps) {
        long now = System.currentTimeMillis();
        for (String[] tuple : newProps) {
            String propName = tuple[0];
            String clusterPropName = tuple[1];
            String description = tuple[2];
            String defaultValue = tuple[3];

            propLock.writeLock().lock();
            try {
                if (defaultValue != null) _properties.setProperty(propName + SUFFIX_DEFAULT, defaultValue);
                if (description != null) _properties.setProperty(propName + SUFFIX_DESC, description);
                if (clusterPropName != null) _properties.setProperty(propName + SUFFIX_CLUSTER_KEY, clusterPropName);
            } finally {
                propLock.writeLock().unlock();
            }

            String value = getPropertyUncached(propName, true);
            valueCache.put(propName, new CachedValue(value, now));
        }
    }

    public void setPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        propLock.writeLock().lock();
        try {
            if (this.propertyChangeListener != null) throw new IllegalStateException("propertyChangeListener already set!");
            this.propertyChangeListener = propertyChangeListener;
        } finally {
            propLock.writeLock().unlock();
        }
    }

    public void setClusterPropertyCache(final ClusterPropertyCache clusterPropertyCache) {
        propLock.writeLock().lock();
        try {
            if (this.clusterPropertyCache != null) throw new IllegalStateException("clusterPropertyCache already set!");
            this.clusterPropertyCache = clusterPropertyCache;
        } finally {
            propLock.writeLock().unlock();
        }

        prepopulateSystemProperties();
    }

    @Override
    public void clusterPropertyChanged(final ClusterProperty clusterPropertyOld, final ClusterProperty clusterPropertyNew) {
        clusterPropertyEvent(clusterPropertyNew, clusterPropertyOld);
    }

    @Override
    public void clusterPropertyDeleted(final ClusterProperty clusterProperty) {
        clusterPropertyEvent(clusterProperty, clusterProperty);
    }

    /**
     * @return the requested property, possibly with caching at this layer only
     * unless the system property {@link #NO_CACHE_BY_DEFAULT} is true.
     */
    public String getProperty(String propName) {
        return NO_CACHE_BY_DEFAULT_VALUE ? getPropertyUncached(propName) : getPropertyCached(propName);
    }

    /**
     * @return the requested property, possibly with caching at this layer only
     * unless the system property {@link #NO_CACHE_BY_DEFAULT} is true.
     */
    @Override
    public String getProperty(String propName, String emergencyDefault) {
        String value = getProperty(propName);

        if ( value == null ) {
            value = emergencyDefault;            
        }

        return value;
    }

    /**
     * @return the requested property, or a cached value if hte cached value is less than {@link #DEFAULT_CACHE_AGE}
     * millis old.
     */
    public String getPropertyCached(final String propName) {
        return getPropertyCached(propName, DEFAULT_CACHE_AGE);
    }

    /** @return the requested property, or a cached value if the cached value is less than maxAge millis old. */
    public String getPropertyCached(final String propName, final long maxAge) {
        CachedValue cached = valueCache.get(propName);
        final long now = System.currentTimeMillis();
        if (cached != null) {
            final long age = now - cached.when;
            if (age <= maxAge)
                return cached.value;
        }
        String value = getPropertyUncached(propName);
        valueCache.put(propName, new CachedValue(value, now));
        return value;
    }

    /** @return the requested property, with no caching at this layer. */
    public String getPropertyUncached(String propName) {
        return getPropertyUncached(propName, true);
    }

    /** @return the requested property, with no caching at this layer. */
    public String getPropertyUncached(String propName, boolean includeClusterProperties) {
        String sysPropProp = propName + SUFFIX_SYSPROP;
        String getSysPropProp = propName + SUFFIX_GETSYSPROP;
        String setSysPropProp = propName + SUFFIX_SETSYSPROP;
        String dfltProp = propName + SUFFIX_DEFAULT;
        String clusterKeyProp = propName + SUFFIX_CLUSTER_KEY;
        String clusterAgeProp = propName + SUFFIX_CLUSTER_AGE;

        String systemPropertyName = getServerConfigProperty(sysPropProp);
        String isGetSystemProperty = getServerConfigProperty(getSysPropProp);
        String isSetSystemProperty = getServerConfigProperty(setSysPropProp);
        String defaultValue = getServerConfigProperty(dfltProp);
        String clusterKey = getServerConfigProperty(clusterKeyProp);
        String clusterAge = getServerConfigProperty(clusterAgeProp);

        String value = null;

        ClusterPropertyCache clusterPropertyCache;
        propLock.readLock().lock();
        try {
            clusterPropertyCache = this.clusterPropertyCache;
        } finally {
            propLock.readLock().unlock();
        }

        if ( systemPropertyName != null && systemPropertyName.length() > 0 && !"false".equals(isGetSystemProperty) ) {
            logger.finest("Checking System property " + systemPropertyName);
            value = System.getProperty(systemPropertyName);
        }

        if (value == null && includeClusterProperties && clusterKey != null && clusterKey.length() > 0) {
            if (clusterPropertyCache == null) {
                logger.warning("Property '" + propName + "' has a cluster properties key defined, but the ClusterPropertyCache is not yet available");
            } else {
                logger.finest("Checking for cluster property '" + clusterKey + "'");
                try {
                    int age;
                    if (clusterAge != null && clusterAge.length() > 0) {
                        age = Integer.parseInt(clusterAge);
                    } else {
                        age = CLUSTER_DEFAULT_AGE;
                    }

                    ClusterProperty cp = clusterPropertyCache.getCachedEntityByName(clusterKey, age);
                    if (cp == null) {
                        logger.finest("No cluster property named '" + clusterKey + "'");
                    } else {
                        logger.finest("Using cluster property " + clusterKey + " = '" + cp.getValue() + "'");
                        value = cp.getValue();
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Couldn't find cluster property '" + clusterKey + "'", e);
                }
            }
        }

        if ( value == null ) value = getServerConfigProperty( propName );

        if ( value == null ) {
            logger.finest("Using default value " + defaultValue);
            value = defaultValue;
        }

        if (value != null && value.length() >= 2) {
            value = unquote(value);
        }

        if (value != null && "true".equalsIgnoreCase(isSetSystemProperty) && systemPropertyName != null && systemPropertyName.length() > 0) {
            System.setProperty(systemPropertyName, value);
        }

        return value;
    }

    private String unquote(String value) {
        // Remove surrounding quotes
        final int len = value.length();
        final char fc = value.charAt(0);
        final char lc = value.charAt(len -1);
        if ((fc == '"' && lc == '"') || (fc == '\'' && lc == '\'')) {
            value = value.substring(1, len-1);
        }
        return value;
    }

    /**
     * Get the Map of all declared cluster properties.
     *
     * @return The Map of declared property names to descriptions.
     */
    public Map<String, String> getClusterPropertyNames() {
        return getMappedServerConfigPropertyNames(SUFFIX_CLUSTER_KEY, SUFFIX_DESC);
    }

    /**
     * Get the Map of all declared cluster properties.
     *
     * @return The Map of declared property names to default values (NOT CURRENT VALUE).
     */
    public Map<String, String> getClusterPropertyDefaults() {
        return getMappedServerConfigPropertyNames(SUFFIX_CLUSTER_KEY, SUFFIX_DEFAULT);
    }

    /**
     * Get the Map of all declared cluster properties.
     *
     * @return The Map of declared property names to visibility values.
     */
    public Map<String, String> getClusterPropertyVisibilities() {
        return getMappedServerConfigPropertyNames(SUFFIX_CLUSTER_KEY, SUFFIX_VISIBLE);
    }

    /**
     * Get a server config property value by cluster property name.
     *
     * @param clusterPropertyName The cluster property name.
     * @param includeClusterProperties True to include values from cluster properties.
     * @return The value of the related server config property or null.
     */
    public String getPropertyByClusterName( final String clusterPropertyName,
                                            final boolean includeClusterProperties ) {
        String value = null;

        final String propertyName = getNameFromClusterName( clusterPropertyName );
        if ( propertyName != null ) {
            value = getPropertyUncached( propertyName, includeClusterProperties );
        }

        return value;
    }

    /**
     * Get the cluster property name, if any, for the specified ServerConfig property.
     *
     * @param serverConfigPropertyName the ServerConfig property whose cluster property name to look up.
     * @return the cluster property name, or null if there isn't one.
     */
    public String getClusterPropertyName(String serverConfigPropertyName) {
        return getPropertyUncached(serverConfigPropertyName + SUFFIX_CLUSTER_KEY, false);
    }

    public String getNameFromClusterName(String clusterPropertyName) {
        return getServerConfigPropertyName(SUFFIX_CLUSTER_KEY, clusterPropertyName);
    }

    public String getPropertyDescription(String propName) {
        String sysPropDesc = propName + SUFFIX_DESC;
        return getServerConfigProperty(sysPropDesc);
    }

    private String getServerConfigPropertyName(String suffix, String value) {
        String name = null;
        if(suffix!=null && value!=null) {
            propLock.readLock().lock();
            try {
                Set<Map.Entry<Object,Object>> propEntries = _properties.entrySet();
                for (Map.Entry<Object,Object> propEntry : propEntries) {
                    String propKey = (String) propEntry.getKey();
                    String propVal = (String) propEntry.getValue();

                    if (propKey == null || propVal == null) continue;

                    if (propKey.endsWith(suffix) && propVal.equals(value)) {
                        name = propKey.substring(0, propKey.length() - suffix.length());
                        break;
                    }
                }
            } finally {
                propLock.readLock().unlock();
            }
        }
        return name;
    }

    public long getServerBootTime() {
        return _serverBootTime;
    }

    public String getHostname() {
        String hostname;
        propLock.readLock().lock();
        try {
            hostname = _hostname;
        } finally {
            propLock.readLock().unlock();
        }

        if (hostname == null) {
            hostname = getPropertyCached(ServerConfigParams.PARAM_HOSTNAME);

            if (hostname == null) {
                try {
                    hostname = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
                } catch (UnknownHostException e) {
                    try {
                        hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
                    } catch (UnknownHostException e1) {
                        logger.warning("HostName parameter not set and discovery failed.");
                    }
                }
            }

            if (hostname == null) hostname = "UnknownGateway";

            propLock.writeLock().lock();
            try {
                _hostname = hostname;
            } finally {
                propLock.writeLock().unlock();
            }
        }

        return hostname;
    }

    /**
     * Get the attachment disk spooling threshold in bytes.
     * WARNING: This method is a tad slow and is not recommended to be called on the critical path.
     *
     * @return The theshold in bytes above which MIME parts will be spooled to disk.  Always nonnegative.
     */
    public int getAttachmentDiskThreshold() {
        String str = getPropertyCached(ServerConfigParams.PARAM_ATTACHMENT_DISK_THRESHOLD);

        int ret = 0;
        if (str != null && str.length() > 0) {
            try {
                ret = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                // fallthrough
            }
        }

        if (ret < 1) {
            int def = 1048576;
            String errorMsg = "The property " + ServerConfigParams.PARAM_ATTACHMENT_DISK_THRESHOLD + " is undefined or invalid. Please ensure the SecureSpan " +
                    "Gateway is properly configured.  (Will use default of " + def + ")";
            logger.severe(errorMsg);
            return def;
        }

        return ret;
    }

    public File getAttachmentDirectory() {
        return getLocalDirectoryProperty(ServerConfigParams.PARAM_ATTACHMENT_DIRECTORY, true);
    }

    /**
     * Get a configured local directory, ensuring that it exists, is a directory, is readable, and optionally
     * is writable.
     *
     * @param propName    the name of the serverconfig property that is expected to define a directory
     * @param mustBeWritable  if true, the directory will be checked to ensure that it is writable
     * @return a File that points at what (at the time this method returns) is an existing readable directory.
     *         If mustBeWritable, then it will be writable as well.
     */
    public File getLocalDirectoryProperty( final String propName, final boolean mustBeWritable ) {
        File file = getLocalDirectoryProperty( propName, null, mustBeWritable );
        if ( file == null ) {
            throw new IllegalStateException( "Missing file configuration property '"+propName+"'." );
        }
        return file;
    }

    /**
     * Get a configured local directory, ensuring that it exists, is a directory, is readable, and optionally
     * is writable.
     *
     * @param propName    the name of the serverconfig property that is expected to define a directory
     * @param def         the path to use as an emergency default value if the property is not set
     * @param mustBeWritable  if true, the directory will be checked to ensure that it is writable
     * @return a File that points at what (at the time this method returns) is an existing readable directory.
     *         If mustBeWritable, then it will be writable as well.
     */
    public File getLocalDirectoryProperty(String propName, String def, boolean mustBeWritable) {
        String path = getPropertyCached(propName);

        if (path == null || path.length() < 1) {
            String errorMsg = "The property " + propName + " is not defined.  Please ensure that the " +
                    "Gateway is properly configured.  (Will use default of " + def + ")";
            logger.severe(errorMsg);
            return new File(def);
        }

        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();

        if (!dir.exists()) {
            String errorMsg = "The property " + propName + ", defined as the directory " + path +
                    ", is required but could not be found or created.  Please ensure the " +
                    "Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!dir.isDirectory()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not a directory.  Please ensure the " +
                    "Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!dir.canRead()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not readable.  Please ensure the " +
                    "Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (mustBeWritable && !dir.canWrite()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not writable.  Please ensure the " +
                    "Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return dir;
    }

    @Override
    public int getIntProperty(String propName, int emergencyDefault) {
        String strval = getProperty(propName);
        int val;
        try {
            val = Integer.parseInt(strval);
        } catch (NumberFormatException e) {
            logger.warning("Parameter " + propName + " value '" + strval + "' not a valid integer; using " + emergencyDefault + " instead");
            val = emergencyDefault;
        }
        return val;
    }

    /**
     * Get a serverconfig property converted to an int.
     *
     * @param propName the property name. required
     * @param emergencyDefault  the default to use if a value can't be found, or if the value isn't a valid int
     * @param maxAge maximum number of millisconds a value may be cached for
     * @return the requested value (possibly a default)
     */
    public int getIntPropertyCached(String propName, int emergencyDefault, long maxAge) {
        String strval = getPropertyCached(propName, maxAge);
        int val;
        try {
            val = Integer.parseInt(strval);
        } catch (NumberFormatException e) {
            logger.warning("Parameter " + propName + " value '" + strval + "' not a valid integer; using " + emergencyDefault + " instead");
            val = emergencyDefault;
        }
        return val;
    }

    @Override
    public long getLongProperty(String propName, long emergencyDefault) {
        String strval = getProperty(propName);
        long val;
        try {
            val = Long.parseLong(strval);
        } catch (NumberFormatException e) {
            logger.warning("Parameter " + propName + " value '" + strval + "' not a valid long integer; using " + emergencyDefault + " instead");
            val = emergencyDefault;
        }
        return val;
    }

    /**
     * Get a serverconfig property converted to a long.
     *
     * @param propName the property name. required
     * @param emergencyDefault  the default to use if a value can't be found, or if the value isn't a valid long
     * @param maxAge maximum number of millisconds a value may be cached for
     * @return the requested value (possibly a default)
     */
    public long getLongPropertyCached(String propName, long emergencyDefault, long maxAge) {
        String strval = getPropertyCached(propName, maxAge);
        long val;
        try {
            val = Long.parseLong(strval);
        } catch (NumberFormatException e) {
            logger.warning("Parameter " + propName + " value '" + strval + "' not a valid long integer; using " + emergencyDefault + " instead");
            val = emergencyDefault;
        }
        return val;
    }

    @Override
    public boolean getBooleanProperty(String propName, boolean emergencyDefault) {
        String strval = getProperty(propName);
        return strval == null ? emergencyDefault : Boolean.parseBoolean(strval);
    }

    public boolean getBooleanPropertyCached(String propName, boolean emergencyDefault, long maxAge) {
        String strval = getPropertyCached(propName, maxAge);
        return strval == null ? emergencyDefault : Boolean.parseBoolean(strval);
    }

    public long getTimeUnitPropertyCached(String propName, long emergencyDefault, long maxAge) {
        return asTimeUnit( propName, getPropertyCached(propName, maxAge), emergencyDefault);
    }

    @Override
    public long getTimeUnitProperty(String propName, long emergencyDefault) {
        return asTimeUnit( propName, getProperty(propName), emergencyDefault);
    }

    private long asTimeUnit( final String propName, final String strval, long emergencyDefault ) {
        long val;
        if ( strval == null ) {
            val = emergencyDefault;
        } else {
            try {
                val = TimeUnit.parse(strval, TimeUnit.MINUTES);
            } catch (NumberFormatException e) {
                logger.warning("Parameter " + propName + " value '" + strval + "' not a valid timeunit; using " + emergencyDefault + " instead");
                val = emergencyDefault;
            }
        }
        return val;
    }

    /**
     * Check if the specified property's value may be changed directly at runtime by the Gateway code.
     *
     * @param propName the property to check
     * @return true if the specified property is marked as mutable
     */
    boolean isMutable(String propName) {
        propLock.readLock().lock();
        try {
            return SyspropUtil.getBooleanCached(PROP_TEST_MODE, false) || "true".equals(_properties.getProperty(propName + ".mutable"));
        } finally {
            propLock.readLock().unlock();
        }
    }

    /**
     * Change the value of a property on this node only, but only if the property is marked as mutable.
     *
     * @param propName the property to alter. required
     * @param value  the value to set it to
     * @return true if the property was written; false if it was not mutable
     */
    public boolean putProperty(String propName, String value) {
        if (!isMutable(propName)) return false;
        String oldValue;
        propLock.writeLock().lock();
        try {
            oldValue = (String) _properties.setProperty(propName, value);
        } finally {
            propLock.writeLock().unlock();
        }
        valueCache.remove(propName);

        PropertyChangeListener pcl;
        propLock.readLock().lock();
        try {
            pcl = propertyChangeListener;
        } finally {
            propLock.readLock().unlock();
        }

        if (pcl != null) {
            if (oldValue==null || value==null || !oldValue.equals(value)) {
                PropertyChangeEvent pce = new PropertyChangeEvent(this, propName, oldValue, value);
                try {
                    pcl.propertyChange(pce);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception during property change event dispatch.", e);
                }
            }
        }

        return true;
    }

    /**
     * Remove a property on this node only (restoring it to its default value), but only if the property
     * is marked as mutable.
     *
     * @param propName the property to alter.  required
     * @return true if the property was removed; false if it was not mutable
     */
    public boolean removeProperty(String propName) {
        if (!isMutable(propName)) return false;
        propLock.writeLock().lock();
        try {
            _properties.remove(propName);
        } finally {
            propLock.writeLock().unlock();
        }
        valueCache.remove(propName);
        return true;
    }

    @ManagedResource(description="Server config", objectName="l7tech:type=ServerConfig")
    public static class ManagedServerConfig {
        final ServerConfig serverConfig;

        protected ManagedServerConfig( final ServerConfig serverConfig ) {
            this.serverConfig = serverConfig;    
        }

        @ManagedOperation(description="Get Property Value")
        public String getProperty( final String name ) {
            return serverConfig.getProperty( name );            
        }

        @ManagedAttribute(description="Property Names", currencyTimeLimit=30)
        public Set<String> getPropertyNames(){
            Set<String> names;
            serverConfig.propLock.readLock().lock();
            try {
                names = new TreeSet<String>(serverConfig._properties.stringPropertyNames());
            } finally {
                serverConfig.propLock.readLock().unlock();
            }
            return names;
        }

    }

    //- PRIVATE

    private static final String NO_CACHE_BY_DEFAULT = "com.l7tech.server.ServerConfig.suppressCacheByDefault";
    private static final Boolean NO_CACHE_BY_DEFAULT_VALUE = SyspropUtil.getBoolean(NO_CACHE_BY_DEFAULT);

    private static final Map<String,CachedValue> valueCache = new ConcurrentHashMap<String,CachedValue>();

    //
    private final ReadWriteLock propLock = new ReentrantReadWriteLock(false);
    private final Properties _properties;
    private final long _serverBootTime = System.currentTimeMillis();
    private final Logger logger = Logger.getLogger(getClass().getName());

    // 
    private PropertyChangeListener propertyChangeListener;
    private ClusterPropertyCache clusterPropertyCache;
    private String _hostname;

    protected ServerConfig() {
        _properties = new Properties();

        InputStream propStream = null;
        try {
            String configPropertiesPath = SyspropUtil.getString(PROPS_PATH_PROPERTY, PROPS_PATH_DEFAULT);
            File file = new File(configPropertiesPath);
            if (file.exists()) {
                propStream = new FileInputStream(file);
            } else {
                String configPropertiesResource = SyspropUtil.getString(PROPS_RESOURCE_PROPERTY, PROPS_RESOURCE_PATH);
                propStream = ServerConfig.class.getResourceAsStream(configPropertiesResource);
            }

            if (propStream != null) {
                _properties.load(propStream);
            } else {
                logger.severe("Couldn't load serverconfig.properties!");
                throw new RuntimeException("Couldn't load serverconfig.properties!");
            }
        } catch (IOException ioe) {
            logger.severe("Couldn't load serverconfig.properties!");
            throw new RuntimeException("Couldn't load serverconfig.properties!");
        } finally {
            if (propStream != null)
                try {
                    propStream.close();
                    propStream = null;
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Couldn't close properties file", e);
                }
        }

        // Find and process any override properties
        String overridePath = System.getProperty(PROPS_OVER_PATH_PROPERTY);
        if (overridePath == null) overridePath = PROPS_OVER_PATH_DEFAULT;

        try {
            if (overridePath != null) {
                propStream = new FileInputStream(overridePath);
                Properties op = new Properties();
                op.load(propStream);

                Set opKeys = op.keySet();
                for (Object s : opKeys) {
                    _properties.put(s, op.get(s));
                    logger.log(Level.FINE, "Overriding serverconfig property: " + s);
                }
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.INFO, "Couldn't find serverconfig_override.properties; continuing with no overrides");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading serverconfig_override.properties; continuing with no overrides", e);
        } finally {
            ResourceUtils.closeQuietly( propStream );
        }

        // export as system property. This is required so custom assertions
        // do not need to import in the ServerConfig and the clases referred by it
        // (LogManager, TransportProtocol) to read the single property.  - em20040506
        String cfgDirectory = getPropertyCached("ssg.conf");
        if (cfgDirectory !=null) {
            System.setProperty("ssg.config.dir", cfgDirectory);
        } else {
            logger.warning("The server config directory value is empty");
        }
    }
    
    void invalidateCachedProperty(final String propName) {
        valueCache.remove(propName);
    }

    private void prepopulateSystemProperties() {
        Set<String> propKeys;
        propLock.readLock().lock();
        try {
            propKeys = new HashSet<String>( _properties.stringPropertyNames() );
        } finally {
            propLock.readLock().unlock();
        }

        for (String key : propKeys) {
            if (key == null)
                continue;
            if (key.endsWith(SUFFIX_SETSYSPROP)) {
                String prop = key.substring(0, key.length() - SUFFIX_SETSYSPROP.length());
                if (prop.length() < 1)
                    continue;
                getPropertyUncached(prop);
            }
        }
    }

    private void clusterPropertyEvent(final ClusterProperty clusterProperty, final ClusterProperty clusterPropertyOld) {
        String propertyName = getNameFromClusterName(clusterProperty.getName());
        if (propertyName != null) {
            invalidateCachedProperty(propertyName);

            PropertyChangeListener pcl;
            propLock.readLock().lock();
            try {
                pcl = propertyChangeListener;
            } finally {
                propLock.readLock().unlock();                
            }

            if (pcl != null) {
                String oldValue = clusterPropertyOld!=null ?
                        clusterPropertyOld.getValue() :
                        getPropertyUncached(propertyName, false);
                String newValue = getPropertyCached(propertyName);

                if (oldValue==null || newValue==null || !oldValue.equals(newValue)) {
                    PropertyChangeEvent pce = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                    try {
                        pcl.propertyChange(pce);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unexpected exception during property change event dispatch.", e);
                    }
                }
            }
        }
        SyspropUtil.clearCache();
    }

    private Map<String, String> getMappedServerConfigPropertyNames(String keySuffix, String valueSuffix) {
        Map<String, String> keyValueToMappedValue = new TreeMap<String, String>();
        if(keySuffix!=null) {
            propLock.readLock().lock();
            try {
                for (Map.Entry propEntry : _properties.entrySet()) {
                    String propKey = (String) propEntry.getKey();
                    String propVal = (String) propEntry.getValue();

                    if(propKey==null || propVal==null) continue;

                    if(propKey.endsWith(keySuffix)) {
                        keyValueToMappedValue.put(
                                propVal,
                                getServerConfigProperty(propKey.substring(0, propKey.length()-keySuffix.length()) + valueSuffix));
                    }
                }
            } finally {
                propLock.readLock().unlock();
            }
        }
        return keyValueToMappedValue;
    }

    private String getServerConfigProperty(String prop) {
        String val;
        propLock.readLock().lock();
        try {
            val = _properties.getProperty(prop);
        } finally {
            propLock.readLock().unlock();
        }
        if (val == null) return null;
        if (val.length() == 0) return val;

        StringBuilder val2 = new StringBuilder();
        int pos = val.indexOf('$');
        if (pos >= 0) {
            val2.append(val.substring(0, pos));
            while (pos >= 0) {
                if (val.charAt(pos + 1) == '{') {
                    int pos2 = val.indexOf('}', pos + 1);
                    if (pos2 >= 0) {
                        // there's a reference
                        String prop2 = val.substring(pos + 2, pos2);
                        String ref = getProperty(prop2);
                        if (ref == null) {
                            val2.append("${");
                            val2.append(prop2);
                            val2.append("}");
                        } else {
                            val2.append(ref);
                        }

                        pos = val.indexOf('$', pos + 1);
                        if (pos >= 0) {
                            val2.append(val.substring(pos2 + 1, pos));
                        } else {
                            val2.append(val.substring(pos2 + 1));
                        }
                    } else {
                        // there's no terminating }, pass it through literally
                        val2.append(val.substring(pos));
                        break;
                    }
                } else {
                    val2.append(val.substring(pos));
                    break;                    
                }
            }
        } else {
            val2.append(val);
        }
        return val2.toString();

    }

    private static final class InstanceHolder {
        private static final ServerConfig INSTANCE = new ServerConfig();
    }

    private static final class CachedValue {
        private final String value;
        private final long when;
        public CachedValue(String value, long when) {
            this.value = value;
            this.when = when;
        }
    }
}
