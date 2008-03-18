/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * @author mike
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssReplayProtection extends MessageTargetableAssertion {
    public RequestWssReplayProtection() {
    }
}
