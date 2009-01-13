package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

/**
 *
 */
class SecurityContextTokenImpl extends SigningSecurityTokenImpl implements SecurityContextToken {
    private final SecurityContext secContext;
    private final String identifier;
    private final String elementWsuId;

    public SecurityContextTokenImpl(SecurityContext secContext, Element secConTokEl, String identifier) {
        super(secConTokEl);
        this.secContext = secContext;
        this.identifier = identifier;
        this.elementWsuId = SoapUtil.getElementWsuId(secConTokEl);
    }

    public SecurityContext getSecurityContext() {
        return secContext;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSSC_CONTEXT;
    }

    public String getElementId() {
        return elementWsuId;
    }

    public String getContextIdentifier() {
        return identifier;
    }

    public String toString() {
        return "SecurityContextToken: " + secContext.toString();
    }
}
