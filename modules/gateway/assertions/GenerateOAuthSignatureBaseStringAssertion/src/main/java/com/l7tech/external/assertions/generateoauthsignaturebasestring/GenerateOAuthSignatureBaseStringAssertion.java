package com.l7tech.external.assertions.generateoauthsignaturebasestring;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * Assertion which generates an OAuth signature base string that conforms to the OAuth 1.0 spec.
 */
public class GenerateOAuthSignatureBaseStringAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String SIG_BASE_STRING = "sigBaseString";
    public static final String REQUEST_TYPE = "requestType";
    public static final String HMAC_SHA1 = "HMAC-SHA1";
    public static final String OAUTH_1_0 = "1.0";
    public static final String OAUTH_CALLBACK = "oauth_callback";
    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    public static final String OAUTH_NONCE = "oauth_nonce";
    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_VERIFIER = "oauth_verifier";
    public static final String OAUTH_VERSION = "oauth_version";
    public static final String AUTH_HEADER = "authHeader";
    public static final String REQUEST_TOKEN = "request token";
    public static final String AUTHORIZED_REQUEST_TOKEN = "authorized request token";
    public static final String ACCESS_TOKEN = "access token";
    public static final String ERROR = "error";

    public static enum UsageMode {
        /**
         * Sending an OAuth request.
         *
         * OAuth parameters can be retrieved via query string or assertion fields.
         */
        CLIENT("Client"),

        /**
         * Receiving an OAuth request.
         *
         * OAuth parameters can be retrieved via query string, authorization header, and/or request parameters.
         */
        SERVER("Server");

        UsageMode(final String description) {
            this.description = description;
        }

        private String description;

        public String getDescription() {
            return description;
        }
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(this.getVariablePrefix() + "." + SIG_BASE_STRING),
                new VariableMetadata(this.getVariablePrefix() + "." + REQUEST_TYPE),
                new VariableMetadata(this.getVariablePrefix() + "." + AUTH_HEADER),
                new VariableMetadata(this.getVariablePrefix() + "." + ERROR),
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
                oauthToken, oauthCallback, oauthVerifier);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }
        meta.put(AssertionMetadata.SHORT_NAME, GENERATE_OAUTH_SIGNATURE_BASE_STRING);
        meta.put(AssertionMetadata.LONG_NAME, "Generate OAuth Signature Base String for use in OAuth 1.0 A Signature generation");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.generateoauthsignaturebasestring.console.GenerateOAuthSignatureBaseStringPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(UsageMode.class, "usageMode")
        )));
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new NodeNameFactory());
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

    public UsageMode getUsageMode() {
        return usageMode;
    }

    public void setUsageMode(final UsageMode usageMode) {
        this.usageMode = usageMode;
    }

    private static final String META_INITIALIZED = GenerateOAuthSignatureBaseStringAssertion.class.getName() + ".metadataInitialized";
    private static final String GENERATE_OAUTH_SIGNATURE_BASE_STRING = "Generate OAuth Signature Base String";

    private UsageMode usageMode = UsageMode.CLIENT;

    // applies to both CLIENT and SERVER
    private String requestUrl = "${request.url}";
    private String httpMethod = "${request.http.method}";
    private String queryString = "${request.url.query}";
    private String variablePrefix = "oauth";
    // end applies to both CLIENT and SERVER

    // applies to SERVER
    private boolean useMessageTarget = true;
    private boolean useAuthorizationHeader = true;
    private String authorizationHeader = "${request.http.header.Authorization}";
    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport();
    // end applies to SERVER

    // applies to CLIENT
    private boolean useOAuthVersion = true;
    private String oauthConsumerKey;
    private String oauthToken;
    private String oauthCallback;
    private String oauthVerifier;
    // end applies to CLIENT

    static class NodeNameFactory implements AssertionNodeNameFactory<GenerateOAuthSignatureBaseStringAssertion> {
        @Override
        public String getAssertionName(final GenerateOAuthSignatureBaseStringAssertion assertion, final boolean decorate) {
            String assertionName = GENERATE_OAUTH_SIGNATURE_BASE_STRING;
            if (decorate) {
                if (UsageMode.CLIENT.equals(assertion.getUsageMode())) {
                    String requestType;
                    if (StringUtils.isNotBlank(assertion.getOauthToken())) {
                        if (StringUtils.isNotBlank(assertion.getOauthVerifier())) {
                            requestType = AUTHORIZED_REQUEST_TOKEN;
                        } else {
                            requestType = ACCESS_TOKEN;
                        }
                    } else {
                        requestType = REQUEST_TOKEN;
                    }
                    assertionName = assertion.getUsageMode().getDescription() + " " + GENERATE_OAUTH_SIGNATURE_BASE_STRING + ": " + requestType;
                } else {
                    // cannot determine request type
                    assertionName = assertion.getUsageMode().getDescription() + " " + GENERATE_OAUTH_SIGNATURE_BASE_STRING;
                }
            }
            return assertionName;
        }
    }
}
