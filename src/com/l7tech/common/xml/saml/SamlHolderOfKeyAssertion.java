/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.saml;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public class SamlHolderOfKeyAssertion extends SamlAssertion {
    protected SamlHolderOfKeyAssertion(Element ass) throws SAXException {
        super(ass);
    }

    public X509Certificate getSigningCertificate() {
        return subjectCertificate;
    }
}
