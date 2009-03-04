package com.l7tech.external.assertions.ftpcredential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.logging.Logger;

/**
 * Assertion that requires FTP credentials are present.
 *
 * @author Steve Jones
 */
public class FtpCredentialAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(FtpCredentialAssertion.class.getName());

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return new String[0]; //ExpandVariables.getReferencedNames(...);
    }

    /**
     * The FTP Credential assertion is always a credential source
     *
     * @return true
     */
    public boolean isCredentialSource() {
        return true;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = FtpCredentialAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (!Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
    
            // Set description for GUI
            meta.put(AssertionMetadata.SHORT_NAME, "FTP authentication");
            meta.put(AssertionMetadata.LONG_NAME, "The requestor must provide credentials using FTP authentication");

            // Add to palette folder(s)
            //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
            //   misc, audit, policyLogic, threatProtection
            meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
            meta.put(AssertionMetadata.PALETTE_NODE_NAME, "FTP Credentials");
            meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

            // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
            meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "none");

            // Set up smart Getter for nice, informative policy node name, for GUI
            meta.put(AssertionMetadata.POLICY_NODE_NAME, "Require FTP Authentication");
            meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

            // request default feature set name for our class name, since we are a known optional module
            // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

}
