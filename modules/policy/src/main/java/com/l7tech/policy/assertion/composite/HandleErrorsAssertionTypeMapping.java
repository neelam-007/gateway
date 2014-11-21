package com.l7tech.policy.assertion.composite;


import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

public class HandleErrorsAssertionTypeMapping extends CompositeAssertionMapping {

    public HandleErrorsAssertionTypeMapping(){
        super(new HandleErrorsAssertion(), "HandleErrors");
    }

    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        HandleErrorsAssertion ass = (HandleErrorsAssertion) object.target;
        element.setAttribute("variablePrefix", ass.getVariablePrefix());
        element.setAttribute("includeIOException", String.valueOf(ass.isIncludeIOException()));
        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        HandleErrorsAssertion ass = (HandleErrorsAssertion) cass;
        ass.setVariablePrefix(source.getAttribute("variablePrefix"));
        ass.setIncludeIOException(Boolean.valueOf(source.getAttribute("includeIOException")));
        super.populateObject(cass, source, visitor);
    }

    @Override
    protected String getNsPrefix() {
        return "L7p:";
    }

    @Override
    protected String getNsUri() {
        return WspConstants.L7_POLICY_NS;
    }
}
