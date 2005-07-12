/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import org.w3c.dom.Element;

/**
 * Freeze RequestXpathAssertion.  Actually, this TypeMapping should be able to freeze (and thaw) any XpathBasedAssertion
 * that requires no further configuration beyond its XpathExpression.
 */
class MessagePredicateMapping extends AssertionMapping {
    private final AssertionMapping oldMapper;
    private final XpathExpressionMapping xpathMapper;

    MessagePredicateMapping(Assertion a, String externalName, String oldExternalName) {
        super(a, externalName);
        this.oldMapper = new AssertionMapping(a, oldExternalName);
        this.xpathMapper = new XpathExpressionMapping("MessagePredicate", WspConstants.WSP_POLICY_NS, "wsp");
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (wspWriter.isPre32Compat())
            return oldMapper.freeze(wspWriter, object, container);

        final XpathExpression xpathExpression = ((XpathBasedAssertion)object.target).getXpathExpression();
        final TypedReference xpathTr = new TypedReference(XpathExpression.class, xpathExpression);
        Element messagePredicate = xpathMapper.freeze(wspWriter, xpathTr, container);

        WspUtil.setWspUsageRequired(messagePredicate, "wsp", WspConstants.WSP_POLICY_NS);
        return messagePredicate;
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!"MessagePredicate".equals(source.getLocalName()))
            return super.thaw(source, visitor); // handle old-school format
        WspUtil.enforceWspUsageRequired(source);

        final TypedReference tr = xpathMapper.thaw(source, visitor);
        if (tr == null) throw new InvalidPolicyStreamException("Unable to extract XpathExpression from element " + source.getLocalName()); // can't happen
        final Object target = tr.target;
        if (target == null) throw new InvalidPolicyStreamException("Unable to extract XpathExpression from element " + source.getLocalName()); // can't happen

        try {
            XpathBasedAssertion ass = (XpathBasedAssertion)(this.constructor.newInstance(new Object[0]));
            ass.setXpathExpression((XpathExpression)target); // Save the parsed expression (Bug #1894)
            return new TypedReference(this.getMappedClass(), ass);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to create new " + getMappedClass(), e);
        }
    }
}
