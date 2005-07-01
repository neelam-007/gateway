/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import org.w3c.dom.Element;

/**
 * Mapping that knows how to turn a RequestWssSaml into a wsse:SecurityToken element, and vice versa.
 * <p/>
 * The generic wsse:SecurityToken parser, {@link SecurityTokenTypeMapping}, delegates population of newly-deserialized
 * RequestWssSaml object back to this class.
 */
public class SamlSecurityTokenAssertionMapping extends SecurityTokenAssertionMapping {
    public SamlSecurityTokenAssertionMapping() {
        super(new RequestWssSaml(), "RequestWssSaml", SecurityTokenType.SAML_ASSERTION);
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        Element sct = super.freeze(wspWriter, object, container);
        if ("SecurityToken".equals(sct.getLocalName())) {
            // Extra SAML params have not yet been saved
            String pfx = getNsPrefix(wspWriter);
            if (pfx.endsWith(":")) pfx = pfx.substring(0, pfx.length() - 1);
            if (pfx.length() < 1) pfx = "L7p";
            Element params = XmlUtil.createAndAppendElementNS(sct, "SamlParams", this.getNsUri(), this.getNsPrefix(wspWriter));
            super.populateElement(wspWriter, params, object);
        }
        return sct;
    }

    TypedReference thawRequestWssSaml(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!"SecurityToken".equals(source.getLocalName()))
            throw new InvalidPolicyStreamException("SamlSecurityToken: was expecting wsse:SecurityToken indicating a SAML assertion");
        RequestWssSaml saml = new RequestWssSaml();
        TypedReference tr = new TypedReference(RequestWssSaml.class, saml);
        Element params = XmlUtil.findFirstChildElementByName(source, (String)null, "SamlParams");
        if (params != null)
            super.populateObject(tr, params, visitor);
        return tr;
    }
}
