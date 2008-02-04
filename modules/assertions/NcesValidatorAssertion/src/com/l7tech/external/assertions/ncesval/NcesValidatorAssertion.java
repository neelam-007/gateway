package com.l7tech.external.assertions.ncesval;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Validates that messages are NCES-compliant.  This implies the following:
 * <ul>
 * <li>A wsu:Timestamp is present
 * <li>A wsa:MessageID is present
 * <li>An optional SAML Assertion is present
 * <li>A ds:Signature is present, valid and covers all of the above.
 * </ul>
 */
@RequiresSOAP(wss=true)
public class NcesValidatorAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(NcesValidatorAssertion.class.getName());

    private boolean samlRequired;
    private TargetMessageType target = TargetMessageType.REQUEST;
    private String otherMessageVariableName;

    public TargetMessageType getTarget() {
        return target;
    }

    public void setTarget(TargetMessageType target) {
        this.target = target;
    }

    public String getOtherMessageVariableName() {
        return otherMessageVariableName;
    }

    public void setOtherMessageVariableName(String otherMessageVariableName) {
        this.otherMessageVariableName = otherMessageVariableName;
    }

    public boolean isSamlRequired() {
        return samlRequired;
    }

    public void setSamlRequired(boolean samlRequired) {
        this.samlRequired = samlRequired;
    }

    public String[] getVariablesUsed() {
        if (otherMessageVariableName != null) return new String[] { otherMessageVariableName };
        return new String[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = NcesValidatorAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "NCES Validator");
        meta.put(AssertionMetadata.LONG_NAME, "Validate Message for NCES Compliance");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:NcesValidator" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, NcesValidatorAssertionValidator.class.getName());

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
