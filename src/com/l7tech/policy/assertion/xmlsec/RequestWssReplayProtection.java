/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * @author mike
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssReplayProtection extends Assertion {
    public RequestWssReplayProtection() {
    }
}
