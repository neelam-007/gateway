package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationContext;

import java.util.Set;

/**
 * Interface providing access to needed modular and/or custom assertions registry.
 */
public interface ScannerCallbacks {
    /**
     * Notify all listeners registered with this application of an application-specific event.<br/>
     * Typically it would either be {@link AssertionModuleRegistrationEvent} or {@link AssertionModuleUnregistrationEvent}.
     *
     * @param applicationEvent    the application-specific event.
     */
    void publishEvent(@NotNull ApplicationEvent applicationEvent);

    /**
     * Interface defining callbacks specific for modular assertions.
     */
    public interface ModularAssertion extends ScannerCallbacks {
        /**
         * Scan all modular assertions, associated with the module .aar file, for new cluster properties and register them.
         * @param assertionPrototypes    the set of assertion prototypes, within the .aar file.
         */
        void registerClusterProps(@NotNull Set<? extends Assertion> assertionPrototypes);

        /**
         * Register modular assertion, specified with <tt>aClass</tt> class object.
         * @param aClass    the custom assertion class object.
         */
        void registerAssertion(@NotNull Class<? extends Assertion> aClass);

        /**
         * Unregister modular assertion, specified with <tt>prototype</tt> assertion instance.
         * @param prototype    the assertion instance object to unregister.
         */
        void unregisterAssertion(@NotNull Assertion prototype);

        /**
         * Gather the application context instance.
         * @return spring application context instance.   Never null.
         */
        @NotNull ApplicationContext getApplicationContext();

        /**
         * Determine if the assertion, specified with the <tt>assClassName</tt> class-name, is registered with this registry.
         * @param assClassName    the assertion concrete class-name to check.
         * @return <code>true</code> if this class-name is registered with this registry, <code>false</code> otherwise.
         */
        boolean isAssertionRegistered(@NotNull String assClassName);
    }

    /**
     * Interface defining callbacks specific for modular assertions.
     */
    public interface CustomAssertion extends ScannerCallbacks {
        /**
         * Register custom assertion, specified with the <tt>descriptor</tt>.<br/>
         * For custom assertions the registration process includes registering the assertion descriptor into
         * {@link com.l7tech.server.policy.custom.CustomAssertions CustomAssertions} collection as well as registering
         * custom assertion extension interface.
         *
         * @param descriptor     The custom assertion descriptor. 
         * @param classLoader    The custom assertion class-loader.
         * @throws ModuleException thrown if an error happens during registration process i.e. to indicate registration failure.
         */
        void registerAssertion(@NotNull CustomAssertionDescriptor descriptor, @NotNull ClassLoader classLoader) throws ModuleException;

        /**
         * Unregister custom assertion, specified with the <tt>descriptor</tt>.<br/>
         * Unregister the assertion descriptor from {@link com.l7tech.server.policy.custom.CustomAssertions CustomAssertions} collection.
         *
         * @param descriptor    The custom assertion descriptor.
         */
        void unregisterAssertion(@NotNull CustomAssertionDescriptor descriptor);

        /**
         * Provide access to the {@link ServiceFinder} for locating Layer 7 API Services available to assertions.<br/>
         * For available services see the Layer 7 API documentation.
         */
        ServiceFinder getServiceFinder();
    }
}
