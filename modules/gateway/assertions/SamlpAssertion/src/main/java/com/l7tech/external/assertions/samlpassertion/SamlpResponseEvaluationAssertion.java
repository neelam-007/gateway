package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.Arrays;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationAssertion extends SamlProtocolAssertion implements SetsVariables {

    private static final String META_INITIALIZED = SamlpResponseEvaluationAssertion.class.getName() + ".metadataInitialized";

    // new stuff
    private boolean responseStatusFalsifyAssertion = true;
    private Integer authzDecisionOption;
    private String authzDecisionVariable;
    private boolean authzDecisionFalsifyAssertion = true;
    private String variablePrefixOverride;

    public SamlpResponseEvaluationAssertion() {
        super(false);
        initTargetMessage();
    }

    public SamlpResponseEvaluationAssertion(SamlAuthenticationStatement authnStmt) {
        super(false);
        this.authenticationStatement = authnStmt;
    }

    public SamlpResponseEvaluationAssertion(SamlAttributeStatement attrStmt) {
        super(false);
        this.attributeStatement = attrStmt;
    }

    public SamlpResponseEvaluationAssertion(SamlpAuthorizationStatement authzStmt) {
        super(false);
        this.authorizationStatement = authzStmt;
    }

    private void initTargetMessage() {
        this.setTarget(TargetMessageType.OTHER);
        this.setOtherTargetMessageVariable("samlpResponse.message");
    }

    public boolean isResponseStatusFalsifyAssertion() {
        return responseStatusFalsifyAssertion;
    }

    public void setResponseStatusFalsifyAssertion(boolean responseStatusFalsifyAssertion) {
        this.responseStatusFalsifyAssertion = responseStatusFalsifyAssertion;
    }

    public Integer getAuthzDecisionOption() {
        return authzDecisionOption;
    }

    public void setAuthzDecisionOption(Integer authzDecisionOption) {
        this.authzDecisionOption = authzDecisionOption;
    }

    public String getAuthzDecisionVariable() {
        return authzDecisionVariable;
    }

    public void setAuthzDecisionVariable(String authzDecisionVariable) {
        this.authzDecisionVariable = authzDecisionVariable;
    }

    public boolean isAuthzDecisionFalsifyAssertion() {
        return authzDecisionFalsifyAssertion;
    }

    public void setAuthzDecisionFalsifyAssertion(boolean authzDecisionFalsifyAssertion) {
        this.authzDecisionFalsifyAssertion = authzDecisionFalsifyAssertion;
    }

    public String getVariablePrefixOverride() {
        return variablePrefixOverride;
    }

    public void setVariablePrefixOverride(String variablePrefixOverride) {
        this.variablePrefixOverride = variablePrefixOverride;
    }


    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed variablesUsed = super.doGetVariablesUsed().withExpressions(
            audienceRestriction,
            nameQualifier,
            getTargetName()
        );

        if ( attributeStatement != null ) {
            for ( final SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes() ) {
                variablesUsed.addExpressions(
                        attr.getNamespace(),
                        attr.getNameFormat(),
                        attr.getName(),
                        attr.getValue()
                );
            }
        }

        if ( authorizationStatement != null ) {
            variablesUsed.addExpressions(authorizationStatement.getActions());
            variablesUsed.addExpressions(authorizationStatement.getResource());
        }

        // TODO how could one parameterize the authentication statement at all?
        return variablesUsed;
    }

    protected boolean usesDefaultKeyStore = true;
    protected long nonDefaultKeystoreId;
    protected String keyId;

    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    public String getKeyAlias() {
        return keyId;
    }

    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    final static String baseName = "Evaluate SAML Protocol Response";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SamlpResponseEvaluationAssertion>(){
        @Override
        public String getAssertionName( final SamlpResponseEvaluationAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder sb = new StringBuilder(baseName);

            if (assertion.getAuthenticationStatement() != null)
                sb.append(" (Authentication)");
            else if (assertion.getAuthorizationStatement() != null)
                sb.append(" (Authorization Decision)");
            else if (assertion.getAttributeStatement() != null)
                sb.append(" (Attribute Query)");

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Evaluate a SAML Protocol response.");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Protocol Response Wizard");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.SamlpResponseEvaluationAssertionPropertiesEditor");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new BeanTypeMapping(SamlpAuthorizationStatement.class, "samlpAuthorizationInfo"),
            new ArrayTypeMapping(new String[0], "actions")
        )));
        
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata("samlpResponse.status", false, true, null, false, DataType.STRING),
                getAuthorizationStatement() != null ? new VariableMetadata("samlpResponse.authz.decision", false, false, null, false, DataType.STRING) : null,
                getAttributeStatement() != null ? new VariableMetadata("samlpResponse.attribute", true, false, null, false, DataType.UNKNOWN) : null
        );
    }
}
