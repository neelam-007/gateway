package com.l7tech.external.assertions.analytics;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

/**
 * @author rraquepo, 7/4/14
 */
public class AnalyticsAssertionTypeMapping extends CompositeAssertionMapping {
    public AnalyticsAssertionTypeMapping() {
        super(new AnalyticsAssertion(), "Analytics");
    }

    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        AnalyticsAssertion targetAss = (AnalyticsAssertion) object.target;
        element.setAttribute("connectionName", targetAss.getConnectionName());
        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        AnalyticsAssertion targetAss = (AnalyticsAssertion) cass;
        targetAss.setConnectionName(source.getAttribute("connectionName"));
        super.populateObject(cass, source, visitor);
    }

    @Override
    protected String getNsPrefix() {
        return "L7p";
    }

    @Override
    protected String getNsUri() {
        return WspConstants.L7_POLICY_NS;
    }
}
