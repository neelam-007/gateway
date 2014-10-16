package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.external.assertions.policybundleexporter.server.PolicyBundleExporterAdminImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.util.Injector;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * 
 */
public class PolicyBundleExporterAssertion extends Assertion {
    //
    // Metadata
    //
    private static final String META_INITIALIZED = PolicyBundleExporterAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final PolicyBundleExporterAdmin instance;
                instance = new PolicyBundleExporterAdminImpl(appContext);
                final Injector injector = appContext.getBean("injector", Injector.class);
                injector.inject(instance);
                final ExtensionInterfaceBinding<PolicyBundleExporterAdmin> binding = new ExtensionInterfaceBinding<>(PolicyBundleExporterAdmin.class,  PolicyBundleExporterAssertion.class.getName(), instance);
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{"com.l7tech.external.assertions.policybundleexporter.console.PolicyBundleExporterAction"});
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterLifecycle");

        // TODO change to "(fromClass)" when Exporter is no longer beta Layer 7 internal.  This will set feature set as "assertion:PolicyBundleExporter" (will also need to add to GatewayFeatureSets).
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
