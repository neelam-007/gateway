package com.l7tech.external.assertions.salesforceinstaller;

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
 * 
 */
public class SalesforceInstallerAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(SalesforceInstallerAssertion.class.getName());
    private static final String META_INITIALIZED = SalesforceInstallerAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final PolicyBundleInstallerAdmin instance;
                try {
                    instance = new SalesforceInstallerAdminImpl("/com/l7tech/external/assertions/salesforceinstaller/bundles/", "SalesforceBundleInfo.xml", "http://ns.l7tech.com/2013/02/salesforce-bundle", appContext);
                    final Injector injector = appContext.getBean("injector", Injector.class);
                    injector.inject(instance);
                } catch (PolicyBundleInstallerAdmin.PolicyBundleInstallerException e) {
                    logger.warning("Could not load Salesforce Installer: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                }
                final ExtensionInterfaceBinding<PolicyBundleInstallerAdmin> binding = new ExtensionInterfaceBinding<>(PolicyBundleInstallerAdmin.class,  SalesforceInstallerAssertion.class.getName(), instance);
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{ "com.l7tech.external.assertions.salesforceinstaller.console.SalesforceInstallerAction" });
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAdminImpl");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
