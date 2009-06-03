/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import org.w3c.dom.Element;

/**
 * Utilities for reading a SecureSpan 3.0 policy, which might refer to assertions whose classes no longer exist.
 */
class WspUpgradeUtilFrom30 {

    static final TypeMapping httpClientCertCompatibilityMapping =
            new CompatibilityAssertionMapping(new SslAssertion(), "HttpClientCert") {
                protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) {
                    SslAssertion sa = (SslAssertion)ass;
                    sa.setOption(SslAssertion.REQUIRED);
                    sa.setRequireClientAuthentication(true);
                }
            };
    
    public static TypeMapping samlSecurityCompatibilityMapping =
            new CompatibilityAssertionMapping(new RequireWssSaml(), "SamlSecurity") {
                protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) {
                    RequireWssSaml saml = (RequireWssSaml)ass;
                    saml.setRequireHolderOfKeyWithMessageSignature(true);
                    saml.setSubjectConfirmations(new String[] { SamlConstants.CONFIRMATION_HOLDER_OF_KEY });
                    saml.setNameFormats(SamlConstants.ALL_NAMEIDENTIFIERS);
                    final SamlAuthenticationStatement as = new SamlAuthenticationStatement();
                    as.setAuthenticationMethods(SamlConstants.ALL_AUTHENTICATIONS);
                    saml.setAuthenticationStatement(as);
                }
            };
}
