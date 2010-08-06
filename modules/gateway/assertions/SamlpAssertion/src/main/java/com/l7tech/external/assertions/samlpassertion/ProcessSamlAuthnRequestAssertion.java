package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Assertion for processing SAML protocol AuthnRequest messages.
 */
public class ProcessSamlAuthnRequestAssertion extends MessageTargetableAssertion {

    //- PUBLIC

    public static final String SUFFIX_SUBJECT = "subject";
    public static final String SUFFIX_X509CERT_BASE64 = "x509CertBase64";
    public static final String SUFFIX_X509CERT = "x509Cert";
    public static final String SUFFIX_ACS_URL = "acsUrl";
    public static final String SUFFIX_ID = "id";
    public static final String SUFFIX_VERSION = "version";
    public static final String SUFFIX_ISSUE_INSTANT = "issueInstant";
    public static final String SUFFIX_DESTINATION = "destination";
    public static final String SUFFIX_CONSENT = "consent";
    public static final String SUFFIX_ISSUER = "issuer";
    public static final String SUFFIX_ISSUER_NAME_QUALIFIER = "issuer.nameQualifier";
    public static final String SUFFIX_ISSUER_SP_NAME_QUALIFIER = "issuer.spNameQualifier";
    public static final String SUFFIX_ISSUER_FORMAT = "issuer.format";
    public static final String SUFFIX_ISSUER_SP_PROVIDED_ID = "issuer.spProvidedId";
    public static final String SUFFIX_EXTENSIONS = "extensions";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection( Arrays.asList(
        SUFFIX_SUBJECT,
        SUFFIX_X509CERT_BASE64,
        SUFFIX_X509CERT,
        SUFFIX_ACS_URL,
        SUFFIX_ID,
        SUFFIX_VERSION,
        SUFFIX_ISSUE_INSTANT,
        SUFFIX_DESTINATION,
        SUFFIX_CONSENT,
        SUFFIX_ISSUER,
        SUFFIX_ISSUER_NAME_QUALIFIER,
        SUFFIX_ISSUER_SP_NAME_QUALIFIER,
        SUFFIX_ISSUER_FORMAT,
        SUFFIX_ISSUER_SP_PROVIDED_ID,
        SUFFIX_EXTENSIONS
    ) );

    public String getAudienceRestriction() {
        return audienceRestriction;
    }

    public void setAudienceRestriction( final String audienceRestriction ) {
        this.audienceRestriction = audienceRestriction;
    }

    public boolean isCheckValidityPeriod() {
        return checkValidityPeriod;
    }

    public void setCheckValidityPeriod( final boolean checkValidityPeriod ) {
        this.checkValidityPeriod = checkValidityPeriod;
    }

    public SamlProtocolBinding getSamlProtocolBinding() {
        return samlProtocolBinding;
    }

    public void setSamlProtocolBinding( final SamlProtocolBinding samlProtocolBinding ) {
        this.samlProtocolBinding = samlProtocolBinding;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
    }

    public boolean isVerifySignature() {
        return verifySignature;
    }

    public void setVerifySignature( final boolean verifySignature ) {
        this.verifySignature = verifySignature;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Process and optionally validate a SAML authentication request.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Process Authentication Request Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.ProcessSamlAuthnRequestPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<ProcessSamlAuthnRequestAssertion>(){
            @Override
            public String getAssertionName( final ProcessSamlAuthnRequestAssertion assertion, boolean decorate) {
                if(!decorate) return baseName;
                final StringBuilder sb = new StringBuilder(baseName);

                if ( assertion.getSamlProtocolBinding() != null ) {
                    switch ( assertion.getSamlProtocolBinding() ) {
                        case HttpPost:
                            sb.append( ", HTTP Post Binding" );
                            break;
                        case HttpRedirect:
                            sb.append( ", HTTP Redirect Binding" );
                            break;
                    }
                }

                if ( assertion.isVerifySignature() ) {
                    sb.append( "; Verify Signature" );
                }

                return AssertionUtils.decorateName(assertion, sb);
            }
        });

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder( Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping( SamlProtocolBinding.class, "samlProtocolBinding")
        )));
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] metadata;

        if ( variablePrefix == null ) {
            metadata = new VariableMetadata[0];
        } else {
            metadata = new VariableMetadata[] {
                new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_X509CERT_BASE64, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_X509CERT, false, false, null, false, DataType.CERTIFICATE),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ACS_URL, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_VERSION, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUE_INSTANT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_DESTINATION, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_CONSENT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUER_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUER_SP_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUER_FORMAT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_ISSUER_SP_PROVIDED_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+SUFFIX_EXTENSIONS, false, false, null, false, DataType.MESSAGE),
            };
        }

        return metadata;
    }

    public enum SamlProtocolBinding {
        HttpPost,
        HttpRedirect
    }

    //- PRIVATE

    private static final String baseName = "Process SAML Authentication Request";

    private String variablePrefix = "authnRequest";
    private boolean checkValidityPeriod = true;
    private String audienceRestriction;
    private boolean verifySignature = true;
    private SamlProtocolBinding samlProtocolBinding = null;
}
