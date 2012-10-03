package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.external.assertions.apiportalintegration.server.upgrade.UpgradePortalAdmin;
import com.l7tech.external.assertions.apiportalintegration.server.upgrade.UpgradePortalAdminImpl;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * Assertion whose only purpose is to register an Upgrade Portal task.
 */
public class UpgradePortalAssertion extends Assertion {
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{"com.l7tech.external.assertions.apiportalintegration.console.UpgradePortalAction"});
        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<UpgradePortalAdmin>(
                        UpgradePortalAdmin.class,
                        null,
                        new UpgradePortalAdminImpl(appContext));
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });
        return meta;
    }
}
