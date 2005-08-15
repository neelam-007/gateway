/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.policy.assertion;

/**
 * This assertion interrupts the policy execution and instruct the http transport
 * sub-system to drop the connection with the requestor.
 *
 * @author flascelles@layer7-tech.com
 */
public class DropConnection extends Assertion {
    public DropConnection() {}
}
