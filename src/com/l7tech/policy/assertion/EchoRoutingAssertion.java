package com.l7tech.policy.assertion;

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
}
