package com.l7tech.external.assertions.whichmodule;

import com.l7tech.external.assertions.whichmodule.server.GenericEntityManagerDemoServerSupport;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * A hidden assertion that regisers the demo generic entity extension interface and GUI action.
 */
public class GenericEntityManagerDemoAssertion extends Assertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.whichmodule.console.ManageDemoGenericEntitiesAction" });

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return GenericEntityManagerDemoServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        return meta;
    }
}
