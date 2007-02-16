package com.l7tech.external.assertions.echorouting;

import com.l7tech.policy.assertion.RoutingAssertion;

/**
 * <code>EchoRoutingAssertion</code> is an assertion that copies the request content into the response.
 *
 * @author emil
 * @version 21-Mar-2005
 */
public class EchoRoutingAssertion extends RoutingAssertion {

    public EchoRoutingAssertion() {
        // This is because this was the effective default when the Server
        // assertion was not actually checking this property.
        setCurrentSecurityHeaderHandling(LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
    }


    /*
        super("Echo Routing Assertion", "com/l7tech/console/resources/Edit16.gif");

policy node:        return "Echo request to response";
            public String getName() {
                return "Routing Properties";
            }

            public String getDescription() {
                return "View and edit routing properties";
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/Properties16.gif";
            }

            "com/l7tech/console/resources/xmlsignature.gif"

        super("Message Routing", "routing");
            
     */
}
