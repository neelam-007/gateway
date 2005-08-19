/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.policy.assertion;

/**
 * This assertion flags the policy context so that if the policy results in an error (policy is not succesfull) then
 * the transport does not return an error but drops the client connection instead.
 *
 * @author flascelles@layer7-tech.com
 */
public class StealthFault extends Assertion {
    public StealthFault() {}
}
