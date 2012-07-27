package com.l7tech.external.assertions.generateoauthsignaturebasestring;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Assertion which generates an OAuth signature base string that conforms to the OAuth 1.0 spec.
 */
public class GenerateOAuthSignatureBaseStringAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String SIG_BASE_STRING = "sigBaseString";
    public static final String REQUEST_TYPE = "requestType";
    public static final String OAUTH_CALLBACK = "oauth_callback";
    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    public static final String OAUTH_NONCE = "oauth_nonce";
    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_VERIFIER = "oauth_verifier";
    public static final String OAUTH_VERSION = "oauth_version";
    public static final String AUTH_HEADER = "authHeader";
    public static final String AUTO = "<auto>";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(this.getVariablePrefix() + "." + SIG_BASE_STRING),
                new VariableMetadata(this.getVariablePrefix() + "." + REQUEST_TYPE),
                new VariableMetadata(this.getVariablePrefix() + "." + AUTH_HEADER),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_CONSUMER_KEY),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_SIGNATURE_METHOD),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_TIMESTAMP),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_NONCE),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_VERSION),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_TOKEN),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_CALLBACK),
                new VariableMetadata(this.getVariablePrefix() + "." + OAUTH_VERIFIER)};
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(requestUrl, httpMethod, queryString, authorizationHeader, oauthConsumerKey,
                oauthTimestamp, oauthNonce, oauthToken, oauthCallback, oauthVerifier, oauthVersion);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }
        meta.put(AssertionMetadata.SHORT_NAME, "Generate OAuth Signature Base String");
        meta.put(AssertionMetadata.LONG_NAME, "Generate OAuth Signature Base String for use in OAuth 1.0 A Signature generation");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.generateoauthsignaturebasestring.console.GenerateOAuthSignatureBaseStringPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }

    public boolean isUseMessageTarget() {
        return useMessageTarget;
    }

    public void setUseMessageTarget(final boolean useMessageTarget) {
        this.useMessageTarget = useMessageTarget;
    }

    public boolean isUseManualParameters() {
        return useManualParameters;
    }

    public void setUseManualParameters(final boolean useManualParameters) {
        this.useManualParameters = useManualParameters;
    }

    public boolean isUseAuthorizationHeader() {
        return useAuthorizationHeader;
    }

    public void setUseAuthorizationHeader(final boolean useAuthorizationHeader) {
        this.useAuthorizationHeader = useAuthorizationHeader;
    }

    public boolean isUseOAuthVersion() {
        return useOAuthVersion;
    }

    public void setUseOAuthVersion(final boolean useOAuthVersion) {
        this.useOAuthVersion = useOAuthVersion;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(final String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public String getOauthConsumerKey() {
        return oauthConsumerKey;
    }

    public void setOauthConsumerKey(final String oauthConsumerKey) {
        this.oauthConsumerKey = oauthConsumerKey;
    }

    public String getOauthSignatureMethod() {
        return oauthSignatureMethod;
    }

    public void setOauthSignatureMethod(final String oauthSignatureMethod) {
        this.oauthSignatureMethod = oauthSignatureMethod;
    }

    public String getOauthTimestamp() {
        return oauthTimestamp;
    }

    public void setOauthTimestamp(final String oauthTimestamp) {
        this.oauthTimestamp = oauthTimestamp;
    }

    public String getOauthNonce() {
        return oauthNonce;
    }

    public void setOauthNonce(final String oauthNonce) {
        this.oauthNonce = oauthNonce;
    }

    public String getOauthVersion() {
        return oauthVersion;
    }

    public void setOauthVersion(final String oauthVersion) {
        this.oauthVersion = oauthVersion;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(final String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public String getOauthCallback() {
        return oauthCallback;
    }

    public void setOauthCallback(final String oauthCallback) {
        this.oauthCallback = oauthCallback;
    }

    public String getOauthVerifier() {
        return oauthVerifier;
    }

    public void setOauthVerifier(final String oauthVerifier) {
        this.oauthVerifier = oauthVerifier;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public MessageTargetableSupport getMessageTargetableSupport() {
        return messageTargetableSupport;
    }

    private static final String META_INITIALIZED = GenerateOAuthSignatureBaseStringAssertion.class.getName() + ".metadataInitialized";
    private String requestUrl = "${request.url}";
    private String httpMethod = "${request.http.method}";
    private String queryString;
    /**
     * True if request parameters should be included in the signature base string.
     */
    private boolean useMessageTarget = true;
    /**
     * True if the assertion fields should be included in the signature base string.
     */
    private boolean useManualParameters = true;
    /**
     * True if authorization header parameters should be included in the signature base string.
     */
    private boolean useAuthorizationHeader = true;
    /**
     * True if the oauth version field should be included in the signature base string (only applies if useManualParameters=true).
     */
    private boolean useOAuthVersion = true;
    private String authorizationHeader = "${request.http.header.Authorization}";
    private String oauthConsumerKey;
    private String oauthSignatureMethod = "HMAC-SHA1";
    private String oauthTimestamp = "<auto>";
    private String oauthNonce = AUTO;
    private String oauthVersion = "1.0";
    private String oauthToken;
    private String oauthCallback;
    private String oauthVerifier;
    private String variablePrefix = "oauth";
    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport();
}
