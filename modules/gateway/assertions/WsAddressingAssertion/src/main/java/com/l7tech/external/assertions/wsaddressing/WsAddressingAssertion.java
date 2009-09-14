package com.l7tech.external.assertions.wsaddressing;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.util.EnumSet;
import java.util.Set;

/**
 * Assertion for WS-Addressing.
 *
 * <p>Can be used to require a version of WS-Addressing in the request.</p>
 *
 * <p>Optionally sets variables for the message properties found.</p> 
 */
@RequiresSOAP
public class WsAddressingAssertion extends MessageTargetableAssertion implements IdentityTargetable, SetsVariables, SecurityHeaderAddressable, UsesEntities {
    
    //- PUBLIC
    
    public static final String VAR_SUFFIX_TO = "to";
    public static final String VAR_SUFFIX_ACTION = "action";
    public static final String VAR_SUFFIX_MESSAGEID = "messageid";
    public static final String VAR_SUFFIX_FROM = "from";
    public static final String VAR_SUFFIX_REPLYTO= "replyto";
    public static final String VAR_SUFFIX_FAULTTO = "faultto";
    public static final String VAR_SUFFIX_NAMESPACE = "namespace";
    public static final String VAR_SUFFIX_ELEMENTS = "elements";

    /**
     * Create a WS-Addressing assertion with default values;
     */
    public WsAddressingAssertion() {
        setEnableWsAddressing10(true);
    }

    public String[] getVariableSuffixes() {
        return new String[] {
            VAR_SUFFIX_TO,
            VAR_SUFFIX_ACTION,
            VAR_SUFFIX_MESSAGEID,
            VAR_SUFFIX_FROM,
            VAR_SUFFIX_REPLYTO,
            VAR_SUFFIX_FAULTTO,
            VAR_SUFFIX_NAMESPACE,
            VAR_SUFFIX_ELEMENTS    
        };
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
    @Override
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
                new VariableMetadata(prefix + "." + VAR_SUFFIX_ELEMENTS, false, true, null, false, DataType.ELEMENT),
            };
        } else {
            return new VariableMetadata[0];
        }
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget(IdentityTarget identityTarget) {
        this.identityTarget = identityTarget;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }    

    /**
     * Get the meta data for this assertion.
     *
     * @return The metadata for this assertion
     */
    @Override
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
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;

    private final static String baseName ="Require WS-Addressing";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WsAddressingAssertion>(){
        @Override
        public String getAssertionName( final WsAddressingAssertion assertion, final boolean decorate) {
            StringBuilder sb = new StringBuilder("Require ");

            if ( assertion.isRequireSignature() ) {
                sb.append("signed ");
            }

            sb.append("WS-Addressing");

            return (decorate)? AssertionUtils.decorateName(assertion, sb): baseName;
        }
    };

    /**
     * Populate the given metadata.
     */
    private void populateMeta(final DefaultAssertionMetadata meta) {
        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Require WS-Addressing with optional signing.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, WsAddressingAssertion>(){
            @Override
            public Set<ValidatorFlag> call(WsAddressingAssertion assertion) {
                Set<ValidatorFlag> flags = EnumSet.noneOf(ValidatorFlag.class);

                if ( assertion.isRequireSignature() ) {
                    flags.add(ValidatorFlag.REQUIRE_SIGNATURE);
                }

                // Suppress warning on non-local WSS recipient since this assertion will still enforce presence
                // of WS-Addressing headers in the SOAP Header.  (They don't go in the Security header.)
                flags.add(ValidatorFlag.PROCESSES_NON_LOCAL_WSS_RECIPIENT);

                return flags;
            }
        });

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Addressing Properties");
        
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.wsaddressing.WsAddressingAssertionValidator");
        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/Information16.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, true);

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:WsAddressing" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
    }
}
