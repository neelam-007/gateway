package com.l7tech.external.assertions.simplepolicybundleinstaller;

import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.util.Injector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Modular entry point to configure this installer.
 */
public class SimplePolicyBundleInstallerAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(SimplePolicyBundleInstallerAssertion.class.getName());
    private static final String META_INITIALIZED = SimplePolicyBundleInstallerAssertion.class.getName() + ".metadataInitialized";

    PolicyBundleInstallerAdmin policyBundleInstallerAdmin;

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                try {
                    policyBundleInstallerAdmin = new SimplePolicyBundleInstallerAdminImpl("/com/l7tech/external/assertions/simplepolicybundleinstaller/bundles/", "SimplePolicyBundleInfo.xml", "http://ns.l7tech.com/2013/10/simple-policy-bundle", appContext);
                    final Injector injector = appContext.getBean("injector", Injector.class);
                    injector.inject(policyBundleInstallerAdmin);
                } catch (PolicyBundleInstallerAdmin.PolicyBundleInstallerException e) {
                    logger.warning("Could not load Simple Policy Bundle Installer: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                }
                final ExtensionInterfaceBinding<PolicyBundleInstallerAdmin> binding = new ExtensionInterfaceBinding<>(PolicyBundleInstallerAdmin.class,  SimplePolicyBundleInstallerAssertion.class.getName(), policyBundleInstallerAdmin);
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{ "com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerAction" });
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAdminImpl");

        // this assertion is for a development only, leave as "set:modularAssertion", no need to change to "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
