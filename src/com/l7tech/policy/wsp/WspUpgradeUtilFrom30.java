/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SslAssertion;
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
            new CompatibilityAssertionMapping(new FalseAssertion(), "SamlSecurity") {
                protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) {
                    // TODO this needs to be implemented

                }
            };
}
