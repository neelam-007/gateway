/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

import java.util.Calendar;
import java.util.Date;

/**
 * @author emil
 * @version Feb 1, 2005
 */
public class AuthenticationStatement extends SubjectStatement {
    private String authenticationMethod;
    private Calendar authenticationInstant = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);

    public AuthenticationStatement(LoginCredentials credentials, Confirmation confirmation, KeyInfoInclusionType keyInfoType, NameIdentifierInclusionType nameIdType, String overrideNameValue, String overrideNameFormat) {
        super(credentials, confirmation, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat);

        this.authenticationMethod = mapAuthMethod(credentials.getCredentialSourceAssertion());
        long when = credentials.getAuthInstant();
        if (when == 0) when = System.currentTimeMillis();
        this.authenticationInstant.setTime(new Date(when));
    }

    private static String mapAuthMethod(Class credentialSourceClass) {
        String authMethod = SamlConstants.UNSPECIFIED_AUTHENTICATION;
        if (credentialSourceClass == null)
            return authMethod;

        if (SslAssertion.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION;
        } else if (RequestWssX509Cert.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = SamlConstants.XML_DSIG_AUTHENTICATION;
        } else if (HttpCredentialSourceAssertion.class.isAssignableFrom(credentialSourceClass) ||
          WssBasic.class.isAssignableFrom(credentialSourceClass)) {
            authMethod = SamlConstants.PASSWORD_AUTHENTICATION;
        }
        return authMethod;

    }

    public Calendar getAuthenticationInstant() {
        return authenticationInstant;
    }

    public void setAuthenticationInstant(Calendar authenticationInstant) {
        this.authenticationInstant = authenticationInstant;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }
}