package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

/**
 *
 */
class SecurityContextTokenImpl extends SigningSecurityTokenImpl implements SecurityContextToken {
    private final SecurityContext secContext;
    private final String identifier;
    private final String namespace;
    private final String elementWsuId;

    SecurityContextTokenImpl(SecurityContext secContext, Element secConTokEl, String identifier) throws InvalidDocumentFormatException {
        super(secConTokEl);
        this.secContext = secContext;
        this.identifier = identifier;
        this.namespace = secConTokEl!=null ? secConTokEl.getNamespaceURI() : SoapUtil.WSSC_NAMESPACE2;
        this.elementWsuId = secConTokEl!=null ? SoapUtil.getElementWsuId(secConTokEl) : null;
    }

    @Override
    public SecurityContext getSecurityContext() {
        return secContext;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.WSSC_CONTEXT;
    }

    @Override
    public String getElementId() {
        return elementWsuId;
    }

    @Override
    public String getContextIdentifier() {
        return identifier;
    }

    @Override
    public String getWsscNamespace() {
        return namespace;
    }

    public String toString() {
        return "SecurityContextToken: " + secContext.toString();
    }
}
