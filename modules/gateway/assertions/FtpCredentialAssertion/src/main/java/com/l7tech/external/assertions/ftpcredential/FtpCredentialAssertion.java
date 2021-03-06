package com.l7tech.external.assertions.ftpcredential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.logging.Logger;

/**
 * Assertion that requires FTP credentials are present.
 *
 * @author Steve Jones
 */
public class FtpCredentialAssertion extends Assertion {
    protected static final Logger logger = Logger.getLogger(FtpCredentialAssertion.class.getName());

    /**
     * The FTP Credential assertion is always a credential source
     *
     * @return true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = FtpCredentialAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (!Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
    
            // Set description for GUI
            meta.put(AssertionMetadata.SHORT_NAME, "Require FTP Credentials");
            meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide credentials using FTP authentication");

            // Add to palette folder(s)
            //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
            //   misc, audit, policyLogic, threatProtection
            meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
            meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

            // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
            meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "none");

            meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

            // request default feature set name for our class name, since we are a known optional module
            // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

}
