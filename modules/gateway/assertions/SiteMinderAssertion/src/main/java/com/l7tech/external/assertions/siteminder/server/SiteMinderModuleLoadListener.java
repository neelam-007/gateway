package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderAgentContextCache;
import com.ca.siteminder.SiteMinderAgentContextCacheManager;
import com.l7tech.external.assertions.siteminder.SiteMinderExternalReferenceFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ca.siteminder.SiteMinderAgentContextCacheManager.*;
import static com.l7tech.server.ServerConfig.PropertyRegistrationInfo.prInfo;
import static com.l7tech.util.CollectionUtils.list;

public class SiteMinderModuleLoadListener {
    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static SiteMinderExternalReferenceFactory externalReferenceFactory;
    private static SiteMinderAgentContextCacheManager siteMinderAgentContextCacheManager;

    private static final List<ServerConfig.PropertyRegistrationInfo> MODULE_CLUSTER_PROPERTIES = list(
        prInfo( SM_USE_GLOBAL_CACHE_PROP, SM_USE_GLOBAL_CACHE_CLUSTER_PROP, SM_USE_GLOBAL_CACHE_DESC, String.valueOf(SM_USE_GLOBAL_CACHE_DEFAULT) ),
        prInfo( SM_RESOURCE_CACHE_SIZE_PROP, SM_RESOURCE_CACHE_SIZE_CLUSTER_PROP, SM_RESOURCE_CACHE_SIZE_DESC, String.valueOf(SM_RESOURCE_CACHE_SIZE_DEFAULT) ),
        prInfo( SM_RESOURCE_CACHE_MAX_AGE_PROP, SM_RESOURCE_CACHE_MAX_AGE_CLUSTER_PROP, SM_RESOURCE_CACHE_MAX_AGE_DESC, String.valueOf(SM_RESOURCE_CACHE_MAX_AGE_DEFAULT) ),
        prInfo( SM_AUTHENTICATION_CACHE_SIZE_PROP, SM_AUTHENTICATION_CACHE_SIZE_CLUSTER_PROP, SM_AUTHENTICATION_CACHE_SIZE_DESC, String.valueOf(SM_AUTHENTICATION_CACHE_SIZE_DEFAULT) ),
        prInfo( SM_AUTHENTICATION_CACHE_MAX_AGE_PROP, SM_AUTHENTICATION_CACHE_MAX_AGE_CLUSTER_PROP, SM_AUTHENTICATION_CACHE_MAX_AGE_DESC, String.valueOf(SM_AUTHENTICATION_CACHE_MAX_AGE_DEFAULT) ),
        prInfo( SM_AUTHORIZATION_CACHE_SIZE_PROP, SM_AUTHORIZATION_CACHE_SIZE_CLUSTER_PROP, SM_AUTHORIZATION_CACHE_SIZE_DESC, String.valueOf(SM_AUTHORIZATION_CACHE_SIZE_DEFAULT) ),
        prInfo( SM_AUTHORIZATION_CACHE_MAX_AGE_PROP, SM_AUTHORIZATION_CACHE_MAX_AGE_CLUSTER_PROP, SM_AUTHORIZATION_CACHE_MAX_AGE_DESC, String.valueOf(SM_AUTHORIZATION_CACHE_MAX_AGE_DEFAULT) )
    );

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        registerExternalReferenceFactory(context);
        initializeModuleClusterProperties(context);
        updateDefaultCacheSettings(context);
    }

    public static synchronized void onModuleUnloaded() {
        unregisterExternalReferenceFactory();
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory(); // ensure not already registered

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new SiteMinderExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory!=null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }

    private static void updateDefaultCacheSettings(final ApplicationContext context) {
        final ServerConfig serverConfig = context.getBean("serverConfig", ServerConfig.class);
        final SiteMinderAgentContextCacheManager siteMinderAgentContextCacheManager = context.getBean("siteMinderAgentContextCacheManager", SiteMinderAgentContextCacheManager.class);

        int resourceCacheSize = serverConfig.getIntProperty(SM_RESOURCE_CACHE_SIZE_PROP, SM_RESOURCE_CACHE_SIZE_DEFAULT);
        long resourceCacheMaxAgeMillis = serverConfig.getLongProperty(SM_RESOURCE_CACHE_MAX_AGE_PROP, SM_RESOURCE_CACHE_MAX_AGE_DEFAULT);

        int authenticationCacheSize = serverConfig.getIntProperty(SM_AUTHENTICATION_CACHE_SIZE_PROP, SM_AUTHENTICATION_CACHE_SIZE_DEFAULT);
        long authenticationCacheMaxAgeMillis = serverConfig.getLongProperty(SM_AUTHENTICATION_CACHE_MAX_AGE_PROP, SM_AUTHENTICATION_CACHE_MAX_AGE_DEFAULT);

        int authorizationCacheSize = serverConfig.getIntProperty(SM_AUTHORIZATION_CACHE_SIZE_PROP, SM_AUTHORIZATION_CACHE_SIZE_DEFAULT);
        long authorizationCacheMaxAgeMillis = serverConfig.getLongProperty(SM_AUTHORIZATION_CACHE_MAX_AGE_PROP, SM_AUTHORIZATION_CACHE_MAX_AGE_DEFAULT);

        boolean useGlobalCache = serverConfig.getBooleanProperty(SM_USE_GLOBAL_CACHE_PROP, SM_USE_GLOBAL_CACHE_DEFAULT);



        siteMinderAgentContextCacheManager.setDefaultCacheSettings(
                resourceCacheSize, resourceCacheMaxAgeMillis,
                authenticationCacheSize, authenticationCacheMaxAgeMillis,
                authorizationCacheSize, authorizationCacheMaxAgeMillis,
                useGlobalCache);
    }

    private static void initializeModuleClusterProperties(final ApplicationContext context) {
        final ServerConfig serverConfig = context.getBean("serverConfig", ServerConfig.class);
        final Map<String, String> names = serverConfig.getClusterPropertyNames();
        final List<ServerConfig.PropertyRegistrationInfo> toAdd = new ArrayList<>();
        for ( final ServerConfig.PropertyRegistrationInfo info : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey( info.getName() )) {
                // create it
                toAdd.add(info);
            }
        }
        serverConfig.registerServerConfigProperties( toAdd );
    }
}