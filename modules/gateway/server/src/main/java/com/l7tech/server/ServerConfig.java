package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyListener;
import com.l7tech.util.*;
import com.l7tech.util.CollectionUtils.MapBuilder;
import com.l7tech.util.ConfigFactory.ConfigProviderSpi;
import com.l7tech.util.ConfigFactory.DefaultConfig;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.ValidationUtils.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.optional;
import static java.util.Arrays.asList;

/**
 * Provides cached access to Gateway global configuration items, including (once the
 * database and cluster property manager have initialized) cluster properties.
 *
 * @author alex
 */
public class ServerConfig extends DefaultConfig implements ClusterPropertyListener {
    private final AtomicReference<Map<String, String>> namesFromClusterNames = new AtomicReference<>();

    //- PUBLIC

    public static ServerConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Register a group of new serverConfig properties dynamically.
     *
     * The database will be checked for up-to-date values afterward, as long as
     * the cluster property manager is ready.
     *
     * @param registrationInfos The properties to register
     */
    public void registerServerConfigProperties( @NotNull final Iterable<PropertyRegistrationInfo> registrationInfos ) {
        for ( final PropertyRegistrationInfo info : registrationInfos ) {
            propLock.writeLock().lock();
            try {
                invalidateCaches();
                if (info.defaultValue != null) _properties.setProperty(info.name + SUFFIX_DEFAULT, info.defaultValue);
                if (info.description != null) _properties.setProperty(info.name + SUFFIX_DESC, info.description);
                if (info.clusterPropName != null) _properties.setProperty(info.name + SUFFIX_CLUSTER_KEY, info.clusterPropName);
            } finally {
                propLock.writeLock().unlock();
            }
        }
    }

    /**
     * Register a group of new serverConfig properties dynamically.  The database will be checked for up-to-date
     * values afterward, as long as the cluster property manager is ready.
     *
     * @param newProps  an array of 4-tuples, each of which is
     *                 [serverConfigPropertyName, clusterPropertyName, description, defaultValue].
     * @deprecated Use the method taking PropertyRegistrationInfo instead
     */
    @Deprecated
    public void registerServerConfigProperties(String[][] newProps) {
        registerServerConfigProperties( map( asList( newProps ), new Unary<PropertyRegistrationInfo,String[]>(){
            @Override
            public PropertyRegistrationInfo call( final String[] tuple ) {
                return new PropertyRegistrationInfo( tuple[0], tuple[1], tuple[2], tuple[3] );
            }
        } ) );
    }

    public void setPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        propLock.writeLock().lock();
        try {
            invalidateCaches();
            if (this.propertyChangeListener != null) throw new IllegalStateException("propertyChangeListener already set!");
            this.propertyChangeListener = propertyChangeListener;
        } finally {
            propLock.writeLock().unlock();
        }
    }

    @SuppressWarnings({ "AccessStaticViaInstance" })
    public void setClusterPropertyCache(final ClusterPropertyCache clusterPropertyCache) {
        this.clusterPropertyCache.compareAndSet( null, clusterPropertyCache );
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
     * Get the Map of all cluster property names to validators.
     *
     * @return The Map of declared property names to their validators.
     */
    public Map<String, Validator<String>> getClusterPropertyValidators() {
        final MapBuilder<String,Validator<String>> builder = CollectionUtils.mapBuilder();

        final ValidatorFactory<Validator<String>, Validator<String>, Validator<String>, Validator<String>> factory =
                new ValidatorFactory<Validator<String>, Validator<String>, Validator<String>, Validator<String>>(){
            @Override
            protected Validator<String> buildIntegerValidator( final int min, final int max ) {
                return ValidationUtils.getIntegerValidator(
                                    ConversionUtils.getTextToIntegerConverter(),
                                    min,
                                    max);
            }

            @Override
            protected Validator<String> buildLongValidator( final long min, final long max ) {
                return ValidationUtils.getLongValidator(
                                    ConversionUtils.getTextToLongConverter(),
                                    min,
                                    max);
            }

            @Override
            protected Validator<String> buildPatternValidator( final String pattern ) throws PatternSyntaxException {
                return ValidationUtils.getPatternTextValidator( Pattern.compile( pattern ) );
            }

            @Override
            protected Validator<String> buildTimeUnitValidator( final long min, final long max ) {
                return ValidationUtils.getLongValidator(
                                    ConversionUtils.getTimeUnitTextToLongConverter(),
                                    min,
                                    max);
            }
        };

        final Map<String, String> clusterProperties = getMappedServerConfigPropertyNames(SUFFIX_CLUSTER_KEY, SUFFIX_DESC);
        for ( final String clusterPropertyName : clusterProperties.keySet() ) {
            final String configName = getNameFromClusterName( clusterPropertyName );
            builder.put( clusterPropertyName, buildValidator( configName, factory ) );
        }

        return builder.unmodifiableMap();
    }

    /**
     * Get the cluster property name, if any, for the specified ServerConfig property.
     *
     * @param propertyName the ServerConfig property whose cluster property name to look up.
     * @return the cluster property name, or null if there isn't one.
     */
    public String getClusterPropertyName(String propertyName) {
        return getServerConfigProperty( propertyName + SUFFIX_CLUSTER_KEY );
    }

    public String getNameFromClusterName(String clusterPropertyName) {
        Map<String, String> reverseMap = namesFromClusterNames.get();

        if (reverseMap == null) {
            synchronized (namesFromClusterNames) {
                reverseMap = namesFromClusterNames.get();
                if (reverseMap == null) {
                    reverseMap = getCurrentServerConfigPropertyNameMap(SUFFIX_CLUSTER_KEY);
                    namesFromClusterNames.set(reverseMap);
                }
            }
        }

        return reverseMap.get(clusterPropertyName);
    }

    public String getPropertyDescription(String propertyName) {
        return getServerConfigProperty( propertyName + SUFFIX_DESC );
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
            hostname = getProperty( ServerConfigParams.PARAM_HOSTNAME, null );

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
                invalidateCaches();
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
        String str = getProperty( ServerConfigParams.PARAM_ATTACHMENT_DISK_THRESHOLD, null );

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
        String path = getProperty( propName, null );

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

    /**
     * Check if the specified property's value may be changed directly at runtime by the Gateway code.
     *
     * @param propName the property to check
     * @return true if the specified property is marked as mutable
     */
    boolean isMutable(String propName) {
        propLock.readLock().lock();
        try {
            return "true".equals(_properties.getProperty(propName + ".mutable"));
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
            invalidateCaches();
            oldValue = (String) _properties.setProperty(propName, value);
        } finally {
            propLock.writeLock().unlock();
        }

        notifyConfigPropertyChanged( propName );

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
            invalidateCaches();
            _properties.remove(propName);
        } finally {
            propLock.writeLock().unlock();
        }

        notifyConfigPropertyChanged( propName );

        return true;
    }

    /**
     * Immutable bean for server config property registration.
     */
    public static final class PropertyRegistrationInfo {
        @NotNull  private final String name;
        @Nullable private final String clusterPropName;
        @Nullable private final String description;
        @Nullable private final String defaultValue;

        public PropertyRegistrationInfo( @NotNull  final String name,
                                         @Nullable final String clusterPropName,
                                         @Nullable final String description,
                                         @Nullable final String defaultValue ) {
            this.name = name;
            this.clusterPropName = clusterPropName;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public static PropertyRegistrationInfo prInfo( @NotNull  final String propName,
                                                       @Nullable final String clusterPropName,
                                                       @Nullable final String description,
                                                       @Nullable final String defaultValue ) {
            return new PropertyRegistrationInfo( propName, clusterPropName, description, defaultValue );
        }

        @NotNull
        public String getName() {
            return name;
        }
    }

    @ManagedResource(description="Server config", objectName="l7tech:type=ServerConfig")
    public static class ManagedServerConfig {
        final ServerConfig serverConfig;

        protected ManagedServerConfig( final ServerConfig serverConfig ) {
            this.serverConfig = serverConfig;    
        }

        @ManagedOperation(description="Get Property Value")
        public String getProperty( final String name ) {
            return serverConfig.getPropertyInternal( name );
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

    public static class ServerConfigConfigProviderSpi implements ConfigProviderSpi {
        @Override
        public Config newConfig( final Properties properties, final long cacheAge ) {
            return new ServerConfig( properties, cacheAge );
        }
    }

    //- PROTECTED

    @Override
    protected Option<String> getConfigPropertyDirect( final String propertyName ) {
        String configName;

        if ( propertyName.startsWith( SYSPROP_NAME_PREFIX ) ) {
            configName = systemPropertyNameToConfigNameMap.get().get( propertyName );
            if ( configName == null ) {
                return optional( SyspropUtil.getProperty( propertyName ) );
            }
        } else {
            configName = propertyName;
        }

        return optional( getPropertyInternal( configName, true ) );
    }

    //- PRIVATE

    private static final String SUFFIX_SYSPROP = ".systemProperty";
    private static final String SUFFIX_SETSYSPROP = ".setSystemProperty";
    private static final String SUFFIX_DESC = ".description";
    private static final String SUFFIX_DEFAULT = ".default";
    private static final String SUFFIX_VISIBLE = ".visible";
    private static final String SUFFIX_CLUSTER_KEY = ".clusterProperty";

    private static final String SYSPROP_NAME_PREFIX = "com.l7tech.";

    private final AtomicReference<Map<String,String>> systemPropertyNameToConfigNameMap =
            new AtomicReference<Map<String, String>>( Collections.<String, String>emptyMap() );
    private final AtomicReference<Map<String,String>> configNameToSystemPropertyNameMap =
            new AtomicReference<Map<String, String>>( Collections.<String, String>emptyMap() );
    private final ReadWriteLock propLock = new ReentrantReadWriteLock(false);
    private final Properties _properties;
    private final long _serverBootTime = System.currentTimeMillis();
    private final Logger logger = Logger.getLogger(getClass().getName());

    private PropertyChangeListener propertyChangeListener;
    private static AtomicReference<ClusterPropertyCache> clusterPropertyCache = new AtomicReference<ClusterPropertyCache>();
    private String _hostname;

    protected ServerConfig( final Properties properties,
                            final long cacheAge ) {
        super( properties, cacheAge );
        _properties = new Properties();

        for ( final String name : properties.stringPropertyNames() ) {
            _properties.setProperty( name, properties.getProperty( name ) );
        }

        final MapBuilder<String,String> systemBuilder = CollectionUtils.mapBuilder();
        final MapBuilder<String,String> configBuilder = CollectionUtils.mapBuilder();
        for ( final String name : _properties.stringPropertyNames() ) {
            if ( name.endsWith( SUFFIX_SYSPROP ) ) {
                final String value = properties.getProperty( name );
                final String systemPropertyName = expand( value );
                if ( systemPropertyName != null && systemPropertyName.startsWith(SYSPROP_NAME_PREFIX) ) {
                    final String configName = name.substring( 0, name.length()-SUFFIX_SYSPROP.length() );
                    systemBuilder.put( systemPropertyName, configName );
                    configBuilder.put( configName, systemPropertyName );
                }
            }

        }

        systemPropertyNameToConfigNameMap.set( systemBuilder.unmodifiableMap() );
        configNameToSystemPropertyNameMap.set( configBuilder.unmodifiableMap() );

        prepopulateSystemProperties();
    }
    
    private String getPropertyInternal( String propName ) {
        return getPropertyInternal( propName, true );
    }

    @SuppressWarnings({ "AccessStaticViaInstance" })
    private String getPropertyInternal( final String propName,
                                        final boolean includeClusterProperties ) {
        String sysPropProp = propName + SUFFIX_SYSPROP;
        String setSysPropProp = propName + SUFFIX_SETSYSPROP;
        String dfltProp = propName + SUFFIX_DEFAULT;
        String clusterKeyProp = propName + SUFFIX_CLUSTER_KEY;

        String systemPropertyName = getServerConfigProperty(sysPropProp);
        String isSetSystemProperty = getServerConfigProperty(setSysPropProp);
        String defaultValue = getServerConfigProperty(dfltProp);
        String clusterKey = getServerConfigProperty(clusterKeyProp);

        String value = null;

        ClusterPropertyCache clusterPropertyCache = this.clusterPropertyCache.get();

        if ( systemPropertyName != null && systemPropertyName.length() > 0 ) {
            logger.finest("Checking System property " + systemPropertyName);
            value = SyspropUtil.getProperty( systemPropertyName );
        }

        if (value == null && includeClusterProperties && clusterKey != null && clusterKey.length() > 0) {
            if (clusterPropertyCache == null) {
                logger.warning("Property '" + propName + "' has a cluster properties key defined, but the ClusterPropertyCache is not yet available");
            } else {
                logger.finest("Checking for cluster property '" + clusterKey + "'");
                try {
                    ClusterProperty cp = clusterPropertyCache.getCachedEntityByName(clusterKey);
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
                getPropertyInternal( prop );
            }
        }
    }

    private void notifyConfigPropertyChanged( final String propName ) {
        notifyPropertyChanged( propName );
        final String systemProperyName = configNameToSystemPropertyNameMap.get().get( propName );
        if ( systemProperyName != null ) {
            notifyPropertyChanged( systemProperyName );
        }
    }

    private void clusterPropertyEvent(final ClusterProperty clusterProperty, final ClusterProperty clusterPropertyOld) {
        String propertyName = getNameFromClusterName(clusterProperty.getName());
        if (propertyName != null) {
            notifyConfigPropertyChanged( propertyName );

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
                        getPropertyInternal( propertyName, false );
                String newValue = getProperty( propertyName, null );

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

    private void invalidateCaches() {
        namesFromClusterNames.set(null);
    }

    private Map<String,String> getCurrentServerConfigPropertyNameMap(String suffix) {
        Map<String,String> ret = new HashMap<>();
        if(suffix!=null) {
            propLock.readLock().lock();
            try {
                Set<Map.Entry<Object,Object>> propEntries = _properties.entrySet();
                for (Map.Entry<Object,Object> propEntry : propEntries) {
                    String propKey = (String) propEntry.getKey();
                    String propVal = (String) propEntry.getValue();

                    if (propKey == null || propVal == null) continue;

                    if (propKey.endsWith(suffix)) {
                        String name = propKey.substring(0, propKey.length() - suffix.length());
                        ret.put(propVal, name);
                    }
                }
            } finally {
                propLock.readLock().unlock();
            }
        }
        return ret;
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
                                getServerConfigProperty(propKey.substring(0, propKey.length()-keySuffix.length()) + valueSuffix) );
                    }
                }
            } finally {
                propLock.readLock().unlock();
            }
        }
        return keyValueToMappedValue;
    }

    private String getServerConfigProperty( final String prop ) {
        propLock.readLock().lock();
        try {
            return expand( _properties.getProperty(prop) );
        } finally {
            propLock.readLock().unlock();
        }
    }

    private static final class InstanceHolder {
        private static final ServerConfig INSTANCE = (ServerConfig) ConfigFactory.getCachedConfig();
    }
}
