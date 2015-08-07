package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.external.assertions.xmlsec.VariableCredentialSourceAssertion;
import com.l7tech.security.token.SecurityToken;

import java.security.cert.X509Certificate;

/**
 * Holds utility methods used by non-soap security assertions.
 */
class NonSoapSecurityServerUtil {
    /**
     * Gather the specified value as credentials for the specified auth context, if the type is recognized.
     * Currently this method supports values that are instances of either X509Certificate or SecurityToken.
     *
     * @param authContext      the auth context to which the credential shall be added.  Required.
     * @param value            the value that is to be set as credentials.  Required.
     * @param credentialSourceClass  the class of the credential source assertion to use, or null to choose a hopefully-appropriate one.
     * @throws UnsupportedTokenTypeException
     */
    public static void addObjectAsCredentials(AuthenticationContext authContext, Object value, Class<? extends Assertion> credentialSourceClass) throws UnsupportedTokenTypeException {
        if (value == null)
            throw new UnsupportedTokenTypeException("Credential value is null");
        if (credentialSourceClass == null)
            credentialSourceClass = VariableCredentialSourceAssertion.class;
        SecurityToken token;
        if (value instanceof SecurityToken) {
            token = (SecurityToken) value;
        } else if (value instanceof X509Certificate) {
            X509Certificate certificate = (X509Certificate) value;
            token = new TlsClientCertToken(certificate);
        } else {
            throw new UnsupportedTokenTypeException("Unsupported credential type: " + value.getClass().getSimpleName());
        }
        authContext.addCredentials( LoginCredentials.makeLoginCredentials(token, credentialSourceClass));
    }

}
