/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.common.security.saml.SamlConstants;

import java.util.Calendar;
import java.util.Date;

/**
 * @author emil
 * @version Feb 1, 2005
 */
class AuthenticationStatement extends SubjectStatement {
    private final String authenticationMethod;
    private final Calendar authenticationInstant = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);

    public AuthenticationStatement(LoginCredentials credentials, Confirmation confirmation) {
        super(credentials, confirmation);

        this.authenticationMethod = mapAuthMethod(credentials.getCredentialSourceAssertion());
        this.authenticationInstant.setTime(new Date(credentials.getAuthInstant()));
    }

    private static final String mapAuthMethod(Class credentialSourceClass) {
        String authMethod = SamlConstants.UNSPECIFIED_AUTHENTICATION;
        if (credentialSourceClass.isAssignableFrom(HttpClientCert.class)) {
            authMethod = SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION;
        } else if (credentialSourceClass.isAssignableFrom(RequestWssX509Cert.class)) {
            authMethod = SamlConstants.XML_DSIG_AUTHENTICATION;
        } else if (credentialSourceClass.isAssignableFrom(HttpCredentialSourceAssertion.class) ||
          credentialSourceClass.isAssignableFrom(WssBasic.class)) {
            authMethod = SamlConstants.PASSWORD_AUTHENTICATION;
        }
        return authMethod;

    }

    public Calendar getAuthenticationInstant() {
        return authenticationInstant;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }
}