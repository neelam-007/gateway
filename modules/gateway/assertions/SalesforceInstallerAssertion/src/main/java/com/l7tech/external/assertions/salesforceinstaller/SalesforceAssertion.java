package com.l7tech.external.assertions.salesforceinstaller;

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

public class SalesforceAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(SalesforceAssertion.class.getName());
    private static final String META_INITIALIZED = SalesforceAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Do not show this assertion in the palette.
        // meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        // meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.salesforceinstaller.console.SalesforceInstallerAction" });

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final String bundleBaseName = "/com/l7tech/external/assertions/salesforceinstaller/bundles/";
                final SalesforceInstallerAdmin instance;
                try {
//                    AllModulesClassLoader allModulesClassLoader = appContext.getBean("allModulesClassLoader", AllModulesClassLoader.class);
//                    allModulesClassLoader.loadClass("com.l7tech.custom.salesforce.assertion.SalesforceOperationAssertion");
//
////                    Class.forName("com.l7tech.custom.salesforce.assertion.SalesforceOperationAssertion", false, SalesforceInstallerAdminImpl.class.getClassLoader());
//
                    instance = new SalesforceInstallerAdminImpl(bundleBaseName, appContext);
                    final Injector injector = appContext.getBean("injector", Injector.class);
                    injector.inject(instance);
//                } catch (ClassNotFoundException e) {
//                    logger.warning("Salesforce Toolkit jar are not installed on this Gateway: " + ExceptionUtils.getMessage(e));
//                    throw new RuntimeException("Salesforce Toolkit jar are not installed on this Gateway");
                } catch (SalesforceInstallerAdmin.SalesforceInstallationException e) {
                    logger.warning("Could not load Salesforce Toolkit: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                }

                final ExtensionInterfaceBinding<SalesforceInstallerAdmin> binding = new ExtensionInterfaceBinding<SalesforceInstallerAdmin>(SalesforceInstallerAdmin.class, null, instance);
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAdminImpl");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
