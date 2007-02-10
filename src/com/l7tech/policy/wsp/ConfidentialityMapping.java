/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import org.w3c.dom.Element;

/**
 * Handle freezing of RequestWssConfidentiality to a wsse:Confidentiality (or pre32 format),
 * and thawing of either wsse:Confidentiality
 * or pre32 format element into a RequetsWssConfidentiality assertion.
 */
class ConfidentialityMapping extends AssertionMapping {
    private final XpathExpressionMapping xpathMapper = new XpathExpressionMapping("MessageParts", SoapUtil.SECURITY_NAMESPACE, "wsse");

    ConfidentialityMapping(XmlSecurityAssertionBase a, String externalName) {
        super(a, externalName);
        if (!(a instanceof RequestWssConfidentiality))
            throw new IllegalArgumentException("Can only map RequestWssConfidentiality");
        if (!"RequestWssConfidentiality".equals(externalName) && !"Confidentiality".equals(externalName))
            throw new IllegalArgumentException("oldExternalName must be RequestWssConfidentiality or Confidentiality");
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!source.getLocalName().equals("Confidentiality") || WspConstants.L7_POLICY_NS.equals(source.getNamespaceURI()))
            return super.thaw(source, visitor);

        WspUtil.enforceWspUsageRequired(source);
        RequestWssConfidentiality ass = new RequestWssConfidentiality();
        TypedReference tr = new TypedReference(RequestWssConfidentiality.class, ass);

        Element recipContextEl = XmlUtil.findFirstChildElementByName(source, (String)null, "xmlSecurityRecipientContext");
        if (recipContextEl != null) {
            TypedReference rctr = new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext").thaw(recipContextEl, visitor);
            if (rctr.target instanceof XmlSecurityRecipientContext) {
                XmlSecurityRecipientContext x = (XmlSecurityRecipientContext)rctr.target;
                ass.setRecipientContext(x);
            }
        }

        Element messageParts = XmlUtil.findFirstChildElementByName(source, (String)null, "MessageParts");
        TypedReference xpathRef = xpathMapper.thaw(messageParts, visitor);
        if (xpathRef == null || !(xpathRef.target instanceof XpathExpression))
            throw new InvalidPolicyStreamException("Could not recover xpath expression from Confidentiality/MessageParts");
        ass.setXpathExpression((XpathExpression)xpathRef.target);

        Element alg = XmlUtil.findFirstChildElementByName(source, (String)null, "Algorithm");
        if (alg != null) {
            String algUri = alg.getAttribute("URI");
            if (algUri != null && algUri.length() > 0)
                ass.setXEncAlgorithm(algUri);
        }

        return tr;
    }
}
