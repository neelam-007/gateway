/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.identity;

import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.proxy.policy.assertion.UnimplementedClientAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientMemberOfGroup extends UnimplementedClientAssertion {
    public ClientMemberOfGroup( MemberOfGroup data ) {
        super(data);
    }
}
