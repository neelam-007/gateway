/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import org.w3c.dom.Element;

/**
 * Handle freezing of RequestWssIntegrity to a wsse:Integrity (or pre32 format), and thawing of either wsse:Integrity
 * or pre32 format element into a RequetsWssIntegrity assertion.
 */
class IntegrityMapping extends AssertionMapping {
    private final XpathExpressionMapping xpathMapper = new XpathExpressionMapping("MessageParts", SoapConstants.SECURITY_NAMESPACE, "wsse");
    private final XmlSecurityAssertionBase templateAssertion;
    private final boolean freeze;

    IntegrityMapping(XmlSecurityAssertionBase a, String externalName, boolean freeze, String version) {
        super(a, externalName, version);
        this.templateAssertion = a;
        this.freeze = freeze;
        if (!(a instanceof RequireWssSignedElement))
            throw new IllegalArgumentException("Can only map RequireWssSignedElement");
    }

    @Override
    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (!freeze || object.target == null)
            return super.freeze(wspWriter, object, container);

        if (!(object.target instanceof RequireWssSignedElement))
            throw new InvalidPolicyTreeException("IntegrityMapping is unable to freeze object of type " + object.target.getClass());

        RequireWssSignedElement ass = (RequireWssSignedElement)object.target;
        Element integrity = DomUtils.createAndAppendElementNS(container, "Integrity", SoapConstants.SECURITY_NAMESPACE, "wsse");

        if (ass.getRecipientContext() != null && !ass.getRecipientContext().equals(templateAssertion.getRecipientContext())) {
            TypeMapping rctm = new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext");
            rctm.freeze(wspWriter, new TypedReference(XmlSecurityRecipientContext.class, ass.getRecipientContext(), "xmlSecurityRecipientContext"), integrity);
        }

        TypedReference xpathTr = new TypedReference(XpathExpression.class, ass.getXpathExpression());
        xpathMapper.freeze(wspWriter, xpathTr, integrity);
        WspUtil.setWspUsageRequired(integrity, "wsp", SoapConstants.WSP_NAMESPACE);

        boolean enabled = ass.isEnabled();
        if (!enabled) {
            TypeMapping booleanTm = new BasicTypeMapping(boolean.class, "booleanValue");
            booleanTm.freeze(wspWriter, new TypedReference(boolean.class, enabled, "Enabled"), integrity);
        }

        return integrity;
    }

    @Override
    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!source.getLocalName().equals("Integrity"))
            return super.thaw(source, visitor);

        WspUtil.enforceWspUsageRequired(source);

        RequireWssSignedElement ass = new RequireWssSignedElement();
        TypedReference tr = new TypedReference(RequireWssSignedElement.class, ass);

        Element recipContextEl = DomUtils.findFirstChildElementByName(source, (String)null, "xmlSecurityRecipientContext");
        if (recipContextEl != null) {
            TypedReference rctr = new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext").thaw(recipContextEl, visitor);
            if (rctr.target instanceof XmlSecurityRecipientContext) {
                XmlSecurityRecipientContext x = (XmlSecurityRecipientContext)rctr.target;
                ass.setRecipientContext(x);
            }
        }

        Element messageParts = DomUtils.findFirstChildElementByName(source, (String)null, "MessageParts");
        TypedReference xpathRef = xpathMapper.thaw(messageParts, visitor);
        if (xpathRef == null || !(xpathRef.target instanceof XpathExpression))
            throw new InvalidPolicyStreamException("Could not recover xpath expression from Integrity/MessageParts");
        ass.setXpathExpression((XpathExpression)xpathRef.target);

        Element enabledElem = DomUtils.findFirstChildElementByName(source, (String) null, "Enabled");
        if (enabledElem != null) {
            String boolValue = enabledElem.getAttribute("booleanValue");
            if (boolValue != null) {
                ass.setEnabled(Boolean.parseBoolean(boolValue));
            }
        }

        return tr;
    }
}
