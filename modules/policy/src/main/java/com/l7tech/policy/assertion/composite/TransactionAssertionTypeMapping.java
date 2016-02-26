package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

/**
 * Custom type mapping for TransactionAssertion composite assertion.
 */
public class TransactionAssertionTypeMapping extends CompositeAssertionMapping {
    public TransactionAssertionTypeMapping() {
        super(new TransactionAssertion(), "Transaction");
    }

    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        TransactionAssertion tass = (TransactionAssertion)object.target;
        if(tass.getConnectionName()!=null) {
            element.setAttribute("connectionName", tass.getConnectionName());
            element.setAttribute("hasConnection", Boolean.toString(true));
        }

        // TODO copy any assertion field values into attributes here, once we have any

        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        TransactionAssertion tass = (TransactionAssertion)cass;
        if(source.hasAttribute("connectionName")) {
            tass.setConnectionName(source.getAttribute("connectionName"));
        }

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
