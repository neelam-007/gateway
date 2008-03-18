package com.l7tech.external.assertions.kerberosmapping.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.mapping.LdapAttributeMapping;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.event.EntityInvalidationEvent;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Controller for LDAP Identity Provider configuration.
 *
 * @author steve
 */
public class CachingLdapGroupManagerController {

    //- PUBLIC

    /**
     * Initialize caching of LDAP Groups and Kerberos attribute mappings.
     *
     * <p>Register listener for changes to cluster properties and identity
     * providers.</p>
     *
     * @param context The application context
     */
    public static void onModuleLoaded( final ApplicationContext context ) {
        logger.info( "Installing LDAP cache and kerberos attribute controller." );

        ApplicationEventProxy applicationEventProxy = (ApplicationEventProxy)context.getBean("applicationEventProxy", ApplicationEventProxy.class);

        listener = new ApplicationListener() {
            public void onApplicationEvent( final ApplicationEvent event ) {
                if ( isReloadEvent(event) ) {
                    updateLdapIdentityProviders(context);
                }
            }
        };

        updateLdapIdentityProviders( context );
        applicationEventProxy.addApplicationListener( listener );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CachingLdapGroupManagerController.class.getName());

    private static final String DISABLE_CACHE_PROPERTY =  CachingLdapGroupManager.class.getName() + ".disable";


    private static final String CLUSTER_PROPERTY_PREFIX = "kerberosLdapAttribute.";
    private static final String LIP_KERBEROS_FIELD_OBFS = "e";
    private static final String LIP_KERBEROS_FIELD = "kerberosLdapAttributeMapping";
    private static final String LIP_KERBEROS_FIELD_DEFAULT = "userPrincipalName";

    private static final String SET_ATTR_NAME_42 = "setAttributeName";
    private static final String SET_ATTR_NAME = "setCustomAttributeName";

    @SuppressWarnings( { "FieldCanBeLocal" } )
    private static ApplicationListener listener = null; // keep reference to listener to prevent GC

    /**
     * Should a reload of configuration be performed due to the given event? 
     */
    private static boolean isReloadEvent( final ApplicationEvent event ) {
        boolean reload = false;

        if ( event instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            Class entityClass = eie.getEntityClass();
            if ( entityClass != null ) {
                if ( ClusterProperty.class.isAssignableFrom( entityClass ) ||
                     IdentityProviderConfig.class.isAssignableFrom( entityClass )) {
                    reload = true;
                }
            }
        }

        return reload;
    }

    /**
     * Update identity providers by wrapping the group manager with a caching
     * interceptor and updating the mapped kerberos attribute if one is
     * specified by a cluster property.
     */
    private static void updateLdapIdentityProviders( final ApplicationContext context ) {
        try {
            ClusterPropertyManager cpm = (ClusterPropertyManager) context.getBean( "clusterPropertyManager", ClusterPropertyManager.class );
            IdentityProviderConfigManager ipcm = (IdentityProviderConfigManager) context.getBean( "identityProviderConfigManager", IdentityProviderConfigManager.class );
            IdentityProviderFactory ipf = (IdentityProviderFactory) context.getBean( "identityProviderFactory", IdentityProviderFactory.class );
            Collection<IdentityProvider> ips = ipf.findAllIdentityProviders( ipcm );

            for (IdentityProvider ip : ips) {
                if ( ip instanceof LdapIdentityProvider ) {
                    LdapIdentityProvider lip = (LdapIdentityProvider) ip;
                    String providerName = lip.getConfig().getName();

                    if ( !Boolean.getBoolean( DISABLE_CACHE_PROPERTY ) ) {
                        if ( !(ip.getGroupManager() instanceof CachingLdapGroupManager) ) {
                            logger.config( "Installing caching group manager for identity provider '"+providerName+"'." );
                            lip.setGroupManager( new CachingLdapGroupManager( lip, lip.getGroupManager() ) );
                        }
                    }

                    String customAttribute = cpm.getProperty( CLUSTER_PROPERTY_PREFIX + providerName );
                    if ( customAttribute == null ) customAttribute = LIP_KERBEROS_FIELD_DEFAULT;
                    logger.config( "Configuring kerberos LDAP attribute mapping for '"+providerName+"' as '"+customAttribute+"'.");
                    LdapAttributeMapping lmap = new LdapAttributeMapping();
                    Method attributeNameSetter;
                    try {
                        attributeNameSetter = lmap.getClass().getMethod( SET_ATTR_NAME_42, String.class );
                    } catch (NoSuchMethodException nsme) {
                        attributeNameSetter = lmap.getClass().getMethod( SET_ATTR_NAME, String.class );
                    }
                    attributeNameSetter.invoke( lmap, customAttribute );

                    Field kmapField;
                    try {
                        // "e" in obfuscated build
                        kmapField = lip.getClass().getDeclaredField( LIP_KERBEROS_FIELD_OBFS );
                    } catch ( NoSuchFieldException nsfe ) {
                        kmapField = lip.getClass().getDeclaredField( LIP_KERBEROS_FIELD );
                    }
                    kmapField.setAccessible( true );
                    kmapField.set( lip, lmap );
                }
            }
        } catch (Exception e) {
            logger.log( Level.WARNING, "Error configuring LDAP identity providers", e );
        }
    }
}
