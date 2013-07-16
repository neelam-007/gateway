package com.l7tech.external.assertions.authandmgmtserviceinstaller;

import com.l7tech.policy.assertion.*;
import java.util.logging.Logger;

/**
 * The assertion class for API Portal Authentication and Management Service Installer.
 *
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtServiceInstallerAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(ApiPortalAuthAndMgmtServiceInstallerAssertion.class.getName());

    public String[] getVariablesUsed() {
        return new String[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ApiPortalAuthAndMgmtServiceInstallerAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Do not show this assertion in the palette.
        //meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.authandmgmtserviceinstaller.console.ApiPortalAuthAndMgmtServiceInstallerAction" });

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}