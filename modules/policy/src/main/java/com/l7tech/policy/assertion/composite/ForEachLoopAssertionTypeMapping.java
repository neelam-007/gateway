package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

/**
 * Custom type mapping for ForEachLoopAssertion, since this is the first CompositeAssertion that has properties other than just its child assertions.
 */
public class ForEachLoopAssertionTypeMapping extends CompositeAssertionMapping {
    public ForEachLoopAssertionTypeMapping() {
        super(new ForEachLoopAssertion(), "ForEachLoop");
    }

    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        ForEachLoopAssertion loopAss = (ForEachLoopAssertion)object.target;
        element.setAttribute("loopVariable", loopAss.getLoopVariableName());
        element.setAttribute("variablePrefix", loopAss.getVariablePrefix());

        final int iterationLimit = loopAss.getIterationLimit();
        if (iterationLimit > 0)
            element.setAttribute("iterationLimit", String.valueOf(iterationLimit));

        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        ForEachLoopAssertion loopAss = (ForEachLoopAssertion)cass;
        loopAss.setLoopVariableName(source.getAttribute("loopVariable"));
        loopAss.setVariablePrefix(source.getAttribute("variablePrefix"));

        String iterationLimitStr = source.getAttribute("iterationLimit");
        int iterationLimit = 0;
        try {
            iterationLimit = Integer.parseInt(iterationLimitStr);
        } catch (NumberFormatException e) {
            // ignore and be zero
        }
        loopAss.setIterationLimit(iterationLimit);

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
