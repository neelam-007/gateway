/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.xml.saml.SamlAssertion;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public interface SamlSecurityToken extends SecurityToken {
    SamlAssertion asSamlAssertion();
    X509Certificate getSubjectCertificate();
    boolean isPossessionProved();
}
