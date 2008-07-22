package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.xml.SoapFaultLevel;
import org.w3c.dom.Element;

/**
 * Encapsulated code to handle translation of deprecated StealthFault assertion into special case of FaultLevel assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 12, 2006<br/>
 * $Id$
 */
public class StealthReplacedByFaultLevel {
    static final TypeMapping faultDropCompatibilityMapping =
        new CompatibilityAssertionMapping(new FaultLevel(), "StealthFault") {
            protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                new BeanTypeMapping(FaultLevel.class, "").populateObject(new TypedReference(FaultLevel.class, ass), source, visitor);
                // todo, verify this works as expected
                FaultLevel fl = (FaultLevel)ass;
                fl.getLevelInfo().setLevel(SoapFaultLevel.DROP_CONNECTION);
            }
        };
}
