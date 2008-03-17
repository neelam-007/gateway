package com.l7tech.external.assertions.wsaddressing;

import com.l7tech.common.util.Functions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Collections;
import java.util.Set;

/**
 * Assertion for WS-Addressing.
 *
 * <p>Can be used to require a version of WS-Addressing in the request.</p>
 *
 * <p>Optionally sets variables for the message properties found.</p> 
 */
public class WsAddressingAssertion extends Assertion implements SetsVariables {

    //- PUBLIC
    
    public static final String VAR_SUFFIX_TO = "to";
    public static final String VAR_SUFFIX_ACTION = "action";
    public static final String VAR_SUFFIX_MESSAGEID = "messageid";
    public static final String VAR_SUFFIX_FROM = "from";
    public static final String VAR_SUFFIX_REPLYTO= "replyto";
    public static final String VAR_SUFFIX_FAULTTO = "faultto";
    public static final String VAR_SUFFIX_NAMESPACE = "namespace";

    /**
     * Create a WS-Addressing assertion with default values;
     */
    public WsAddressingAssertion() {
        setEnableWsAddressing10(true);
    }

    /**
     * Is WS-Addressing 1.0 enabled.
     *
     * @return True if enabled.
     */
    public boolean isEnableWsAddressing10() {
        return enableWsAddressing10;
    }

    /**
     * Enable or disable WS-Addressing 1.0.
     *
     * @param enableWsAddressing10 True to enable
     */
    public void setEnableWsAddressing10(boolean enableWsAddressing10) {
        this.enableWsAddressing10 = enableWsAddressing10;
    }

    /**
     * Is WS-Addressing 2008/08 enabled.
     *
     * @return True if enabled.
     */
    public boolean isEnableWsAddressing200408() {
        return enableWsAddressing200408;
    }

    /**
     * Enable or disable WS-Addressing 2008/08.
     *
     * @param enableWsAddressing200408 True to enable
     */
    public void setEnableWsAddressing200408(boolean enableWsAddressing200408) {
        this.enableWsAddressing200408 = enableWsAddressing200408;
    }

    /**
     * Is a signature required for WS-Addressing headers.
     *
     * @return True if required.
     */
    public boolean isRequireSignature() {
        return requireSignature;
    }

    /**
     * Set WS-Addressing header signature requirements.
     *
     * @param requireSignature True to require a signature.
     */
    public void setRequireSignature(boolean requireSignature) {
        this.requireSignature = requireSignature;
    }

    /**
     * Get the variable prefix (may be null)
     *
     * @return The variable prefix
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    /**
     * Set the variable prefix (may be null)
     *
     * <p>If the prefix is null then no variables will be set.</p>
     *
     * @param variablePrefix The prefix to use for variable names (may be null)
     */
    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public String getEnableOtherNamespace() {
        return enableOtherNamespace;
    }

    public void setEnableOtherNamespace(String enableOtherNamespace) {
        this.enableOtherNamespace = enableOtherNamespace;
    }

    /**
     * If a prefix is set then this will return a full set of addressing variables.
     *
     * @return The variable metadata
     */
    public VariableMetadata[] getVariablesSet() {
        String prefix = getVariablePrefix();

        if ( prefix != null ) {
            return new VariableMetadata[] {
                // Note default prefixes are used here for property lookup purposes
                new VariableMetadata(prefix + "." + VAR_SUFFIX_TO, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_ACTION, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_MESSAGEID, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_FROM, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_REPLYTO, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_FAULTTO, false, false, null, false, DataType.STRING),
                new VariableMetadata(prefix + "." + VAR_SUFFIX_NAMESPACE, false, false, null, false, DataType.STRING),
            };
        } else {
            return new VariableMetadata[0];
        }
    }

    /**
     * Get the meta data for this assertion.
     *
     * @return The metadata for this assertion
     */
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (!Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            populateMeta(meta);
            meta.put(META_INITIALIZED, Boolean.TRUE);
        }

        return meta;
    }

    //- PRIVATE

    // Metadata flag
    private static final String META_INITIALIZED = WsAddressingAssertion.class.getName() + ".metadataInitialized";

    private boolean enableWsAddressing10 = true;
    private boolean enableWsAddressing200408 = false;
    private String variablePrefix;
    private boolean requireSignature;
    private String enableOtherNamespace = null;


    /**
     * Populate the given metadata.
     */
    private void populateMeta(final DefaultAssertionMetadata meta) {
        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Require WS-Addressing");
        meta.put(AssertionMetadata.LONG_NAME, "Require WS-Addressing in the request message.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, WsAddressingAssertion>() {
            public String call(WsAddressingAssertion addressingAssertion) {
                StringBuilder sb = new StringBuilder("Require ");

                if ( addressingAssertion.isRequireSignature() ) {
                    sb.append("signed ");
                }

                sb.append("WS-Addressing in request");

                return sb.toString();
            }
        });

        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, WsAddressingAssertion>(){
            public Set<ValidatorFlag> call(WsAddressingAssertion assertion) {
                Set<ValidatorFlag> flags = Collections.emptySet();

                if ( assertion.isRequireSignature() ) {
                    flags = Collections.singleton(ValidatorFlag.REQUIRE_SIGNATURE);
                }

                return flags;
            }
        });

        meta.put(AssertionMetadata.USED_BY_CLIENT, true);

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WsAddressing" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
    }
}
