package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 * Gateway makes an HTTP GET request with HTTP Basic credentials to a SAML
 * single-signon endpoint, remembering the redirect URL in a context variable
 * for subsequent assertions.
 * <p>
 * TODO make this configurable to use POST with a login form as well as GET with HTTP Basic
 *
 * @author alex
 */
public class SamlBrowserArtifact extends Assertion {
    public static final String DEFAULT_ARTIFACT_QUERY_PARAM = "SAMLart";

    private String ssoEndpointUrl;
    private String artifactQueryParameter = DEFAULT_ARTIFACT_QUERY_PARAM;

    public SamlBrowserArtifact() {
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
}
