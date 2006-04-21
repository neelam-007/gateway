/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import org.apache.ws.policy.PrimitiveAssertion;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mike
 */
class SignedSupportingTokens extends WsspVisitor {
    private final PrimitiveAssertion primitiveAssertion;
    private boolean gatheredProperties = false;

    private Map converterMap = new HashMap();
    {
        converterMap.put("UsernameToken", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                String includeToken = p.getAttribute(new QName(p.getName().getNamespaceURI(), "IncludeToken"));
                if (includeToken != null && includeToken.length() > 0 && !includeToken.equals("http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient"))
                    throw new PolicyConversionException("Unsupported sp:UsernameToken sp:IncludeToken attribute value: " + includeToken);
                return new WssBasic();
            }
        });
    }

    protected SignedSupportingTokens(WsspVisitor parent, PrimitiveAssertion p) {
        super(parent);
        this.primitiveAssertion = p;
    }

    protected Map getConverterMap() {
        return converterMap;
    }

    public Assertion toLayer7Policy() throws PolicyConversionException {
        return gatherPropertiesFromSubPolicy(primitiveAssertion);
    }
}
