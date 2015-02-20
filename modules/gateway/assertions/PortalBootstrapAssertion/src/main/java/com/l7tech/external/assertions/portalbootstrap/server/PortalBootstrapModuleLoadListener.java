package com.l7tech.external.assertions.portalbootstrap.server;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Initializes Portal Bootstrap extension.
 */
public class PortalBootstrapModuleLoadListener {

    public static synchronized void onModuleLoaded( @NotNull ApplicationContext context ) {
        PortalBootstrapManager.initialize( context );
    }

}
