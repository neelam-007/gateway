/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.common.xml.saml.SamlAssertion;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public interface SamlSecurityToken extends SigningSecurityToken {
    SamlAssertion asSamlAssertion();
    X509Certificate getSubjectCertificate();
    X509Certificate getIssuerCertificate();
}
