package com.l7tech.external.assertions.pbkvs.server;

import com.l7tech.objectmodel.polback.KeyValueStore;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Invoked when module is loaded.
 */
public class ModuleLoadListener {

    /**
     * Called reflectively when this .aar file is loaded.
     *
     * @param context spring context.  Required.
     */
    public static synchronized void onModuleLoaded( @NotNull ApplicationContext context ) {
        PolicyBackedServiceRegistry pbsreg = context.getBean( "policyBackedServiceRegistry", PolicyBackedServiceRegistry.class );
        pbsreg.registerPolicyBackedServiceTemplate( KeyValueStore.class );
    }
}
