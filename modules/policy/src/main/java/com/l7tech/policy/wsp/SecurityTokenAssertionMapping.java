/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;

import org.w3c.dom.Element;

/**
 * An AssertionMapping that will serialize to either a wsse:SecurityToken element, or an old-style pre32 assertion,
 * depending on the current WspWriter compatibility bit; and that will deserialize either form into the appropriate
 * policy assertion object.
 * <p/>
 * When parsing, delegates any wsse:SecurityToken elements to the general SecurityToken parser, {@link SecurityTokenTypeMapping}.
 */
class SecurityTokenAssertionMapping extends BeanTypeMapping {

    protected final SecurityTokenType tokenType;
    private static final SecurityTokenTypeMapping securityTokenTypeMapping = new SecurityTokenTypeMapping(); // delegate: general wsse:SecurityToken parser

    /**
     * Create a mapping for serializing assertions to wsse:SecurityToken elements, and parsing pre32 assertion elements.
     * @param prototype          a prototype instance of the assertion this mapping will freeze/thaw.  Required.
     * @param oldExternalName    the old external name of this assertion, in pre32 format (non-SecurityToken)
     * @param tokenType          tokenType attribute to expect/use in the wsse:SecurityToken element.
     */
    public SecurityTokenAssertionMapping(Assertion prototype, String oldExternalName, SecurityTokenType tokenType) {
        super(prototype.getClass(), oldExternalName);
        this.tokenType = tokenType;
    }

    /** Create a mapping for parsing wsse:SecurityToken elements. */
    public SecurityTokenAssertionMapping() {
        super(XmlSecurityToken.class, "SecurityToken");
        this.tokenType = null;
    }

    protected String getPropertiesElementName(SecurityTokenType tokenType) {
        // For backward compat with pre-4.0, use SamlParams for saml token properties
        return SecurityTokenType.SAML_ASSERTION.equals(tokenType) ||
               SecurityTokenType.SAML2_ASSERTION.equals(tokenType) ? "SamlParams" : "Properties";
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (tokenType == null)
            throw new InvalidPolicyTreeException("Unable to freeze object of type " + getMappedClass());

        // Create a SecurityToken element representing this RequestWssX509Cert
        final String wsseNs = SoapConstants.SECURITY_NAMESPACE;
        final String wssePfx = "wsse";
        Element st = DomUtils.createAndAppendElementNS(container, "SecurityToken", wsseNs, wssePfx);
        securityTokenTypeMapping.freeze(wspWriter, new TypedReference(tokenType.getClass(), tokenType), st);

        if ("SecurityToken".equals(st.getLocalName())) {
            // Extra SAML params have not yet been saved
            String pfx = getNsPrefix();
            if (pfx.endsWith(":")) pfx = pfx.substring(0, pfx.length() - 1);
            if (pfx.length() < 1) pfx = "L7p";
            Element params = DomUtils.createAndAppendElementNS(st,
                                                              getPropertiesElementName(tokenType),
                                                              getNsUri(),
                                                              pfx);
            super.populateElement(wspWriter, params, object);
        }

        return st;
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!"SecurityToken".equals(source.getLocalName()))
            return super.thaw(source, visitor); // delegate to old-style parser

        Element tt = DomUtils.findFirstChildElementByName(source, (String)null, "TokenType");
        if (tt == null)
            throw new InvalidPolicyStreamException("wsse:SecurityToken element has no TokenType subelement");

        TypedReference tokenTypeRef = securityTokenTypeMapping.thaw(tt, visitor);
        if (tokenTypeRef == null || tokenTypeRef.target == null || !(tokenTypeRef.target instanceof SecurityTokenType))
            throw new InvalidPolicyStreamException("wsse:SecurityToken element has unrecognized TokenType subelement");

        SecurityTokenType tokenType = (SecurityTokenType)tokenTypeRef.target;

        final TypedReference ret;
        if (SecurityTokenType.SAML_ASSERTION.equals(tokenType))
            ret = new TypedReference(RequestWssSaml.class, new RequestWssSaml());
        else if (SecurityTokenType.SAML2_ASSERTION.equals(tokenType))
            ret = new TypedReference(RequestWssSaml2.class, new RequestWssSaml2());
        else if (SecurityTokenType.WSS_USERNAME.equals(tokenType))
            ret = new TypedReference(WssBasic.class, new WssBasic());
        else if (SecurityTokenType.WSSC_CONTEXT.equals(tokenType))
            ret = new TypedReference(SecureConversation.class,  new SecureConversation());
        else if (SecurityTokenType.WSS_X509_BST.equals(tokenType))
            ret = new TypedReference(RequestWssX509Cert.class, new RequestWssX509Cert());
        else
            throw new InvalidPolicyStreamException("Unsupported wsse:SecurityToken TokenType " + tokenType);

        Element params = DomUtils.findFirstChildElementByName(source, (String)null, getPropertiesElementName(tokenType));
        if (params != null)
            super.populateObject(ret, params, visitor);

        return ret;
    }
}
