/**
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * A hardware-accelerated version of {@link ServerRequestXpathAssertion}.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ServerPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 */
public class ServerRequestAcceleratedXpathAssertion extends ServerAcceleratedXpathAssertion {
    public ServerRequestAcceleratedXpathAssertion(RequestXpathAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, new ServerRequestXpathAssertion(assertion, applicationContext));
    }
}
