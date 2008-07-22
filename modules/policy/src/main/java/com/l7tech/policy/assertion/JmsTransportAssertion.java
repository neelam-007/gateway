/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * Validates that the soap request came in throught the JMS transport.
 * @author alex
 * @version $Revision$
 */
public class JmsTransportAssertion extends TransportAssertion {
    public JmsTransportAssertion() {
        super();
    }
}
