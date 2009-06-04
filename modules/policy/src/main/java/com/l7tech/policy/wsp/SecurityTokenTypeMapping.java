/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Element;

import java.util.regex.Pattern;

/**
 * TypeMapping for stand-alone wsse:TokenType WS-Policy element.  Will serialize instances of {@link SecurityTokenType}
 * and deserialize arbitrary wsse:TokenType
 * elements into the appropriate SecurityTokenType instance.
 */
class SecurityTokenTypeMapping implements TypeMapping {
    @Override
    public Class getMappedClass() {
        return SecurityTokenType.class;
    }

    @Override
    public String getSinceVersion() {
        return null;
    }

    @Override
    public String getExternalName() {
        return "TokenType";
    }

    @Override
    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (object == null || object.target == null)
            throw new InvalidPolicyTreeException("Unable to freeze a null SecurityToken");
        if (!(object.target instanceof SecurityTokenType))
            throw new InvalidPolicyTreeException("Unable to freeze SecurityToken of type " + object.target.getClass());
        SecurityTokenType tokenType = (SecurityTokenType)object.target;
        String typeUri = tokenType.getWstTokenTypeUri();

        final String wsseNs = SoapConstants.SECURITY_NAMESPACE;
        final String wssePfx = "wsse";
        Element tt = DomUtils.createAndAppendElementNS(container, "TokenType", wsseNs, wssePfx);
        tt.appendChild(DomUtils.createTextNode(tt, typeUri));
        return tt;
    }

    private final Pattern PX509 = Pattern.compile("[:#][Xx]509v3$");
    private final Pattern PUT   = Pattern.compile("[:#]UsernameToken$");
    private final Pattern PSCT  = Pattern.compile("/sc/sct$");
    private final Pattern PSAML = Pattern.compile("[:#]Assertion$|^SAML$");
    private final Pattern PKERB = Pattern.compile("[:#]GSS_Kerberosv5_AP_REQ$");

    @Override
    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        String uri = DomUtils.getTextValue(source).trim();

        if (PX509.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.WSS_X509_BST);
        if (PUT.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.WSS_USERNAME);
        if (PSCT.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.WSSC_CONTEXT);
        if (PSAML.matcher(uri).find()) {
            if (uri.indexOf("2.0") > 0)
                return new TypedReference(getMappedClass(), SecurityTokenType.SAML2_ASSERTION);
            else
                return new TypedReference(getMappedClass(), SecurityTokenType.SAML_ASSERTION);
        }
        if (PKERB.matcher(uri).find())
            return new TypedReference(getMappedClass(), SecurityTokenType.WSS_KERBEROS_BST);

        throw new InvalidPolicyStreamException("wsse:SecurityToken has unsupported TokenType: " + uri);
    }

    @Override
    public TypeMappingFinder getSubtypeFinder() {
        return null;
    }
}
