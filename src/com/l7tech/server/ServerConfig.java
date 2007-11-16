/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterPropertyCache;
import com.l7tech.cluster.ClusterPropertyListener;
import com.l7tech.common.util.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
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
public class ServerConfig implements ClusterPropertyListener {

    //- PUBLIC

    public static final long DEFAULT_CACHE_AGE = 30000;

    public static final String PARAM_SERVER_ID = "serverId";
    public static final String PARAM_PARTITION_NAME = "partitionName";
    public static final String PARAM_KEYSTORE = "keystorePropertiesPath";
    public static final String PARAM_LDAP_TEMPLATES = "ldapTemplatesPath";
    public static final String PARAM_UDDI_TEMPLATES = "uddiTemplatesPath";
    public static final String PARAM_HIBERNATE = "hibernatePropertiesPath";
    public static final String PARAM_SERVERXML = "serverXmlPath";
    public static final String PARAM_IPS = "ipAddresses";
    public static final String PARAM_HOSTNAME = "hostname";
    public static final String PARAM_HTTPPORT = "httpPort";
    public static final String PARAM_HTTPSPORT = "httpsPort";
    public static final String PARAM_SYSTEMPROPS = "systemPropertiesPath";
    public static final String PARAM_SERVERCOMPONENTS = "serverComponents";
    public static final String PARAM_JMS_THREAD_POOL_SIZE = "jmsThreadPoolSize";
    public static final String PARAM_MULTICAST_ADDRESS = "multicastAddress";
    public static final String PARAM_SSG_HOME_DIRECTORY = "ssgHome";
    public static final String PARAM_CONFIG_DIRECTORY = "configDirectory";
    public static final String PARAM_ATTACHMENT_DIRECTORY = "attachmentDirectory";
    public static final String PARAM_ATTACHMENT_DISK_THRESHOLD = "attachmentDiskThreshold";
    public static final String PARAM_MODULAR_ASSERTIONS_DIRECTORY = "modularAssertionsDirectory";
    public static final String PARAM_MODULAR_ASSERTIONS_RESCAN_MILLIS = "modularAssertionsRescanMillis";
    public static final String PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS = "modularAssertionsFileExtensions";

    public static final String PARAM_AUDIT_MESSAGE_THRESHOLD = "auditMessageThreshold";
    public static final String PARAM_AUDIT_ADMIN_THRESHOLD = "auditAdminThreshold";
    public static final String PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD = "auditClientSystemLogsThreshold";
    public static final String PARAM_AUDIT_PURGE_MINIMUM_AGE = "auditPurgeMinimumAge";

    public static final String PARAM_AUDIT_REFRESH_PERIOD_SECS = "auditViewerRefreshSeconds";
    public static final String PARAM_AUDIT_LOG_REFRESH_PERIOD_SECS = "auditLogViewerRefreshSeconds";

    public static final String PARAM_AUDIT_HINTING_ENABLED = "auditHintingEnabled";
    public static final String PARAM_AUDIT_ASSERTION_STATUS_ENABLED = "auditAssertionStatusEnabled";

    public static final String PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD = "auditAssociatedLogsThreshold";
    public static final String PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD = "auditAssociatedLogsThresholdRespected";

    public static final String CONFIG_AUDIT_SIGN_CLUSTER = "audit.signing";

    public static final String PARAM_ANTIVIRUS_ENABLED = "savseEnable";
    public static final String PARAM_ANTIVIRUS_HOST = "savseHost";
    public static final String PARAM_ANTIVIRUS_PORT = "savsePort";

    public static final String PARAM_METRICS_FINE_INTERVAL = "metricsFineInterval";

    public static final String PARAM_IO_FRONT_BLOCKED_READ_TIMEOUT = "ioInReadTimeout";
    public static final String PARAM_IO_FRONT_SLOW_READ_THRESHOLD = "ioInSlowReadThreshold";
    public static final String PARAM_IO_FRONT_SLOW_READ_RATE = "ioInSlowReadRate";
    public static final String PARAM_IO_BACK_CONNECTION_TIMEOUT = "ioOutConnectionTimeout";
    public static final String PARAM_IO_BACK_READ_TIMEOUT = "ioOutReadTimeout";
    public static final String PARAM_IO_BACK_HTTPS_HOST_CHECK = "ioHttpsHostVerify";
    public static final String PARAM_IO_STALE_CHECK_PER_INTERVAL = "ioStaleCheckCount";
    public static final String PARAM_IO_STALE_MAX_HOSTS = "ioStaleCheckHosts";
    public static final String PARAM_IO_XML_PART_MAX_BYTES = "ioXmlPartMaxBytes";
    public static final String PARAM_SIGNED_PART_MAX_BYTES = "ioAttachmentSignedMaxBytes";

    public static final String PARAM_XSLT_CACHE_MAX_ENTRIES = "xsltMaxCacheEntries";
    public static final String PARAM_XSLT_CACHE_MAX_AGE = "xsltMaxCacheAge";

    public static final String PARAM_SCHEMA_CACHE_MAX_ENTRIES = "schemaMaxCacheEntries";
    public static final String PARAM_SCHEMA_CACHE_MAX_AGE = "schemaMaxCacheAge";

    public static final String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY = "schemaRecompileLatency";
    public static final String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE = "schemaRecompileMinAge";
    public static final String PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE = "schemaRecompileMaxAge";

    public static final String PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES = "ephemeralKeyMaxCacheEntries";
    public static final String PARAM_PRIVATE_KEY_CACHE_MAX_AGE = "privateKeyCacheMaxAgeMillis";

    public static final String PARAM_SCHEMA_SOFTWARE_FALLBACK = "schemaSoftwareFallback";

    public static final String PARAM_KEYSTORE_SEARCH_FOR_ALIAS = "keyStoreSearchForAlias";

    public static final String PARAM_CERT_EXPIRY_CHECK_PERIOD = "trustedCert.expiryCheckPeriod";
    public static final String PARAM_CERT_EXPIRY_FINE_AGE = "trustedCert.expiryFineAge";
    public static final String PARAM_CERT_EXPIRY_INFO_AGE = "trustedCert.expiryInfoAge";
    public static final String PARAM_CERT_EXPIRY_WARNING_AGE = "trustedCert.expiryWarningAge";

    public static final String PARAM_CLUSTER_PORT = "clusterPort";

    public static final String PARAM_IO_HTTP_POOL_MAX_CONCURRENCY = "ioHttpPoolMaxConcurrency";
    public static final String PARAM_IO_HTTP_POOL_MAX_IDLE_TIME = "ioHttpPoolMaxIdleTime";
    public static final String PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS = "ioHttpPoolMinSpareThreads";

    public static final String PARAM_POLICY_VALIDATION_MAX_CONCURRENCY = "serverPolicyValidation.maxConcurrency";

    public static final String MAX_LDAP_SEARCH_RESULT_SIZE = "maxLdapSearchResultSize";

    public static final int DEFAULT_JMS_THREAD_POOL_SIZE = 200;

    public static final String PROPS_PATH_PROPERTY = "com.l7tech.server.serverConfigPropertiesPath";
    public static final String PROPS_PATH_DEFAULT = "/ssg/etc/conf/serverconfig.properties";
    public static final String PROPS_RESOURCE_PATH = "serverconfig.properties";

    public static final String PROPS_OVER_PATH_PROPERTY = "com.l7tech.server.serverConfigOverridePropertiesPath";
    public static final String PROPS_OVER_PATH_DEFAULT =
            System.getProperty("com.l7tech.server.home") == null
            ? "/ssg/etc/conf/serverconfig_override.properties"
            : System.getProperty("com.l7tech.server.home") + File.separator + "etc" + File.separator + "conf" + File.separator + "serverconfig_override.properties";

    private static final String SUFFIX_JNDI = ".jndi";
    private static final String SUFFIX_SYSPROP = ".systemProperty";
    private static final String SUFFIX_SETSYSPROP = ".setSystemProperty";
    private static final String SUFFIX_DESC = ".description";
    private static final String SUFFIX_DEFAULT = ".default";
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

            if (defaultValue != null) _properties.put(propName + SUFFIX_DEFAULT, defaultValue);
            if (description != null) _properties.put(propName + SUFFIX_DESC, description);
            if (clusterPropName != null) _properties.put(propName + SUFFIX_CLUSTER_KEY, clusterPropName);

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
    }

    public void clusterPropertyChanged(final ClusterProperty clusterPropertyOld, final ClusterProperty clusterPropertyNew) {
        clusterPropertyEvent(clusterPropertyNew, clusterPropertyOld);
    }

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
     * @return the requested property, or a cached value if hte cached value is less than {@link #DEFAULT_CACHE_AGE}
     * millis old.
     */
    public String getPropertyCached(final String propName) {
        return getPropertyCached(propName, DEFAULT_CACHE_AGE);
    }

    /** @return the requested property, or a cached value if hte cached value is less than maxAge millis old. */
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
        String setSysPropProp = propName + SUFFIX_SETSYSPROP;
        String jndiProp = propName + SUFFIX_JNDI;
        String dfltProp = propName + SUFFIX_DEFAULT;
        String clusterKeyProp = propName + SUFFIX_CLUSTER_KEY;
        String clusterAgeProp = propName + SUFFIX_CLUSTER_AGE;

        String systemPropertyName = getServerConfigProperty(sysPropProp);
        String isSetSystemProperty = getServerConfigProperty(setSysPropProp);
        String jndiName = getServerConfigProperty(jndiProp);
        String defaultValue = getServerConfigProperty(dfltProp);
        String clusterKey = getServerConfigProperty(clusterKeyProp);
        String clusterAge = getServerConfigProperty(clusterAgeProp);

        String value = null;

        ClusterPropertyCache clusterPropertyCache;
        InitialContext icontext;
        propLock.readLock().lock();
        try {
            clusterPropertyCache = this.clusterPropertyCache;
            icontext = this._icontext;
        } finally {
            propLock.readLock().unlock();
        }

        if ( systemPropertyName != null && systemPropertyName.length() > 0 ) {
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

        if (value == null && jndiName != null && jndiName.length() > 0 ) {
            try {
                logger.finest("Checking JNDI property " + jndiName);
                if (icontext == null) {
                    icontext = new InitialContext();
                    propLock.writeLock().lock();
                    try {
                        _icontext = icontext;
                    } finally {
                        propLock.writeLock().unlock();
                    }
                }
                value = (String)icontext.lookup(jndiName);
            } catch (NamingException ne) {
                logger.fine(ne.getMessage());
            }
        }

        if ( value == null ) value = getServerConfigProperty( propName );

        if ( value == null ) {
            logger.finest("Using default value " + defaultValue);
            value = defaultValue;
        }

        if (value != null && "true".equalsIgnoreCase(isSetSystemProperty)) {
            System.setProperty(systemPropertyName, value);
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
        }
        return name;
    }

    public int getServerId() {
        int serverId;
        propLock.readLock().lock();
        try{
            serverId = _serverId;
        } finally {
            propLock.readLock().unlock();
        }

        if (serverId == 0) {
            String sid = null;
            try {
                sid = getPropertyCached(PARAM_SERVER_ID);
                if (sid != null && sid.length() > 0)
                    serverId = Byte.parseByte(sid);
            } catch (NumberFormatException nfe) {
                logger.log(Level.WARNING, "Invalid ServerID value '" + sid + "'", nfe);
            }

            if (serverId == 0) {
                try {
                    InetAddress localhost = InetAddress.getLocalHost();
                    byte[] ip = localhost.getAddress();
                    serverId = ip[3] & 0xff;
                    logger.info("ServerId parameter not set, assigning server ID " + serverId +
                      " from server's IP address");
                } catch (UnknownHostException e) {
                    serverId = 1;
                    logger.severe("Couldn't get server's local host!  Using server ID " + serverId);
                }
            }

            propLock.writeLock().lock();
            try {
                _serverId = serverId;
            } finally {
                propLock.writeLock().unlock();
            }
        }

        return serverId;
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
            hostname = getPropertyCached(PARAM_HOSTNAME);

            if (hostname == null) {
                try {
                    hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    logger.warning("HostName parameter not set and discovery failed.");
                }
            }

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
        String str = getPropertyCached(PARAM_ATTACHMENT_DISK_THRESHOLD);

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
            String errorMsg = "The property " + PARAM_ATTACHMENT_DIRECTORY + " is undefined or invalid. Please ensure the SecureSpan " +
                    "Gateway is properly configured.  (Will use default of " + def + ")";
            logger.severe(errorMsg);
            return def;
        }

        return ret;
    }

    public File getAttachmentDirectory() {
        return getLocalDirectoryProperty(PARAM_ATTACHMENT_DIRECTORY, "/ssg/var/attachments", true);
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
            String errorMsg = "The property " + propName + " is not defined.  Please ensure that the SecureSpan " +
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
                    "SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!dir.isDirectory()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not a directory.  Please ensure the " +
                    "SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!dir.canRead()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not readable.  Please ensure the " +
                    "SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (mustBeWritable && !dir.canWrite()) {
            String errorMsg = "The property " + propName + " defined directory " + path +
                    " which is present but is not writable.  Please ensure the " +
                    "SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return dir;
    }

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

    public boolean getBooleanProperty(String propName, boolean emergencyDefault) {
        String strval = getProperty(propName);
        return strval == null ? emergencyDefault : Boolean.parseBoolean(strval);
    }

    public boolean getBooleanPropertyCached(String propName, boolean emergencyDefault, long maxAge) {
        String strval = getPropertyCached(propName, maxAge);
        return strval == null ? emergencyDefault : Boolean.parseBoolean(strval);
    }

    public long getTimeUnitPropertyCached(String propName, long emergencyDefault, long maxAge) {
        String strval = getPropertyCached(propName, maxAge);
        long val;
        try {
            val = TimeUnit.parse(strval, TimeUnit.MINUTES);
        } catch (NumberFormatException e) {
            logger.warning("Parameter " + propName + " value '" + strval + "' not a valid timeunit; using " + emergencyDefault + " instead");
            val = emergencyDefault;
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
        return "true".equals(_properties.get(propName + ".mutable"));
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
        _properties.put(propName, value);
        valueCache.remove(propName);

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
        _properties.remove(propName);
        valueCache.remove(propName);
        return true;
    }

    //- PRIVATE

    private static final String NO_CACHE_BY_DEFAULT = "com.l7tech.server.ServerConfig.suppressCacheByDefault";
    private static final Boolean NO_CACHE_BY_DEFAULT_VALUE = Boolean.getBoolean(NO_CACHE_BY_DEFAULT);

    private static final Map<String,CachedValue> valueCache = new ConcurrentHashMap<String,CachedValue>();

    //
    private final ReadWriteLock propLock = new ReentrantReadWriteLock(false);
    private final Properties _properties;
    private final long _serverBootTime = System.currentTimeMillis();
    private final Logger logger = Logger.getLogger(getClass().getName());

    // 
    private PropertyChangeListener propertyChangeListener;
    private ClusterPropertyCache clusterPropertyCache;
    private int _serverId;
    private String _hostname;
    private InitialContext _icontext;

    ServerConfig() {
        _properties = new Properties();

        String configPropertiesPath = System.getProperty(PROPS_PATH_PROPERTY);
        if (configPropertiesPath == null) configPropertiesPath = PROPS_PATH_DEFAULT;

        InputStream propStream = null;
        try {

            File file = new File(configPropertiesPath);
            if (file.exists())
                propStream = new FileInputStream(file);
            else
                propStream = ServerConfig.class.getClassLoader().getResourceAsStream(PROPS_RESOURCE_PATH);

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
            if (propStream != null) try{ propStream.close(); }catch(IOException ioe){ /* ok */ }
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
                String newValue = getPropertyCached(propertyName);
                String oldValue = clusterPropertyOld!=null ?
                        clusterPropertyOld.getValue() :
                        getPropertyUncached(propertyName, false);

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
    }

    private Map<String, String> getMappedServerConfigPropertyNames(String keySuffix, String valueSuffix) {
        Map<String, String> keyValueToMappedValue = new TreeMap<String, String>();
        if(keySuffix!=null) {
            for (Map.Entry propEntry : _properties.entrySet()) {
                String propKey = (String) propEntry.getKey();
                String propVal = (String) propEntry.getValue();

                if(propKey==null || propVal==null) continue;

                if(propKey.endsWith(keySuffix)) {
                    keyValueToMappedValue.put(
                            propVal,
                            _properties.getProperty(propKey.substring(0, propKey.length()-keySuffix.length()) + valueSuffix));
                }
            }
        }
        return keyValueToMappedValue;
    }

    private String getServerConfigProperty(String prop) {
        String val = (String)_properties.get(prop);
        if (val == null) return null;
        if (val.length() == 0) return val;

        StringBuffer val2 = new StringBuffer();
        int pos = val.indexOf('$');
        if (pos >= 0) {
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
                        val2.append(val);
                        break;
                    }
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