package com.l7tech.external.assertions.createroutingstrategy.server;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Initializes RoutingStrategyManager.
 */
public class ModuleLoadListener {

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(@NotNull ApplicationContext context) {
        RoutingStrategyManager.createInstance(context);
    }
}
