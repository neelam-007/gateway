package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class SamlpResponseBuilderAssertion extends MessageTargetableAssertionPrivateKeyable implements SamlIssuerConfig {

    public SamlpResponseBuilderAssertion(){
        setTargetModifiedByGateway(true);
        setSourceUsedByGateway(false);
    }

    /**
     * Create a new instance of SamlpResponseBuilderAssertion with default properties configured.
     * <p/>
     * Supports new validateWebSsoRules property default value of false for new assertion instances.
     *
     * @return A new instance with default properties. Never null.
     */
    @NotNull
    public static SamlpResponseBuilderAssertion newInstance() {
        SamlpResponseBuilderAssertion responseBuilderAssertion = new SamlpResponseBuilderAssertion();
        responseBuilderAssertion.setValidateWebSsoRules(false);
        return responseBuilderAssertion;
    }

    /**
     * Left in for backwards compatibility
     * @param samlVersion samlVersion from old policy xml
     */
    @Deprecated
    public void setSamlVersion(SamlVersion samlVersion) {
        switch (samlVersion) {
            case SAML2:
                version = 2;
                break;
            case SAML1_1:
                version = 1;
                break;
            default:
                throw new IllegalArgumentException("Unsupported SAML version: " + samlVersion);
        }
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isSignResponse() {
        return signResponse;
    }

    public void setSignResponse(boolean signResponse) {
        this.signResponse = signResponse;
    }

    @Override
    public String getCustomIssuerValue() {
        return issuerValue;
    }

    @Override
    public void setCustomIssuerValue(@Nullable String customIssuerValue) {
        this.issuerValue = customIssuerValue;
    }

    @Override
    public String getCustomIssuerFormat() {
        return issuerFormat;
    }

    @Override
    public void setCustomIssuerFormat(@Nullable String customIssuerFormat) {
        this.issuerFormat = customIssuerFormat;
    }

    @Override
    public String getCustomIssuerNameQualifier() {
        return issuerNameQualifier;
    }

    @Override
    public void setCustomIssuerNameQualifier(@Nullable String customIssuerNameQualifier) {
        this.issuerNameQualifier = customIssuerNameQualifier;
    }

    public String getSamlStatusCode() {
        return samlStatusCode;
    }

    public void setSamlStatusCode(@NotNull String samlStatusCode) {
        this.samlStatusCode = samlStatusCode;
    }

    /**
     * Deprecated. Required for backwards compatibility.
     * @param samlStatus status to set
     */
    @Deprecated
    public void setSamlStatus(@NotNull SamlStatus samlStatus) {
        this.samlStatusCode = samlStatus.getValue();
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

    @Override
    public boolean samlProtocolUsage() {
        return true;
    }

    @Override
    public boolean includeIssuer() {
        return addIssuer;
    }

    /**
     * Independent property separate from the interface defining includeIssuer()
     * @return if user configured the Issuer to be added. Will never apply to SAML 1.1
     */
    public boolean isAddIssuer() {
        return addIssuer;
    }

    public void setAddIssuer(boolean addIssuer) {
        this.addIssuer = addIssuer;
    }

    @Override
    public void includeIssuer(boolean includeIssuer) {
        this.addIssuer = includeIssuer;
    }

    public boolean isValidateWebSsoRules() {
        return validateWebSsoRules;
    }

    public void setValidateWebSsoRules(boolean validateWebSsoRules) {
        this.validateWebSsoRules = validateWebSsoRules;
    }

    public boolean isIncludeSignerCertChain() {
        return includeSignerCertChain;
    }

    public void setIncludeSignerCertChain( boolean includeSignerCertChain ) {
        this.includeSignerCertChain = includeSignerCertChain;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        final String baseName = "Build SAML Protocol Response";
        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Build a SAML Protocol Response with optional signing. Optionally enable Web Browser SSO Profile rules validation.");
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

        meta.put(ASSERTION_FACTORY, new Functions.Unary<SamlpResponseBuilderAssertion, SamlpResponseBuilderAssertion>(){
            @Override
            public SamlpResponseBuilderAssertion call(final SamlpResponseBuilderAssertion responseBuilderAssertion) {
                return newInstance();
            }
        });

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
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed variablesUsed = super.doGetVariablesUsed().withExpressions(
                getSamlStatusCode(),
                getStatusMessage(),
                getStatusDetail(),
                getResponseId(),
                getIssueInstant(),
                getInResponseTo(),
                getResponseAssertions()
        );

        switch (getVersion()){
            case 2:
                variablesUsed.addExpressions(
                        getDestination(),
                        getConsent(),
                        getResponseExtensions(),
                        getEncryptedAssertions(),
                        getCustomIssuerValue(),
                        getCustomIssuerFormat(),
                        getCustomIssuerNameQualifier());
                break;
            case 1:
                variablesUsed.addExpressions( getRecipient() );
        }

        return variablesUsed;
    }

    // - PRIVATE
    private Integer version = 2;
    private boolean signResponse;
    private boolean addIssuer;
    private String samlStatusCode = SamlStatus.SAML2_SUCCESS.getValue();
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

    /**
     * Default of true for backwards compatibility, when Web SSO rules were validated by default.
     *
     * See {@link #newInstance()} which configures this to false for new instances.
     */
    private boolean validateWebSsoRules = true;

    /**
     * Allow a custom Issuer value via the assertion.
     */
    private String issuerValue;
    private String issuerFormat;
    private String issuerNameQualifier;

    /**
     * Include the signer's full certificate chain in the KeyInfo rather than just the signer cert (SSG-6065).
     * <p/>
     * Default of false for backwards compatibility.
     */
    private boolean includeSignerCertChain = false;


    private static final String META_INITIALIZED = SamlpResponseBuilderAssertion.class.getName() + ".metadataInitialized";
}
