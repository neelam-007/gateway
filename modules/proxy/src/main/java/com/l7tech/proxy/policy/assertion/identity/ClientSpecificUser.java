/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.identity;

import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.proxy.policy.assertion.UnimplementedClientAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientSpecificUser extends UnimplementedClientAssertion {
    public ClientSpecificUser( SpecificUser data ) {
        super(data);
    }
}
