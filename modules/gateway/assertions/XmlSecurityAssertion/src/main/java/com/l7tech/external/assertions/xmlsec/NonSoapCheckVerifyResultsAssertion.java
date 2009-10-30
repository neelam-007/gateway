package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.util.Functions;

import java.util.*;

/**
 * Utility assertion for more easily checking the results of a NonSoapVerifyElementAssertion.
 */
public class NonSoapCheckVerifyResultsAssertion extends NonSoapSecurityAssertionBase implements HasVariablePrefix {
    private static final String META_INITIALIZED = NonSoapCheckVerifyResultsAssertion.class.getName() + ".metadataInitialized";

    protected String variablePrefix = "";
    private boolean gatherCertificateCredentials = true;
    private boolean allowMultipleSigners = false;
    private String[] permittedSignatureMethodUris = getDefaultPermittedSignatureMethodUris();
    private String[] permittedDigestMethodUris = getDefaultPermittedDigestMethodUris();

    /**
     * Create an assertion with default configuration.
     */
    public NonSoapCheckVerifyResultsAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> variables = new ArrayList<String>();
        variables.addAll( Arrays.asList( super.getVariablesUsed() ) );
        variables.addAll(Arrays.asList(
                prefix(NonSoapVerifyElementAssertion.VAR_ELEMENTS_VERIFIED),
                prefix(NonSoapVerifyElementAssertion.VAR_DIGEST_METHOD_URIS),
                prefix(NonSoapVerifyElementAssertion.VAR_SIGNATURE_METHOD_URIS),
                prefix(NonSoapVerifyElementAssertion.VAR_SIGNATURE_VALUES),
                prefix(NonSoapVerifyElementAssertion.VAR_SIGNING_CERTIFICATES),
                prefix(NonSoapVerifyElementAssertion.VAR_SIGNATURE_ELEMENTS)
        ));
        return variables.toArray( new String[variables.size()] );
    }

    @Override
    public String getDefaultXpathExpressionString() {
        return "//ElementsThatShouldHaveBeenSigned";
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public boolean isCredentialSource() {
        return isGatherCertificateCredentials();
    }

    public static String[] getDefaultPermittedSignatureMethodUris() {
        Set<String> uris = new HashSet<String>(Functions.map(SupportedSignatureMethods.getDefaultMethods(), new Functions.Unary<String, SupportedSignatureMethods>() {
            @Override
            public String call(SupportedSignatureMethods supportedSignatureMethods) {
                return supportedSignatureMethods.getAlgorithmIdentifier();
            }
        }));
        return uris.toArray(new String[uris.size()]);
    }

    public static String[] getDefaultPermittedDigestMethodUris() {
        Set<String> got = new HashSet<String>(Functions.map(SupportedSignatureMethods.getDefaultMethods(), new Functions.Unary<String, SupportedSignatureMethods>() {
            @Override
            public String call(SupportedSignatureMethods supportedSignatureMethods) {
                return supportedSignatureMethods.getMessageDigestIdentifier();
            }
        }));
        return got.toArray(new String[got.size()]);
    }

    /**
     * @return true if the assertion should gather the signing certificate as request credentials.
     */
    public boolean isGatherCertificateCredentials() {
        return gatherCertificateCredentials;
    }

    /**
     * @param gatherCertificateCredentials true if the assertion should gather the signing certificate as request credentials.
     */
    public void setGatherCertificateCredentials(boolean gatherCertificateCredentials) {
        this.gatherCertificateCredentials = gatherCertificateCredentials;
    }

    /**
     * @return true if the assertion should gather more than one credential if an element is covered by more than one signature.
     *              If false, assertion will fail instead.
     */
    public boolean isAllowMultipleSigners() {
        return allowMultipleSigners;
    }

    /**
     * @param allowMultipleSigners true if the assertion should gather more than one credential if an element is covered by more than one signature.
     *              If false, assertion will fail instead.
     */
    public void setAllowMultipleSigners(boolean allowMultipleSigners) {
        this.allowMultipleSigners = allowMultipleSigners;
    }

    /**
     * @return array of SignatureMethod algorithm URIs that are to be permitted.  May be empty, but never null.
     */
    public String[] getPermittedSignatureMethodUris() {
        return permittedSignatureMethodUris;
    }

    /**
     * @param permittedSignatureMethodUris array of SignatureMethod algorithm URIs that are to be permitted.
     */
    public void setPermittedSignatureMethodUris(String[] permittedSignatureMethodUris) {
        if (permittedSignatureMethodUris == null)
            permittedSignatureMethodUris = new String[0];
        this.permittedSignatureMethodUris = permittedSignatureMethodUris;
    }

    /**
     * @return array of DigestMethod algorithm URIs that are to be permitted.  May be empty, but never null.
     */
    public String[] getPermittedDigestMethodUris() {
        return permittedDigestMethodUris;
    }

    /**
     * @param permittedDigestMethodUris array of DigestMethod algorithm URIs that are to be permitted.
     */
    public void setPermittedDigestMethodUris(String[] permittedDigestMethodUris) {
        if (permittedDigestMethodUris == null)
            permittedDigestMethodUris = new String[0];
        this.permittedDigestMethodUris = permittedDigestMethodUris;
    }

    public String prefix(String var) {
        String prefix = getVariablePrefix();
        return prefix == null || prefix.trim().length() < 1 ? var : prefix.trim() + "." + var;
    }

    private final static String baseName = "Check Non-SOAP XML Verify Results";

    @Override
    public String getDisplayName() {
        return baseName;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(META_PROP_VERB, "check");
        meta.put(AssertionMetadata.DESCRIPTION, "Check the results of verifying a non-SOAP XML signature to see if expected elements were signed.  " +
                                                "This does not require a SOAP Envelope. This requires context variables from a Non-SOAP XML Verify assertion " +
                                                "(but does not examine or produce WS-Security processor results). " +
                                                "The XPath should match the elements which are expected to have been signed.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1150);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapCheckVerifyResultsAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, NonSoapCheckVerifyResultsValidator.class.getName());

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
