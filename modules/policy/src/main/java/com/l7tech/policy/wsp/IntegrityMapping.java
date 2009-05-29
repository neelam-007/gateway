/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import org.w3c.dom.Element;

/**
 * Handle freezing of RequestWssIntegrity to a wsse:Integrity (or pre32 format), and thawing of either wsse:Integrity
 * or pre32 format element into a RequetsWssIntegrity assertion.
 */
class IntegrityMapping extends AssertionMapping {
    private final XpathExpressionMapping xpathMapper = new XpathExpressionMapping("MessageParts", SoapConstants.SECURITY_NAMESPACE, "wsse");
    private final RequestWssIntegrity templateAssertion;

    IntegrityMapping(XmlSecurityAssertionBase a, String externalName) {
        super(a, externalName);
        if (!(a instanceof RequestWssIntegrity))
            throw new IllegalArgumentException("Can only map RequestWssIntegrity");
        if (!"RequestWssIntegrity".equals(externalName) && !"Integrity".equals(externalName))
            throw new IllegalArgumentException("oldExternalName must be RequestWssIntegrity or Integrity");
        this.templateAssertion = (RequestWssIntegrity) a;
    }

    @Override
    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {

        if (object.target == null)
            return super.freeze(wspWriter, object, container);

        if (!(object.target instanceof RequestWssIntegrity))
            throw new InvalidPolicyTreeException("IntegrityMapping is unable to freeze object of type " + object.target.getClass());

        RequestWssIntegrity ass = (RequestWssIntegrity)object.target;
        Element integrity = DomUtils.createAndAppendElementNS(container, "Integrity", SoapConstants.SECURITY_NAMESPACE, "wsse");

        if (ass.getRecipientContext() != null && !ass.getRecipientContext().equals(templateAssertion.getRecipientContext())) {
            TypeMapping rctm = new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext");
            rctm.freeze(wspWriter, new TypedReference(XmlSecurityRecipientContext.class, ass.getRecipientContext(), "xmlSecurityRecipientContext"), integrity);
        }

        if (ass.getIdentityTarget() != null && !ass.getIdentityTarget().equals(templateAssertion.getIdentityTarget())) {
            TypeMapping ittm = new BeanTypeMapping(IdentityTarget.class, "IdentityTarget");
            ittm.freeze(wspWriter, new TypedReference(IdentityTarget.class, ass.getIdentityTarget(), "IdentityTarget"), integrity);
        }

        if (!new MessageTargetableSupport(ass).equals(new MessageTargetableSupport(templateAssertion))) {
            TypeMapping mttm = new BeanTypeMapping(MessageTargetableSupport.class, "MessageTarget");
            mttm.freeze(wspWriter, new TypedReference(MessageTargetableSupport.class, new MessageTargetableSupport(ass), "MessageTarget"), integrity);
        }

        if ( ass.getXpathExpression() != null ) {
            TypedReference xpathTr = new TypedReference(XpathExpression.class, ass.getXpathExpression());
            xpathMapper.freeze(wspWriter, xpathTr, integrity);
            WspUtil.setWspUsageRequired(integrity, "wsp", SoapConstants.WSP_NAMESPACE);
        } else {
            integrity.setAttribute("messageParts", "false");
        }

        boolean enabled = ass.isEnabled();
        if (!enabled) {
            TypeMapping booleanTm = new BasicTypeMapping(boolean.class, "booleanValue");
            booleanTm.freeze(wspWriter, new TypedReference(boolean.class, enabled, "Enabled"), integrity);
        }

        String prefix = ass.getVariablePrefix();
        if ( prefix != null && prefix.trim().length()!=0 ) {
            TypeMapping stringTm = new BasicTypeMapping(String.class, "stringValue");
            stringTm.freeze(wspWriter, new TypedReference(String.class, prefix.trim(), "VariablePrefix"), integrity);
        }

        String signedElementsVar = ass.getSignedElementsVariable();
        if ( signedElementsVar != null && signedElementsVar.trim().length()!=0 ) {
            TypeMapping stringTm = new BasicTypeMapping(String.class, "stringValue");
            stringTm.freeze(wspWriter, new TypedReference(String.class, signedElementsVar.trim(), "ElementsVariable"), integrity);
        }

        return integrity;
    }

    @Override
    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!source.getLocalName().equals("Integrity"))
            return super.thaw(source, visitor);

        WspUtil.enforceWspUsageRequired(source);

        RequestWssIntegrity ass = new RequestWssIntegrity();
        TypedReference tr = new TypedReference(RequestWssIntegrity.class, ass);

        Element recipContextEl = DomUtils.findFirstChildElementByName(source, (String)null, "xmlSecurityRecipientContext");
        if (recipContextEl != null) {
            TypedReference rctr = new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext").thaw(recipContextEl, visitor);
            if (rctr.target instanceof XmlSecurityRecipientContext) {
                XmlSecurityRecipientContext x = (XmlSecurityRecipientContext)rctr.target;
                ass.setRecipientContext(x);
            }
        }

        Element identityTargetEl = DomUtils.findFirstChildElementByName(source, (String)null, "IdentityTarget");
        if (identityTargetEl != null) {
            TypedReference ittr = new BeanTypeMapping(IdentityTarget.class, "IdentityTarget").thaw(identityTargetEl, visitor);
            if (ittr.target instanceof IdentityTarget) {
                ass.setIdentityTarget((IdentityTarget) ittr.target);
            }
        }

        Element messageTargetEl = DomUtils.findFirstChildElementByName(source, (String)null, "MessageTarget");
        if (messageTargetEl != null) {
            TypedReference mttr = new BeanTypeMapping(MessageTargetableSupport.class, "MessageTarget").thaw(messageTargetEl, visitor);
            if (mttr.target instanceof MessageTargetableSupport) {
                ass.setTargetMessage((MessageTargetableSupport)mttr.target);
            }
        }

        Element messageParts = DomUtils.findFirstChildElementByName(source, (String)null, "MessageParts");
        if (messageParts != null) {
            TypedReference xpathRef = xpathMapper.thaw(messageParts, visitor);
            if (xpathRef == null || !(xpathRef.target instanceof XpathExpression))
                throw new InvalidPolicyStreamException("Could not recover xpath expression from Integrity/MessageParts");
            ass.setXpathExpression((XpathExpression)xpathRef.target);
        } else if ( "false".equals(source.getAttribute("messageParts")) ) {
            ass.setXpathExpression(null);
        }

        Element enabledElem = DomUtils.findFirstChildElementByName(source, (String) null, "Enabled");
        if (enabledElem != null) {
            String boolValue = enabledElem.getAttribute("booleanValue");
            if (boolValue != null) {
                ass.setEnabled(Boolean.parseBoolean(boolValue));
            }
        }

        Element varPrefixElem = DomUtils.findFirstChildElementByName(source, (String)null, "VariablePrefix");
        if ( varPrefixElem != null ) {
            TypedReference varPrefixRef = new BasicTypeMapping(String.class, "stringValue").thaw(varPrefixElem, visitor);
            if ( varPrefixRef != null && varPrefixRef.target instanceof String ) {
                ass.setVariablePrefix( (String) varPrefixRef.target );
            }
        }


        Element signedElementsVariableElem = DomUtils.findFirstChildElementByName(source, (String)null, "ElementsVariable");
        if ( signedElementsVariableElem != null ) {
            TypedReference signedElementsVariableRef = new BasicTypeMapping(String.class, "stringValue").thaw(signedElementsVariableElem, visitor);
            if ( signedElementsVariableRef != null && signedElementsVariableRef.target instanceof String ) {
                ass.setSignedElementsVariable( (String) signedElementsVariableRef.target );
            }
        }

        return tr;
    }
}
