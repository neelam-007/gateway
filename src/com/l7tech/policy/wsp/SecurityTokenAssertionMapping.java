/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import org.w3c.dom.Element;

/**
 * An AssertionMapping that will serialize to either a wsse:SecurityToken element, or an old-style pre32 assertion,
 * depending on the current WspWriter compatibility bit; and that will deserialize either form into the appropriate
 * policy assertion object.
 * <p/>
 * When parsing, delegates any wsse:SecurityToken elements to the general SecurityToken parser, {@link SecurityTokenTypeMapping}.
 */
public class SecurityTokenAssertionMapping extends AssertionMapping {

    protected final SecurityTokenType tokenType;
    private static final SecurityTokenTypeMapping securityTokenTypeMapping = new SecurityTokenTypeMapping(); // delegate: general wsse:SecurityToken parser
    private static final SamlSecurityTokenAssertionMapping samlMapper = new SamlSecurityTokenAssertionMapping(); // parse extra SAML parameters

    /** Create a mapping for serializing assertions to wsse:SecurityToken elements, and parsing pre32 assertion elements. */
    public SecurityTokenAssertionMapping(Assertion prototype, String oldExternalName, SecurityTokenType tokenType) {
        super(prototype, oldExternalName);
        this.tokenType = tokenType;
    }

    /** Create a mapping for parsing wsse:SecurityToken elements. */
    public SecurityTokenAssertionMapping() {
        super(SecurityToken.class, "SecurityToken");
        this.tokenType = null;
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (tokenType == null)
            throw new InvalidPolicyTreeException("Unable to freeze object of type " + getMappedClass());
        if (wspWriter.isPre32Compat())
            return super.freeze(wspWriter, object, container);

        // Create a SecurityToken element representing this RequestWssX509Cert
        final String wsseNs = SoapUtil.SECURITY_NAMESPACE;
        final String wssePfx = "wsse";
        Element st = XmlUtil.createAndAppendElementNS(container, "SecurityToken", wsseNs, wssePfx);
        securityTokenTypeMapping.freeze(wspWriter, new TypedReference(tokenType.getClass(), tokenType), st);
        return st;
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!"SecurityToken".equals(source.getLocalName()))
            return super.thaw(source, visitor); // delegate to old-style parser

        Element tt = XmlUtil.findFirstChildElementByName(source, (String)null, "TokenType");
        if (tt == null)
            throw new InvalidPolicyStreamException("wsse:SecurityToken element has no TokenType subelement");

        TypedReference tokenTypeRef = securityTokenTypeMapping.thaw(tt, visitor);
        if (tokenTypeRef == null || tokenTypeRef.target == null || !(tokenTypeRef.target instanceof SecurityTokenType))
            throw new InvalidPolicyStreamException("wsse:SecurityToken element has unrecognized TokenType subelement");

        SecurityTokenType tokenType = (SecurityTokenType)tokenTypeRef.target;

        if (SecurityTokenType.SAML_ASSERTION.equals(tokenType))
            return samlMapper.thawRequestWssSaml(source, visitor);
        if (SecurityTokenType.USERNAME.equals(tokenType))
            return new TypedReference(WssBasic.class, new WssBasic());
        if (SecurityTokenType.WSSC_CONTEXT.equals(tokenType))
            return new TypedReference(SecureConversation.class,  new SecureConversation());
        if (SecurityTokenType.X509.equals(tokenType))
            return new TypedReference(RequestWssX509Cert.class, new RequestWssX509Cert());

        throw new InvalidPolicyStreamException("Unsupported wsse:SecurityToken TokenType " + tokenType);
    }
}
