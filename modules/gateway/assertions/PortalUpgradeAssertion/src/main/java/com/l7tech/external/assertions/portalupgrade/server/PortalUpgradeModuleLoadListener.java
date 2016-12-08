package com.l7tech.external.assertions.portalupgrade.server;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Initializes Portal Bootstrap extension.
 */
public class PortalUpgradeModuleLoadListener {

    public static synchronized void onModuleLoaded( @NotNull ApplicationContext context ) {
        PortalUpgradeManager.initialize( context );
    }

}
