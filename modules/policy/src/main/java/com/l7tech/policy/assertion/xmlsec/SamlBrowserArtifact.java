package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

/**
 * Gateway makes an HTTP GET request with HTTP Basic credentials to a SAML
 * single-signon endpoint, remembering the redirect URL in a context variable
 * for subsequent assertions.
 *
 * @author alex
 */
public class SamlBrowserArtifact extends Assertion implements SetsVariables {
    public static final String DEFAULT_ARTIFACT_QUERY_PARAM = "SAMLart";
    public static final String VAR_ARTIFACT = "samlBrowserArtifact.artifact";
    public static final String VAR_REDIRECT_URL = "samlBrowserArtifact.redirectUrl";

    private String ssoEndpointUrl;
    private String artifactQueryParameter = DEFAULT_ARTIFACT_QUERY_PARAM;
    private AuthenticationProperties authenticationProperties;

    public SamlBrowserArtifact() {
        authenticationProperties = new AuthenticationProperties();
    }

    public String getSsoEndpointUrl() {
        return ssoEndpointUrl;
    }

    public void setSsoEndpointUrl(String ssoEndpointUrl) {
        if (ssoEndpointUrl == null || ssoEndpointUrl.length() == 0)
            throw new IllegalArgumentException("ssoEndpointUrl must not be null or empty!");
        this.ssoEndpointUrl = ssoEndpointUrl;
    }

    /**
     * The name of the HTTP GET query parameter whose value contains the SAML artifact.
     *
     * Default value is {@link #DEFAULT_ARTIFACT_QUERY_PARAM}.
     */
    public String getArtifactQueryParameter() {
        return artifactQueryParameter;
    }

    public void setArtifactQueryParameter(String artifactQueryParameter) {
        if (artifactQueryParameter == null || artifactQueryParameter.length() == 0)
            throw new IllegalArgumentException("artifactQueryParameter must not be null or empty!");
        this.artifactQueryParameter = artifactQueryParameter;
    }

    public AuthenticationProperties getAuthenticationProperties() {
        return new AuthenticationProperties(authenticationProperties);
    }

    public void setAuthenticationProperties(AuthenticationProperties authenticationProperties) {
        this.authenticationProperties = new AuthenticationProperties(authenticationProperties);
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(VAR_ARTIFACT, false, false, null, false),
            new VariableMetadata(VAR_REDIRECT_URL, false, false, null, false),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.SHORT_NAME, "Retrieve SAML Browser Artifact");
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway makes an HTTP GET request with HTTP Basic credentials to a SAML single-signon endpoint, remembering the redirect URL for subsequent assertions.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddSamlBrowserArtifactAdvice");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Browser Artifact Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditSamlBrowserArtifactAction");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Binary<String, SamlBrowserArtifact, Boolean>() {
            @Override
            public String call(final SamlBrowserArtifact samlBrowserArtifact, final Boolean decorate) {
                final String assertionName = "Retrieve SAML Browser Artifact";
                return (decorate)? assertionName + " from " + samlBrowserArtifact.getSsoEndpointUrl(): assertionName;
            }
        });
        return meta;
    }

}
