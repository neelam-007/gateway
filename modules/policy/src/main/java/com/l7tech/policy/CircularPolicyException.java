/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import java.text.MessageFormat;

/**
 * @author alex
*/
public class CircularPolicyException extends Exception {
    public CircularPolicyException(Policy policy, String includedGuid, String includedName) {
        super(MessageFormat.format("Policy #{0} ({1}) includes a circular reference to Policy #{2} ({3})", policy.getGoid(), policy.getName(), includedGuid, includedName));
    }
}
