/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.common.security.saml.KeyInfoInclusionType;
import com.l7tech.common.security.saml.NameIdentifierInclusionType;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alex
 */
public class SamlIssuerAssertion extends SamlPolicyAssertion implements SetsVariables, UsesVariables {
    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    private boolean signAssertion = true;
    private String subjectConfirmationMethodUri;
    private NameIdentifierInclusionType nameIdentifierType = NameIdentifierInclusionType.FROM_CREDS;
    private String nameIdentifierFormat;
    private String nameIdentifierValue;
    private KeyInfoInclusionType signatureKeyInfoType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = KeyInfoInclusionType.CERT;

    public NameIdentifierInclusionType getNameIdentifierType() {
        return nameIdentifierType;
    }

    public void setNameIdentifierType(NameIdentifierInclusionType nameIdentifierType) {
        this.nameIdentifierType = nameIdentifierType;
    }

    public String getSubjectConfirmationMethodUri() {
        return subjectConfirmationMethodUri;
    }

    public void setSubjectConfirmationMethodUri(String subjectConfirmationMethodUri) {
        this.subjectConfirmationMethodUri = subjectConfirmationMethodUri;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public String[] getVariablesUsed() {
        Set<String> varNames = new HashSet<String>();
        collectVars(varNames, nameIdentifierFormat);
        collectVars(varNames, nameIdentifierValue);
        collectVars(varNames, subjectConfirmationMethodUri);
        collectVars(varNames, audienceRestriction);
        collectVars(varNames, nameQualifier);
        if (attributeStatement != null) {
            for (SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes()) {
                collectVars(varNames, attr.getNamespace());
                collectVars(varNames, attr.getNameFormat());
                collectVars(varNames, attr.getName());
                collectVars(varNames, attr.getValue());
            }
        } else if (authorizationStatement != null) {
            collectVars(varNames, authorizationStatement.getAction());
            collectVars(varNames, authorizationStatement.getActionNamespace());
            collectVars(varNames, authorizationStatement.getResource());
        } else {
            // TODO how could one parameterize the authentication statement at all?
        }
        return varNames.toArray(new String[0]);
    }

    private void collectVars(Set<String> varNames, String s) {
        if (s == null || s.length() == 0) return;
        String[] vars = ExpandVariables.getReferencedNames(s);
        varNames.addAll(Arrays.asList(vars));
    }

    public int getConditionsNotBeforeSecondsInPast() {
        return conditionsNotBeforeSecondsInPast;
    }

    public void setConditionsNotBeforeSecondsInPast(int conditionsNotBeforeSecondsInPast) {
        this.conditionsNotBeforeSecondsInPast = conditionsNotBeforeSecondsInPast;
    }

    public int getConditionsNotOnOrAfterExpirySeconds() {
        return conditionsNotOnOrAfterExpirySeconds;
    }

    public void setConditionsNotOnOrAfterExpirySeconds(int conditionsNotOnOrAfterExpirySeconds) {
        this.conditionsNotOnOrAfterExpirySeconds = conditionsNotOnOrAfterExpirySeconds;
    }

    public void setNameIdentifierFormat(String formatUri) {
        this.nameIdentifierFormat = formatUri;
    }

    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    public void setNameIdentifierValue(String value) {
        this.nameIdentifierValue = value;
    }

    public String getNameIdentifierValue() {
        return nameIdentifierValue;
    }

    public KeyInfoInclusionType getSubjectConfirmationKeyInfoType() {
        return subjectConfirmationKeyInfoType;
    }

    public void setSubjectConfirmationKeyInfoType(KeyInfoInclusionType subjectConfirmationKeyInfoType) {
        this.subjectConfirmationKeyInfoType = subjectConfirmationKeyInfoType;
    }

    public KeyInfoInclusionType getSignatureKeyInfoType() {
        return signatureKeyInfoType;
    }

    public void setSignatureKeyInfoType(KeyInfoInclusionType signatureKeyInfoType) {
        this.signatureKeyInfoType = signatureKeyInfoType;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.saml.SamlIssuerAssertionPropertiesEditor");
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(NameIdentifierInclusionType.class, "nameIdentifierType"),
                new Java5EnumTypeMapping(KeyInfoInclusionType.class, "subjectConfirmationKeyInfoType")
                )));
        return meta;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] { new VariableMetadata("issuedSamlAssertion", false, false, null, false, DataType.STRING) };
    }
}
