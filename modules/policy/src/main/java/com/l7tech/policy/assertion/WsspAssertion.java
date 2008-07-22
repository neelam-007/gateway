package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Assertion for WS-Security Policy compliance.
 *
 * <p>This is just a marker assertion, no runtime work is performed.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
@RequiresSOAP()
public class WsspAssertion extends Assertion {
}
