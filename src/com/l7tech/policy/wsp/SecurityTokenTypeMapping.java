/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Element;

import java.util.regex.Pattern;

/**
 * TypeMapping for stand-alone wsse:TokenType WS-Policy element.  Will serialize instances of {@link SecurityTokenType}
 * and deserialize arbitrary wsse:TokenType
 * elements into the appropriate SecurityTokenType instance.
 */
public class SecurityTokenTypeMapping implements TypeMapping {
    public Class getMappedClass() {
        return SecurityTokenType.class;
    }

    public String getExternalName() {
        return "TokenType";
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (object == null || object.target == null)
            throw new InvalidPolicyTreeException("Unable to freeze a null SecurityToken");
        if (!(object.target instanceof SecurityTokenType))
            throw new InvalidPolicyTreeException("Unable to freeze SecurityToken of type " + object.target.getClass());
        SecurityTokenType tokenType = (SecurityTokenType)object.target;
        String typeUri = tokenType.getWstTokenTypeUri();

        final String wsseNs = SoapUtil.SECURITY_NAMESPACE;
        final String wssePfx = "wsse";
        Element tt = XmlUtil.createAndAppendElementNS(container, "TokenType", wsseNs, wssePfx);
        tt.appendChild(XmlUtil.createTextNode(tt, typeUri));
        return tt;
    }

    private final Pattern PX509 = Pattern.compile("[:#][Xx]509v3$");
    private final Pattern PUT   = Pattern.compile("[:#]UsernameToken$");
    private final Pattern PSCT  = Pattern.compile("/sc/sct$");
    private final Pattern PSAML = Pattern.compile("[:#]Assertion$|^SAML$");
    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        String uri = XmlUtil.getTextValue(source).trim();

        if (PX509.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.X509);
        if (PUT.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.USERNAME);
        if (PSCT.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.WSSC_CONTEXT);
        if (PSAML.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.SAML_ASSERTION);

        throw new InvalidPolicyStreamException("wsse:SecurityToken has unsupported TokenType: " + uri);
    }
}
