/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import org.w3c.dom.Element;

/**
 * TypeMapping for WSP-compatible RequestWssX509 assertion (wsp SecurityToken)
 */
public class WssX509Mapping extends AssertionMapping {
    WssX509Mapping(Assertion a, String externalName) {
        super(a, externalName);
    }

    public WssX509Mapping(Assertion a, String externalName, String nsUri, String nsPrefix) {
        super(a, externalName, nsUri, nsPrefix);
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (wspWriter.isPre32Compat() || !(source instanceof RequestWssX509Cert))
            return super.freeze(wspWriter, object, container);

        // Create a SecurityToken element representing this RequestWssX509Cert
        final String wsseNs = SoapUtil.SECURITY_NAMESPACE;
        final String wssePfx = "wsse";
        Element st = XmlUtil.createAndAppendElementNS(container, "SecurityToken", wsseNs, wssePfx);
        Element tt = XmlUtil.createAndAppendElementNS(st, "TokenType", wsseNs, wssePfx);
        tt.appendChild(XmlUtil.createTextNode(tt, wsseNs + "#X509v3"));
        return st;
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if ("SecurityToken".equals(source.getLocalName())) {
            return new TypedReference(clazz, new RequestWssX509Cert());

            //Element el = source.getOwnerDocument().createElementNS(this.getNsUri(), this.getNsPrefix() + "RequestWssX509Cert");
            //return createObject(source, "included", visitor);
        }

        return super.thaw(source, visitor);
    }
}
