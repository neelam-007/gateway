package com.l7tech.util;

import com.l7tech.util.CollectionUtils.MapBuilder;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.cached;
import static com.l7tech.util.Functions.partial;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.trim;
import static java.util.Collections.list;
import static java.util.Collections.reverse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for global Config instances with convenience methods for static use.
 *
 * <p>It is preferable to pass Config instances where possible. Static methods
 * can be used instead of system property access for "com.l7tech.*" properties.</p>
 */
public class ConfigFactory {

    //- PUBLIC

    /**
     * Get a String configuration property with caching.
     *
     * @param propertyName The name of the property
     * @return The value of the property or null
     */
    @Nullable
    public static String getProperty( final String propertyName ) {
        return getCachedConfig().getProperty(propertyName);
    }

    /**
     * Get a String configuration property with caching.
     *
     * @param propertyName The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property or the default value
     */
    @Nullable
    public static String getProperty( final String propertyName, @Nullable final String defaultValue ) {
        return getCachedConfig().getProperty(propertyName, defaultValue);
    }

    /**
     * Get an integer configuration property with caching.
     *
     * @param propertyName The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property or the default value
     */
    public static int getIntProperty( final String propertyName, final int defaultValue ) {
        return getCachedConfig().getIntProperty( propertyName, defaultValue );
    }

    /**
     * Get a long configuration property with caching.
     *
     * @param propertyName The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property or the default value
     */
    public static long getLongProperty( final String propertyName, final long defaultValue ) {
        return getCachedConfig().getLongProperty( propertyName, defaultValue );
    }

    /**
     * Get a boolean configuration property with caching.
     *
     * @param propertyName The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property or the default value
     */
    public static boolean getBooleanProperty( final String propertyName, final boolean defaultValue ) {
        return getCachedConfig().getBooleanProperty( propertyName, defaultValue );
    }

    /**
     * Get a time unit configuration property with caching.
     *
     * @param propertyName The name of the property
     * @param defaultValue The default value for the property
     * @return The value of the property or the default value
     */
    public static long getTimeUnitProperty( final String propertyName, final long defaultValue ) {
        return getCachedConfig().getTimeUnitProperty( propertyName, defaultValue );
    }

    /**
     * Get an uncached global configuration instance.
     *
     * @return The global configuration instance
     */
    public static Config getUncachedConfig() {
        return ConfigFactoryHolder.INSTANCE.config;
    }

    /**
     * Get a cached global configuration instance.
     *
     * @return The global configuration instance
     */
    public static Config getCachedConfig() {
        return ConfigFactoryHolder.INSTANCE.cachedConfig;
    }

    /**
     * Clear any cached global configuration values.
     */
    public static void clearCachedConfig() {
        ConfigFactoryHolder.INSTANCE.reset();
    }

    /**
     * Add a configuration listener.
     *
     * <p>Added listeners will not be subject to garbage collection.</p>
     *
     * @param listener The listener to add
     * @param propertyNames The property names to subscribe to
     */
    public static void addListener( final ConfigurationListener listener,
                                    final String... propertyNames ) {
        listeners.add( smart( listener, propertyNames ) );
    }

    /**
     * Interface for listening to configuration changes.
     */
    public interface ConfigurationListener {
        /**
         * Called when a property has changed (or may have changed)
         *
         * @param properyName The property name
         */
        void notifyPropertyChanged( String properyName );
    }

    /**
     * Interface for listening to a subset of configuration changes.
     */
    public interface SmartConfigurationListener extends ConfigurationListener {
        /**
         * Does this listener support notifications for the given property.
         *
         * @param propertyName The property name
         * @return True if notifications are desired for the property
         */
        boolean supportsProperty( String propertyName );
    }

    /**
     * Support for listening to a set of configuration properties
     */
    public abstract static class SmartConfigurationListenerSupport implements SmartConfigurationListener {
        private final Set<String> names;

        public SmartConfigurationListenerSupport ( final Collection<String> names ) {
            this.names = Collections.unmodifiableSet( new HashSet<String>( names ) );
        }

        @Override
        public boolean supportsProperty( final String propertyName ) {
            return names.isEmpty() || names.contains( propertyName );
        }
    }

    /**
     * Default config implementation.
     *
     * <p>Supports access of configuration / system properties and caching.</p>
     */
    public static class DefaultConfig implements Config {

        private static final String SYSPROP_NAME_PREFIX = "com.l7tech.";
        private static final String SYSPROP_PROP_SUFFIX = ".systemProperty";
        private static final String DEFAULT_PROP_SUFFIX = ".default";

        private final long cacheAge;
        private final Map<String,String> propertyMap;
        private final Map<String,String> configNameToSystemPropertyNameMap;
        private final Map<String,String> systemPropertyNameToConfigNameMap;
        private final AtomicReference<Unary<Option<String>,String>> propertyLookup = new AtomicReference<Unary<Option<String>,String>>();

        public DefaultConfig( final Properties properties,
                              final long cacheAge ) {
            this.cacheAge = cacheAge;
            final MapBuilder<String,String> defaultsBuilder = CollectionUtils.mapBuilder();
            final MapBuilder<String,String> configBuilder = CollectionUtils.mapBuilder();
            final MapBuilder<String,String> systemBuilder = CollectionUtils.mapBuilder();
            final Map<String,String> propertyMap = new HashMap<String, String>();
            final Unary<Option<String>,String> getter = expanding( getter( properties ) );
            for ( final String propertyName : properties.stringPropertyNames() ) {
                final Option<String> value =
                        optional( expandPropertyValue( properties.getProperty( propertyName ), getter ) );
                propertyMap.put( propertyName, value.toNull() );
                defaultsBuilder.put( removeSuffix( propertyName, DEFAULT_PROP_SUFFIX ), value );
                final Option<String> configName = removeSuffix( propertyName, SYSPROP_PROP_SUFFIX );
                configBuilder.put( configName, value );
                systemBuilder.put( value, configName );
            }
            final Map<String,String> defaultValues = defaultsBuilder.unmodifiableMap();
            for ( final Entry<String,String> defaultEntry : defaultValues.entrySet() ) {
                if ( !propertyMap.containsKey( defaultEntry.getKey() ) ) {
                    propertyMap.put( defaultEntry.getKey(), defaultEntry.getValue() );
                }
            }
            this.propertyMap = Collections.unmodifiableMap( propertyMap );
            this.configNameToSystemPropertyNameMap = configBuilder.unmodifiableMap();
            this.systemPropertyNameToConfigNameMap = systemBuilder.unmodifiableMap();
            this.propertyLookup.set( buildPropertyLookup() );
        }

        @Override
        public final String getProperty( @NotNull final String propertyName ) {
            return getProperty( propertyName, Option.<String>none(), propertyLookup.get(), stringConverter );
        }

        @Override
        public final String getProperty( @NotNull final String propertyName, final String defaultValue ) {
            return getProperty( propertyName, optional( defaultValue ), propertyLookup.get(), stringConverter );
        }

        @Override
        public final int getIntProperty( @NotNull final String propertyName, final int defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), propertyLookup.get(), intConverter );
        }

        @Override
        public final long getLongProperty( @NotNull final String propertyName, final long defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), propertyLookup.get(), longConverter );
        }

        @Override
        public final boolean getBooleanProperty( @NotNull final String propertyName, final boolean defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), propertyLookup.get(), booleanConverter );
        }

        @Override
        public final long getTimeUnitProperty( @NotNull final String propertyName, final long defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), propertyLookup.get(), timeUnitConverter );
        }

        /**
         * Access a configuration property without any conversion or default.
         *
         * <p>The property should be expanded if appropriate.</p>
         *
         * @param propertyName The name of the property (as from client)
         * @return The optional value
         */
        protected Option<String> getConfigPropertyDirect( final String propertyName ) {
            String configName;
            Option<String> syspropName ;
            if ( propertyName.startsWith( SYSPROP_NAME_PREFIX ) ) {
                configName = systemPropertyNameToConfigNameMap.get( propertyName );
                syspropName = some( propertyName );
            } else {
                configName = propertyName;
                syspropName = optional( configNameToSystemPropertyNameMap.get( propertyName ) );
            }

            return syspropName.map( systemPropertyLookup )
                    .orElse( optional( propertyMap.get( configName ) ) );
        }

        /**
         * Expand any property references in the given text.
         *
         * <p>The format for a reference is <code>${property}</code>, where
         * "property" is the name of the referenced property. If a property
         * reference cannot be resolve the original text is left in place.</p>
         *
         * @param text The text to expand (may be null)
         * @return The expanded text
         */
        protected final String expand( final String text ) {
            return expandPropertyValue( text, propertyLookup.get() );
        }

        /**
         * Notify a property change for the given property name.
         *
         * <p>Note that if a Config supports access to properties using an
         * alias (such as a system property name) then notification should be
         * performed separately for the alias.</p>
         *
         * @param name The property name
         */
        protected final void notifyPropertyChanged( final String name ) {
            reset();
            firePropertyChanged( name );
        }

        /**
         * Reset the cache for this config (if any)
         */
        protected final void reset() {
            if ( cacheAge > 0L ) {
                this.propertyLookup.set( buildPropertyLookup() );
            }
        }

        /**
         * Remove the given suffix from the text if present.
         *
         * @param text The text to process
         * @param suffix The suffix to remove
         * @return The text with optional suffix removed
         */
        protected final Option<String> removeSuffix( final String text, final String suffix ) {
            return text.endsWith( suffix ) ?
                    some( text.substring( 0, text.length() - suffix.length() ) ) :
                    Option.<String>none();
        }

        private Unary<Option<String>, String> buildPropertyLookup() {
            final Unary<Option<String>, String> propertyLookup =  new Unary<Option<String>, String>(){
                @Override
                public Option<String> call( final String propertyName ) {
                    return getConfigPropertyDirect( propertyName );
                }
            };

            return cacheAge==0L ?
                     propertyLookup :
                     cached( propertyLookup , cacheAge );
        }

        private <T> T getProperty( final String propertyName,
                                   final Option<T> defaultValue,
                                   final Unary<Option<String>, String> propertyLookup,
                                   final Unary<Either<String,T>,String> propertyConverter ) {
            final Option<String> propertyValue = propertyLookup.call( propertyName );
            final Option<T> value;

            if ( propertyValue.isSome() ) {
                final Either<String,T> conversionResult = propertyConverter.call( propertyValue.some() );
                value = conversionResult.toRightOption().orElse( defaultValue );
                if ( conversionResult.isLeft() ) {
                    logger.log( Level.WARNING,
                            "Configuration property {0} value ''{1}'' is invalid; using default value ''{2}'' instead.",
                            new Object[]{propertyName, propertyValue.some(), defaultValue.toNull()} );
                }
            } else {
                value = defaultValue;
            }
            return value.toNull();
        }
    }

    /**
     * Interface implemented by configuration providers.
     */
    public interface ConfigProviderSpi {
        /**
         * Create a new configuration instance using the specified properties
         *
         * @param properties The properties to use
         * @param cacheAge The maximum age
         * @return The new config instance
         */
        Config newConfig( Properties properties, long cacheAge );
    }

    /**
     * The default configuration provider.
     */
    public static final class DefaultConfigProviderSpi implements ConfigProviderSpi {
        @Override
        public Config newConfig( final Properties properties, final long cacheAge ) {
            return new DefaultConfig( properties, cacheAge );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ConfigFactory.class.getName() );

    private static final String PROP_CACHE_AGE = "com.l7tech.util.configCacheAge";
    private static final long DEFAULT_CACHE_AGE = 30000L;
    private static final String PROPS_RESOURCE_PATH = "com/l7tech/config.properties";

    private static final CopyOnWriteArrayList<SmartConfigurationListener> listeners
            = new CopyOnWriteArrayList<SmartConfigurationListener>();

    private final Config config;
    private final Config cachedConfig;

    private ConfigFactory( final ConfigProviderSpi provider,
                           final Properties properties ) {
        this.config = provider.newConfig( properties, 0L );
        this.cachedConfig = provider.newConfig(
                properties,
                config.getLongProperty( PROP_CACHE_AGE, DEFAULT_CACHE_AGE ) );
    }

    private void reset() {
        if ( this.cachedConfig instanceof DefaultConfig ) {
            ((DefaultConfig)this.cachedConfig).reset();
        }
    }

    private static String expandPropertyValue( final String val,
                                               final Unary<Option<String>,String> propertyGetter ) {
        if ( val == null || val.isEmpty() ) return val;

        StringBuilder val2 = new StringBuilder();
        int pos = val.indexOf('$');
        if (pos >= 0) {
            val2.append(val.substring(0, pos));
            while (pos >= 0) {
                if ( (int) val.charAt( pos + 1 ) == (int) '{' ) {
                    int pos2 = val.indexOf('}', pos + 1);
                    if (pos2 >= 0) {
                        // there's a reference
                        String prop2 = val.substring(pos + 2, pos2);
                        Option<String> ref = propertyGetter.call( prop2 );
                        if ( !ref.isSome() ) {
                            val2.append("${");
                            val2.append(prop2);
                            val2.append("}");
                        } else {
                            val2.append(ref.some());
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

    private static Either<String,Integer> asInteger( final String strval ) {
        try {
            return right(Integer.parseInt(trim(strval)));
        } catch ( NumberFormatException e ) {
            return left("Invalid integer value '" + strval + "'");
        }
    }
    private static Either<String,Long> asLong( final String strval ) {
        try {
            return right(Long.parseLong(trim(strval)));
        } catch ( NumberFormatException e ) {
            return left("Invalid long value '" + strval + "'");
        }
    }

    private static Either<String,Boolean> asBoolean( final String strval ) {
        return right( Boolean.parseBoolean( trim(strval) ) );
    }

    private static Either<String,Long> asTimeUnit( final String strval ) {
        try {
            return right(TimeUnit.parse(trim(strval), TimeUnit.MINUTES));
        } catch ( NumberFormatException e ) {
            return left("Invalid timeunit value '" + strval + "'");
        }
    }

    private static ConfigProviderSpi getConfigProvider() {
        final ConfigProviderSpi configProvider;
        final ServiceLoader<ConfigProviderSpi> providerServiceLoader = ServiceLoader.load( ConfigProviderSpi.class );
        final Iterator<ConfigProviderSpi> providerServiceIterator = providerServiceLoader.iterator();
        if ( providerServiceIterator.hasNext() ) {
            configProvider = providerServiceIterator.next();
        } else {
            configProvider = new DefaultConfigProviderSpi();
        }
        return configProvider;
    }

    private static ClassLoader getConfigClassLoader() {
        return ConfigFactory.class.getClassLoader();
    }

    private static Properties loadProperties() {
        final Properties properties = new Properties();
        final List<URL> urls = listResources();
        for ( final URL propsUrl : urls ) {
            mergeProperties( properties, propertiesAccessor( propsUrl, propsUrl.toString() ) );
        }

        final Unary<Option<String>,String> propertyGetter = expanding( getter( properties ) );
        final String includedResource = expandPropertyValue( properties.getProperty( "include-resource" ), propertyGetter );
        if ( includedResource != null ) {
            final URL propsUrl = getConfigClassLoader().getResource( includedResource );
            mergeProperties( properties, propertiesAccessor( propsUrl, includedResource ) );
        }

        final String includedFile = expandPropertyValue( properties.getProperty( "include-file" ), propertyGetter );
        if ( includedFile != null ) {
            final File propsFile = new File( includedFile );
            mergeProperties( properties, propertiesAccessor( propsFile ) );
        }

        return properties;
    }

    private static List<URL> listResources() {
        try {
            final List<URL> urls = list( getConfigClassLoader().getResources( PROPS_RESOURCE_PATH ) );
            reverse( urls );
            return urls;
        } catch ( final IOException e ) {
            logger.log(
                    Level.WARNING,
                    "Error loading configuration properties '" + ExceptionUtils.getMessage( e ),
                    ExceptionUtils.getDebugException( e ) );
            return Collections.emptyList();
        }
    }

    private static Nullary<Properties> propertiesAccessor( final URL url,
                                                           final String description ) {
        return new Nullary<Properties>(){
            @Override
            public Properties call() {
                if ( url == null ) {
                    if (description != null) {
                        logger.warning( "Resource not found '"+description+"'" );
                    }
                } else {
                    try {
                        return loadProperties( url.openStream(), url.toString() );
                    } catch ( final IOException e ) {
                        logger.log(
                                Level.WARNING,
                                "Error loading configuration properties from '"+url.toString()+"': " + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ) );
                    }
                }
                return new Properties();
            }
        };
    }

    private static Nullary<Properties> propertiesAccessor( final File file ) {
        return new Nullary<Properties>(){
            @Override
            public Properties call() {
                if ( file.exists() ) {
                    //Logging might not be initialized during config load so log at FINE
                    logger.fine( "Loading configuration properties from file '" + file + "'." );

                    try {
                        return loadProperties( new FileInputStream( file ), file.getAbsolutePath() );
                    } catch ( FileNotFoundException e ) {
                        logger.log(
                                Level.WARNING,
                                "Error loading configuration properties from '"+file.getAbsolutePath()+"': '" + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ) );
                    }
                }
                return new Properties();
            }
        };
    }

    private static Properties loadProperties( final InputStream in,
                                              final String description ) {
        final Properties properties = new Properties();
        try {
            properties.load( in );
        } catch ( final IOException pe ) {
            //TODO [jdk7] Multicatch
            logger.log(
                    Level.WARNING,
                    "Error loading configuration properties from '"+description+"': '" + ExceptionUtils.getMessage( pe ),
                    ExceptionUtils.getDebugException( pe ) );
        } catch ( final IllegalArgumentException pe ) {
            logger.log(
                    Level.WARNING,
                    "Error loading configuration properties from '"+description+"': '" + ExceptionUtils.getMessage( pe ),
                    ExceptionUtils.getDebugException( pe ) );
        }

        return properties;
    }

    private static void mergeProperties( final Properties target, final Nullary<Properties> extras ) {
        mergeProperties( target, extras.call() );
    }

    private static void mergeProperties( final Properties target, final Properties extras ) {
        for ( final String propertyName : extras.stringPropertyNames() ) {
            target.setProperty( propertyName, extras.getProperty( propertyName ) );
        }
    }

    private static SmartConfigurationListener smart( final ConfigurationListener listener,
                                                     final String[] names ) {
        if ( listener instanceof SmartConfigurationListener ) {
            return (SmartConfigurationListener) listener;
        } else {
            return new DelegatingConfigurationListener( listener, names );
        }
    }

    private static void firePropertyChanged( final String name ) {
        for ( final SmartConfigurationListener listener : listeners ) {
            if ( listener.supportsProperty( name ) ) {
                listener.notifyPropertyChanged( name );
            }
        }
    }

    private static Unary<Option<String>,String> expanding( final Unary<Option<String>,String> propertyGetter ) {
        return new Unary<Option<String>,String>() {
            @Override
            public Option<String> call( final String property ) {
                return propertyGetter.call( property + DefaultConfig.SYSPROP_PROP_SUFFIX )
                        .map( systemPropertyLookup )
                        .orElse( optional( expandPropertyValue( propertyGetter.call( property ).toNull( ), this ) ) );
            }
        };
    }

    private static Unary<Option<String>,String> getter( final Properties properties ) {
        return new Unary<Option<String>, String>() {
            @Override
            public Option<String> call( final String propertyName ) {
                return optional( properties.getProperty( propertyName ) )
                        .orElse( optional( properties.getProperty( propertyName + DefaultConfig.DEFAULT_PROP_SUFFIX ) ) );
            }
        };
    }

    private static final Unary<String, String> systemPropertyLookup = new Unary<String, String>() {
        @Override
        public String call( final String property ) {
            return SyspropUtil.getProperty( property );
        }
    };

    private static final Unary<Either<String,String>,String> stringConverter = new Unary<Either<String,String>,String>(){
        @Override
        public Either<String, String> call( final String value ) {
            return right(value);
        }
    };

    private static final Unary<Either<String,Integer>,String> intConverter = new Unary<Either<String,Integer>,String>(){
        @Override
        public Either<String, Integer> call( final String value ) {
            return asInteger( value );
        }
    };

    private static final Unary<Either<String,Long>,String> longConverter = new Unary<Either<String,Long>,String>(){
        @Override
        public Either<String, Long> call( final String value ) {
            return asLong( value );
        }
    };

    private static final Unary<Either<String,Boolean>,String> booleanConverter = new Unary<Either<String,Boolean>,String>(){
        @Override
        public Either<String, Boolean> call( final String value ) {
            return asBoolean( value );
        }
    };

    private static final Unary<Either<String,Long>,String> timeUnitConverter = new Unary<Either<String,Long>,String>(){
        @Override
        public Either<String, Long> call( final String value ) {
            return asTimeUnit( value );
        }
    };

    private static final class ConfigFactoryHolder {
        private static final ConfigFactory INSTANCE = new ConfigFactory( getConfigProvider(), loadProperties() );
    }

    private static final class DelegatingConfigurationListener extends SmartConfigurationListenerSupport {
        private final ConfigurationListener delegate;

        private DelegatingConfigurationListener( final ConfigurationListener delegate,
                                                 final String[] names ) {
            super( Arrays.asList( names ) );
            this.delegate = delegate;
        }

        @Override
        public void notifyPropertyChanged( final String properyName ) {
            delegate.notifyPropertyChanged( properyName );
        }
    }
}
