/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.X509CertificateAttributesExtractor;

import java.security.cert.X509Certificate;

/** @author alex */
public class X509CertificateSelector implements ExpandVariables.Selector<X509Certificate> {

    @Override
    public Selection select(X509Certificate cert, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        X509CertificateAttributesExtractor extractor = new X509CertificateAttributesExtractor(cert);

        try {
            return new Selection(extractor.getAttributeValue(name));
        } catch (IllegalArgumentException e) {
            String msg = handler.handleBadVariable(name + " in "  + cert.getClass().getName());
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    @Override
    public Class<X509Certificate> getContextObjectClass() {
        return X509Certificate.class;
    }
}
