/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * @author alex
 * @version $Revision$
 */
@RequiresSOAP(wss=true)
public abstract class WssCredentialSourceAssertion extends Assertion {
    /**
     * Always an credential source
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }
}
