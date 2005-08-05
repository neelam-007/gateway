package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.http.GenericHttpClient;

import java.util.Map;
import java.util.Collections;

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
    public static final String DEFAULT_USERNAME_FIELD = "username";
    public static final String DEFAULT_PASSWORD_FIELD = "password";

    private String ssoEndpointUrl;
    private String artifactQueryParameter = DEFAULT_ARTIFACT_QUERY_PARAM;
    private String method = GenericHttpClient.METHOD_GET;
    private String usernameFieldname = DEFAULT_USERNAME_FIELD;
    private String passwordFieldname = DEFAULT_PASSWORD_FIELD;
    private Map extraFields = Collections.EMPTY_MAP;

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        if (method == null || method.length() == 0)
            throw new IllegalArgumentException("method must not be null or empty!");

        this.method = method;
    }

    public String getUsernameFieldname() {
        return usernameFieldname;
    }

    public void setUsernameFieldname(String usernameFieldname) {
        if (usernameFieldname == null || usernameFieldname.length() == 0)
            throw new IllegalArgumentException("usernameFieldname must not be null or empty!");

        this.usernameFieldname = usernameFieldname;
    }

    public String getPasswordFieldname() {
        return passwordFieldname;
    }

    public void setPasswordFieldname(String passwordFieldname) {
        if (passwordFieldname == null || passwordFieldname.length() == 0)
            throw new IllegalArgumentException("passwordFieldname must not be null or empty!");

        this.passwordFieldname = passwordFieldname;
    }

    public Map getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(Map extraFields) {
        if (extraFields == null)
            throw new IllegalArgumentException("extraFields must not be null!");
        this.extraFields = extraFields;
    }
}
