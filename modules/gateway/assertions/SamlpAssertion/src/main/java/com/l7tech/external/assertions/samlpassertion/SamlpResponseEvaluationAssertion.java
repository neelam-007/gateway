package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationAssertion extends SamlProtocolAssertion implements SetsVariables {

    // new stuff
    private boolean responseStatusFalsifyAssertion = true;
    private Integer authzDecisionOption;
    private String authzDecisionVariable;
    private boolean authzDecisionFalsifyAssertion = true;
    private String variablePrefixOverride;

    public SamlpResponseEvaluationAssertion() {
        initTargetMessage();
    }

    public SamlpResponseEvaluationAssertion(SamlAuthenticationStatement authnStmt) {
        this.authenticationStatement = authnStmt;
    }

    public SamlpResponseEvaluationAssertion(SamlAttributeStatement attrStmt) {
        this.attributeStatement = attrStmt;
    }

    public SamlpResponseEvaluationAssertion(SamlpAuthorizationStatement authzStmt) {
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


    public String[] getVariablesUsed() {
        Set<String> varNames = new HashSet<String>();
        collectVars(varNames, audienceRestriction);
        collectVars(varNames, nameQualifier);
        // new stuff
        collectVars(varNames, getTargetName());
        if (attributeStatement != null) {
            for (SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes()) {
                collectVars(varNames, attr.getNamespace());
                collectVars(varNames, attr.getNameFormat());
                collectVars(varNames, attr.getName());
                collectVars(varNames, attr.getValue());
            }
        }

        if (authorizationStatement != null) {
            for (String action : authorizationStatement.getActions()) {
                collectVars(varNames, action);
            }
            collectVars(varNames, authorizationStatement.getResource());
        }

        // TODO how could one parameterize the authentication statement at all?
        return varNames.toArray(new String[0]);
    }

    private void collectVars(Set<String> varNames, String s) {
        if (s == null || s.length() == 0) return;
        String[] vars = Syntax.getReferencedNames(s);
        varNames.addAll(Arrays.asList(vars));
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


    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.SamlpResponseEvaluationAssertionPropertiesEditor");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, SamlpResponseEvaluationAssertion>() {
            public String call(SamlpResponseEvaluationAssertion assertion) {
                StringBuilder sb = new StringBuilder("SAMLP Evaluator");

                if (assertion.getAuthenticationStatement() != null)
                    sb.append(" (Authentication)");
                else if (assertion.getAuthorizationStatement() != null)
                    sb.append(" (Authorization Decision)");
                else if (assertion.getAttributeStatement() != null)
                    sb.append(" (Attribute Query)");

                return sb.toString();
            }
        });

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new BeanTypeMapping(SamlpAuthorizationStatement.class, "samlpAuthorizationInfo"),
            new ArrayTypeMapping(new String[0], "actions")
        )));
        
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, SamlpResponseEvaluationAssertionValidator.class.getName());
        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "SAMLP Evaluator");
        meta.put(AssertionMetadata.LONG_NAME, "Evaluate a SAMLP response message");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    public VariableMetadata[] getVariablesSet() {

        ArrayList<VariableMetadata> varList = new ArrayList<VariableMetadata>();

        varList.add(new VariableMetadata("samlpResponse.status", false, true, null, false, DataType.STRING));

        if (getAuthorizationStatement() != null) {
            varList.add(new VariableMetadata("samlpResponse.authz.decision", false, false, null, false, DataType.STRING));

        } else if (getAttributeStatement() != null) {
            varList.add(new VariableMetadata("samlpResponse.attribute", true, false, null, false, DataType.UNKNOWN));
        }

        VariableMetadata[] fullList = new VariableMetadata[varList.size()];
        fullList = varList.toArray(fullList);
        return fullList;
    }
}
