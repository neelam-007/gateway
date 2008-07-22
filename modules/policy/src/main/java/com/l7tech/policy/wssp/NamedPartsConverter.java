/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import org.apache.ws.policy.PrimitiveAssertion;

import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
class NamedPartsConverter implements PrimitiveAssertionConverter {
    private final String prefix;

    public NamedPartsConverter(String prefix) {
        this.prefix = prefix;
    }

    public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
        List terms = p.getTerms();
        for (Iterator i = terms.iterator(); i.hasNext();) {
            org.apache.ws.policy.Assertion t = (org.apache.ws.policy.Assertion)i.next();
            if (t.getType() != org.apache.ws.policy.Assertion.PRIMITIVE_TYPE)
                throw new PolicyConversionException(prefix + " parts contains unexpected composite assertion: " + t.getClass().getName());
            PrimitiveAssertion pa = (PrimitiveAssertion)t;
            String ns = pa.getName().getNamespaceURI();
            if (!ns.equals(p.getName().getNamespaceURI()))
                throw new PolicyConversionException(prefix + " parts contains unexpected namespace URI: " + pa.getName());
            String name = pa.getName().getLocalPart();
            if ("Body".equals(name)) {
                v.setSimpleProperty(prefix + "Body", true);
            } else if ("Header".equals(name)) {
                throw new PolicyConversionException("Named part not yet supported: " + name);
            } else
                throw new PolicyConversionException(prefix + " parts contains unrecognized named part: " + pa.getName());
        }
        return null;
    }
}
