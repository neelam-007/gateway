/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.server.policy;

import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * Runtime policy execution exception thrown by an assertion that wants to interrupt
 * the policy altogether.
 *
 * @author flascelles@layer7-tech.com
 */
public class PolicyInterruptedException extends PolicyAssertionException {
    public PolicyInterruptedException(String msg) {
        super(msg);
    }
}
