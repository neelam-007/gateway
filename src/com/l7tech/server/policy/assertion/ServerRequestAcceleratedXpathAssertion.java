/**
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RequestAcceleratedXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * A hardware-accelerated version of {@link ServerRequestXpathAssertion}.
 */
public class ServerRequestAcceleratedXpathAssertion extends ServerAcceleratedXpathAssertion {
    public ServerRequestAcceleratedXpathAssertion(RequestAcceleratedXpathAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, new ServerRequestXpathAssertion(assertion, applicationContext));
    }
}
