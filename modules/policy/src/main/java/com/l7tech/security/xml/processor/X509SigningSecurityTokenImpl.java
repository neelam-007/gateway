/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.X509SigningSecurityToken;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public abstract class X509SigningSecurityTokenImpl extends SigningSecurityTokenImpl implements X509SigningSecurityToken {
    public X509SigningSecurityTokenImpl(Element element) {
        super(element);
    }

    @Override
    public final X509Certificate getCertificate() {
        return getMessageSigningCertificate();
    }
}
