package com.l7tech.external.assertions.oauthinstaller;

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
 * Assertion used to install the OTK policies.
 */
public class OAuthInstallerAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(OAuthInstallerAssertion.class.getName());

    private static final String META_INITIALIZED = OAuthInstallerAssertion.class.getName() + ".metadataInitialized";
    public static final String SECURE_ZONE_STORAGE_COMP_ID = "b082274b-f00e-4fbf-bbb7-395a95ca2a35";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Do not show this assertion in the palette.
//        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.oauthinstaller.console.OAuthInstallerAction" });

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final OAuthInstallerAdmin instance;
                try {
                    instance = new OAuthInstallerAdminImpl("/com/l7tech/external/assertions/oauthinstaller/bundles/", "OAuthToolkitBundleInfo.xml", "http://ns.l7tech.com/2012/11/oauth-toolkit-bundle", appContext);
                    final Injector injector = appContext.getBean("injector", Injector.class);
                    injector.inject(instance);
                } catch (PolicyBundleInstallerAdmin.PolicyBundleInstallerException e) {
                    logger.warning("Could not load OAuth Toolkit Installer: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                }

                final ExtensionInterfaceBinding<OAuthInstallerAdmin> binding = new ExtensionInterfaceBinding<>(OAuthInstallerAdmin.class, null, instance);
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdminImpl");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
