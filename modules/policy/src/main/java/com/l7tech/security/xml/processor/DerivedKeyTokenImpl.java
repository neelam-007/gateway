package com.l7tech.security.xml.processor;

import com.l7tech.security.token.DerivedKeyToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

/**
 *
 */
class DerivedKeyTokenImpl extends ParsedElementImpl implements DerivedKeyToken {
    private final byte[] finalKey;
    private final XmlSecurityToken sourceToken;
    private final String elementWsuId;

    public DerivedKeyTokenImpl(Element dktel, byte[] finalKey, XmlSecurityToken sourceToken) {
        super(dktel);
        this.finalKey = finalKey;
        this.sourceToken = sourceToken;
        elementWsuId = SoapUtil.getElementWsuId(dktel);
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSSC_DERIVED_KEY;
    }

    public String getElementId() {
        return elementWsuId;
    }

    byte[] getComputedDerivedKey() {
        return finalKey;
    }

    public XmlSecurityToken getSourceToken() {
        return sourceToken;
    }

    public String toString() {
        return "DerivedKeyToken: " + finalKey.toString();
    }

    public byte[] getSecretKey() {
        return finalKey;
    }
}
