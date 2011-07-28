package com.l7tech.server.communityschemas;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.util.Config;
import com.l7tech.util.Resolver;
import com.l7tech.util.ValidatedConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Configuration for schema subsystem.
 */
public class SchemaConfiguration implements ApplicationContextAware, PropertyChangeListener {

    public SchemaConfiguration( final Config config ) {
        this.config = validated(config);
        updateCacheConfiguration();
        updateRemoteResourceRegex();
    }

    public int getMaxCacheAge(){
        return cacheConfigurationReference.get().maxCacheAge;
    }

    public int getMaxStaleAge(){
        return cacheConfigurationReference.get().maxStaleAge;    
    }

    public int getMaxCacheEntries(){
        return cacheConfigurationReference.get().maxCacheEntries;
    }

    public int getHardwareRecompileLatency(){
        return cacheConfigurationReference.get().hardwareRecompileLatency;
    }

    public int getHardwareRecompileMinAge(){
        return cacheConfigurationReference.get().hardwareRecompileMinAge;
    }

    public int getHardwareRecompileMaxAge(){
        return cacheConfigurationReference.get().hardwareRecompileMaxAge;
    }

    public long getMaxSchemaSize(){
        return cacheConfigurationReference.get().maxSchemaSize;
    }

    public boolean isSoftwareFallback(){
        return cacheConfigurationReference.get().softwareFallback;
    }

    public boolean isAllowDoctype(){
        return cacheConfigurationReference.get().allowDoctype;
    }

    public Pattern getRemoteResourcePattern() {
        return remoteResourcePattern.get();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if ( ServerConfigParams.PARAM_SCHEMA_REMOTE_URL_REGEX.equals( evt.getPropertyName() )) {
            logger.config( "Updating remote XML Schema resource regular expression." );
            updateRemoteResourceRegex();
        } else {
            logger.config( "(Re)Loading schema configuration." );
            updateCacheConfiguration();
            final ApplicationContext applicationContext = this.applicationContext;
            if ( applicationContext != null ) {
                applicationContext.publishEvent( new SchemaConfigurationReloadedEvent(this) );               
            }
        }
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Application event that is published when the configuration is reloaded.
     */
    public static class SchemaConfigurationReloadedEvent extends ApplicationEvent {
        public SchemaConfigurationReloadedEvent( final SchemaConfiguration source ) {
            super( source );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( SchemaConfiguration.class.getName() );

    private final Config config;
    private final AtomicReference<Pattern> remoteResourcePattern = new AtomicReference<Pattern>();
    private final AtomicReference<CacheConfiguration> cacheConfigurationReference = new AtomicReference<CacheConfiguration>();
    private ApplicationContext applicationContext;

    private void updateRemoteResourceRegex() {
        final String regex = config.getProperty( ServerConfigParams.PARAM_SCHEMA_REMOTE_URL_REGEX, ".*" );
        try {
            final Pattern pattern = Pattern.compile( regex.trim() );
            remoteResourcePattern.set( pattern );
            logger.config( "Using '"+ ServerConfigParams.PARAM_SCHEMA_REMOTE_URL_REGEX+"' value '"+regex+"'." );
        } catch ( final PatternSyntaxException pse ) {
            remoteResourcePattern.set( null );
            logger.warning( "Invalid '"+ ServerConfigParams.PARAM_SCHEMA_REMOTE_URL_REGEX+"' ('"+regex+"'), remote XML Schema resource access will fail." );
        }
    }

    private void updateCacheConfiguration() {
        cacheConfigurationReference.set(new CacheConfiguration(config));
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                return ServerConfig.getInstance().getClusterPropertyName( key );
            }
        } );

        vc.setMinimumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 0 );
        vc.setMaximumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 1000000 );

        vc.setMinimumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_STALE_AGE, -1 );

        vc.setMinimumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE, 1 );
        vc.setMinimumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE, 1 );
        vc.setMinimumValue( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 1000 );

        return vc;
    }

    private static class CacheConfiguration {
        private final int maxCacheAge;
        private final int maxStaleAge;
        private final int maxCacheEntries;
        private final int hardwareRecompileLatency;
        private final int hardwareRecompileMinAge;
        private final int hardwareRecompileMaxAge;
        private final long maxSchemaSize;
        private final boolean softwareFallback;
        private final boolean allowDoctype;

        CacheConfiguration( Config config) {
            maxCacheAge = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_AGE, 300000);
            maxStaleAge = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_STALE_AGE, -1);
            maxCacheEntries = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 100);
            hardwareRecompileLatency = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 10000);
            hardwareRecompileMinAge = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE, 500);
            hardwareRecompileMaxAge = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE, 30000);
            maxSchemaSize = config.getLongProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);

            // This isn't "true".equals(...) just in case ServerConfig returns null--we want to default to true.
            softwareFallback = !("false".equals(config.getProperty( ServerConfigParams.PARAM_SCHEMA_SOFTWARE_FALLBACK, "true")));
            allowDoctype = config.getBooleanProperty( ServerConfigParams.PARAM_SCHEMA_ALLOW_DOCTYPE, false );
        }
    }


}
