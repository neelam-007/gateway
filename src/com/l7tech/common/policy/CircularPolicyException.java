/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import java.text.MessageFormat;

/**
 * @author alex
*/
public class CircularPolicyException extends RuntimeException {
    public CircularPolicyException(Policy policy, long includedOid, String includedName) {
        super(MessageFormat.format("Policy #{0} ({1}) includes a circular reference to Policy #{2} ({3})", policy.getOid(), policy.getName(), includedOid, includedName));
    }
}
