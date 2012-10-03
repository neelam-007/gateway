package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;

/**
 * Holds the ApiKeyManager instance, until we find a way to stash it in the application context.
 */
public class ApiKeyManagerFactory {
    private static PortalGenericEntityManager<ApiKeyData> instance;

    public static final PortalGenericEntityManager<ApiKeyData> getInstance() {
        return instance;
    }

    public static void setInstance(final PortalGenericEntityManager<ApiKeyData> instance) {
        ApiKeyManagerFactory.instance = instance;
    }
}
