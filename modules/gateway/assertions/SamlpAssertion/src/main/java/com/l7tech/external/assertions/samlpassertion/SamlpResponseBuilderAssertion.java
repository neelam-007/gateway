package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class SamlpResponseBuilderAssertion extends MessageTargetableAssertion implements PrivateKeyable{

    public SamlVersion getSamlVersion() {
        return samlVersion;
    }

    public void setSamlVersion(SamlVersion samlVersion) {
        this.samlVersion = samlVersion;
    }

    public boolean isSignResponse() {
        return signResponse;
    }

    public void setSignResponse(boolean signResponse) {
        this.signResponse = signResponse;
    }

    public SamlStatus getSamlStatus() {
        return samlStatus;
    }

    public void setSamlStatus(SamlStatus samlStatus) {
        this.samlStatus = samlStatus;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getIssueInstant() {
        return issueInstant;
    }

    public void setIssueInstant(String issueInstant) {
        this.issueInstant = issueInstant;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public void setInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getConsent() {
        return consent;
    }

    public void setConsent(String consent) {
        this.consent = consent;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getResponseAssertions() {
        return responseAssertions;
    }

    public void setResponseAssertions(String responseAssertions) {
        this.responseAssertions = responseAssertions;
    }

    public String getEncryptedAssertions() {
        return encryptedAssertions;
    }

    public void setEncryptedAssertions(String encryptedAssertions) {
        this.encryptedAssertions = encryptedAssertions;
    }

    public String getResponseExtensions() {
        return responseExtensions;
    }

    public void setResponseExtensions(String responseExtensions) {
        this.responseExtensions = responseExtensions;
    }

    public boolean isAddIssuer() {
        return addIssuer;
    }

    public void setAddIssuer(boolean addIssuer) {
        this.addIssuer = addIssuer;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        final String baseName = "Build SAML Protocol Response";
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Build a SAML Protocol Response with optional signing. This assertion is compatible with the Web Browser SSO Profile of SAML.");
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(PROPERTIES_ACTION_NAME, "SAML Protocol Response Properties");

        meta.put(POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<SamlpResponseBuilderAssertion>(){
            @Override
            public String getAssertionName( final SamlpResponseBuilderAssertion assertion, final boolean decorate ) {
                if(!decorate) return baseName;

                if(!assertion.isSignResponse()) return AssertionUtils.decorateName(assertion, baseName);

                return AssertionUtils.decorateName(assertion, baseName +"; Sign samlp:Response");
            }
        } );

        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping( SamlVersion.class, "samlVersion" ),
                new Java5EnumTypeMapping( SamlStatus.class, "samlStatus" )
        ) ));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>();

        StringBuilder builder = new StringBuilder();
        builder.append(getStatusMessage());
        builder.append(getStatusDetail());

        builder.append(getResponseId());
        builder.append(getIssueInstant());
        builder.append(getInResponseTo());

        builder.append(getResponseAssertions());

        switch (getSamlVersion()){

            case SAML2:
                builder.append(getDestination());
                builder.append(getConsent());
                builder.append(getResponseExtensions());
                builder.append(getEncryptedAssertions());
                break;
            case SAML1_1:
                builder.append(getRecipient());
        }

        final String[] refVars = Syntax.getReferencedNames(builder.toString());
        vars.addAll(Arrays.asList(refVars));
        
        return vars.toArray(new String[vars.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if(this.getTarget() == TargetMessageType.OTHER){
            List<VariableMetadata> list = new ArrayList<VariableMetadata>();
            final String targetVar = this.getOtherTargetMessageVariable();
            VariableMetadata meta = new VariableMetadata(targetVar, false, false, this.getOtherTargetMessageVariable(), true, DataType.MESSAGE);
            list.addAll(Arrays.asList(super.getVariablesSet()));
            list.add(meta);
            return list.toArray(new VariableMetadata[list.size()]);

        }
        return super.getVariablesSet();
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privateKeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        privateKeyableSupport.setUsesDefaultKeyStore(usesDefault);
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privateKeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        privateKeyableSupport.setNonDefaultKeystoreId(nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return privateKeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyid) {
        privateKeyableSupport.setKeyAlias(keyid);
    }

    // - PRIVATE
    private SamlVersion samlVersion = SamlVersion.SAML2;
    private boolean signResponse;
    private boolean addIssuer;
    private SamlStatus samlStatus = SamlStatus.SAML2_SUCCESS;
    private String statusMessage;
    private String statusDetail;

    private String responseId;//used for both versions (ID in SAML 2.0 and ResponseId on SAML 1.1)
    private String issueInstant;
    private String inResponseTo;
    private String destination;
    private String consent;
    private String recipient;

    private String responseAssertions;
    private String encryptedAssertions;
    private String responseExtensions;
    private final PrivateKeyableSupport privateKeyableSupport = new PrivateKeyableSupport();

    private static final String META_INITIALIZED = SamlpResponseBuilderAssertion.class.getName() + ".metadataInitialized";
}
