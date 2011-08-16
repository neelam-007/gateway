package com.l7tech.util;

import com.l7tech.util.CollectionUtils.MapBuilder;
import static com.l7tech.util.ConversionUtils.getTextToIntegerConverter;
import static com.l7tech.util.ConversionUtils.getTextToLongConverter;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.cached;
import static com.l7tech.util.Option.join;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.trim;
import com.l7tech.util.ValidationUtils.Validator;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        private static final Logger logger = Logger.getLogger( DefaultConfig.class.getName() );

        private static final String SYSPROP_NAME_PREFIX = "com.l7tech.";
        private static final String SYSPROP_PROP_SUFFIX = ".systemProperty";
        private static final String VALIDATION_REGEX_SUFFIX = ".validation.regex";
        private static final String VALIDATION_TYPE_SUFFIX = ".validation.type";
        private static final String VALIDATION_MIN_SUFFIX = ".validation.min";
        private static final String VALIDATION_MAX_SUFFIX = ".validation.max";
        private static final String DEFAULT_PROP_SUFFIX = ".default";

        private final long cacheAge;
        private final Map<String,String> propertyMap;
        private final Map<String,String> configNameToSystemPropertyNameMap;
        private final Map<String,String> systemPropertyNameToConfigNameMap;
        private final AtomicReference<Unary<Option<String>,String>> propertyLookup = new AtomicReference<Unary<Option<String>,String>>();
        private final Unary<Option<ValidatorHolder<?>>,String> validatorLookup;

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
            this.validatorLookup = buildValidatorLookup();
        }

        @Override
        public final String getProperty( @NotNull final String propertyName ) {
            return getProperty( propertyName, Option.<String>none(), lookup( propertyName ), stringConverter, validator( propertyName, String.class ) );
        }

        @Override
        public final String getProperty( @NotNull final String propertyName, final String defaultValue ) {
            return getProperty( propertyName, optional( defaultValue ), lookup( propertyName ), stringConverter, validator( propertyName, String.class ) );
        }

        @Override
        public final int getIntProperty( @NotNull final String propertyName, final int defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), lookup( propertyName ), intConverter, validator( propertyName, Integer.class ) );
        }

        @Override
        public final long getLongProperty( @NotNull final String propertyName, final long defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), lookup(propertyName), longConverter, validator( propertyName, Long.class ) );
        }

        @Override
        public final boolean getBooleanProperty( @NotNull final String propertyName, final boolean defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), lookup( propertyName ), booleanConverter, validator( propertyName, Boolean.class ) );
        }

        @Override
        public final long getTimeUnitProperty( @NotNull final String propertyName, final long defaultValue ) {
            return getProperty( propertyName, some( defaultValue ), lookup( propertyName ), timeUnitConverter, validator( propertyName, Long.class ) );
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

        /**
         * Construct a "validator" for the property using the given factory.
         *
         * @param propertyName The property to be validated
         * @param factory The constructor for "validators"
         * @param <BVT> The base validation type
         * @param <LVT> Long validator type
         * @param <IVT> Integer validator type
         * @param <PVT> Pattern validator type
         * @return The validator or null
         */
        protected <BVT, LVT extends BVT, IVT extends BVT, PVT extends BVT> BVT buildValidator(
                final String propertyName,
                final ValidatorFactory<BVT, IVT, LVT, PVT> factory ) {
            final Option<String> propRegex = propertyLookup.get().call( propertyName + VALIDATION_REGEX_SUFFIX );
            final Option<String> propType = propertyLookup.get().call( propertyName + VALIDATION_TYPE_SUFFIX );
            final Option<String> propMin = propertyLookup.get().call( propertyName + VALIDATION_MIN_SUFFIX );
            final Option<String> propMax = propertyLookup.get().call( propertyName + VALIDATION_MAX_SUFFIX );

            BVT validator = null;
            if ( propRegex.isSome() ) {
                try {
                    validator = factory.buildPatternValidator( propRegex.some() );
                } catch ( PatternSyntaxException pse ) {
                    logger.warning( "Ignoring invalid validation regex for '" + propertyName + "': " + ExceptionUtils.getMessage( pse ));
                }
            } else if ( propType.isSome() ) {
                final String type = propType.some();
                final Option<Long> min = join( propMin.map( getTextToLongConverter() ) );
                final Option<Long> max = join( propMax.map( getTextToLongConverter() ) );
                if ( "integer".equalsIgnoreCase( type ) ) {
                    final Option<Integer> minInt = join( propMin.map( getTextToIntegerConverter() ) );
                    final Option<Integer> maxInt = join( propMax.map( getTextToIntegerConverter() ) );
                    validator = factory.buildIntegerValidator( minInt.orSome( Integer.MIN_VALUE ), maxInt.orSome( Integer.MAX_VALUE ) );
                } else if ( "timeUnit".equalsIgnoreCase( type ) ) {
                    validator = factory.buildTimeUnitValidator( min.orSome( Long.MIN_VALUE ), max.orSome( Long.MAX_VALUE ) );
                } else if ( "long".equalsIgnoreCase( type ) ) {
                    validator = factory.buildLongValidator( min.orSome( Long.MIN_VALUE ), max.orSome( Long.MAX_VALUE ) );
                } else {
                    logger.warning( "Ignoring unknown type '"+type+"' for validation of property '"+propertyName+"'" );
                }
            }

            return validator;
        }

        /**
         * Factory for "validator" instances.
         *
         * @param <BVT> The base validation type
         * @param <LVT> Long validator type
         * @param <PVT> Pattern validator type
         */
        protected static abstract class ValidatorFactory<BVT, IVT extends BVT, LVT extends BVT, PVT extends BVT> {
            protected abstract IVT buildIntegerValidator( int min, int max );
            protected abstract LVT buildLongValidator( long min, long max );
            protected abstract PVT buildPatternValidator( String pattern ) throws PatternSyntaxException;
            protected abstract LVT buildTimeUnitValidator( long min, long max );
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

        private Unary<Option<ValidatorHolder<?>>,String> buildValidatorLookup() {
            // Validator factory that builds wrapped (typed) validators
            final ValidatorFactory<ValidatorHolder<?>, ValidatorHolder<Integer>, ValidatorHolder<Long>, ValidatorHolder<String>> factory =
                    new ValidatorFactory<ValidatorHolder<?>, ValidatorHolder<Integer>, ValidatorHolder<Long>, ValidatorHolder<String>>(){
                @Override
                protected ValidatorHolder<Integer> buildIntegerValidator( final int min, final int max ) {
                    return new ValidatorHolder<Integer>( ValidationUtils.getIntegerValidator(
                                        ConversionUtils.<Integer>getIdentityConverter(),
                                        min,
                                        max), Integer.class );
                }

                @Override
                protected ValidatorHolder<Long> buildLongValidator( final long min, final long max ) {
                    return new ValidatorHolder<Long>( ValidationUtils.getLongValidator(
                                        ConversionUtils.<Long>getIdentityConverter(),
                                        min,
                                        max), Long.class );
                }

                @Override
                protected ValidatorHolder<String> buildPatternValidator( final String pattern ) throws PatternSyntaxException {
                    return new ValidatorHolder<String>( ValidationUtils.getPatternTextValidator( Pattern.compile( pattern ) ), String.class );
                }

                @Override
                protected ValidatorHolder<Long> buildTimeUnitValidator( final long min, final long max ) {
                    return buildLongValidator( min, max );
                }
            };

            return cached( new Unary<Option<ValidatorHolder<?>>, String>() {
                @Override
                public Option<ValidatorHolder<?>> call( final String propertyName ) {
                    return Option.<ValidatorHolder<?>>optional( buildValidator( propertyName, factory ) );
                }
            }, VALIDATOR_CACHE_AGE );
        }

        private Option<String> lookup( final String propertyName ) {
            return propertyLookup.get().call( propertyName );
        }

        private <T> Unary<Boolean,T> validator( final String propertyName,
                                                final Class<T> validationType ) {
            return validatorLookup.call( propertyName )
                    .map( new Unary<Unary<Boolean, T>, ValidatorHolder<?>>() {
                        @Override
                        public Unary<Boolean, T> call( final ValidatorHolder<?> validatorHolder ) {
                            return logging( validatorHolder.get( validationType ).toNull(), propertyName );
                        }
                    } ).orSome(
                        new Unary<Boolean, T>() {
                            @Override
                            public Boolean call( final T t ) {
                                return true;
                            }
                        } );
        }

        private <T> Unary<Boolean,T> logging( final Unary<Boolean,T> validator,
                                              final String propertyName ) {
            return validator==null ? null : new Unary<Boolean, T>(){
                @Override
                public Boolean call( final T value ) {
                    final Boolean result = validator.call( value );

                    if ( !result ) {
                        logger.warning( "Configuration property '"+propertyName+"' has an invalid value '"+value+"', using default value." );
                    }

                    return result;
                }
            };
        }

        private <T> T getProperty( final String propertyName,
                                   final Option<T> defaultValue,
                                   final Option<String> propertyValue,
                                   final Unary<Either<String,T>,String> propertyConverter,
                                   final Unary<Boolean,T> propertyValidator ) {
            final Option<T> value;

            if ( propertyValue.isSome() ) {
                final Either<String,T> conversionResult = propertyConverter.call( propertyValue.some() );
                value = conversionResult.toRightOption().filter( propertyValidator ).orElse( defaultValue );
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

        private static final class ValidatorHolder<T> {
            private final Class<T> type;
            private final Validator<T> validator;

            private ValidatorHolder( final Validator<T> validator, final Class<T> type ) {
                this.validator = validator;
                this.type = type;
            }

            private <I> Option<Validator<I>> get( final Class<I> instanceClass ) {
                return type.equals( instanceClass ) ?
                        Option.some( (Validator<I>) validator ) :
                        Option.<Validator<I>>none();
            }
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
    private static final long DEFAULT_CACHE_AGE = TimeUnit.SECONDS.toMillis( 30L );
    private static final long VALIDATOR_CACHE_AGE = TimeUnit.MINUTES.toMillis( 5L );
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
                if ( val.length()>pos+1 && (int) val.charAt( pos + 1 ) == (int) '{' ) {
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
