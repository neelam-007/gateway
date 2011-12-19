package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.external.assertions.samlpassertion.server.ProtocolRequestUtilities;
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

import static com.l7tech.external.assertions.samlpassertion.server.ProtocolRequestUtilities.*;

/**
 * Assertion for processing SAML protocol AuthnRequest messages.
 */
public class ProcessSamlAuthnRequestAssertion extends MessageTargetableAssertion {

    //- PUBLIC

    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection( Arrays.asList(
        SUFFIX_SUBJECT,
        SUFFIX_SUBJECT_NAME_QUALIFIER,
        SUFFIX_SUBJECT_SP_NAME_QUALIFIER,
        SUFFIX_SUBJECT_FORMAT,
        SUFFIX_SUBJECT_SP_PROVIDED_ID,
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
        SUFFIX_REQUEST
    ) );

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
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Process a SAML 2.0 authentication request with optional signing, context variables are set for extracted values. This assertion is compatible with the HTTP POST and HTTP Redirect bindings and the Web Browser SSO Profile.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Authentication Request Properties");
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

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public enum SamlProtocolBinding {
        HttpPost("HTTP Post"),
        HttpRedirect("HTTP Redirect");

        private final String description;

        private SamlProtocolBinding( final String description ) {
            this.description = description;
        }

        public String toString() {
            return description;
        }
    }

    //- PROTECTED

    @Override
    protected VariablesSet doGetVariablesSet() {
        return variablePrefix == null ?
                super.doGetVariablesSet() :
                super.doGetVariablesSet().withVariables(
                        new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT, false, false, null, false, DataType.STRING),
                        new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                        new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT_SP_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                        new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT_FORMAT, false, false, null, false, DataType.STRING),
                        new VariableMetadata(variablePrefix+"."+SUFFIX_SUBJECT_SP_PROVIDED_ID, false, false, null, false, DataType.STRING),
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
                        new VariableMetadata(variablePrefix+"."+SUFFIX_REQUEST, false, false, null, false, DataType.MESSAGE)
                );
    }

    //- PRIVATE

    private static final String META_INITIALIZED = ProcessSamlAuthnRequestAssertion.class.getName() + ".metadataInitialized";

    private static final String baseName = "Process SAML Authentication Request";

    private String variablePrefix = "authnRequest";
    private boolean verifySignature = true;
    private SamlProtocolBinding samlProtocolBinding = null;
}
